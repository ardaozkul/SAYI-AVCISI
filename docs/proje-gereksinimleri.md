# PROJE: SAYI AVCISI – ÇOCUKLAR İÇİN MATEMATİK BULMACA OYUNU

## 1. PROJENİN AMACI

Android telefon ve tabletlerde çalışacak, 3. sınıf seviyesindeki çocuklara yönelik, eğlenceli, animasyonlu ve aşamalı bir matematik bulmaca oyunu geliştir.

Uygulamanın adı: SAYI AVCISI.

Hedef: Çocuğun toplama, çıkarma, çarpma ve bölme işlemlerini oyun oynarken öğrenmesini ve hızlandırmasını sağlamak.

Oyun, klasik matematik testlerinden farklı olmalı. Çocuk işlemleri doğrudan sorular halinde çözmek yerine, bir sayı tablosundaki uygun sayıları bulup eşleştirerek blokları yok etmeli.

Oyun 5 günlük bir macera olarak tasarlanmalı. Her gün yaklaşık 60 dakika oynanabilecek yeterli içerik bulunmalı.

Uygulama gerçek bir Android APK olarak derlenebilmeli.

## 2. ANA OYUN MEKANİĞİ

Ekranda Sudoku benzeri, ancak Sudoku kurallarına bağlı olmayan, sayılardan oluşan büyük bir oyun tahtası bulunmalı.

Örnek:
8  2  5  0  3  7
4  6  1  9  2  5
7  3  8  4  6  1
0  5  2  7  9  3
6  4  1  8  5  2
3  7  9  0  4  6

Çocuk aynı hedefi sağlayan sayıları bulduğunda bu sayı blokları yok olmalı.

Eşleşme kuralları:

- Yatay sağa ve sola.
- Dikey yukarı ve aşağı.
- Çapraz tüm yönlerde.
- Ters yönde okuma.
- Birbirine bitişik olmayan sayılar.
- Aralarında başka bir sayı bulunmayan, aynı doğrultudaki sayılar.
- Aynı satırda, sütunda veya çapraz hatta bulunan uygun sayılar.
- Uygun olduğu durumlarda farklı satır ve sütunlardaki sayıların serbest eşleştirilmesi.

ÖNEMLİ: Eşleştirme algoritması açık ve tutarlı olmalı. Aralarında başka bir sayı bulunan iki sayının, yalnızca görsel olarak aynı doğrultuda oldukları için eşleşmesine izin verme. Serbest eşleştirme modu ayrı bir oyun kuralı olarak sunulmalı.

## 3. TOPLAMA MODU

En kolay seviyeden başlanmalı.

Hedef toplamlar:

- 1'e ulaşma.
- 2'ye ulaşma.
- 3'e ulaşma.
- 4'e ulaşma.
- 5'e ulaşma.
- 6'ya ulaşma.
- 7'ye ulaşma.
- 8'e ulaşma.
- 9'a ulaşma.
- 10'a ulaşma.
- 11-15 arası.
- 16-20 arası.
- 21-30 arası.
- 31-50 arası.
- 51-100 arası.

Sıfır özel olarak öğretilmeli.

Örnek:
0+1=1
0+5=5
0+0=0
3+4=7
2+5=7
8+2=10
6+7=13

Başlangıçta iki sayı eşleştirilmeli. İlerleyen seviyelerde üç sayının toplamı hedeflenmeli.

Örnek:
2+3+2=7
5+4+1=10
8+7+5=20

Çocuk hedef toplamı ekranda görmeli.

Örnek: HEDEF: 10

Tahtada doğru kombinasyonu bulduğunda sayılar animasyonla kaybolmalı.

## 4. ÇIKARMA MODU

Çıkarma seviyeleri 0'dan başlamalı.

Örnekler:
5-5=0
7-6=1
9-4=5
10-3=7
15-8=7
20-12=8

İlk seviyelerde iki sayı kullanılmalı. Sonraki seviyelerde üç sayılı zincir işlemleri eklenmeli.

