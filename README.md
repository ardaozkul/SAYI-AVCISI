# 🦊 SAYI AVCISI — Çocuklar İçin Sayı Avlama Oyunu (Android APK)

**SAYI AVCISI**, toplama, çıkarma, çarpma ve bölmeyi oyun oynayarak pekiştiren,
**Kotlin & Jetpack Compose** ile yazılmış, **%100 çevrimdışı** çalışan bir Android bulmaca oyunudur.

Bir sayıya basılı tut, parmağını komşu sayılara sürükle ve bırak. Hamle kendiliğinden kontrol edilir:
doğruysa taşlar yeşil parlar, yanlışsa kırmızı olup sarsılır ve puan azalır. Düğmeye basmak yok.

> **5.0.0 oyunu büyüttü.** Tahtalar **8 sütun** genişliğe çıktı: kolay hedefler **8×12**, genel
> hedefler **8×8**, bölüm finalleri **8×60**, Sonsuz Av **8×120** (geniş tablette **20×120**).
> Kombo zinciri artık **sınırsız büyür**: her 3 doğru hamlede puan çarpanı ikiye katlanır
> (×2, ×4, ×8, ×16 …). Dört işlem dengeli bir torbadan sırayla gelir. Seçilen taşlarda **1, 2, 3**
> sıra rozetleri, animasyonlu ortam ışıkları ve zengin kombo kutlamaları vardır.
> **Sonsuz Av** komşuluk aramaz; görevler `a × b × c = 21`, `a ÷ b = 4` gibi açık formüllerle yazılır.

---

## 📲 KURULUM DOSYASI (APK)

Derlenmiş ve doğrudan Android telefon/tabletlere kurulmaya hazır **APK v5.0.0**:

