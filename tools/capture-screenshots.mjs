#!/usr/bin/env node
/**
 * SAYI AVCISI v2 ekran görüntüsü yakalayıcı.
 *
 * Headless Android emülatöründe kurulu uygulamayı adb üzerinden sürer
 * (uiautomator dump + dokunma + screencap) ve gerçek oyun içi kareleri docs/screenshots altına yazar.
 *
 * Kullanım:
 *   node tools/capture-screenshots.mjs [apk-yolu]
 *   SKIP_TO=ebeveyn node tools/capture-screenshots.mjs     # yalnızca ebeveyn paneli
 *   SKIP_TO=serbest node tools/capture-screenshots.mjs     # 10-12: serbest mod + ebeveyn paneli
 */
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const SDK = process.env.SDK || 'C:/Users/ixend/AppData/Local/Android/Sdk';
const ADB = join(SDK, 'platform-tools', 'adb.exe');
const PKG = 'com.sayiavcisi.app';
const OUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs', 'screenshots');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const adb = (...args) => execFileSync(ADB, args, { encoding: 'buffer', maxBuffer: 64 * 1024 * 1024 });
const adbText = (...args) => adb(...args).toString('utf8');

/** uiautomator Türkçe karakterleri ve emojileri sayısal entity olarak yazar; metni geri çöz. */
function decodeXml(value) {
  return value
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(+d))
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&');
}

function parseNodes(xml) {
  const nodes = [];
  for (const m of xml.matchAll(/<node\s([^>]*?)\/>/g)) {
    const attrs = {};
    for (const a of m[1].matchAll(/([\w-]+)="([^"]*)"/g)) attrs[a[1]] = a[2];
    const b = (attrs.bounds || '').match(/(\d+),(\d+)\D+(\d+),(\d+)/);
    if (!b) continue;
    nodes.push({
      text: decodeXml(attrs.text || ''),
      desc: decodeXml(attrs['content-desc'] || ''),
      cx: (+b[1] + +b[3]) / 2,
      cy: (+b[2] + +b[4]) / 2,
    });
  }
  return nodes;
}

/** Ekran geçişlerinde dump boş dönebilir; ağaç dolana kadar tekrarlanır. */
async function dumpNodes(attempts = 8) {
  let nodes = [];
  for (let i = 0; i < attempts; i += 1) {
    try {
      adbText('shell', 'uiautomator', 'dump', '/sdcard/ui.xml');
      nodes = parseNodes(adbText('shell', 'cat', '/sdcard/ui.xml'));
      if (nodes.length > 0) return nodes;
    } catch {
      /* geçiş ekranında dump başarısız, tekrar denenecek */
    }
    await sleep(500);
  }
  return nodes;
}

async function findNode(matcher, attempts = 24) {
  for (let i = 0; i < attempts; i += 1) {
    const hit = (await dumpNodes()).find(matcher);
    if (hit) return hit;
    await sleep(600);
  }
  return null;
}

const byText = (needle) => (n) => n.text.includes(needle);
const byDesc = (needle) => (n) => n.desc.includes(needle);

async function hasText(needle) {
  return (await dumpNodes()).some(byText(needle));
}

async function waitForText(needle, label = needle) {
  const node = await findNode(byText(needle));
  if (!node) throw new Error(`Ekran beklenirken bulunamadı: ${label}`);
  return node;
}

async function tapNode(matcher, label) {
  const node = await findNode(matcher);
  if (!node) throw new Error(`Düğüm bulunamadı: ${label}`);
  adbText('shell', 'input', 'tap', String(Math.round(node.cx)), String(Math.round(node.cy)));
  await sleep(700);
  return node;
}

async function tapAt(x, y) {
  adbText('shell', 'input', 'tap', String(Math.round(x)), String(Math.round(y)));
  await sleep(320);
}

async function shoot(name) {
  const png = adb('exec-out', 'screencap', '-p');
  writeFileSync(join(OUT_DIR, name), png);
  console.log('kaydedildi:', name, png.length, 'bayt');
}

async function scrollDown(times = 2) {
  for (let i = 0; i < times; i += 1) {
    adbText('shell', 'input', 'swipe', '540', '1600', '540', '700', '400');
    await sleep(420);
  }
}