Negatif sonuçlara başlangıç seviyelerinde izin verme.

Eşleştirme sırasında büyük sayıdan küçük sayı çıkarılması zorunlu olmalı.

Çocuk çıkarma işleminin anlamını kavrayabilsin diye eksilen sayıdan nesne eksilme animasyonu kullan.

## 5. ÇARPMA MODU

Çarpım tablosunu ezberletmek yerine tekrar yoluyla öğrenmesini sağla.

Öncelik:
0, 1, 2, 3, 4, 5, 10 çarpımları.

Daha sonra:
6, 7, 8, 9 çarpımları.

Örnek:
2×3=6
4×5=20
3×7=21
6×6=36
8×7=56
9×9=81

Tahtadaki iki sayı çarpılarak hedef sonuç bulunmalı.

İlerleyen seviyelerde eksik çarpanı bulma özelliği ekle.

Örnek:
?×4=20

Yanlış cevapta doğrudan başarısızlık ekranı gösterme. Görsel ipucu ve kısa açıklama sun.

## 6. BÖLME MODU

Bölme, çarpma işleminin tersi olarak öğretilmeli.

İlk seviyelerde yalnızca kalansız bölme kullanılmalı.

Örnek:
0÷2=0
6÷2=3
10÷5=2
12÷3=4
20÷4=5
36÷6=6
56÷7=8
81÷9=9

Sıfıra bölme kesinlikle sorulmamalı.

Bölme işlemi için çocuk büyük sayıyı seçip bölen sayıyı bulmalı veya bölme işlemine uygun iki sayıyı eşleştirmeli.

İlerleyen seviyelerde eksik bölen ve bölünen soruları ekle.

Örnek:
20÷?=5

## 7. OYUN TAHTASI VE SEVİYE TASARIMI

Oyun tahtası giderek büyümeli.

Başlangıç:
4×4

Kolay:
5×5

Orta:
6×6

Zor:
8×8

Uzman:
10×10

Her bölüm yeterli sayıda hamle içermeli.

Özellikle her oyun tahtası önceden matematiksel olarak doğrulanmalı.

Şu kurallar zorunludur:

- Her üretilen tahtanın en az bir geçerli çözümü olmalı.
- Oyunun tamamlanması için gereken bütün sayı blokları çözülebilmeli.
- Eşleşmeyen sayı bırakılmamalı.
- Rastgele sayı üretimi çözümsüz tablo oluşturmamalı.
- Bir hamle sonrasında yeni eşleşmeler oluşabilmeli.
- Tahta kilitlenirse otomatik kurtarma devreye girmeli.
- Çözüm doğrulama algoritması oyuna başlamadan çalışmalı.
- Çözülebilirlik testinden geçmeyen tahta oyuncuya gösterilmemeli.

Tahtalar rastgele görünmeli ama arkalarında garantili bir çözüm dizisi bulunmalı.

Bölüm sonunda bütün bloklar temizlenmiş olmalı.

## 8. BONUS VE JOKER SİSTEMİ

Başlangıçta üç joker ver.

Joker 1: İPUCU
Doğru eşleşmenin hangi sayılardan oluştuğunu gösterir.
Puan: -5

Joker 2: YENİDEN DÜZENLE
Tahtayı çözülebilir yeni bir düzene sokar.
Puan: -10

Joker 3: ÇİFTE HAMLE
Bir sonraki doğru eşleşmenin puanını artırır.
Puan: -5

Bonusların kullanımı günlük puan sınırını değiştirmemeli.

## 9. GİZLİ KURTARMA SİSTEMİ

Bu sistem çok önemlidir.

Üç joker de tükenirse ve oyun tahtasında geçerli hamle bulunamazsa otomatik olarak devreye giren dördüncü bir kurtarma sistemi oluştur.

Bu sistemin adı: ALTIN KURTARMA.

