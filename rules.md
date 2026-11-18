# rules.md — PerioMeasure proje kuralları

> Bu dosya projenin kurallar dosyasıdır (adı kullanıcının tercihiyle
> `rules.md`). Her oturumun başında, herhangi bir iş yapmadan önce okunur.

Fiji/ImageJ için perioküler (periorbital) ölçüm eklentisi. Hekim landmark'ları
tıklar, yazılım hesaplar. **Tıbbi araştırmada kullanılır; hesaplama hatası
kabul edilemez.** Kullanıcı okuloplastik cerrahi alanında çalışan bir hekimdir;
iletişim dili Türkçe.

---

## 1. Mutlak kurallar

### 1.1 Hasta verisi
- **Hasta verisi (fotoğraf, CSV, ROI .zip, anotasyonlu PNG, Excel) hiçbir
  koşulda depoya girmez.** `.gitignore` bunu engeller; `.gitignore`'dan bu
  satırları kaldırmak yasaktır.
- Fotoğraflar **yalnızca yerel bilgisayarda** saklanır. Bulut senkronlu
  klasörler (OneDrive, Google Drive, Dropbox, iCloud) hasta verisi için
  kullanılmaz. Eklenti, girdi/çıktı yolu bulut senkronlu bir klasördeyse
  kullanıcıyı uyarmalıdır (Faz 2).
- Testler gerçek fotoğraf kullanmaz; sentetik görüntüler test sırasında
  `target/` altında üretilir ve depoya girmez.
- Hata mesajlarına, loglara ve dosya adlarına hasta kimliği yazılmaz;
  CSV'de dosya adı yerine dosya adının hash'i tutulur.

### 1.2 Çalışma ortamı
- Proje klasörünün dışına dosya yazılmaz. İstisnalar (kullanıcı onayıyla):
  - Fiji kurulumu: `C:\Users\Lenovo\Fiji.app` (OneDrive dışında; Fiji
    stable, gömülü Java 1.8.0_452 Zulu, ImageJ 1.54p). Derlenen jar
    `C:\Users\Lenovo\Fiji.app\plugins\PerioMeasure_.jar` olarak kurulur.
  - Maven'in kendi önbellekleri: `%USERPROFILE%\.m2\`.
- **Git: yalnızca okuma.** `git status`, `git diff`, `git log` serbest.
  `git init`, `commit`, `push`, `reset`, `checkout`, `stash`, `rebase` vb.
  ÇALIŞTIRILMAZ; kullanıcı yapar.
- Her fazın sonunda: değişen/eklenen dosyaların listesi + önerilen commit
  mesajı (İngilizce, kısa başlık + madde madde açıklama). Sonra durulur;
  kullanıcının Fiji'de denemesi ve commit atması beklenir.
- Gerçek fotoğraf gerektiren bir adımda kullanıcıya sorulur; `samples/`'a
  fotoğraf eklenene kadar beklenir.

### 1.3 Kalite
- `mvnw.cmd verify` her zaman geçmeli. Testi geçmeyen hiçbir iş "bitti"
  sayılmaz. Test atlamak (`-DskipTests`, `@Disabled`) yasaktır.
- Çekirdek test öncelikli (TDD) yazılır. Her fonksiyon için analitik olarak
  bilinen değerlerle testler: döndürülmüş eksen, aynalanmış göz, negatif MRD1,
  eğik (elips) etiket, gürültülü kenar noktaları.
- Sentetik uçtan uca testlerde ölçüm hatası < 0,05 mm olmalı.
- Testler Türkçe yerel ayarla (`tr_TR`) çalışır (surefire `argLine`);
  ondalık virgül / Türkçe "I" hatalarını yakalamak için. Sayı biçimlendirme
  ve ayrıştırma her zaman açık `Locale` ile yapılır, varsayılan locale'e
  güvenilmez.
- Derleme uyarıları hata sayılır (`-Xlint:all -Werror`).

---

## 2. Kesinleşmiş kararlar (değiştirmeden önce kullanıcıya sor)

- **Yaklaşım:** deterministik. Landmark'ları hekim tıklar. Otomatik landmark
  tespiti / AI ajanı YOK (ileride ayrı faz olabilir).
- **Platform:** Fiji için Java eklentisi; Maven; `net.imagej:ij` (`provided`
  kapsamında, SciJava Maven deposu); JUnit 5. Kurulum: tek bir `.jar`
  (`PerioMeasure_.jar`) Fiji'nin `plugins/` klasörüne kopyalanır.