/** Bir bölüm çipine (content-desc = bolum-D1_S1-...) görünür olduğunda basar. */
async function openLevel(levelId, label) {
  const matcher = byDesc(`bolum-${levelId}`);
  for (let i = 0; i < 14; i += 1) {
    const nodes = await dumpNodes();
    const hit = nodes.find(matcher);
    if (hit) {
      await tapAt(hit.cx, hit.cy);
      await sleep(900);
      return;
    }
    scrollDown(1).catch(() => {});
    await sleep(450);
  }
  throw new Error(`Bölüm çipi bulunamadı: ${label || levelId}`);
}

/** Bölüm açılınca kendini gösteren "NASIL OYNANIR?" kartını kapatır. */
async function dismissHelp() {
  await tapNode(byText('ANADIM'), 'nasil-oynanir kapat').catch(() => {});
}

/**
 * Tahtayı uiautomator ağacından okur: içerik açıklamaları "Sayı v, satır r, sütun c" biçimindedir.
 * Hedef ve işlem, hedef kartındaki metinden alınır.
 */
async function readBoard() {
  const nodes = await dumpNodes();
  const cells = [];
  for (const n of nodes) {
    const m = n.desc.match(/^Sayı (\d+), satır (\d+), sütun (\d+)/);
    if (m) cells.push({ value: +m[1], row: +m[2], col: +m[3], x: n.cx, y: n.cy });
  }
  const objective = nodes.find((n) => /HEDEF \d+/.test(n.text));
  const target = objective ? +(objective.text.match(/HEDEF (\d+)/)[1]) : NaN;
  const detail = nodes.find((n) => /Toplama|Çıkarma|Çarpma|Bölme/.test(n.text) && n.text.length < 60)?.text || '';
  const body = nodes.map((n) => n.text).join(' ');
  let op = 'ADD';
  if (/Çıkarma/.test(detail)) op = 'SUB';
  else if (/Çarpma/.test(detail)) op = 'MUL';
  else if (/Bölme/.test(detail)) op = 'DIV';

  // Üçlü grup da alınabiliyorsa bölüm 2/3 karışıktır: onay düğmesi gerekir.
  const confirm = nodes.find((n) => n.text.includes('EŞLEŞTİR'));
  const mode = /SERBEST/.test(body) ? 'FREE' : 'LOS';
  const sizes = confirm ? [2, 3] : [2];
  return { cells, target, op, mode, sizes, needConfirm: !!confirm };
}

function collinearAndClear(a, b, blockers) {
  const dr = Math.sign(b.row - a.row);
  const dc = Math.sign(b.col - a.col);
  if (!(a.row === b.row || a.col === b.col || Math.abs(b.row - a.row) === Math.abs(b.col - a.col))) return false;
  let r = a.row + dr;
  let c = a.col + dc;
  while (r !== b.row || c !== b.col) {
    if (blockers.some((x) => x.row === r && x.col === c)) return false;
    r += dr;
    c += dc;
  }
  return true;
}

function mathOk(values, target, op) {
  if (op === 'ADD') return values.reduce((a, b) => a + b, 0) === target;
  if (op === 'MUL') return values.reduce((a, b) => a * b, 1) === target;
  if (op === 'SUB') return Math.abs(values[0] - values[1]) === target;
  if (op === 'DIV') {
    const [x, y] = values;
    if (y !== 0 && x % y === 0 && x / y === target) return true;
    return x !== 0 && y % x === 0 && y / x === target;
  }
  return false;
}

