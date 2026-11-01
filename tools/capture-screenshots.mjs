#!/usr/bin/env node
/**
 * SAYI AVCISI ekran görüntüsü yakalayıcı.
 *
 * Android emülatöründe kurulu uygulamayı adb üzerinden sürer (uiautomator dump + dokunma + screencap)
 * ve gerçek oyun içi kareleri docs/screenshots altına yazar.
 *
 * Kullanım:
 *   SDK=<Android SDK> node tools/capture-screenshots.mjs
 */
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SDK = process.env.SDK || 'C:/Users/ixend/AppData/Local/Android/Sdk';
const ADB = join(SDK, 'platform-tools', 'adb.exe');
const PKG = 'com.sayiavcisi.app';
const OUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs', 'screenshots');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function adb(...args) {
  return execFileSync(ADB, args, { encoding: 'buffer', maxBuffer: 64 * 1024 * 1024 });
}
function adbText(...args) {
  return adb(...args).toString('utf8');
}

/** uiautomator XML'i Türkçe karakterleri ve emojileri sayısal entity olarak yazar; metni geri çöz. */
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

/** Emülatörde geçiş ekranlarında dump boş dönebiliyor; ağaç dolana kadar tekrarlanır. */
async function dumpNodes(attempts = 8) {
  let nodes = [];
  for (let i = 0; i < attempts; i += 1) {
    try {
      adbText('shell', 'uiautomator', 'dump', '/sdcard/ui.xml');
      nodes = parseNodes(adbText('shell', 'cat', '/sdcard/ui.xml'));
      if (nodes.length > 0) return nodes;
    } catch {
      /* ekran geçişinde dump başarısız olur, tekrar denenecek */
    }
    await sleep(600);
  }
  return nodes;
}

async function findNode(matcher, attempts = 30) {
  for (let i = 0; i < attempts; i += 1) {
    const hit = (await dumpNodes()).find(matcher);
    if (hit) return hit;
    await sleep(700);
  }
  return null;
}

const byText = (needle) => (n) => n.text.includes(needle);
const byDesc = (needle) => (n) => n.desc.includes(needle);

async function waitForText(needle, label = needle) {
  const node = await findNode(byText(needle));
  if (!node) throw new Error(`Ekran beklenirken bulunamadı: ${label}`);
  return node;
}

async function tapNode(matcher, label) {
  const node = await findNode(matcher);
  if (!node) throw new Error(`Düğüm bulunamadı: ${label}`);
  adbText('shell', 'input', 'tap', String(Math.round(node.cx)), String(Math.round(node.cy)));
  await sleep(900);
  return node;
}

async function shoot(name) {
  const png = adb('exec-out', 'screencap', '-p');
  writeFileSync(join(OUT_DIR, name), png);
  console.log('kaydedildi:', name, png.length, 'bayt');
}

async function scrollDown(times = 2) {
  for (let i = 0; i < times; i += 1) {
    adbText('shell', 'input', 'swipe', '540', '1600', '540', '700', '400');
    await sleep(400);
  }
}

/**
 * Tahtadaki aktif blokları, hedefi ve işlem türünü uiautomator ağacından okur.
 * Kural: aynı satır/sütun/çapraz üzerinde, aralarında aktif blok olmayan 2 veya 3 blok.
 */
async function readBoard() {
  const nodes = await dumpNodes();
  const cells = [];
  for (const n of nodes) {
    const m = n.desc.match(/^Sayı (\d+), satır (\d+), sütun (\d+)/);
    if (m) cells.push({ value: +m[1], row: +m[2], col: +m[3], x: n.cx, y: n.cy });
  }
  const promptNode = nodes.find((n) => /HEDEF:|×|÷/.test(n.text) && n.text.length < 24);
  const helperNode = nodes.find((n) => /(Toplama|Çıkarma|Çarpma|Bölme|Eksik)/.test(n.text) && n.text.length < 90);
  const counter = nodes.find((n) => n.text.includes('Seçilen:'));
  const operandCount = +(counter?.text.match(/\/(\d)/)?.[1] || 2);

  const prompt = promptNode?.text || '';
  const helper = helperNode?.text || '';
  const target = prompt.includes('=')
    ? +(prompt.split('=').pop() || '').replace(/\D/g, '')
    : +(prompt.match(/HEDEF:\s*(\d+)/)?.[1] ?? NaN);

  let op = 'ADD';
  if (/Çıkarma/.test(helper)) op = 'SUB';
  else if (/Çarpma|Eksik Çarpan/.test(helper)) op = 'MUL';
  else if (/Bölme|Eksik Bölen|Eksik Bölünen/.test(helper)) op = 'DIV';

  const gridSize = cells.length ? Math.max(...cells.map((c) => Math.max(c.row, c.col))) + 1 : 0;
  return { cells, target, operandCount, op, gridSize, prompt };
}

