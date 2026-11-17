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
- **Arayüz dili:** Türkçe (varsayılan) ve İngilizce. Tüm arayüz metinleri tek
  bir kaynak dosyada; bir test her anahtarın iki dilde de bulunduğunu
  doğrular.
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
- İsteğe bağlı: TPS, kaş yüksekliği, superior/inferior skleral show, sağ–sol
  farkları.

### 4.4 Belirsizlik
- Her uzunluk L için iki bileşen ayrı ayrı raporlanır:
  - Tıklama: `σ_click = 1 px × mm_per_px`
  - Kalibrasyon: `σ_cal = L × (σ_büyük_eksen / büyük_eksen)`
- Birleşik (onaylandı): `σ = √(σ_click² + σ_cal²)`.
- **Arayüzde yalnızca birleşik değer** gösterilir (ör. `PFW 28,41 ± 0,07 mm`);
  bileşenler (`σ_click`, `σ_cal`, `σ`) CSV'de ayrı sütunlardır.
- PFI (oran) belirsizliği: `σ_PFI = PFI × √((σ_PFH/PFH)² + (σ_PFW/PFW)²)`,
  yalnızca tıklama bileşenleriyle (kalibrasyon ölçeği oranda sadeleşir).

---

## 5. Bilinen ImageJ tuzakları (kaçın)
- Global "Set Scale" kullanılmaz; ölçek her görüntü için eklentinin kendi
  durumunda tutulur. Görüntü dosyasındaki kalibrasyon (TIFF/EXIF) yok
  sayılır ve kullanıcı uyarılır.
- Orijinal piksel verisi asla değiştirilmez; tüm işaretlemeler overlay'dir.
- Piksel koordinat konvansiyonu tek yerde (bkz. 4.1).
- Görüntü genişliği < 2000 px veya JPEG kalitesi düşükse uyar.

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
- `com.periomeasure.synthetic` — sentetik test görüntüsü üretici (saf Java /
  `java.awt.image`).
- `com.periomeasure.io` — CSV, karşılaştırma, tekrar modu dışa aktarımı.
- `com.periomeasure.i18n` — TR/EN metinler. Tek dosya:
  `src/main/resources/com/periomeasure/i18n/messages.properties`
  (`<dil>.<anahtar>=<metin>`, UTF-8). Anahtarlar `Msg` enum'unda; yeni metin
  = enum sabiti + iki dilde satır. `MessagesTest` eksik/fazla/tekrarlı
  anahtarı ve yer tutucu uyumsuzluğunu yakalar. `MessageFormat`
  KULLANILMAZ (Türkçe kesme işaretini yutar); `Messages.format` yalnızca
  `{0}` değiştirir.
- `com.periomeasure.ui` — ImageJ/Fiji eklentileri (`ij.plugin.PlugIn`).
- Komutlar `src/main/resources/plugins.config` ile `Plugins > PerioMeasure`
  menüsüne kaydedilir.
- Sürüm tek kaynaktan: `pom.xml` → (resource filtering)
  `periomeasure-version.properties` → `Version.get()`.

### Komutlar
```
mvnw.cmd clean verify                 # derle + tüm testler + jar (target\PerioMeasure_.jar)
mvnw.cmd clean verify -Pfiji-runtime  # testleri Fiji'nin gömülü Java 8'inde çalıştır
```
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

**Sıradaki:** Faz 1 — hesaplama çekirdeği + testler + sentetik görüntü üretici.

**Bekleyen sorular / işler:**
- Kurallar dosyası kullanıcının kararıyla `rules.md` olarak kalıyor
  (2026-11-17). Claude Code bu adı otomatik okumaz; her oturum başta bu
  dosyayı açıkça okumalıdır.
- Kullanıcı `git init` yapacak.
- Proje klasörü OneDrive altında (`OneDrive\Desktop\PerioMeasure`);
  `samples/` buraya konursa fotoğraflar buluta gider. Kullanıcıya taşıma
  adımları anlatıldı; hedef `C:\dev\PerioMeasure`. Taşınmadan gerçek
  fotoğrafla çalışılmaz.

---

## 9. Fazlar
- **Faz 0:** İskelet. ✅ (kullanıcının Fiji'de denemesi ve commit'i bekleniyor)
- **Faz 1:** Hesaplama çekirdeği + testler (UI yok) + sentetik görüntü üretici.
- **Faz 2:** Manuel kalibrasyon + landmark UI + PFW, PFH, MRD1, MRD2,
  PFH/PFW, kantal açı + CSV + Fiji kurulum talimatı. Sentetik görüntülerle
  denenebilir.
- **Faz 3:** Anotasyonlu PNG, ROI kaydet/aç, pre/post karşılaştırma, tekrar
  modu.
- **Faz 4:** Otomatik etiket algılama (önce sentetik). Faza gelince
  `samples/`'da gerçek fotoğraf var mı kontrol et; yoksa sor, ayar adımını
  bekleyen iş olarak buraya yaz.
- **Faz 5:** Doğrulama raporu (sentetik + cetvel fotoğrafları).