function findMove(board) {
  const { cells, target, op, mode, sizes } = board;
  for (const size of sizes) {
    const combos = [];
    const pick = (start, left, chosen) => {
      if (left === 0) return void combos.push([...chosen]);
      for (let i = start; i <= cells.length - left; i += 1) {
        chosen.push(cells[i]);
        pick(i + 1, left - 1, chosen);
        chosen.pop();
      }
    };
    pick(0, size, []);
    for (const combo of combos) {
      if (!mathOk(combo.map((c) => c.value), target, op)) continue;
      if (size === 2) {
        if (mode === 'FREE' || collinearAndClear(combo[0], combo[1], cells.filter((c) => c !== combo[0] && c !== combo[1]))) {
          return combo;
        }
        continue;
      }
      // Üçlü grup: ortadaki taşı bul, her iki komşu çiftin hattı açık olmalı.
      for (const mid of combo) {
        const ends = combo.filter((c) => c !== mid);
        const clear = (a, b) => collinearAndClear(a, b, cells.filter((c) => c !== a && c !== b));
        if (clear(ends[0], mid) && clear(mid, ends[1])) return [ends[0], mid, ends[1]];
      }
    }
  }
  return null;
}

/** Geometriyi bozmayan ama hedefi tutmayan bir çift: ebeveyn analizi için gerçek hata kaydı. */
function findWrongMove(board) {
  const { cells, target, op, mode } = board;
  for (let i = 0; i < cells.length; i += 1) {
    for (let j = i + 1; j < cells.length; j += 1) {
      const pair = [cells[i], cells[j]];
      if (mathOk(pair.map((c) => c.value), target, op)) continue;
      if (mode === 'FREE' || collinearAndClear(pair[0], pair[1], cells.filter((c) => c !== pair[0] && c !== pair[1]))) {
        return pair;
      }
    }
  }
  return null;
}

/** Bölümü planlayarak bitirir; her doğru eşleşmede onay gerekiyorsa EŞLEŞTİR'e basar. */
async function playLevel(maxMatches = 80, stopAt = 0, mistakes = 0) {
  let matches = 0;
  for (let step = 0; step < maxMatches; step += 1) {
    if (await hasText('BÖLÜM BİTTİ')) return { done: true, matches };
    if (stopAt && matches >= stopAt) return { done: false, matches };
    const board = await readBoard();
    if (!board.cells.length || Number.isNaN(board.target)) break;
    if (mistakes > 0 && matches >= 2) {
      const wrong = findWrongMove(board);
      if (wrong) {
        for (const cell of wrong) await tapAt(cell.x, cell.y);
        mistakes -= 1;
        await sleep(600);
        continue;
      }
    }
    const move = findMove(board);
    if (!move) {
      console.log('  hamle yok -> ALTIN KURTARMA için yeniden düzenle jokeri');
      await tapNode(byText('YENİDEN'), 'joker-duzenle').catch(() => {});
      continue;
    }
    for (const cell of move) await tapAt(cell.x, cell.y);
    if (board.needConfirm) await tapNode(byText('EŞLEŞTİR'), 'eslestir').catch(() => {});
    matches += 1;
    await sleep(500);
  }
  return { done: await hasText('BÖLÜM BİTTİ'), matches };
}

async function wakeAndStart() {
  adbText('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP');
  adbText('shell', 'wm', 'dismiss-keyguard');
  await sleep(500);
}

/** Uygulamayı açar ve giriş ekranına dönene kadar geri tuşunu dener. */
async function goHome() {
  const onHome = async () => (await dumpNodes()).some((n) => n.text.includes('MACERAYA BAŞLA'));
  for (let i = 0; i < 6; i += 1) {
    await wakeAndStart();
    if (await onHome()) return;
    adbText('shell', 'input', 'keyevent', 'KEYCODE_BACK');
    await sleep(1000);
    if (await onHome()) return;
    adbText('shell', 'am', 'start', '-n', `${PKG}/.MainActivity`);
    await sleep(2500);
  }
  throw new Error('Giriş ekranı açılamadı');
}

/**
 * İlerleme dosyasına bölümler ekler: ileri günlerin tahtalarını (çıkarma/çarpma/bölme/10x10)
 * yakalayabilmek için kilidi açar. Uygulama tamamen gerçek kayıtları yazmış olur, biz sadece
 * completed_levels setine tamamlanmış kimlikler ekleriz.
 */