function lineOfSight(a, b, blockers) {
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
  if (op === 'SUB') {
    let acc = values[0];
    for (let i = 1; i < values.length; i += 1) {
      if (acc < values[i]) return false; // negatif sonuç yok
      acc -= values[i];
    }
    return acc === target;
  }
  if (op === 'DIV') {
    if (values.length !== 2 || values[1] === 0) return false;
    return values[0] % values[1] === 0 && values[0] / values[1] === target;
  }
  return false;
}

function allPermutations(items) {
  if (items.length <= 1) return [items];
  return items.flatMap((first, i) =>
    allPermutations([...items.slice(0, i), ...items.slice(i + 1)]).map((rest) => [first, ...rest]),
  );
}

function findMove(board) {
  const { cells, target, operandCount, op } = board;
  const combos = [];
  const pick = (start, size, chosen) => {
    if (size === 0) {
      combos.push([...chosen]);
      return;
    }
    for (let i = start; i <= cells.length - size; i += 1) {
      chosen.push(cells[i]);
      pick(i + 1, size - 1, chosen);
      chosen.pop();
    }
  };
  pick(0, operandCount, []);

  for (const combo of combos) {
    const others = cells.filter((c) => !combo.includes(c));
    for (const order of allPermutations(combo)) {
      if (!mathOk(order.map((c) => c.value), target, op)) continue;
      let geometric = true;
      for (let i = 1; i < order.length; i += 1) {
        const blockers = others.concat(order.filter((_, j) => j < i - 1));
        if (!lineOfSight(order[i - 1], order[i], blockers)) {
          geometric = false;
          break;
        }
      }
      if (geometric) return order;
    }
  }
  return null;
}

async function hasText(needle) {
  return (await dumpNodes()).some(byText(needle));
}

async function playBoard(maxMatches = 40) {
  for (let step = 0; step < maxMatches; step += 1) {
    if (await hasText('BÖLÜM TAMAMLANDI')) return true;
    const board = await readBoard();
    if (!board.cells.length || Number.isNaN(board.target)) break;
    const move = findMove(board);
    if (!move) {
      console.log('hamle bulunamadı, yeniden düzenle jokeri denenecek');
      await tapNode(byText('DÜZENLE'), 'joker').catch(() => {});
      continue;
    }
    for (const cell of move) {
      adbText('shell', 'input', 'tap', String(Math.round(cell.x)), String(Math.round(cell.y)));
      await sleep(280);
    }
    await sleep(600);
  }
  return hasText('BÖLÜM TAMAMLANDI');
}

async function openHub(label) {
  for (let i = 0; i < 5; i += 1) {
    adbText('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP');
    adbText('shell', 'wm', 'dismiss-keyguard');
    await sleep(400);
    if (await hasText('SAYI AVCISI')) return;
    adbText('shell', 'input', 'keyevent', 'KEYCODE_BACK');
    await sleep(1200);
    if (await hasText('SAYI AVCISI')) return;
    adbText('shell', 'am', 'start', '-n', `${PKG}/.MainActivity`);
    await sleep(3500);
  }
  throw new Error(`Ana ekran açılamadı: ${label}`);
}

/** Bir bölüm kartının başlığı görünür olduğunda yanındaki OYNA düğmesine basar. */
async function tapPlayNear(title, label) {
  for (let i = 0; i < 12; i += 1) {
    const nodes = await dumpNodes();
    const anchor = nodes.find((n) => n.text.includes(title));
    if (anchor) {
      const plays = nodes.filter((n) => n.text === 'OYNA');
      if (plays.length) {
        const best = plays.reduce((a, b) => (Math.abs(a.cy - anchor.cy) < Math.abs(b.cy - anchor.cy) ? a : b));
        adbText('shell', 'input', 'tap', String(Math.round(best.cx)), String(Math.round(best.cy)));
        await sleep(1200);
        return;
      }
    }
    scrollDown(1).catch(() => {});
    await sleep(500);
  }
  throw new Error(`Bölüm kartı bulunamadı: ${label}`);
}