- 🚀 **Doğrudan indirme (ana paket):**
  👉 **[https://github.com/ardaozkul/SAYI-AVCISI/raw/main/SAYI-AVCISI.apk](https://github.com/ardaozkul/SAYI-AVCISI/raw/main/SAYI-AVCISI.apk)**
- 📦 GitHub üzerinde APK dosyası:
  👉 **[SAYI-AVCISI.apk](https://github.com/ardaozkul/SAYI-AVCISI/blob/main/SAYI-AVCISI.apk)**
- 📂 Alternatif kopyalar:
  - [`apk/SAYI-AVCISI-release.apk`](https://github.com/ardaozkul/SAYI-AVCISI/raw/main/apk/SAYI-AVCISI-release.apk)
  - [`apk/SAYI-AVCISI-debug.apk`](https://github.com/ardaozkul/SAYI-AVCISI/raw/main/apk/SAYI-AVCISI-debug.apk)
- 🌐 **Giriş / bilgilendirme sayfası:** [`index.html`](index.html) — APK adresi, kurulum adımları ve ekran görüntüleri.

**Gereksinim:** Android 7.0 (API 24) ve üzeri · ~12 MB · İnternet gerekmez · İlerleme cihazda saklanır.

### 3 adımda kurulum
1. Telefonun tarayıcısından APK bağlantısına dokun ve dosyayı indir.
2. Android “Bilinmeyen kaynaklardan uygulama yükleme” izni isterse bir kez onayla.
3. “Sayı Avcısı” simgesine dokun; oyun çevrimdışı açılır.

---

## 🎮 NASIL OYNANIR

| Adım | Ne olur |
|:---|:---|
| 1 | Bir sayıya **basılı tut**. Taş mürekkep lacivertine döner. |
| 2 | Parmağını **komşu sayılara sürükle**. Seçilen taşlar koyu lacivert kalır. |
| 3 | Parmağını **bırak**. Hamle otomatik kontrol edilir; “EŞLEŞTİR” düğmesi yoktur. |
| ✓ | Doğruysa taşlar yeşil parlayıp patlar, puan üstteki yıldıza uçar. |
| ✗ | Yanlışsa taşlar kırmızı olup sarsılır ve bölüm puanından **2 puan** gider. |

- **Komşuluk:** Yatay, dikey ve çapraz komşular geçerlidir. Aralarında taş kalmayan uzak sayılar
  da (ör. aynı satırdaki `2` ve `5`) komşu sayılır. Düz giderken çapraza dönebilirsin.
- **Grup boyutu:** Kombolar sınırlı değildir; şartı kaç taşla sağlıyorsan o kadar taş seçersin (30 taşa kadar).
  Bölme her zaman iki taşlıdır; Sonsuz Av'da görev neyi isterse o kadar.
- **Çıkarmada sıra:** Önce **büyük** sayı, sonra **küçük** sayı seçilir. Sonucu eksi çıkaracak seçim kabul
  edilmez; yanlış sırada seçim puan götürmez, kısa bir “önce büyük, sonra küçük” uyarısı verir.
- **Bölmede sıra:** Önce **bölünen**, sonra **bölen** seçilir. Bölme her zaman iki taşlıdır; yanlış sırada
  seçim yine puan götürmez, “önce bölünen, sonra bölen” uyarısı çıkar.
- **Sıra rozetleri:** Seçilen taşların üzerinde **1, 2, 3** numaraları görünür; çocuk seçim sırasını takip eder.
- **Tek dokunuşla oynayanlar:** Kesinleşen seçim 140 ms içinde değerlendirilir; doğru ya da yanlış geri
  bildirimi anında gelir. Hedefin altında kalan seçim, ek taş ekleyebilmen için kısa süre bekler.
- **Yıldız desteği:** Geçerli hamle kalmazsa oyun ücretsiz olarak kalan taşları yeniden dizer;
  puan/joker gitmez, ekranda yıldız efektiyle duyurulur.

---

## 🆕 5.0.0 — NELER DEĞİŞTİ

### 1. Daha büyük tahtalar
- Bütün tahtalar **8 sütun** genişliğe çıktı; kolay hedefler **8×12** (96 taş), genel hedefler
  **8×8** (64 taş), bölüm finalleri **8×60** (480 taş) oldu.
- Sonsuz Av tahtası **8×120** (960 taş); geniş tabletlerde **20×120**.
- Ebeveyn panelindeki tablet ayarı artık `Otomatik`, `8`, `12`, `16`, `20` sütun sunar.
  Otomatik mod 1000 dp ve üzerinde **20**, 840 dp ve üzerinde **16**, 700 dp ve üzerinde **12**,
  altında **8** sütun seçer.

### 2. Sınırsız kombo çarpanı
- Her **3 ardışık doğru hamlede** puan çarpanı ikiye katlanır: 3 doğru ×2, 6 doğru ×4,
  9 doğru ×8, 12 doğru ×16 … Kombo zincirinin bir üst sınırı yoktur.
- Çarpan bölüm puanına ve Sonsuz Av’ın yeşil puanına uygulanır; yanlış hamle seriyi sıfırlar.
- Üst barda anlık çarpan (`×4 SERİ`), komboda ise `SERİ n · ×m PUAN` olarak görünür.

### 3. Zengin kombo türleri
- **ZİNCİR:** 2+ ardışık doğru. **HIZLI:** 3,5 saniye içinde gelen doğru.
  **ÜÇLÜ/DÖRTLÜ:** 3 veya 4 taşlık kombo. **KUSURSUZ:** hiç yanlışsız 5+ seri.
  **KARIŞIK:** farklı işlem/hedef türleri ardı ardına.
- Kombo başlıkları büyür: KOMBO → SÜPER → MEGA → EFSANE → İNANILMAZ → SAYI AVCISI.
- Kombo bonusu taş sayısıyla büyümeye devam eder: 3 taş +5, 4 taş +12, sonra her taş +6.

### 4. Dengeli işlem dağılımı
- Sonsuz Av’da dört işlem (toplama, çıkarma, çarpma, bölme) **dengeli bir torbadan** gelir:
  her turda dördü de birer kez çıkar, sonra torba yeniden karışır. Artık rastgelelik tek bir
  işlemi üst üste yığmaz.

### 5. Daha canlı görsel dil
- Ekranın arkasında yavaşça süzülen **animasyonlu ortam ışıkları** ve ince parıltılar vardır.
- Dünya bazlı renk paletleri, seçili taşta büyüme + altın halka + cam parlaklığı, doğru/yanlış
  hamlede net renk ve sarsıntı geri bildirimi.
- Kombo afişi taş sayısını, kazanılan puanı, seriyi, çarpanı ve kombo türü çiplerini gösterir.

---

## 🆕 4.0.0 — NELER DEĞİŞTİ

### 1. Yeni tema ve yazı tipleri
- Turuncu ağırlıklı görünüm tamamen kaldırıldı. Yerine **mürekkep laciverti, eski kâğıt ve
  adaçayı yeşili** geldi; parlak neon gradyan yok, basılmış oyun defteri hissi var.
- Başlıklar **Bricolage Grotesque**, gövde yazıları **Public Sans** ile yazılıyor. İki font da
  projeye gömülüdür; oyun internet olmadan da aynı görünür. Lisanslar `docs/fonts/` altındadır.
- Doğru hamle yeşil, yanlış hamle kiremit kırmızısı; normal puan kâğıt altını, Sonsuz Av puanı yeşil.

### 2. Tahta boyutları ve tablet ayarı
| Bölüm | Tahta |
|:---|:---|
| Kolay hedefler | **6×10** (60 taş) |
| Genel hedefler | **6×6** (36 taş) |
| Bölüm finali | **6×50** (300 taş, dikey kayar) |
| Sonsuz Av | **6×100**; geniş tablette **15×100** |

- Ebeveyn panelinde **Tablet genişliği** ayarı vardır: `Otomatik`, `8`, `12`, `16`, `20` sütun
  (5.0.0’da yenilendi).
- Otomatik mod ekran genişliğine bakar: 1000 dp ve üzeri **20**, 840 dp ve üzeri **16**,
  700 dp ve üzeri **12**, altı **8** sütun.
- Geniş tahtalarda çocuk daha çok sayı görür; satırlar dikey kaydırılır. Çıkarma/çarpma/bölme
  bölümlerinde tek taş artmasın diye sütun sayısı en yakın çift sayıya yuvarlanır.

### 3. Sonsuz Av — yeni mod
- Sabit bir başlık yoktur. Üstte o anki **görev formülü** yazar; görev doğru tamamlanınca yenisi gelir.
- Görev sırası: **4 sayı topla → 3 sayı çıkar → 3 sayı çarp → 2 sayı böl** ve baştan tekrar.
- Komşuluk aranmaz; görevdeki sayılar 8×120 tahtanın her yerinden seçilebilir.
- Doğru hamlede temizlenen kareler **anında** yeni sayılarla dolar; tahta hiç boş kalmaz.
- Her görev için tahtaya oynanabilir bir örnek kombinasyon yerleştirilir; “görev var ama
  yapılacak sayı yok” durumu oluşmaz.
- Sonsuz Av puanı **yeşil puandır** ve normal bölümlerin sarı puanına karışmaz. En iyi skor,
  toplam yeşil puan, çözülen görev, oynanma sayısı ve en iyi seri ayrı kaydedilir.
- “Bitir” ile oturum kapatılır; sonuç ekranında yeşil puan ve ayrı rekor gösterilir.

### 4. Daha hızlı tek dokunuş geri bildirimi
- Sürüklemeden tek tek dokunan çocuk için kesinleşen seçim **140 ms** içinde değerlendirilir.
- Doğru kombinasyon anında kabul edilir; geometrisi bozuk ya da hedefi aşan seçim anında reddedilir.
- Toplamada hedefin altında kalan seçim, 3. ve 4. taşı ekleyebilmesi için kısa bir süre bekler;
  çocuk devam etmezse seçim sonuç olarak denenir.
- Sonsuz Av’da görev 4/3/2 taş istiyorsa, o sayı dolmadan karar verilmez.

### 5. Bütün kombinasyonlar eşit
- Tahta, hedefin **tüm** kombinasyonlarını içeren karıştırılmış bir desteden doldurulur.
  “4 toplamı” bölümünde `0+4`, `1+3` ve `2+2` aynı sıklıkta gelir; tek bir kombinasyona sıkışmaz.
- Toplamada `1+2+3`, `1+1+5`, `0+0+7` gibi üçlüler ve `0+1+2+4` gibi dörtlüler de geçerli.
- Toplama ve çarpmada sıra önemsizdir: `1+4` ile `4+1` aynı kabul edilir. Oyun içinde çıkarma ve
  bölmede ise seçim sırası önemlidir (önce büyük/bölünen).
- 3 taşlı komboda **SÜPER KOMBO**, 4 taşlı komboda **MÜTHİŞ KOMBO** kutlaması çıkar.

### 6. Bölüm yapısı — 66 bölüm + Sonsuz Av
| Bölüm | İçerik | Tahta |
|:---|:---|:---|
| ➕ Toplama | 1’den 30’a ardışık hedefler; 3’lü ve 4’lü kombinasyonlar | **8×60 (hepsi)** |
| ➖ Çıkarma | Farkı 1–12 | 8×12 / 8×8 / 8×60 |
| ✖️ Çarpma Tablosu | 2, 4, 6 … 36 (16 hedef) | 8×12 / 8×8 / 8×60 |
| ➗ Bölme | Kalansız bölme, 1–8 | 8×12 / 8×8 / 8×60 |
| ∞ Sonsuz Av | Anlık görevler | 8×120 / 20×120 |

- Kilit yoktur: çocuk veya veli istediği bölümü açıp deneyebilir; tamamlananlar ✓ ile işaretlenir.

---

## 📸 EKRAN GÖRÜNTÜLERİ (4.0.0 görünümü)

Kareler Android emülatöründe gerçek oynanışla alındı; 5.0.0 aynı temayı kullanır ve
yeni ortam ışıkları, sıra rozetleri ve kombo afişi oyun içinde eklenmiştir.

| Giriş | Bölümler | Kolay Tahta |
|:---:|:---:|:---:|
| ![Giriş](docs/screenshots/v4/01-giris.png) | ![Bölümler](docs/screenshots/v4/02-bolumler.png) | ![Tahta](docs/screenshots/v4/07-oyun-1-6x10.png) |

| Uzun Tahta | 3’lü Kombo | Yanlış Hamle |
|:---:|:---:|:---:|
| ![Uzun tahta](docs/screenshots/v4/08-oyun-30-6x50.png) | ![Kombo](docs/screenshots/v4/14-uclu-kombo.png) | ![Yanlış](docs/screenshots/v4/12-yanlis.png) |

> Eski sürümlerin kareleri [`docs/screenshots`](docs/screenshots) klasöründe tarihsel olarak durur;
> yeni tasarımın kareleri [`docs/screenshots/v4`](docs/screenshots/v4) altındadır.

---

## 🛠️ KAYNAK KODDAN DERLEME

```bash
git clone https://github.com/ardaozkul/SAYI-AVCISI.git
cd SAYI-AVCISI
./gradlew testDebugUnitTest assembleDebug assembleRelease
# debug   -> app/build/outputs/apk/debug/app-debug.apk
# release -> app/build/outputs/apk/release/app-release.apk
```

- Kotlin, Jetpack Compose (BOM 2025.12.01), minSdk 24, targetSdk 35.
- Sürüm: `versionCode 8`, `versionName 5.0.0`.
- Release APK, projede tanımlı debug imzasıyla üretilir; mağaza dışı kurulum içindir.

### Mimari
```
model/GameModels.kt         LevelSpec, Board, BlockCell, MatchResult, EndlessMission
engine/RuleEngine.kt        komşuluk/çizgi geometrisi, matematik + çıkarma/bölme sıra kontrolü
engine/BoardBuilder.kt      garantili tahta döşemesi, kombinasyon destesi, 8×120 Sonsuz tahta
engine/EndlessMissionFactory.kt  anlık Sonsuz Av görevleri ve örnek kombinasyonlar
engine/BoardDoctor.kt       ipucu, tıkanma tespiti, yıldız desteği
engine/CurriculumRepository 4 bölüm / 66 bölüm + Sonsuz Av, tablet sütun ayarı
engine/ScoreEngine.kt       puan, kombo bonusu, yanlış ceza, ödüller, yeşil puan
engine/GameSession.kt       sürükleme/dokunma, anlık kontrol, Sonsuz Av, kayıt/geri yükleme
engine/ProgressRules.kt     bütün bölümler açık, tamamlanan işaretleri
data/LocalProgressStorage   SharedPreferences: ilerleme, ayarlar, yeşil puan rekorları
ui/GameScreen.kt            oyun ekranı, sürükleme katmanı, kombo ve Sonsuz Av başlığı
ui/GameScreenParts.kt       taş (kâğıt/lacivert/yeşil/kırmızı), yazılı düğmeler
ui/GameEffects.kt           popIn, shake, parçacık, yıldız, konfeti, halka, çizgi
ui/AppFonts.kt              Bricolage Grotesque + Public Sans
ui/StartScreen.kt           sade giriş
ui/AdventureMapScreen.kt    dört bölüm kartı + Sonsuz Av ve hedef çipleri
ui/ParentPanelScreen.kt     ebeveyn paneli (istatistik/ayarlar/yeşil puan kayıtları)
```

---

## ✅ TESTLER

`app/src/test/java/com/sayiavcisi/app/EngineRulesTest.kt` — **41 test, 0 başarısız**:

- Toplamada bütün ikili/üçlü/dörtlü kombinasyonlar kabul edilir; ters sıra da geçerli.
- `0+4`, `1+3`, `2+2` aynı tahtada bulunur (eşit dağıtım).
- 3 sayılı çıkarma (`a−b−c`) ve çarpma (`a×b×c`) kuralları doğru çalışır.
- Çıkarmada önce büyük sonra küçük, bölmede önce bölünen sonra bölen zorunludur; ters sıra puan götürmez.
- Eksi sonuçlu çıkarma kombinasyonu reddedilir; bölme her zaman iki elemanlıdır.
- Toplama 1–30’un tamamı en az 8×60 tahtadadır.
- Sonsuz Av FREE moddadır (komşuluk aranmaz) ve görev başlıkları açık formüldür.
- Düz giderken çapraza dönme ve aralarında taş kalmayan uzak sayılar geçerli.
- 66 bölüm × 2 tohum: her tahta tamamen kapanır, her grup hedefi tutar.
- Tablet sütunları (8/12/16/20) tahtayı tamamen kapatır; tek sütunda boş taş kalmaz.
- Sonsuz Av görevi anında oynanır, temizlenen kare yenilenir ve yeşil puan ayrı kaydedilir.
- Plan sırasıyla oynanınca tahta temizlenir; rastgele oynanınca yıldız desteği devreye girer.
- 4 taşlı komboda +12 kombo bonusu ve kutlama tetiklenir.
- Kombo çarpanı her 3 doğruda katlanır (×2, ×4, ×8 …) ve sınırsızdır.
- Sonsuz Av’da dört işlem dengeli torbadan her turda birer kez dağıtılır.
- Yanlış hamlede −2 puan ve seri sıfırlanır.
- Günlük tavan bölüm puanını kesmez; kayıt/geri yükleme ve joker hakları çalışır.
- Müfredat: 30 toplama + 12 çıkarma + 16 çarpma + 8 bölme + Sonsuz Av; hepsi açık.

Ayrıca yeni APK emülatörde kurulup gerçek parmak sürükleme (2’li ve 3’lü kombo), doğru/yanlış
geri bildirimi, 8×12 ve 8×60 tahtalar, kaydırma ve kombo kutlaması adb ile oynanarak doğrulandı.

---

## 🔒 GİZLİLİK

- Hesap, reklam, analitik ve ağ izni yoktur.
- Bütün ilerleme ve istatistikler yalnızca cihazdaki `SharedPreferences` içinde tutulur.
- Ebeveyn panelinden ses, süreli/rahat mod, tablet genişliği ve ilerleme sıfırlama yönetilebilir;
  normal puanlar, yeşil Sonsuz Av puanları ve rekorlar ayrı ayrı görülebilir.