- **Derleme hedefi:** Java 8 bytecode (`maven.compiler.release=8`), çünkü
  Fiji stable Java 8 ile gelir; Java 8 bytecode yeni Fiji'de (Java 21) de
  çalışır. `var`, record, text block, switch ifadesi, `List.of` vb. Java 9+
  API'leri KULLANILMAZ (`--release 8` derlemede zaten engeller).
  `ij` sürümü Fiji'deki jar ile aynı tutulur (şu an 1.54p).
- **plugins.config yalnızca ASCII** (ImageJ onu platform kodlamasıyla okur;
  Türkçe harf menüde bozulur). Menü etiketleri bu yüzden ASCII; diğer tüm
  metinler `messages.properties` üzerinden UTF-8.
- **Kalibrasyon referansı:** alın orta hattına yapıştırılmış 24,0 mm çaplı
  dairesel etiket; değer ayarlardan değiştirilebilir.
- **Arayüz dili:** İngilizce (varsayılan; kullanıcı kararı 2026-11-18, önceden
  Türkçe idi) ve Türkçe, üst bardan seçilir ve hatırlanır. Tercih anahtarı
  `periomeasure.uiLanguage`; eski `periomeasure.language` anahtarı bilerek
  okunmaz (eski sürümler her kayıtta "tr" yazıyordu). Tüm arayüz metinleri
  tek bir kaynak dosyada; bir test her anahtarın iki dilde de bulunduğunu
  doğrular.
- **Yapıştırma (Ctrl+V / Yapıştır / sürükle-bırak aynı kural):** önce
  kopyalanmış dosya (orijinal dosya açılır), sonra yol metni, en son piksel
  görüntüsü. Piksel yapıştırmada piksel birebir korunur ama her zaman
  `PASTED_IMAGE` uyarısı eklenir (küçültülmüş kopya olabilir; dosya adı/EXIF
  yok) ve CSV'ye yazılır. Birden fazla fotoğraf kopyalanmışsa hiçbiri
  açılmaz (yanlış fotoğrafı ölçme riski). Kullanıcının gerçek panosu
  testlerde asla okunmaz/değiştirilmez.
- **Öncelikli ölçümler:** PFI, ICD, PFW, PFH. Sonuç tablosunda ve CSV'de ilk
  sırada; erişimi kolay olmalı.
- **Arayüz basit olmalı** (kullanıcı hekim, yazılımcı değil): ekranda az ve
  net bilgi; ayrıntılar (belirsizlik bileşenleri, ham koordinatlar) CSV'de.
- **ICD** = medial interkantal mesafe. Lateral interkantal mesafe şimdilik
  YOK (ileride düşünülebilir).
- **Build aracı:** Maven Wrapper (`mvnw.cmd`), Maven 3.9.16 sabit sürüm,
  dağıtım SHA-256 ile doğrulanır; tüm eklenti sürümleri sabitlenir;
  `maven-enforcer-plugin` Java/Maven sürümünü ve sürüm sabitlemeyi denetler;
  bağımlılık indirmelerinde `--strict-checksums`.

---

## 3. İş akışı (kullanıcı tarafı)

1. Fotoğraf açılır. Hasta ID, tarih, göz (OD/OS/her ikisi), Pre-op/Post-op.
2. Kalibrasyon:
   - **A (otomatik, önerilen):** kullanıcı etiketin etrafına kaba dikdörtgen
     çizer; eşik/renk ile segmentasyon, kenara elips oturtma.
   - **B (manuel):** etiket kenarına ≥ 8 nokta; en küçük kareler elips
     (cebirsel + geometrik iyileştirme).
   - `mm_per_px = 24,0 / büyük_eksen_px` (eğik çekimde dairenin küçülmeyen
     ekseni büyük eksendir).
   - Uyarılar: küçük/büyük eksen oranı < 0,97 → "eğik çekim"; etiket çapı
     < 150 px → "çözünürlük yetersiz"; manuel yöntemde RMS artığı raporlanır.
3. Landmark'lar sırayla, ekranda talimatla, her göz için: pupil merkezi (veya
   korneal ışık refleksi), üst kapak kenarı ve alt kapak kenarı (pupil
   merkezinden geçen dikey hatta), medial kantus, lateral kantus.
   İsteğe bağlı: üst kapak kıvrımı, kaş alt sınırı, limbus üst/alt, kapak
   konturu (poligon). Her tıklamada 4–8× büyütme ve artı imleç; noktalar
   sürüklenerek düzeltilebilir; Undo; koordinatlar double (alt-piksel).
4. Sonuçlar tablo halinde gösterilir ve kaydedilir.

---

## 4. Ölçüm tanımları (matematiksel olarak kesin)