async function captureExpertBoard() {
  await openHub('5. gun');
  await tapNode(byText('5. GÜN'), 'gun5');
  await tapPlayNear('10×10 Uzman', 'efsane tapinak');
  await waitForText('Seçilen:', 'uzman tahtası');
  await sleep(1500);
  await shoot('06-oyun-uzman-tahtasi.png');
}

async function captureParentPanel() {
  await openHub('macera haritasi');
  await tapNode(byText('Ebeveyn'), 'ebeveyn');
  await waitForText('Başarı Oranları', 'ebeveyn paneli');
  await sleep(800);
  await shoot('10-ebeveyn-paneli.png');
  await scrollDown(3);
  await shoot('11-ebeveyn-paneli-analiz.png');
  console.log('Bitti.');
}

async function main() {
  mkdirSync(OUT_DIR, { recursive: true });
  if (process.env.SKIP_TO === 'ebeveyn') return captureParentPanel();
  if (process.env.SKIP_TO === 'uzman') return captureExpertBoard();
  adbText('install', '-r', '-g', process.argv[2] || 'app/build/outputs/apk/debug/app-debug.apk');
  adbText('shell', 'am', 'force-stop', PKG);
  adbText('shell', 'pm', 'clear', PKG);
  await sleep(1500);
  adbText('shell', 'settings', 'put', 'global', 'window_animation_scale', '0');
  adbText('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0');
  adbText('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0');
  await openHub('macera haritasi');
  await sleep(1500);

  await shoot('01-macera-haritasi.png');

  // 2. Gün sekmesi: Hazine Mağarası teması
  await tapNode(byText('2. GÜN'), 'gun2');
  await waitForText('Hazine Mağarası', '2. gun teması');
  await shoot('02-macera-haritasi-2-gun.png');

  // Bölüm başlat (ilk OYNA düğmesi)
  await tapNode(byText('OYNA'), 'oyna-1');
  await waitForText('Seçilen:', 'oyun tahtası');
  await sleep(1200);
  await shoot('03-oyun-tahtasi-toplama.png');

  // Doğru eşleştirme yapıp patlama/yıldız animasyonunu ve görsel açıklama kartını yakala
  await playBoard(4);
  await shoot('04-oyun-eslesme-animasyonu.png');

  // Haritaya dön, 3. gün çarpma tahtası
  await tapNode(byText('Harita'), 'harita');
  await openHub('haritaya dönüş');
  await tapNode(byText('3. GÜN'), 'gun3');
  await tapNode(byText('OYNA'), 'oyna-3');
  await waitForText('Seçilen:', 'çarpma tahtası');
  await sleep(1200);
  await shoot('05-oyun-tahtasi-carpma.png');

  // 5. gün uzman (10×10) tahtası
  await tapNode(byText('Harita'), 'harita-2');
  await captureExpertBoard();

  // 1. gün 4×4 bölümünü bitir -> kutlama ve ödül ekranı
  await tapNode(byText('Harita'), 'harita-3');
  await openHub('haritaya dönüş 3');
  await tapNode(byText('1. GÜN'), 'gun1');
  await tapNode(byText('OYNA'), 'oyna-yeniden');
  await waitForText('Seçilen:', '4x4 tahtası');
  const completed = await playBoard(60);
  console.log('bölüm tamamlandı mı:', completed);
  await sleep(700);
  await shoot('07-bolum-tamamlandi.png');

  // Özel Toplama Laboratuvarı
  await tapNode(byText('Haritaya Dön'), 'haritaya-don').catch(async () => {
    await tapNode(byText('Harita'), 'harita-4').catch(() => adbText('shell', 'input', 'keyevent', '4'));
  });
  await openHub('haritaya dönüş 4');
  await tapNode(byText('Hedef Toplam Seç'), 'lab');
  await waitForText('ADIM ADIM TOPLAMA', 'laboratuvar');
  await shoot('08-ozel-toplama-laboratuvari.png');
  await scrollDown(1);
  await shoot('09-ozel-toplama-laboratuvari-secenekler.png');
  await tapNode(byText('← Geri'), 'geri').catch(() => adbText('shell', 'input', 'keyevent', 'KEYCODE_BACK'));
  await openHub('lab dönüşü');

  await captureParentPanel();
}

main().catch((e) => {
  console.error('HATA:', e.message);
  process.exitCode = 1;
});
