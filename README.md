# PerioMeasure

A Fiji/ImageJ plugin for periocular measurements on facial photographs. The
clinician clicks the landmarks; the plugin computes the calibration and all
measurements deterministically (PFI, ICD, PFW, PFH, MRD1, MRD2, canthal angle
and more).

> ⚠️ **For research use only; not a medical device.** Do not use it on its own
> for clinical diagnosis or treatment decisions. Results must be confirmed by
> the researcher's own validation protocol (see below).

**Development status:** Phase 2 — manual calibration, landmark marking,
results window and CSV export. Automatic sticker detection (Phase 4),
annotated PNG / ROI sets / pre-post comparison / repeat mode (Phase 3) are
not yet available. The user interface is in English by default and can be
switched to Turkish in the header bar (the choice is remembered).

---

## 1. Installation

### Requirements
- **Fiji** (https://fiji.sc). Both the "stable" build (Java 8) and newer
  builds (Java 21) are supported; the plugin is compiled for Java 8.
- To build from source: **JDK 17 or newer**. You do **not** need to install
  Maven: the project ships the Maven Wrapper (`mvnw.cmd`), which downloads a
  pinned Maven version on first use and verifies it with SHA-256.

### Build
```bat
mvnw.cmd clean verify
```
If every test passes, `target\PerioMeasure_.jar` is produced. If any test
fails, no jar is produced.

### Install into Fiji
1. Close Fiji if it is running.
2. Copy `target\PerioMeasure_.jar` into Fiji's `plugins\` folder
   (e.g. `C:\Users\<user>\Fiji.app\plugins\`). When upgrading, **overwrite**
   the old file. The jar name has no version number, so two copies can never
   coexist.
3. Start Fiji. The menu **Plugins › PerioMeasure** contains **Measure...**,
   **Demo...**, **Settings...** and **About**. (Menu labels are in English because ImageJ
   cannot read Turkish letters in menu files reliably; all dialogs are in
   the selected language.) If **About** shows the version number,
   installation is complete.

---

## 2. Usage

### Starting PerioMeasure
- **Double-click `PerioMeasure.bat`** in the project folder: the PerioMeasure
  window opens in a few seconds (it uses Fiji's Java and ImageJ; Fiji itself
  does not open).
- **Desktop icon:** double-click `Create_Desktop_Shortcut.bat` once. It puts
  a **PerioMeasure** shortcut with the app icon on your desktop (it runs
  `PerioMeasure.bat`). Run it again if you move the project folder.
- Or in Fiji: **Plugins › PerioMeasure › Measure...** (an image open in Fiji
  is loaded automatically) or **Demo...**.

Everything happens in **one window**: a header bar on top (Open photo, Paste,
Demo face, view controls, **language EN/TR**, **light/dark theme**, Settings,
**Guide**, About), the photo large in the middle, guidance / form / results
on the right, a status line at the bottom.

On the very first start a **step-by-step guide** dims the window and points
with an arrow at each button and area, explaining it in plain words. Open it
again at any time with **Guide** (or F1).

### Opening a photo
- **Open photo…** opens Windows' own "Open" dialog in your *Pictures* folder
  (then the last folder used), or **drag the file onto the window**.
- **Paste (Ctrl+V):**
  - **Best:** in File Explorer right-click the photo file → *Copy*, then
    press Ctrl+V in PerioMeasure. The **original file** is opened (full
    resolution, EXIF orientation and date), exactly as with *Open photo*.
    A copied path ("Copy as path") works the same way.
  - **Copied image pixels** (e.g. *Copy image* in a viewer or browser) are
    used exactly as copied, but they **may be a reduced copy** and have no
    file name, date or orientation. PerioMeasure therefore always shows a
    warning, which is also written to the CSV. Open the original file
    whenever possible.
  - If **several photos** were copied, nothing is opened, so that the wrong
    photo can never be measured by accident: copy only one.
- JPEG, PNG, TIFF (also 16-bit), BMP. Phone photos are shown upright using
  their EXIF orientation, and the capture date is filled in automatically.
- iPhone **HEIC** files cannot be read: save them as JPEG
  (iPhone: Settings › Camera › Formats › Most Compatible).
- Windows desktop programs need no "gallery permission": the photo you choose
  is only read — never changed, never sent anywhere.

### Trying it out (no patient photo needed)
**Demo face ▾** offers four synthetic faces with known true values (normal;
tilted head with oblique sticker; right ptosis; mirrored asymmetric face).
Measuring starts at once; at the end a verification table shows, for every
measurement, the true value, the difference and whether the true value lies
within the reported uncertainty (✓/✗). Demo results go to
`%USERPROFILE%\PerioMeasure\demo-output`.

### Measuring
1. After opening a photo fill in the form on the right: patient (study)
   code, photo date (YYYY-MM-DD), eye (both / OD / OS), pre-op or post-op,
   optional measurements (lid crease, brow, limbus, fissure contour); for a
   single eye the horizontal reference. **Start measuring ▶**.
2. **Click each point once.** The point is placed and the next step follows
   automatically — no extra buttons.
3. **Fine-tuning:** the last point is shown in **yellow** and in the
   **loupe** (6×, adjustable 4–8×) at the right. Drag it inside the loupe for
   sub-pixel precision. You can also drag any point on the photo.
4. **Mouse wheel** zooms at the cursor, **dragging empty space** pans,
   **Fit** shows the whole photo. **Undo** (Ctrl+Z), **◀ Previous**.
5. **Calibration sticker:** click at least 8 points around the edge
   (16–24 recommended), then **Continue ▶**. From 5 points the fitted ellipse
   and its diameter / axis ratio / RMS are shown live.
6. **Landmarks:** both pupils first (so the measurement axes are known), then
   per eye upper lid, lower lid, medial and lateral canthus. While marking
   lid points a dashed orange line through the pupil shows the exact vertical
   along which MRD1 / MRD2 / PFH are measured.
7. **Finish and compute**: the results appear on the right — ICD and PFI
   (with PFH / PFW) first, then the OD / OS / OD − OS table, calibration,
   warnings and the saved files. **Copy to clipboard** pastes into Excel.

The original pixels are never changed; all marks are drawn on top.

### Settings
Language and theme are switched directly in the header bar and remembered.
**Settings** (header bar, or Plugins › PerioMeasure › Settings... in Fiji):
- **Calibration sticker diameter** (default 24.0 mm).
- **CSV decimal separator**: point (default; fields separated by commas) or
  comma (fields separated by semicolons, as Turkish Excel expects).
- **Output folder**: empty = an `output` folder next to the photo; if the
  photo is in a cloud-synchronised folder (e.g. OneDrive), results go to
  `%USERPROFILE%\PerioMeasure\output` instead so they stay on this computer.
- **Loupe magnification**: 4–8× (default 6×).

### Automatic check (developers)
The app can drive its own window — one click on the photo and one in the
loupe per point — and compare the results with the true values:
```bat
"%USERPROFILE%\Fiji.app\java\win64\<jdk>\jre\bin\java.exe" -cp "%USERPROFILE%\Fiji.app\jars\ij-1.54p.jar;%USERPROFILE%\Fiji.app\jars\flatlaf-3.5.1.jar;%USERPROFILE%\Fiji.app\plugins\PerioMeasure_.jar" com.periomeasure.ui.PerioMeasureApp --selftest report.txt
```
It also pastes test content (copied pixels and a copied file) through a
simulated clipboard; your real clipboard is never read or changed.

The app icon is drawn in code (`AppIcon`); `... com.periomeasure.ui.AppIcon
<file.ico> [preview.png]` writes the Windows icon and a preview picture.

### Output files
In the output folder, for every measurement:
- `PM_<code>_<PRE|POST>_<photo date>_<time>.csv` — this measurement only;
- `periomeasure_results.csv` — all measurements, one after another (a new
  numbered file is started automatically if the column layout or decimal
  separator ever changes, so files are never mixed).

One row per eye plus an `OD-OS` row. Columns: plugin version, time, patient
code, eye, pre/post, photo date, SHA-256 of the file name, image size,
calibration details (method, sticker diameter, axes, ratio, RMS, points,
mm/px and its relative uncertainty), horizontal reference, then every
measurement with its total, click and calibration uncertainty, and the
warnings. Files are UTF-8 (with BOM, so Excel shows Turkish letters).

### Main measurements

| Abbreviation | Measurement | Definition |
|---|---|---|
| PFI | Palpebral fissure index | PFH / PFW (dimensionless ratio) |
| ICD | Intercanthal distance | Euclidean distance between the two medial canthi |
| PFW | Palpebral fissure width | \|LC − MC\| |
| PFH | Palpebral fissure height | Upper to lower lid margin, along the vertical axis |
| MRD1 | Margin–reflex distance 1 | Pupil centre to upper lid margin (signed) |
| MRD2 | Margin–reflex distance 2 | Pupil centre to lower lid margin |
| Canthal angle | Canthal tilt | Angle of the MC→LC line to the interpupillary axis; positive if LC is higher |

PFI is shown as a ratio together with the lengths it is computed from, e.g.
`PFI 0.35 ± 0.01 (PFH 9.80 mm / PFW 28.00 mm)`.

Every length is shown with a single uncertainty, e.g. `PFW 28.41 ± 0.07 mm`.
It combines two sources: click precision (1 pixel) and the precision with
which the calibration sticker was measured. The individual components are in
the CSV.

The exact mathematical definitions are in section 4 of `rules.md`.

---

## 3. Photography protocol

A pre-op / post-op comparison is valid only if the photographs are taken
under the same conditions.

- **Distance:** ≥ 1 m (reduces perspective distortion).
- **Focal length:** ≥ 85 mm (35 mm equivalent). Do not use wide-angle lenses
  or a phone's front camera.
- **Head position:** Frankfort horizontal plane parallel to the floor; head
  neither turned nor tilted.
- **Gaze:** primary position, looking straight ahead; camera at eye level.
- **Calibration sticker:** circular, 24.0 mm diameter, **matte**
  (non-reflective), in a colour that contrasts with skin; placed on the
  forehead midline, roughly in the same plane as the eyes, without wrinkles.
- **Lighting:** even and shadow-free; the corneal light reflex must be
  visible.
- **Resolution:** image width ≥ 2000 px; sticker diameter in the image
  ≥ 150 px. Highest JPEG quality or a lossless format (TIFF/PNG).
- **Pre/post:** same camera, lens, distance, lighting and setup.

---

## 4. Data privacy and anonymisation

- Patient photographs must be stored **on the local computer only**. Do
  **not** put them in cloud-synchronised folders (OneDrive, Google Drive,
  Dropbox, iCloud). If this project folder is inside such a folder, keep the
  photographs in a different local folder.
- Do not use patient names or national ID numbers in file names; use a study
  code (e.g. `P017_pre_2026-03-02.jpg`). Keep the code ↔ identity key list
  separate and encrypted.
- Remove EXIF metadata (date, GPS, camera serial number) before sharing
  photographs.
- The plugin's CSV stores a **hash** of the file name instead of the file
  name itself.
- Patient data (photographs, CSV, ROI, PNG) is **never** added to this code
  repository; `.gitignore` keeps it out.

---

## 5. Validation protocol (draft; reported in Phase 5)

1. **Synthetic validation (automatic):** generated images with a 24 mm circle
   at a known scale and landmarks at known positions, at various rotations,
   tilts and resolutions. Acceptance criterion: end-to-end error < 0.05 mm.
   Runs on every build with `mvnw.cmd verify`.
2. **Phantom / ruler photograph:** a millimetre ruler or a pattern of known
   dimensions placed in the sticker plane is photographed with the clinical
   setup; known distances are measured and compared with their true values.
3. **Intra-observer repeatability:** 3 independent markings of the same
   photograph (repeat mode); %CV < 5.
4. **Inter-observer reliability:** ≥ 2 observers; ICC (from the long-format
   output of repeat mode).
5. **Method comparison:** Bland–Altman analysis against conventional manual
   ImageJ measurement.

---

## 6. Developer notes

```bat
mvnw.cmd clean verify                 :: build + all tests + jar
mvnw.cmd clean verify -Pfiji-runtime  :: run the tests on Fiji's own Java 8
```
- Tests run with a Turkish default locale (`tr_TR`) so that decimal-comma
  bugs are caught.
- Compiler warnings are errors; all Maven plugin versions are pinned.
- Project rules and measurement definitions: [`rules.md`](rules.md).