Özellikleri:
- Çocuğa başarısız olduğu hissettirilmemeli.
- Tahta yeniden çözülebilir duruma getirilmeli.
- Önceden doğrulanmış bir çözüm sırası kullanılmalı.
- Gerekiyorsa bir sayı bloğu değiştirilmeli.
- Çocuk hiçbir zaman çözümsüz bir seviyede mahsur kalmamalı.
- Bu sistem için çocuk ekstra puan kaybetmemeli.
- Sistem sessiz ve otomatik çalışmalı.

Bu kurtarma sistemi bir hata değil, oyunun güvenlik mekanizmasıdır.

## 10. PUANLAMA

Bir günde en fazla 100 puan kazanılabilmeli.

Günlük puan tablosu:

Doğru iki sayılı eşleşme: +2
Doğru üç sayılı eşleşme: +3
Arka arkaya 5 doğru işlem: +5
Arka arkaya 10 doğru işlem: +10
Hatasız bölüm bitirme: +5
Süre bonusu: +3
Günün görevini tamamlama: +10

Toplam günlük kazanım kesinlikle 100 puanı geçmemeli.

Jokerler puan düşürmeli.

Yanlış eşleşme: -1 puan.
Joker kullanımı ilgili puan kadar düşüş sağlamalı.

Puan sıfırın altına düşmemeli.

Puan sistemi sunucuya bağlı olmak zorunda değil. Cihazda güvenli yerel kayıt sistemi kullanılabilir.

## 11. SÜRE VE GERİLİM MEKANİĞİ

Bazı seviyelerde süreli oyun modu bulunmalı.

Kolay seviyeler:
Süre baskısı olmasın.

Orta seviyeler:
90 saniye.

Zor seviyeler:
60 saniye.

Uzman seviyeler:
45 saniye.

Süre dolunca çocuk cezalandırılmamalı. Süreli modu tekrar deneyebilmeli.

Ayrıca süre sınırı olmayan rahat oyun modu bulunmalı.

Çocuğun öğrenmesini olumsuz etkileyebilecek yoğun stres yaratılmamalı.

## 12. BEŞ GÜNLÜK EĞİTİM PROGRAMI

1. GÜN: SAYILARIN GÜCÜ
- 0 ve 1 kavramı.
- 1-10 toplama.
- 1-10 çıkarma.
- İki sayılı eşleştirme.
- 4×4 ve 5×5 tablolar.

2. GÜN: ONLUK AVCISI
- 10-20 toplama.
- 10-20 çıkarma.
- Üç sayılı toplama.
- İki sayılı çıkarma.
- 5×5 ve 6×6 tablolar.

3. GÜN: ÇARPIM ORMANI
- 0, 1, 2, 3, 4, 5 ve 10 çarpımları.
- Görsel gruplama.
- Çarpan bulma.
- 6×6 tablolar.

4. GÜN: BÖLME MAĞARASI
- Kalansız bölme.
- Çarpma ve bölme ilişkisi.
- Eksik bölen bulma.
- 6×6 ve 8×8 tablolar.

5. GÜN: BÜYÜK MATEMATİK FİNALİ
- Dört işlem karışık.
- 20'ye kadar toplama.
- 20'ye kadar çıkarma.
- Çarpım tablosu.
- Kalansız bölme.
- 8×8 ve 10×10 tablolar.
- Büyük final ve ödül.

Her gün yaklaşık 60 dakika içerik bulunmalı.

Günlük 60 dakika şu şekilde bölünebilir:
10 dakika ısınma.
15 dakika ana bulmaca.
5 dakika mola.
15 dakika ikinci bulmaca.
10 dakika karışık işlemler.
5 dakika değerlendirme ve ödül.

## 13. GÖRSEL TASARIM

Oyun çocuğun ilgisini çekecek profesyonel bir mobil oyun gibi görünmeli.

Renkli, canlı ve modern arayüz.

Karakter:
Sevimli bir matematik kaşifi.

Temalar:
- Sayı ormanı.
- Hazine mağarası.
- Çarpım kalesi.
- Bölme uzayı.
- Matematik adası.

