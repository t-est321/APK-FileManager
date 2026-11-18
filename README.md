# File Manager

> The app icon update is coming soon

Lightweight Android file manager. Java 7, no XML layouts, no AppCompat.

**Package:** `com.dlna.FileManager`  
**Version:** 4.1.28 (versionCode 68)  
**SDK:** min 19 / target 29

---

## Downloads (APK)

| File | What it is |
|---|---|
| [`fileManager_016app.apk`](bin/fileManager_016app.apk) | **Signed** APK — install on a device |
| [`fileManager_016app-unsigned.apk`](bin/fileManager_016app-unsigned.apk) | **Unsigned** APK — sign yourself before install |

---

## Features

- Browse storage, tabs, bookmarks, search (`*` glob)
- Copy / move / rename (batch) / delete (trash)
- ZIP, TAR, TAR.GZ — pack, browse in-place, extract
- HashMap `.dat` — pack, browse (`file.dat#/…`), edit entries, unpack
- Text / hex editor, markdown viewer, HTML viewer
- Images (pinch-zoom, GIF, next/prev), video/audio
- APK install from the row menu (user action only)
- Built-in shell (cd, ls, cat, cp, mv, rm, mkdir, …)
- Dark / light theme, EN / RU
- Custom Dark / light theme 
- Settings saved on Back; **Back without save** discards changes
- Backup export/import under `APP_DATA/com.dlna.FileManager/`
- USB / MicroSD via `StorageManager.getStorageVolumes()` and `getExternalFilesDirs`

---

## Layout

```
AndroidManifest.xml
src/com/dlna/FileManager/   Java sources
res/                        App icon
README.md
```

UI is built in code (`MainActivity`, `Ui`, …). There are no `res/layout` XML files.

---

## Permissions

| Permission | Use |
|---|---|
| `READ/WRITE_EXTERNAL_STORAGE` | File access (`requestLegacyExternalStorage`) |
| `REQUEST_INSTALL_PACKAGES` | Install APK from the **Install** menu |
| `INTERNET` | HTML downloads (confirm dialog) |

---

## Build

Sources compile with `javac` against **Maven Android API stubs** (API 19–29). No Android SDK or Gradle project is included.

1. Fetch the API 29 stubs JAR (`org.robolectric:android-all:10-robolectric-5803371`) from Maven and put all `*.java` on the classpath with it.
2. Package compiled DEX classes + `AndroidManifest.xml` + `res/` into an APK.
3. Sign → in the `bin/` folder `fileManager_016app.apk`. Leave a copy unsigned → `fileManager_016app-unsigned.apk`.

Alternatively, you can build the application directly via **Code Studio** (available on Google Play).

`minSdkVersion` 19, `targetSdkVersion` 29.

---

## Usage

- **Tap** — select (75 ms delay)
- **Double-tap** — open
- **⋮** — commands for one item or the selection
- Path bar — type a path; long-press — Settings
- Pull down at the top of the list — refresh

Settings, tabs, bookmarks and notes live in the app files dir. Public export:

`/storage/emulated/0/APP_DATA/com.dlna.FileManager/`

---

## Language

Settings → **Language: EN / RU**. Identifiers in source are English.

---

## License

You can modify java files, but you can't publish this app.
