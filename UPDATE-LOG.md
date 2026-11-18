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
