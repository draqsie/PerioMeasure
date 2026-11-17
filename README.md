# PerioMeasure

A Fiji/ImageJ plugin for periocular measurements on facial photographs. The
clinician clicks the landmarks; the plugin computes the calibration and all
measurements deterministically (PFI, ICD, PFW, PFH, MRD1, MRD2, canthal angle
and more).

> ⚠️ **For research use only; not a medical device.** Do not use it on its own
> for clinical diagnosis or treatment decisions. Results must be confirmed by
> the researcher's own validation protocol (see below).

**Development status:** Phase 1 complete (calculation core, fully tested,
with a synthetic image generator). The menu currently contains only the
`Bilgi / About` command; the measurement interface arrives in Phase 2. The user interface defaults to Turkish and can be switched to
English.

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
3. Start Fiji and choose **Plugins › PerioMeasure › Bilgi / About**. If the
   window shows the version number, installation is complete.

---

## 2. Usage (to be completed in Phase 2)

1. Open the photograph. Enter patient ID, date, eye (OD / OS / both) and
   pre-op / post-op.
2. **Calibration:** a circular sticker of 24.0 mm diameter on the forehead
   midline.
   - Automatic (recommended): draw a rough rectangle around the sticker.
   - Manual: click at least 8 points on the sticker edge.
3. **Landmarks:** for each eye, following the on-screen instructions: pupil
   centre (or corneal light reflex), upper lid margin, lower lid margin,
   medial canthus, lateral canthus. Points can be dragged to correct them;
   Undo is available.
4. **Results:** table + CSV (+ annotated PNG and ROI set in Phase 3).

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
