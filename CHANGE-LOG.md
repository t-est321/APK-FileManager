## 4.2.2 (71)

- Notes button on the New File / New Folder action panel; Hide Notes in settings.
- Files from other apps open in RAM (disk Incoming only if too large).
- Archive view uses the RAM cache and no longer writes a zip-open file.
- Stats: `[ RAM ] Used` is process PSS; `[ Package ]` removed.
- Heap still shows Used / Free.
- Stats Storage no longer lists External.
- Hide Info also hides Info on multi-select (including zip).
- Multi-select Info no longer shows heap / RAM.
- Confirm before closing a tab or removing a bookmark; Disable tab / bookmark delete warning (off by default).
- Pull-to-refresh is a centered chip, not a full-width bar.
- Dead `hintRow` field and always-true `vis(...)` calls removed.
- Close-tab confirm skipped on the last tab; closing a tab left of the current one keeps the same folder.
- Incoming video/audio skip RAM; `file://` opens the path in place.
- RAM cache keys are unique; eviction is LRU (`LinkedHashMap`), not `HashMap` iteration order.
- RAM open falls back to disk for html/md, video/audio, binary (unless Open binary), and images when media preview is off.
- Editor status uses RAM byte length; Save on a RAM-only buffer writes Incoming and toasts Saved.
- Open dialogs are dismissed only for the destroying activity.
- `FileOps.progCb` is cleared only by the job that set it.
- Stats PSS is gathered off the UI thread.
- Pull-to-refresh ignores a few pixels of list overscroll.

## 4.2.1 (70)

- Batch rename: replace whole name, find/replace, prefix, suffix, extension.
- Disable system probes also skips `/sys` and does not run `Probe.forPath` on those trees.
- Hide Info on the file menu.
- Heap Used / Free and a single RAM figure in Info and Stats; hide from Optimization (`optNoHeap`).
- Archive file view cached in RAM (bounded).
- Dead `lowRam` flag removed.
- Zip open no longer re-extracts when the RAM cache hits.

## 4.2.0 (69)

- Custom chrome colors off by default (`useCustomChrome = false`).
- HTML zoom off by default (`htmlZoom = false`).
- Hide Log button on by default (`hideLog = true`).
- **Disable verified** removed (`optNoVerified` / `/sys` probe skip).
- Clear Incoming / ZipOpen / OpenAs on exit removed.
- Dead Lang keys removed (`Show details`, `Show folder counts`, `Dark`, `Light`).
- Dead compact ternary `4 : 4` removed.
- Duplicate chrome color rows collapsed; accent/folder pickers merged.
- Custom chrome colors stored as dark/light arrays (`chromeD` / `chromeL`).
- Open dialogs dismissed on activity destroy (Settings, Terminal, Trash, and other screens).

## 4.1.28 (68)

- App uploaded