async function injectCompleted(levels) {
  const file = 'shared_prefs/sayi_avcisi_offline_prefs.xml';
  adbText('shell', 'am', 'force-stop', PKG);
  await sleep(400);
  let xml = adbText('shell', `run-as ${PKG} cat ${file}`);
  if (!xml.includes('<map')) throw new Error('prefs dosyası okunamadı');
  const missing = levels.filter((id) => !xml.includes(`<string>${id}</string>`));
  if (!missing.length) return;
  // Android StringSet'i <set name=".."><string>DEGER</string></set> olarak yazar;
  // yanlış etiket (string-set ya da <item>) dosyayı okunamaz yapar ve uygulama tüm ilerlemeyi siler.
  const items = missing.map((id) => `        <string>${id}</string>`).join('\n');
  const open = '<set name="completed_levels">';
  xml = xml.includes(open)
    ? xml.replace(open, `${open}\n${items}`)
    : xml.replace('</map>', `    ${open}\n${items}\n    </set>\n</map>`);
  writeFileSync(join(tmpdir(), 'sayi-avcisi-prefs.xml'), xml, 'utf8');
  adbText('push', join(tmpdir(), 'sayi-avcisi-prefs.xml'), '/data/local/tmp/prefs.xml');
  adbText('shell', `run-as ${PKG} sh -c "cp /data/local/tmp/prefs.xml ${file} && chmod 660 ${file}"`);
  if (!adbText('shell', `run-as ${PKG} cat ${file}`).includes(`<string>${missing[0]}</string>`)) {
    throw new Error('ilerleme dosyasına yazılamadı');
  }
  // Uygulamanın dosyayı gerçekten çözdüğünü doğrula: çözemese tüm ilerlemeyi sıfırlar.
  adbText('shell', 'am', 'start', '-n', `${PKG}/.MainActivity`);
  await sleep(4000);
  const written = adbText('shell', `run-as ${PKG} cat ${file}`);
  if (!written.includes('<set name="completed_levels">')) {
    throw new Error('uygulama enjekte edilen ilerlemeyi okuyamadı');
  }
  adbText('shell', 'am', 'force-stop', PKG);
  await sleep(600);
}

async function captureGame(levelId, label, shots, matchesBeforeShot = 1) {
  await openLevel(levelId, label);
  await waitForText('HEDEF', label);
  await dismissHelp();
  await sleep(900);
  await shoot(shots[0]);
  if (shots[1]) {
    await playLevel(80, matchesBeforeShot);
    await sleep(650);
    await shoot(shots[1]);
  }
}

async function captureParentPanel() {
  await goHome();
  await tapNode(byText('EBEVEYN PANELİ'), 'ebeveyn');
  await waitForText('Ebeveyn Paneli', 'ebeveyn paneli');
  await sleep(800);
  await shoot('11-ebeveyn-paneli.png');
  await scrollDown(3);
  await shoot('12-ebeveyn-paneli-analiz.png');
}

async function openMap() {
  await goHome();
  await tapNode(byText('MACERAYA BAŞLA'), 'macera');
  await waitForText('Macera Haritası', 'harita');
}

/** Haritada istenen bölüm çipi görünene kadar kaydırır ve açar. */
async function openLevelFromMap(levelId, label) {
  for (let round = 0; round < 3; round += 1) {
    for (let i = 0; i < 8; i += 1) {
      const nodes = await dumpNodes();
      const hit = nodes.find(byDesc(`bolum-${levelId}`));
      if (hit) {
        if (hit.desc.endsWith('locked')) throw new Error(`Bölüm hâlâ kilitli: ${label || levelId}`);
        await tapAt(hit.cx, hit.cy);
        await sleep(900);
        // Çip kısmen görünüyorsa dokunuş hedefi ıskalayabilir: hedef kartı gelmezse haritaya dön, yinela
        if (await findNode(byText('HEDEF'), 6)) {
          await dismissHelp();
          await sleep(900);
          return;
        }
        if (!(await hasText('Macera Haritası'))) {
          adbText('shell', 'input', 'keyevent', 'KEYCODE_BACK');
          await sleep(1200);
        }
        break;
      }
      // uiautomator ara sıra kök düğüm vermez: ağaç boşsa haritayı yeniden kur
      if (!nodes.length) {
        await openMap();
        break;
      }
      await scrollDown(1);
      await sleep(450);
    }
  }
  throw new Error(`Bölüm haritada bulunamadı: ${label || levelId}`);
}