Animasyonlar:
- Sayı eşleşince patlama efekti.
- Doğru işlemde yıldız yağmuru.
- Yeni bölümde kapı açılma animasyonu.
- Altın ve elmas kazanma animasyonu.
- Karakterin sevinç hareketleri.
- Yanlış işlemde nazik görsel uyarı.
- Bölüm bitince konfeti.
- Puan kazanımında sayacın animasyonlu yükselmesi.

Ses:
- Doğru cevap sesi.
- Yanlış cevapta yumuşak uyarı.
- Bonus kazanma sesi.
- Bölüm tamamlama müziği.

Ses açma ve kapatma ayarı bulunmalı.

## 14. EBEVEYN PANELİ

Ebeveyn için ayrı bir panel oluştur.

Burada şunlar görülebilmeli:

- Hangi gün tamamlandı?
- Toplam kaç işlem yaptı?
- Toplam doğru sayısı.
- Yanlış cevap sayısı.
- Toplama başarı oranı.
- Çıkarma başarı oranı.
- Çarpma başarı oranı.
- Bölme başarı oranı.
- En çok zorlandığı işlemler.
- Çarpım tablosunda hangi sayılarda zorlandığı.
- Ortalama cevap süresi.
- Günlük oyun süresi.

Ebeveyn, süre sınırını ve ses ayarlarını değiştirebilmeli.

## 15. TEKNİK GEREKSİNİMLER

Android APK olarak geliştir.

Öncelik:
- Kotlin ve Jetpack Compose veya
- Flutter.

Oyun tamamen çevrimdışı çalışabilmeli.

İnternet bağlantısı zorunlu olmamalı.

Kayıtlar cihazda saklanmalı.

Uygulama kapatılıp açıldığında kaldığı yerden devam etmeli.

Telefon ve tablet ekranlarına uyum sağlamalı.

Dikey ekran tasarımı öncelikli olmalı.

Dokunmatik alanlar çocukların rahat kullanabileceği büyüklükte olmalı.

Pil tüketimi düşük tutulmalı.

Gereksiz reklam, uygulama içi satın alma ve dış bağlantılar bulunmamalı.

## 16. KALİTE KONTROL VE TESTLER

Uygulamayı teslim etmeden önce şu testleri gerçekleştir:

1. Bütün işlem seviyelerinin matematiksel doğruluğunu test et.
2. Sıfırın kullanıldığı işlemleri test et.
3. Sıfıra bölme olmadığını doğrula.
4. Bütün oyun tahtalarının çözülebilirliğini doğrula.
5. Hiçbir bölümde eşleşmeyen blok kalmadığını doğrula.
6. Üç jokerin doğru çalıştığını test et.
7. Jokerler bitince ALTIN KURTARMA sistemini test et.
8. Günlük puanın 100'ü geçmediğini test et.
9. Oyunun kayıt sistemini test et.
10. Uygulamanın internet olmadan çalıştığını doğrula.
11. Farklı ekran boyutlarında arayüzü test et.
12. Uygulama çökmesi ve takılma testleri yap.

Her seviye için otomatik matematiksel doğrulama ve oyun tahtası çözüm testi çalıştır.

## 17. TESLİMAT

Yalnızca tasarım veya prototip sunma.

Çalışır durumda Android uygulaması geliştir.

Teslim edilmesi gerekenler:

- Derlenmiş APK.
- Kaynak kodları.
- Proje dosyaları.
- Kurulum talimatları.
- Test sonuçları.
- Ebeveyn paneli.
- Beş günlük oyun içeriği.

Uygulama gerçek cihazda test edilmiş ve hatasız çalışır durumda olmalı.

Öncelik sırası:
1. Matematiksel doğruluk.
2. Çözülebilir oyun tahtaları.
3. Öğretici oyun mekanikleri.
4. Çocuk dostu tasarım.
5. Animasyonlar ve ses efektleri.
6. Performans ve kararlılık.

Bu gereksinimlerin hiçbirini yalnızca görsel olarak taklit etme. Gerçek çalışan özellikler geliştir.