### 4.1 Koordinat ve piksel konvansiyonu
- Görüntü koordinatları: **x sağa, y AŞAĞI** artar.
- **Piksel konvansiyonu (tek yerde tanımlanır: `PixelConvention`):** sürekli
  görüntü koordinatlarında `(i, j)` tamsayı indeksli piksel
  `[i, i+1) × [j, j+1)` alanını kaplar; **piksel merkezi `(i+0.5, j+0.5)`**.
  Bu, ImageJ'nin alt-piksel (float) ROI koordinatlarıyla aynıdır. Sentetik
  görüntü üretici de aynı konvansiyonla çizer; testle doğrulanır.
- Tüm koordinatlar `double`; yuvarlama yalnızca gösterimde yapılır.

### 4.2 Referans eksenler
- **u:** sağ pupil merkezinden (OD) sol pupil merkezine (OS) birim vektör.
  Ön yüz (aynalanmamış) fotoğrafta hastanın sağ gözü görüntünün solundadır,
  dolayısıyla u ≈ +x. `P_OD.x > P_OS.x` ise "görüntü aynalanmış olabilir"
  uyarısı verilir.
- Tek göz ölçülüyorsa: kullanıcının çizdiği yatay çizgi ya da görüntü yatayı
  (+x); yön +x tarafına çevrilir. Hangisinin kullanıldığı kaydedilir
  (`axis_source`: `PUPILS` / `USER_LINE` / `IMAGE_HORIZONTAL`).
- **v:** u'ya dik, YUKARI yönü gösteren birim vektör. y aşağı arttığı için
  `v = (u_y, −u_x)` (u ≈ +x iken v ≈ (0, −1)). Değişmez: `v_y < 0`,
  `u·v = 0`, `|u| = |v| = 1` (testle doğrulanır).

### 4.3 Ölçümler (her göz için; Üst/Alt = kapak kenarı noktaları)
- `PFW = |LC − MC|` (Öklid, mm)
- `PFH = (Üst − Alt) · v` (mm)
- `MRD1 = (Üst − Pupil) · v` (işaretli; kapak refleksin altındaysa negatif)
- `MRD2 = (Pupil − Alt) · v`
- Tutarlılık: `|MRD1 + MRD2 − PFH| > 0,3 mm` ise uyar. (Tanım gereği
  MRD1 + MRD2 = PFH olduğundan bu, sayısal/girdi hatası bekçisidir.)
- **PFI (palpebral fissür indeksi)** = `PFH / PFW` (boyutsuz oran).
  Kullanıcı teyitli: PFI = indeks, inklinasyon DEĞİL. **Oran olarak
  gösterilir** (ör. 0,35; ×100 YOK), ekranda ve CSV'de. Oranın birimi
  yoktur; kullanıcı "mm cinsinden de" istediği için ekranda PFI'nin yanında
  bileşenleri mm ile yazılır:
  `PFI 0,35 ± 0,01  (PFH 9,80 mm / PFW 28,00 mm)` (kullanıcı teyitli,
  2026-11-17).
- **Kantal açı** (derece): `d = LC − MC`; o gözün lateral yönü
  `l = −u` (OD) / `l = +u` (OS);
  `açı = atan2(d · v, d · l)`. Lateral kantus medial kantustan YUKARIDAYSA
  pozitif. Sağ–sol ayna simetrisi `l` ile ele alınır. `d · l ≤ 0` ise
  (lateral kantus medialin lateralinde değil) uyar.
- **ICD** = `|MC_OD − MC_OS|` (medial interkantal mesafe, mm).
- **IPD** (interpupiller) = `|P_OD − P_OS|`.
- Palpebral fissür alanı (mm²): kontur poligonu çizildiyse shoelace ×
  `mm_per_px²`.
