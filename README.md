# PerioMeasure

Fiji/ImageJ için perioküler ölçüm eklentisi. Hekim, yüz fotoğrafında
landmark'ları tıklar; eklenti kalibrasyonu ve ölçümleri deterministik olarak
hesaplar (PFI, ICD, PFW, PFH, MRD1, MRD2, PFH/PFW ve diğerleri).

> ⚠️ **Yalnızca araştırma amaçlıdır; tıbbi cihaz değildir.** Klinik tanı veya
> tedavi kararı için tek başına kullanılmamalıdır. Sonuçlar, kullanan
> araştırmacının doğrulama protokolüyle (aşağıda) teyit edilmelidir.

**Geliştirme durumu:** Faz 0 (iskelet). Menüde şimdilik yalnızca
`Bilgi / About` komutu var. Ölçüm arayüzü Faz 2'de gelecek.

---

## 1. Kurulum

### Gereksinimler
- **Fiji** (https://fiji.sc). "Stable" sürümü (Java 8) ve yeni sürümler
  (Java 21) desteklenir; eklenti Java 8 uyumlu derlenir.
- Kaynak koddan derlemek için **JDK 17 veya üstü**. Maven kurmanız
  **gerekmez**: proje Maven Wrapper (`mvnw.cmd`) içerir. Wrapper, ilk
  çalıştırmada sabit sürüm Maven'i indirir ve SHA-256 ile doğrular.

### Derleme
```bat
mvnw.cmd clean verify
```
Tüm testler geçerse `target\PerioMeasure_.jar` oluşur. Test geçmezse jar
oluşmaz.

### Fiji'ye kurulum
1. Fiji açıksa kapatın.
2. `target\PerioMeasure_.jar` dosyasını Fiji'nin `plugins\` klasörüne
   kopyalayın (ör. `C:\Users\<kullanıcı>\Fiji.app\plugins\`).
   Güncellemede eski dosyanın **üzerine yazın**. Jar adı sürüm içermez, böylece
   iki kopya oluşmaz.
3. Fiji'yi açın: **Plugins › PerioMeasure › Bilgi / About**. Pencerede
   sürüm numarası görünüyorsa kurulum tamamdır.

---

## 2. Kullanım (Faz 2'de tamamlanacak)

1. Fotoğrafı açın. Hasta ID, tarih, göz (OD/OS/her ikisi), Pre-op/Post-op
   bilgilerini girin.
2. **Kalibrasyon:** alın orta hattındaki 24,0 mm çaplı dairesel etiket.
   - Otomatik (önerilen): etiketin etrafına kaba bir dikdörtgen çizin.
   - Manuel: etiketin kenarına en az 8 nokta tıklayın.
3. **Landmark'lar:** her göz için ekrandaki talimatla sırayla pupil merkezi
   (veya korneal ışık refleksi), üst kapak kenarı, alt kapak kenarı, medial
   kantus, lateral kantus. Noktalar sürüklenerek düzeltilebilir; Undo vardır.
4. **Sonuçlar:** tablo + CSV (+ anotasyonlu PNG ve ROI seti, Faz 3).

### Ana ölçümler

| Kısaltma | Ölçüm | Tanım |
|---|---|---|
| PFI | Palpebral fissür indeksi | PFH / PFW |
| ICD | İnterkantal mesafe | İki medial kantus arası Öklid mesafe |
| PFW | Palpebral fissür genişliği | \|LC − MC\| |
| PFH | Palpebral fissür yüksekliği | Üst ve alt kapak kenarı arası, dikey eksende |
| MRD1 | Margin-reflex distance 1 | Pupil merkezinden üst kapağa (işaretli) |
| MRD2 | Margin-reflex distance 2 | Pupil merkezinden alt kapağa |
| Kantal açı | Kantal eğim | MC→LC doğrusunun interpupiller eksenle açısı; LC yukarıdaysa + |

Her uzunluk ekranda tek bir hata payıyla gösterilir (ör. `PFW 28,41 ± 0,07 mm`).
Bu pay iki kaynağı birleştirir: tıklama hassasiyeti (1 piksel) ve
kalibrasyon etiketinin ölçülme hassasiyeti. Ayrıntılı bileşenler CSV'dedir.

Kesin matematiksel tanımlar `rules.md` dosyasının 4. bölümündedir.

---

## 3. Fotoğraf çekim protokolü

Pre-op ve post-op karşılaştırması ancak çekim koşulları aynıysa geçerlidir.

- **Mesafe:** ≥ 1 m (perspektif bozulmasını azaltır).
- **Odak uzaklığı:** ≥ 85 mm (35 mm eşdeğeri). Geniş açı ve telefon ön
  kamerası kullanmayın.
- **Baş pozisyonu:** Frankfort horizontal düzlemi yere paralel; baş
  döndürülmemiş, eğilmemiş.
- **Bakış:** primer pozisyon, karşıya; kamera göz hizasında.
- **Kalibrasyon etiketi:** 24,0 mm çaplı, **mat** (parlamayan), cilde zıt
  renkte dairesel etiket; alın orta hattına, gözlerle yaklaşık aynı düzleme,
  kırışıksız yapıştırılır.
- **Işık:** düzgün, gölgesiz; korneal ışık refleksi görünür olmalı.
- **Çözünürlük:** görüntü genişliği ≥ 2000 px; etiket çapı görüntüde
  ≥ 150 px. En yüksek JPEG kalitesi veya kayıpsız format (TIFF/PNG).
- **Pre/post:** aynı kamera, lens, mesafe, ışık ve düzenek.

---

## 4. Veri gizliliği ve anonimleştirme

- Hasta fotoğrafları **yalnızca yerel bilgisayarda** saklanmalıdır.
  OneDrive, Google Drive, Dropbox, iCloud gibi **bulut senkronlu klasörlere
  koymayın**. Bu proje klasörü de böyle bir klasördeyse fotoğrafları başka
  bir yerel klasörde tutun.
- Dosya adlarında hasta adı veya TC kimlik no kullanmayın; çalışma kodu
  kullanın (ör. `P017_pre_2026-03-02.jpg`). Kod ↔ kimlik eşleştirme listesini
  ayrı ve şifreli tutun.
- Fotoğraflardaki EXIF meta verisini (tarih, GPS, kamera seri no) paylaşmadan
  önce silin.
- Eklentinin CSV'sinde dosya adı yerine dosya adının **hash'i** saklanır.
- Bu kod deposuna hasta verisi (fotoğraf, CSV, ROI, PNG) **asla** eklenmez;
  `.gitignore` bunları dışarıda tutar.

---

## 5. Doğrulama protokolü (taslak; Faz 5'te raporlanacak)

1. **Sentetik doğrulama (otomatik):** bilinen ölçekte 24 mm daire ve bilinen
   konumda landmark'lar içeren, farklı rotasyon/eğim/çözünürlükte üretilmiş
   görüntüler. Kabul ölçütü: uçtan uca hata < 0,05 mm. Her derlemede
   `mvnw.cmd verify` ile çalışır.
2. **Fantom / cetvel fotoğrafı:** etiket düzlemine yerleştirilmiş milimetrik
   cetvel veya bilinen ölçülü desen, klinik çekim düzeneğiyle fotoğraflanır;
   bilinen mesafeler ölçülüp gerçek değerle karşılaştırılır.
3. **Gözlemci içi tekrarlanabilirlik:** aynı fotoğrafta 3 bağımsız işaretleme
   (tekrar modu); %CV < 5.
4. **Gözlemciler arası güvenilirlik:** ≥ 2 gözlemci; ICC (tekrar modunun uzun
   format çıktısı).
5. **Yöntem karşılaştırması:** klasik elle ImageJ ölçümüyle Bland–Altman
   analizi.

---

## 6. Geliştirici notları

```bat
mvnw.cmd clean verify                 :: derleme + tüm testler + jar
mvnw.cmd clean verify -Pfiji-runtime  :: testleri Fiji'nin kendi Java 8'inde çalıştır
```
- Testler Türkçe yerel ayarla (`tr_TR`) çalışır; ondalık virgül hataları
  böylece yakalanır.
- Derleme uyarıları hata sayılır; tüm Maven eklenti sürümleri sabittir.
- Proje kuralları ve ölçüm tanımları: [`rules.md`](rules.md).