/** 01-05: giriş, harita (gün 1 açık), taze tahta, eşleşme sonrası ve kutlama. */
async function captureIntroShots() {
  await shoot('01-giris-ekrani.png');

  await openMap();
  await sleep(1200);
  await shoot('02-macera-haritasi.png');

  await openLevelFromMap('D1_S1', 'toplama 4x4');
  await shoot('03-oyun-tahtasi-toplama.png');
  await playLevel(80, 3);
  await sleep(700);
  await shoot('04-eslesme-sonrasi.png');

  const res = await playLevel(90);
  console.log('  bölüm tamamlandı mı:', res.done, 'eşleşme:', res.matches);
  await sleep(900);
  await shoot('05-bolum-bitti-kutlama.png');
}

async function main() {
  mkdirSync(OUT_DIR, { recursive: true });
  if (process.env.SKIP_TO === 'ebeveyn') return captureParentPanel();
  if (process.env.SKIP_TO === 'serbest') {
    for (const scale of ['window_animation_scale', 'transition_animation_scale', 'animator_duration_scale']) {
      adbText('shell', 'settings', 'put', 'global', scale, '0');
    }
    await openMap();
    await openLevelFromMap('D1_S6', 'serbest mod');
    await shoot('10-serbest-mod-tahtasi.png');
    return captureParentPanel();
  }

  const advancedOnly = process.env.SKIP_TO === 'ileri';
  if (!advancedOnly) {
    adbText('install', '-r', '-g', process.argv[2] || 'app/build/outputs/apk/debug/app-debug.apk');
    adbText('shell', 'am', 'force-stop', PKG);
    adbText('shell', 'pm', 'clear', PKG);
    await sleep(1200);
    for (const scale of ['window_animation_scale', 'transition_animation_scale', 'animator_duration_scale']) {
      adbText('shell', 'settings', 'put', 'global', scale, '0');
    }
    await goHome();
    await sleep(1400);
  } else {
    // 01-05 zaten yakalandı; emülatördeki gerçek ilerlemeyi koruyarak ileri gün tahtalarına geç
    adbText('shell', 'settings', 'put', 'global', 'window_animation_scale', '0');
    adbText('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0');
    adbText('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0');
  }

  if (!advancedOnly) await captureIntroShots();

  // İleri gün tahtalarını yakalamak için kilidi aç (tamamlanan gerçek kayıtların üstüne)
  await injectCompleted([
    'D1_S2', 'D1_S3', 'D1_S4', 'D1_S5',
    'D2_S1', 'D2_S2', 'D2_S3',
    'D3_S1', 'D3_S2', 'D3_S3',
    'D4_S1', 'D4_S2',
    'D5_S1', 'D5_S2', 'D5_S3', 'D5_S4',
  ]);
  await sleep(1500);

  // 06 çıkarma (2. gün mağara), 07 çarpma (3. gün kale), 08 bölme (4. gün uzay)
  // Her tahtada birkaç gerçek eşleşme oynanır: ebeveyn paneli farklı işlemlerden veri görür.
  await openMap();
  await scrollDown(1);
  await openLevelFromMap('D2_S4', 'çıkarma');
  await shoot('06-oyun-tahtasi-cikarma.png');
  await playLevel(10, 0, 2);

  await openMap();
  await scrollDown(2);
  await openLevelFromMap('D3_S4', 'çarpma');
  await shoot('07-oyun-tahtasi-carpma.png');
  await playLevel(10, 0, 2);

  await openMap();
  await scrollDown(3);
  await openLevelFromMap('D4_S3', 'bölme');
  await shoot('08-oyun-tahtasi-bolme.png');
  await playLevel(10, 0, 2);

  // 09 10x10 büyük final, 10 serbest mod tahtası
  await openMap();
  await scrollDown(4);
  await openLevelFromMap('D5_S5', 'buyuk final');
  await shoot('09-buyuk-final-10x10.png');

  await openMap();
  await openLevelFromMap('D1_S6', 'serbest mod');
  await shoot('10-serbest-mod-tahtasi.png');

  await captureParentPanel();
  console.log('Bitti.');
}

main().catch((e) => {
  console.error('HATA:', e.message);
  process.exitCode = 1;
});