- İsteğe bağlı (Faz 1'de uygulandı; tanımlar kullanıcı teyidi bekliyor):
  - `TPS = (Kıvrım − Üst) · v` (üst kapak kenarından kapak kıvrımına)
  - `Kaş yüksekliği = (Kaş alt sınırı − Pupil) · v` (pupilden kaşa)
  - `SSS = (Üst − Limbus_üst) · v`; negatif = kapak limbusu örtüyor
  - `ISS = (Limbus_alt − Alt) · v`; negatif = kapak limbusu örtüyor
  - Sağ–sol farkları: her ölçüm için OD − OS.
- Ek uyarılar: görüntü aynalanmış (`P_OD.x > P_OS.x`); lateral kantus
  medialin lateralinde değil (`d·l ≤ 0`); PFH ≤ 0; kapak noktası pupil
  dikeyinden > 1,0 mm uzakta (dikey ölçümleri etkilemez, yalnızca uyarı).

### 4.4 Belirsizlik
- Her uzunluk L için iki bileşen ayrı ayrı raporlanır:
  - Tıklama: `σ_click = 1 px × mm_per_px`
  - Kalibrasyon: `σ_cal = L × (σ_büyük_eksen / büyük_eksen)`
- Birleşik (onaylandı): `σ = √(σ_click² + σ_cal²)`.
- **Arayüzde yalnızca birleşik değer** gösterilir (ör. `PFW 28,41 ± 0,07 mm`);
  bileşenler (`σ_click`, `σ_cal`, `σ`) CSV'de ayrı sütunlardır.
- PFI (oran) belirsizliği, kısmi türevlerle (PFH = 0'da da tanımlı):
  `σ_PFI = √((σ_click/PFW)² + (PFH·σ_click/PFW²)²)`; kalibrasyon bileşeni 0
  (ölçek oranda sadeleşir).
- Kantal açı: `σ = atan(1 px / PFW_px)` derece; kalibrasyon bileşeni 0.
- Alan: `σ_cal = 2·A·göreli_σ`; `σ_click = çevre × 1 px × mm_per_px²`
  (çevre boyunca 1 piksellik bant).
- Sağ–sol farkı: `σ_click = √(σ₁² + σ₂²)`; iki göz aynı ölçeği paylaştığı
  için kalibrasyon hatası tam koreledir: `σ_cal = |fark| × göreli_σ`.
- Kalibrasyonun göreli σ'sı, elips oturtmanın kovaryansından gelir
  (`σ² (JᵀJ)⁻¹`, `σ² = RSS/(n−5)`); Monte Carlo testiyle doğrulandı. Tam
  daire için yönelim belirsiz olduğundan θ sabit tutularak hesaplanır.

---

## 5. Bilinen ImageJ tuzakları (kaçın)
- Global "Set Scale" kullanılmaz; ölçek her görüntü için eklentinin kendi
  durumunda tutulur. Görüntü dosyasındaki kalibrasyon (TIFF/EXIF) yok
  sayılır ve kullanıcı uyarılır.
- Orijinal piksel verisi asla değiştirilmez; tüm işaretlemeler overlay'dir.
- Piksel koordinat konvansiyonu tek yerde (bkz. 4.1).
- Görüntü genişliği < 2000 px veya JPEG kalitesi düşükse uyar (tahmini IJG
  kalitesi < 85; `JpegQuality`).
- **Tıklama koordinatı:** ImageJ'nin `offScreenXD(sx)` metodu ekran
  pikselinin SOL KENARINI verir (0,5/büyütme px sapma). Bunun yerine
  `ViewMapping.screenToImage` (piksel merkezi) kullanılır; işaretler de
  `ViewMapping.imageToScreenPixel` ile çizilir.
- **Yakınlaştırma sırası:** `ImageCanvas.setSourceRect()` büyütmeyi
  `tuval genişliği / kaynak genişliği` olarak YENİDEN HESAPLAR (ör. 5,97).
  Önce `setSourceRect`, SONRA `setMagnification` çağrılır; aksi halde
  büyütme tam sayı olmaz ve tıklama mantığı bozulur (test ile bulundu).
- (Yukarıdaki iki madde ImageJ penceresi üzerinde çalışan ilk Faz 2
  arayüzü içindi. 2026-11-18'den beri eklenti KENDİ tek penceresini
  kullanır; ImageJ yalnızca dosya açmak (Opener) ve Fiji menüsü için
  kullanılır. Görüntü çizimi ve tıklama dönüşümü `ViewState` +
  `ViewMapping` ile tek yerde; `PaintingTest` ekranda gösterilen pikselin
  tıklamanın seçtiği piksel olduğunu her ölçekte doğrular.)
- `plugins.config` ASCII olmalı; menü etiketleri bu yüzden İngilizce
  (Measure..., Demo..., Settings..., About), pencere seçili dilde.
- Pencereli Fiji'de komut satırı `-eval` / `-macro` çalışmadı (test
  edildi). Pencereli otomatik deneme: Fiji'nin Java'sı + `ij-1.54p.jar` +
  eklenti jar'ı ile `PerioMeasureApp --selftest <rapor>`: gerçek pencerede
  her noktayı büyük görüntüde tek tıkla koyar, büyüteçte tek tıkla ince
  ayar yapar, işaretin doğru piksele çizildiğini denetler, Devam/Bitir
  düğmelerine basar, sonucu gerçek değerlerle karşılaştırır, rapor ve
  ekran görüntüleri (`selftest_measuring.png`, `selftest_results.png`)
  yazar, kapanır. Her UI değişikliğinden sonra çalıştırılır ve ekran
  görüntülerine bakılır. Süreç yalnızca kendi PID'iyle izlenir. Pencere
  ekranda açıldığı için gerçek fare imleci fotoğrafın üzerindeyse büyüteç
  imleci gösterir; test bu yüzden imleç bilgisini sıfırlayıp çizimi aynı
  EDT görevinde yapar (aksi halde rastgele FAIL verir — 2026-11-18'de görüldü).
- Kullanıcı başlatıcısı: proje kökündeki `PerioMeasure.bat` (çift tıklama)
  → tek PerioMeasure penceresi (Fiji açılmaz). Fiji'de
  `Plugins › PerioMeasure › Measure... / Demo...` aynı pencereyi açar.
- **Asla** tüm `java` süreçlerini toplu kapatan komut çalıştırılmaz
  (kullanıcının başka Java uygulamaları kapanabilir); yalnızca kendi
  başlatılan sürecin PID'i hedeflenir.

---

## 6. Çıktılar
- Anotasyonlu PNG (noktalar, çizgiler, değerler).
- CSV (UTF-8, ondalık ayırıcı ayarlanabilir; varsayılan nokta): hasta_id,
  göz, pre/post, tarih, dosya adı hash'i, kalibrasyon yöntemi, büyük/küçük
  eksen, eksen oranı, mm_per_px, tüm ölçümler ve belirsizlikleri, uyarılar,
  eklenti sürümü.
- Varsayılan çıktı klasörü `output/` (kullanıcı değiştirebilir).
- Landmark'lar ROI seti (.zip) olarak kaydedilir; yeniden açılıp
  düzenlenebilir; yeniden ölçüm aynı sonucu vermeli.
- Pre/Post karşılaştırma: iki CSV eşleştirilir; her ölçüm için Δ ve % değişim.
- Tekrar modu: aynı fotoğrafta 3 bağımsız işaretleme → ortalama, SD, %CV
  (%CV > 5 ise uyar); ICC analizine uygun uzun format.

---

## 7. Mimari
- `com.periomeasure.core` — **saf Java, ImageJ'ye bağımlı DEĞİL**: geometri,
  piksel konvansiyonu, elips oturtma, kalibrasyon, ölçümler, belirsizlik.
  Bir test, bu paketin `ij.*` import etmediğini denetler.
  Ana sınıflar: `Vec2`, `PixelConvention`, `Ellipse` (Eberly nokta–elips
  mesafesi), `EllipseFitter` (Halir–Flusser cebirsel → Levenberg–Marquardt
  geometrik, analitik Jacobian; Kåsa daire yedeği), `Calibration`, `Scale`,
  `ReferenceAxes`, `EyeLandmarks`, `MeasurementEngine`, `Quantity`
  (değer + σ_click + σ_cal), `MeasureId` (öncelik sırası: PFI, ICD, PFW,
  PFH, …), `Warning`/`WarningCode`. `ArchitectureTest` bu paketin `ij.*`,
  `java.awt`, `javax.*` import etmediğini denetler.
- `com.periomeasure.synthetic` — sentetik test görüntüsü üretici (ImageJ'siz;
  `java.awt.image`). `FaceModel` (mm, yüz düzlemi: X hastanın soluna, Y
  yukarı), `SyntheticScene` (aynalama → y çevirme → roll → ölçek → öteleme),
  `GroundTruth` (ölçüm motorunu KULLANMADAN, yüz düzleminde bağımsız
  beklenen değerler), `SyntheticRenderer` (etiket tam alan kapsamasıyla,
  8×8 süper örnekleme), `SyntheticImageWriter` (PNG + doğruluk CSV'si),
  `SyntheticImageTool` (standart set → `output/synthetic`).
- `com.periomeasure.io` — ImageJ'siz: `Settings` (+`PrefsStore`),
  `Formats` (ekran: TR virgül/EN nokta), `ResultCsv` (UTF-8 BOM, CRLF,
  RFC 4180 tırnaklama, formül enjeksiyonu koruması, virgül ondalıkta `;`
  ayırıcı; oturum dosyası + kümülatif dosya, başlık farklıysa yeni
  numaralı dosya), `MeasurementRecord`, `FileHash` (dosya ADI SHA-256),
  `CloudFolders`, `JpegQuality`, `ImageChecks`. (Faz 3: karşılaştırma,
  tekrar modu.)
- `com.periomeasure.session` — saf Java (ImageJ ve AWT YOK): `StepPlan`
  (kalibrasyon → [tek göz çizgi] → tüm pupiller → göz başına kapaklar,
  kantuslar, isteğe bağlılar), `MeasurementSession` (yerleştirme,
  sürükleme, geri al, ileri/geri/atla, canlı elips, hesaplama),
  `ClickPolicy` (tek tık yerleştirir, nokta üstüne basmak sürükler; adım
  dolunca kendiliğinden ilerler — etiket kenarı ve kontur "Devam" bekler),
  `ViewMapping`, `ZoomPlan` (artık kullanılmıyor; Faz 3'te kaldırılabilir).
- `com.periomeasure.i18n` — TR/EN metinler. Tek dosya:
  `src/main/resources/com/periomeasure/i18n/messages.properties`
  (`<dil>.<anahtar>=<metin>`, UTF-8). Anahtarlar `Msg` enum'unda; yeni metin
  = enum sabiti + iki dilde satır. `MessagesTest` eksik/fazla/tekrarlı
  anahtarı ve yer tutucu uyumsuzluğunu yakalar. `MessageFormat`
  KULLANILMAZ (Türkçe kesme işaretini yutar); `Messages.format` yalnızca
  `{0}` değiştirir.
- `com.periomeasure.ui` — tek pencereli uygulama (Swing) + Fiji menüsü:
  `PerioMeasureApp` (giriş noktası, tek pencere, `--selftest`),
  `MainWindow` (araç çubuğu, büyük görüntü, sağ panel, durum çubuğu,
  sürükle-bırak, dosya seçici Resimler klasöründe başlar), `AppModel`
  (SWING'SİZ tüm iş akışı: NO_IMAGE → SETUP → MEASURING → RESULTS, tek tık,
  otomatik ilerleme, seçili nokta, büyüteçle ince ayar, sürükleme, bitirme;
  `AppModelTest`), `ImageViewer` (tekerlek = yakınlaştır, boş alan sürükle =
  kaydır, nokta sürükle = taşı), `LoupeView` (4–8× büyüteç; imleç
  fotoğraftayken orayı, değilse seçili noktayı gösterir; içinde sürükleyerek
  1/zoom px ince ayar), `ViewState`, `ImagePyramid` (büyük fotoğraf için
  küçültülmüş kopyalar; yalnız görüntü), `ImagePainter`, `OverlayPainter`,
  `PhotoLoader` (ImageJ Opener; EXIF yönü uygulanır; HEIC açıklamalı ret),
  `WelcomePanel`, `SetupPanel`, `StepsPanel` (+`PanelState`),
  `ResultsPanel` (+`ResultsSummary`), `SettingsDialog`, `MeasurePlugin`,
  `DemoPlugin`, `SettingsPlugin`, `AboutPlugin`, `ImageJPrefsStore`,
  `SelfTest`.
- Çıktı klasörü: Ayarlar'da verilmişse o (bulutsa uyarı CSV'ye yazılır);
  verilmemişse fotoğrafın yanında `output`, ama fotoğraf bulut klasöründeyse
  `%USERPROFILE%\PerioMeasure\output` (sonuçlar buluta gitmesin). Deneme
  yüzleri: `%USERPROFILE%\PerioMeasure\demo-output`.
- Komutlar `src/main/resources/plugins.config` ile `Plugins > PerioMeasure`
  menüsüne kaydedilir.
- Sürüm tek kaynaktan: `pom.xml` → (resource filtering)
  `periomeasure-version.properties` → `Version.get()`.

### Komutlar
```
mvnw.cmd clean verify                 # derle + tüm testler + jar (target\PerioMeasure_.jar)
mvnw.cmd clean verify -Pfiji-runtime  # testleri Fiji'nin gömülü Java 8'inde çalıştır
```
Sentetik görüntü seti (Faz 2'de Fiji'de denemek için; `output/` git dışında):
```
"C:\Users\Lenovo\Fiji.app\java\win64\zulu8.86.0.25-ca-fx-jdk8.0.452-win_x64\bin\java.exe" "-Djava.awt.headless=true" -cp target\classes com.periomeasure.synthetic.SyntheticImageTool
```
(PowerShell'de `-D...` argümanı tırnak içinde olmalı, yoksa noktadan bölünür.)
Kurulum: `target\PerioMeasure_.jar` → `C:\Users\Lenovo\Fiji.app\plugins\`
(eski sürümün üzerine yazılır; jar adı sürüm içermez ki iki kopya oluşmasın).

Fiji içinde headless duman testi (klasik başlatıcı; yeni `fiji-windows-x64.exe`
headless modda konsola çıktı vermiyor):
```
ImageJ-win64.exe --headless --console -eval "run(\"Bilgi / About\");"
```
Bir fazı "bitti" saymadan önce: `clean verify` + `-Pfiji-runtime` ikisi de
geçmeli, jar Fiji'ye kurulup komut(lar) Fiji içinde çalıştırılmalı.

---

## 8. Durum

**Tamamlanan:** Faz 0 (2026-11-17) — Maven Wrapper (Maven 3.9.16, SHA-256
doğrulamalı), pom (sabit eklenti sürümleri, enforcer, `-Werror`, Java 8
hedef, `tr_TR` testler, `fiji-runtime` profili), .gitignore, .gitattributes,
rules.md (ilk adı CLAUDE.md), README taslağı, TR/EN metin altyapısı, `Bilgi / About` komutu,
13 test (JDK 25 ve Fiji Java 8'de geçiyor). Fiji stable kuruldu ve eklenti
Fiji içinde çalıştırılarak doğrulandı.

**Tamamlanan:** Faz 1 (2026-11-17) — saf Java hesaplama çekirdeği ve
sentetik görüntü üretici, TDD ile (her modülde önce başarısız test).
108 test, JDK 25 ve Fiji Java 8'de geçiyor. Uçtan uca: 162 senaryo
(3 yüz × 3 ölçek × 3 roll × 3 etiket eğikliği × aynalı/değil) kesin girdiyle
hata < 1e-6 mm (kabul ölçütü 0,05 mm). Gürültülü etiket tıklamalarında
(σ = 0,5 px, 24 nokta, 600 px etiket) raporlanan σ_cal gerçek hatayı ~%95
kapsıyor; en büyük IPD hatası < 0,15 mm. Mutasyon kontrolü yapıldı: lateral
yön işareti ve piksel köşe/merkez hatası bilerek eklendiğinde testler
yakaladı. README İngilizceye çevrildi.

**Tamamlanan:** Faz 2 (2026-11-18, sürüm 0.2.0) — Ayarlar, yeni ölçüm
diyaloğu, adım paneli, iki tıklamalı (yakınlaştır → kesin nokta) manuel
kalibrasyon ve landmark işaretleme, sürükleme, geri al, canlı elips,
pupil dikey kılavuz çizgisi, sonuç penceresi (ICD + PFI önce), panoya
kopyalama, CSV (oturum + kümülatif), görüntü kontrolleri (genişlik, JPEG,
dosya kalibrasyonu, bulut klasörü). 193 test JDK 25 ve Fiji Java 8'de
geçiyor; gerçek ImageJ tuvali ekran dışı boyanarak tıklama–görüntü–işaret
hizası piksel düzeyinde doğrulandı. Menü kayıtları Fiji'de (headless)
doğrulandı. Kullanıcı isteğiyle (2026-11-18) kolay deneme eklendi:
Demo komutu ve sonuçta doğrulama tablosu.

**Faz 2 arayüz yeniden tasarımı (2026-11-18, kullanıcı geri bildirimiyle):**
kullanıcı iki tıklamalı yakınlaştırmayı zahmetli, ayrı ImageJ pencerelerini
dağınık buldu ve gerçek fotoğrafı seçebilmek istedi. Yeni: tek pencere
(`PerioMeasure.bat` veya Fiji menüsü), tek tıkla yerleştirme + otomatik
ilerleme, 4–8× büyüteçte sürükleyerek ince ayar (spesifikasyondaki "her
tıklamada 4–8× büyütme" büyüteçle karşılanır), tekerlekle yakınlaştırma,
sürükleyerek kaydırma, dosya seçici (Resimler) + sürükle-bırak, EXIF yönü
ve çekim tarihi, HEIC uyarısı, bulut fotoğrafında yerel çıktı. 217 test
JDK 25 ve Fiji Java 8'de geçiyor; `PerioMeasureApp --selftest` gerçek
pencerede PASS (26 tık + büyüteç ince ayarı; 14/14 ölçüm 2σ içinde, en
büyük hata 0,008 mm); ekran görüntüleri incelendi. **Kullanıcının elle
denemesi bekleniyor.**

**Arayüz cilası (2026-11-18, kullanıcı geri bildirimiyle):** yazılar küçük,
düğmeler sıkışık, dil değiştirilemiyor, Java dosya seçicisinin düğmeleri
kesiliyordu. Yeni: FlatLaf (Fiji ile gelir, `flatlaf-3.5.1.jar`, `provided`;
başlatıcı sınıf yoluna ekler) ile aydınlık/karanlık tema (`Themes`, ayarda
`theme`), varsayılan yazı +3 pt ve yüksek DPI ölçekleme, turkuaz vurgu
(#1F7A8C), simgeli üst bar (`HeaderBar`: dil açılır listesi, tema düğmesi,
Ayarlar, Rehber, Hakkında), vektör simgeler (`Icons`), Swing ayarlar
penceresi (`SettingsDialog`), Windows'un yerel "Aç" penceresi (`FileDialog`),
ilk açılışta oklu adım adım rehber (`TourOverlay`, 10 durak; F1 / "Rehber";
`periomeasure.tourDone` tercihi). `SelfTest` artık rehberin her durağını,
tema ve dil değişimini de sınar ve ekran görüntüsü alır; kullanıcının tema,
dil ve rehber tercihlerini test sonunda geri yükler. 218 test JDK 25 ve
Fiji Java 8'de geçiyor; `--selftest` PASS (14/14, en büyük hata 0,007 mm).
Not: Fiji içinden açıldığında FlatLaf görünümü Fiji'nin Swing pencerelerine
de uygulanır.

**Yapıştırma, İngilizce varsayılan, uygulama simgesi (2026-11-18):**
`ClipboardPhoto` (dosya > yol metni > piksel; birden çok fotoğrafta ret),
Ctrl+V / Shift+Insert, üst barda ve karşılama kartında "Yapıştır" düğmesi,
sürükle-bırak aynı kuralla (bırakılan veri bırakma anında okunur).
`PhotoLoader.fromPasted` + `PASTED_IMAGE` uyarısı. Varsayılan dil İngilizce
(dil listesinde ilk sırada). Kodla çizilen uygulama simgesi `AppIcon`
(turkuaz kare, göz + ışık refleksi, sarı ölçüm çizgisi; 16–256 px; pencere,
görev çubuğu, `.ico`). `Create_Desktop_Shortcut.bat`: simgeyi
`%USERPROFILE%\PerioMeasure\PerioMeasure.ico` olarak üretir ve masaüstüne
`PerioMeasure.bat`'i (küçültülmüş) açan kısayol koyar; kullanıcı kendisi
çalıştırır. `SelfTest` sahte pano içeriğiyle piksel ve dosya yapıştırmayı
da sınar. 234 test JDK 25 ve Fiji Java 8'de geçiyor; `--selftest` PASS
(14/14, en büyük hata 0,007 mm; yapıştırma OK). Kısayol betiğinin komutları
geçici klasörde denendi (masaüstüne yazılmadı); kullanıcının çalıştırması
bekleniyor.

**Sıradaki:** Faz 3 — anotasyonlu PNG, ROI kaydet/aç, pre/post
karşılaştırma, tekrar modu.

**Faz 1'den öğrenilen (Faz 2 için önemli):** Uzun mesafelerin (IPD, ICD)
doğruluğunu çoğunlukla etiketin görüntüdeki piksel boyutu ve kenar tıklama
sayısı belirler. Etiket ≥ 600 px ve ≥ 16–24 kenar noktası önerilmeli.

**Bekleyen sorular / işler:**
- Kurallar dosyası kullanıcının kararıyla `rules.md` olarak kalıyor
  (2026-11-17). Claude Code bu adı otomatik okumaz; her oturum başta bu
  dosyayı açıkça okumalıdır.
- İsteğe bağlı ölçüm tanımlarının (TPS, kaş yüksekliği, SSS/ISS; bkz. 4.3)
  kullanıcı tarafından teyidi.
- Faz 2'nin pencereli Fiji'de kullanıcı tarafından denenmesi (sentetik
  görüntüler: `output/synthetic/*.png`, gerçek değerler `*_truth.csv`).
- Proje klasörü OneDrive altında (`OneDrive\Desktop\PerioMeasure`);
  `samples/` buraya konursa fotoğraflar buluta gider. Kullanıcıya taşıma
  adımları anlatıldı; hedef `C:\dev\PerioMeasure`. Taşınmadan gerçek
  fotoğrafla çalışılmaz.

---

## 9. Fazlar
- **Faz 0:** İskelet. ✅
- **Faz 1:** Hesaplama çekirdeği + testler (UI yok) + sentetik görüntü üretici. ✅
  (kullanıcının commit'i bekleniyor)
- **Faz 2:** Manuel kalibrasyon + landmark UI + PFW, PFH, MRD1, MRD2,
  PFH/PFW, kantal açı + CSV + Fiji kurulum talimatı. Sentetik görüntülerle
  denenebilir. ✅ (kullanıcı denemesi ve commit'i bekleniyor)
- **Faz 3:** Anotasyonlu PNG, ROI kaydet/aç, pre/post karşılaştırma, tekrar
  modu.
- **Faz 4:** Otomatik etiket algılama (önce sentetik). Faza gelince
  `samples/`'da gerçek fotoğraf var mı kontrol et; yoksa sor, ayar adımını
  bekleyen iş olarak buraya yaz.
- **Faz 5:** Doğrulama raporu (sentetik + cetvel fotoğrafları).
