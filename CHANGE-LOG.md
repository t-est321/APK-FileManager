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
