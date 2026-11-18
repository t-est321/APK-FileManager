## 4.1.28 (68)

- **Disable logging** is on by default (`optNoLog = true`).
- **Disable Info SHA256** (`optNoSha256`): skip SHA-256 in Info.
- SHA-256 is skipped on virtual zip / `.dat` paths.
- Hex viewer removed (`openHex`, editor hex). Binary files toast `Binary file`.
- Add picker starts at `/storage/emulated/0`.
- Dedicated archive bottom bar removed. The normal buttons work inside zip / tar / `.dat`:
  - New File / New Folder — create an entry in the archive
  - Copy — extract out
  - Move — extract then delete from the archive
  - Delete — remove from the archive (not Trash)
- Version was not bumped after this.

## 4.1.27 (67)

- Button drawing and palette restored to **4.1.1** (black `BG`, old `SURFACE` / `STROKE`).
- Settings buttons use `SURFACE_HI` again, not `cardBg` / `BTN_FACE`.
- Custom chrome fields were **not** reset.
- **Get USB IDs (demo)** — `(demo)` on the button and in help.
- Leave zip/`.dat`: `ZipBrowse.clear` / `MapBrowse.clear`.
- `MainActivity.onDestroy`: `listView.setAdapter(null)`.
- HTML download holds a `WeakReference` to the activity.
- Viewer: recycle bitmaps, drop progress callbacks on destroy.

## 4.1.26 (66)

- Leak fixes: dialogs/handlers, HTML, thumbs, map cache, editor, markdown bitmaps, ops log.
- `MainActivity.onDestroy`: `removeCallbacksAndMessages`, clear zip/map/thumbs.
- `MdActivity`: recycle bitmaps on destroy.
- `SoftBmp`: recycle only in `clear()`, not when the eldest entry is dropped.
- Dead code and duplicates removed.
- `Theme`: unused `fieldBg` / `btnBg` removed.

## 4.1.25 (65)

- New custom chrome **field defaults** (not only UI labels):
  - `useCustomChrome = true`
  - dark: bg `000000`, text `EDEDED`, surface `080423`, stroke/sel `050218`
  - light: bg `E9E9E9`, text `000000`, surface `FFFFFF`, stroke `808080`, sel `C9C0FF`
  - `customFolder = 0xFFB99737`

## 4.1.24 (64)

- Custom chrome split for dark / light: background, text, surface, outline, selection.
- **Custom button** color field removed.
- `Theme.paintChrome`, separate selection colors.

## 4.1.23 (63)

- Custom accent and folder colors (hex RRGGBB); **Custom** item in the accent/folder menus.
- Custom chrome: background, text, surface, button.
- New theme palette (`BG` `0F0F1A`, `SURFACE` `1A1A2E`); `cardBg` / `parseHex` / `toHex`.
- **Compress image display** — smaller decode cap in thumbs and the image viewer.
- Editor find: current hit amber, other hits cyan; hex view in a `ScrollView`.
- Copy/Move overwrite confirm only when the destination already has that name; No skips overwrite.
- USB volumes via `StorageManager.getStorageVolumes()` (API 24+) and `getExternalFilesDirs`; `/proc/mounts` scan dropped.
- 
