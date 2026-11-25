package com.dlna.FileManager;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
public final class EditorActivity extends Activity {
  private File file;
  private String archivePath;
  private String archiveInner;
  private String ramKey;
  private long ramLen;
  private EditText editor;
  private TextView gutter;
  private TextView status;
  private Button saveBtn;
  private LinearLayout findRow;
  private EditText findBox;
  private boolean loaded = false;
  private boolean dirty = false;
  private boolean wrap;
  private final ArrayList<String> undo = new ArrayList<String>();
  private final ArrayList<String> redo = new ArrayList<String>();
  private boolean rec = true;
  private int findFrom;
  private int findHit = -1;
  private ViewTreeObserver.OnScrollChangedListener scrollL;
  private boolean truncated;
  private static final int HUGE_CHARS = 200000;
  private static final int UNDO_MAX = 8;
  private static final int UNDO_CHARS = 200000;
  private static final long MAX_LOAD = 2097152L;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(savedInstanceState);
    String path = getIntent().getStringExtra("path");
    file = new File(path == null ? "" : path);
    archivePath = getIntent().getStringExtra("archive");
    archiveInner = getIntent().getStringExtra("inner");
    ramKey = getIntent().getStringExtra("ram");
    if (ramKey != null && ramKey.length() == 0) {
      ramKey = null;
    }
    if (archivePath != null && archivePath.length() == 0) {
      archivePath = null;
    }
    if (archiveInner != null && archiveInner.length() == 0) {
      archiveInner = null;
    }
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    wrap = AppSettings.get().editorWordWrap;
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Theme.BG);
    int m = Theme.dp(this, Theme.compact(this) ? 6 : 8);
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setBackgroundColor(Theme.SURFACE);
    row.setPadding(m, m / 2, m, m / 2);
    TextView title = new TextView(this);
    title.setTextColor(Theme.TEXT);
    title.setText(file.getName());
    title.setTextSize(13);
    title.setSingleLine(true);
    title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
    row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    row.addView(tool("Undo", new View.OnClickListener() {
      @Override
      public void onClick(View v) { undo(); }
    }));
    row.addView(tool("Redo", new View.OnClickListener() {
      @Override
      public void onClick(View v) { redo(); }
    }));
    row.addView(tool("Find", new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        findRow.setVisibility(findRow.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        if (findRow.getVisibility() == View.VISIBLE) {
          findBox.requestFocus();
        }
      }
    }));
    saveBtn = Ui.btn(this, "Save", Theme.TEXT_DIM);
    saveBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (dirty) {
          saveNow();
        } else {
          Ui.toast(EditorActivity.this, "No changes");
        }
      }
    });
    row.addView(saveBtn);
    root.addView(row);
    findRow = new LinearLayout(this);
    findRow.setGravity(Gravity.CENTER_VERTICAL);
    findRow.setPadding(m, 0, m, m / 2);
    findRow.setVisibility(View.GONE);
    findBox = new EditText(this);
    findBox.setSingleLine(true);
    findBox.setHint("Find");
    findBox.setHintTextColor(Theme.TEXT_DIM);
    findBox.setTextColor(Theme.TEXT);
    findBox.setBackground(Theme.rounded(this, Theme.SURFACE_HI, 8));
    int fp = Theme.dp(this, 8);
    findBox.setPadding(fp, fp, fp, fp);
    findBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
      @Override
      public void onTextChanged(CharSequence s, int a, int b, int c) {}
      @Override
      public void afterTextChanged(Editable s) {
        highlightFind();
      }
    });
    findRow.addView(findBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    Button next = Ui.btn(this, "Next", Theme.ACCENT);
    next.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { findNext(); }
    });
    findRow.addView(next);
    root.addView(findRow);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.HORIZONTAL);
    gutter = new TextView(this);
    gutter.setTypeface(Typeface.MONOSPACE);
    gutter.setTextColor(Theme.TEXT_DIM);
    gutter.setTextSize(AppSettings.FONT_SP[AppSettings.get().editorFont]);
    gutter.setGravity(Gravity.RIGHT);
    gutter.setPadding(Theme.dp(this, 6), m, Theme.dp(this, 6), m);
    gutter.setBackgroundColor(Theme.SURFACE);
    if (!AppSettings.get().editorLineNo) {
      gutter.setVisibility(View.GONE);
    }
    editor = new EditText(this);
    editor.setTextColor(Theme.TEXT);
    editor.setBackgroundColor(Theme.BG);
    editor.setGravity(Gravity.TOP | Gravity.LEFT);
    editor.setTypeface(Typeface.MONOSPACE);
    editor.setTextSize(AppSettings.FONT_SP[AppSettings.get().editorFont]);
    editor.setHorizontallyScrolling(!wrap);
    editor.setFocusable(true);
    editor.setFocusableInTouchMode(true);
    editor.setEnabled(true);
    editor.setCursorVisible(true);
    editor.setPadding(m, m, m, m);
    editor.setVerticalScrollBarEnabled(false);
    editor.setHorizontalScrollBarEnabled(false);
    editor.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        if (loaded && rec && !truncated && s.length() < HUGE_CHARS) {
          pushUndo(s.toString());
        }
      }
      @Override
      public void onTextChanged(CharSequence s, int a, int b, int c) {
        if (loaded) {
          dirty = true;
          updateSaveBtn();
        }
      }
      @Override
      public void afterTextChanged(Editable s) {
        if (loaded) {
          if (!truncated && s.length() < HUGE_CHARS) {
            refreshGutter();
          }
          highlightFind();
          updateStatus();
        }
      }
    });
    editor.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { updateStatus(); }
    });
    scrollL = new ViewTreeObserver.OnScrollChangedListener() {
      @Override
      public void onScrollChanged() {
        if (gutter != null && editor != null) {
          gutter.scrollTo(0, editor.getScrollY());
        }
      }
    };
    editor.getViewTreeObserver().addOnScrollChangedListener(scrollL);
    if (wrap) {
      body.addView(gutter, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
      body.addView(editor, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
      root.addView(body, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    } else {
      HorizontalScrollView hsv = new HorizontalScrollView(this) {
        float ax;
        float ay;
        boolean decided;
        boolean horiz;
        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
          int a = e.getActionMasked();
          if (a == MotionEvent.ACTION_DOWN) {
            ax = e.getX();
            ay = e.getY();
            decided = false;
            horiz = false;
            super.onInterceptTouchEvent(e);
            return false;
          }
          if (a == MotionEvent.ACTION_MOVE && !decided) {
            float dx = Math.abs(e.getX() - ax);
            float dy = Math.abs(e.getY() - ay);
            int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (dx > slop || dy > slop) {
              decided = true;
              horiz = dx > dy;
            }
          }
          if (a == MotionEvent.ACTION_CANCEL || a == MotionEvent.ACTION_UP) {
            decided = false;
            horiz = false;
          }
          if (!horiz) {
            return false;
          }
          return super.onInterceptTouchEvent(e);
        }
      };
      hsv.setHorizontalScrollBarEnabled(false);
      hsv.setVerticalScrollBarEnabled(false);
      body.addView(gutter, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
      body.addView(editor, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
      hsv.addView(body);
      root.addView(hsv, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    }
    status = new TextView(this);
    status.setTextColor(Theme.TEXT_DIM);
    status.setTextSize(11);
    status.setTypeface(Typeface.MONOSPACE);
    status.setPadding(m, Theme.dp(this, 4), m, Theme.dp(this, 6));
    status.setBackgroundColor(Theme.SURFACE);
    root.addView(status);
    setContentView(root);
    Ui.bar(this);
    Ui.bg(this, "Loading...", new Runnable() {
      @Override
      public void run() {
        try {
          long cap = Math.max(MAX_LOAD, AppSettings.get().openLimit());
          byte[] ram = ZipBrowse.getRam(ramKey);
          final boolean cut;
          String raw;
          if (ram != null) {
            ramLen = ram.length;
            cut = ram.length > cap;
            int n = ram.length > cap ? (int) cap : ram.length;
            raw = new String(ram, 0, n, "UTF-8");
          } else {
            ramLen = file.length();
            cut = file.length() > cap;
            raw = FileOps.readStringLimit(file, cap);
          }
          if (AppSettings.get().editorSpaces) {
            raw = expandTabs(raw, AppSettings.get().tabSpaces());
          }
          final String text = raw;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                loaded = false;
                rec = false;
                truncated = cut;
                editor.setText(text);
                if (editor.length() > 0) {
                  editor.setSelection(0);
                }
                rec = true;
                loaded = true;
                dirty = false;
                undo.clear();
                redo.clear();
                if (truncated || text.length() >= HUGE_CHARS || !AppSettings.get().editorLineNo) {
                  gutter.setVisibility(View.GONE);
                }
                editor.setEnabled(!truncated);
                editor.setFocusable(!truncated);
                editor.setFocusableInTouchMode(!truncated);
                editor.setCursorVisible(!truncated);
                updateSaveBtn();
                if (!truncated && text.length() < HUGE_CHARS && AppSettings.get().editorLineNo) {
                  refreshGutter();
                }
                updateStatus();
                if (cut) {
                  Ui.toast(EditorActivity.this, "Loaded first " + FileOps.humanSize(MAX_LOAD));
                }
              } catch (Throwable e) {
                OpLog.err(EditorActivity.this, "editor show", e);
                Ui.toast(EditorActivity.this, "Cannot open text");
                finish();
              }
            }
          });
        } catch (Exception e) {
          OpLog.err(EditorActivity.this, "editor read", e);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(EditorActivity.this, "Read error");
              finish();
            }
          });
        }
      }
    }, null);
  }
  private Button tool(String label, View.OnClickListener l) {
    Button b = Ui.btn(this, label, Theme.TEXT);
    b.setOnClickListener(l);
    return b;
  }
  private void pushUndo(String snap) {
    if (snap == null) {
      return;
    }
    if (undo.size() > 0 && snap.equals(undo.get(undo.size() - 1))) {
      return;
    }
    undo.add(snap);
    redo.clear();
    trimHist();
  }
  private int histChars() {
    int n = 0;
    int i;
    for (i = 0; i < undo.size(); i++) {
      n += undo.get(i).length();
    }
    for (i = 0; i < redo.size(); i++) {
      n += redo.get(i).length();
    }
    return n;
  }
  private void trimHist() {
    while (undo.size() > UNDO_MAX) {
      undo.remove(0);
    }
    while (redo.size() > UNDO_MAX) {
      redo.remove(0);
    }
    while (histChars() > UNDO_CHARS && undo.size() > 1) {
      undo.remove(0);
    }
    while (histChars() > UNDO_CHARS && redo.size() > 0) {
      redo.remove(0);
    }
  }
  private void applyHist(ArrayList<String> from, ArrayList<String> to, String offMsg, String emptyMsg) {
    if (truncated || editor.getText().length() >= HUGE_CHARS) {
      Ui.toast(this, offMsg);
      return;
    }
    if (from.size() == 0) {
      Ui.toast(this, emptyMsg);
      return;
    }
    rec = false;
    to.add(editor.getText().toString());
    trimHist();
    String next = from.remove(from.size() - 1);
    editor.setText(next);
    editor.setSelection(next.length());
    rec = true;
    dirty = true;
    updateSaveBtn();
    refreshGutter();
    updateStatus();
  }
  private void undo() {
    applyHist(undo, redo, "Undo off for large files", "Nothing to undo");
  }
  private void redo() {
    applyHist(redo, undo, "Redo off for large files", "Nothing to redo");
  }
  private void findNext() {
    String q = findBox.getText().toString();
    if (q.length() == 0) {
      return;
    }
    String text = editor.getText().toString();
    int at = text.indexOf(q, findFrom);
    if (at < 0) {
      at = text.indexOf(q, 0);
    }
    if (at < 0) {
      Ui.toast(this, "Not found");
      return;
    }
    findHit = at;
    editor.setSelection(at, at + q.length());
    findFrom = at + q.length();
    highlightFind();
    updateStatus();
  }
  private void highlightFind() {
    if (editor == null || findBox == null) {
      return;
    }
    Editable ed = editor.getText();
    BackgroundColorSpan[] bg = ed.getSpans(0, ed.length(), BackgroundColorSpan.class);
    int i;
    for (i = 0; i < bg.length; i++) {
      ed.removeSpan(bg[i]);
    }
    ForegroundColorSpan[] fg = ed.getSpans(0, ed.length(), ForegroundColorSpan.class);
    for (i = 0; i < fg.length; i++) {
      ed.removeSpan(fg[i]);
    }
    String q = findBox.getText().toString();
    if (q.length() == 0) {
      findHit = -1;
      return;
    }
    String text = ed.toString();
    int cur = findHit;
    if (cur < 0 || cur >= text.length() || text.indexOf(q, cur) != cur) {
      cur = text.indexOf(q);
      findHit = cur;
    }
    int from = 0;
    int ink = 0xFF111111;
    int hitNow = 0xFFFFB300;
    int hitNext = 0xFF00B8D4;
    while (from < text.length()) {
      int at = text.indexOf(q, from);
      if (at < 0) {
        break;
      }
      int bgc = at == cur ? hitNow : hitNext;
      ed.setSpan(new BackgroundColorSpan(bgc), at, at + q.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      ed.setSpan(new ForegroundColorSpan(ink), at, at + q.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      from = at + q.length();
      if (from <= at) {
        from = at + 1;
      }
    }
  }
  private static String expandTabs(String s, int tab) {
    if (s == null || s.indexOf('\t') < 0) {
      return s == null ? "" : s;
    }
    if (tab < 1) {
      tab = 4;
    }
    StringBuilder sb = new StringBuilder(s.length());
    int col = 0;
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (ch == '\n') {
        sb.append('\n');
        col = 0;
      } else if (ch == '\t') {
        int n = tab - (col % tab);
        int k;
        for (k = 0; k < n; k++) {
          sb.append(' ');
        }
        col += n;
      } else {
        sb.append(ch);
        col++;
      }
    }
    return sb.toString();
  }
  private void refreshGutter() {
    if (!AppSettings.get().editorLineNo) {
      gutter.setVisibility(View.GONE);
      return;
    }
    int n = 1;
    CharSequence t = editor.getText();
    int cap = 8000;
    for (int i = 0; i < t.length(); i++) {
      if (t.charAt(i) == '\n') {
        n++;
        if (n > cap) {
          gutter.setText("1\n...");
          return;
        }
      }
    }
    StringBuilder sb = new StringBuilder(n * 4);
    for (int i = 1; i <= n; i++) {
      if (i > 1) {
        sb.append('\n');
      }
      sb.append(i);
    }
    gutter.setText(sb.toString());
  }
  private void updateStatus() {
    int pos = editor.getSelectionStart();
    if (pos < 0) {
      pos = 0;
    }
    int line = 1;
    int col = 1;
    CharSequence t = editor.getText();
    for (int i = 0; i < pos && i < t.length(); i++) {
      if (t.charAt(i) == '\n') {
        line++;
        col = 1;
      } else {
        col++;
      }
    }
    Layout lay = editor.getLayout();
    int vis = lay == null ? 0 : lay.getLineForVertical(editor.getScrollY()) + 1;
    status.setText("Ln " + line + "  Col " + col + "  " + t.length() + " chars"
      + (dirty ? "  *" : "") + "  " + FileOps.humanSize(ramKey != null ? ramLen : file.length())
      + (vis > 0 ? "  top " + vis : "")
      + (truncated ? "  truncated" : "")
      + (t.length() >= HUGE_CHARS ? "  large" : ""));
  }
  private void updateSaveBtn() {
    if (saveBtn != null) {
      saveBtn.setTextColor(dirty ? Theme.GOOD : Theme.TEXT_DIM);
    }
  }
  private void saveNow() {
    if (archivePath != null && archiveInner != null) {
      if (FileOps.isProtected(new File(archivePath))) {
        Ui.toast(this, "Protected path");
        return;
      }
    } else if (FileOps.isProtected(file) && !"notes.txt".equals(file.getName())) {
      Ui.toast(this, "Protected path");
      return;
    }
    if (truncated) {
      Ui.toast(this, "File was truncated; not saved");
      return;
    }
    String raw = editor.getText().toString();
    if (AppSettings.get().editorSpaces) {
      raw = expandTabs(raw, AppSettings.get().tabSpaces());
    }
    final String text = raw;
    Ui.bg(this, "Saving...", new Runnable() {
      @Override
      public void run() {
        try {
          byte[] data = text.getBytes("UTF-8");
          final long n = data.length;
          if (ramKey != null) {
            ZipBrowse.putRam(ramKey, data);
          }
          File nextFile = file;
          String nextRam = ramKey;
          if (archivePath != null && archiveInner != null) {
            File src = file;
            if (ramKey != null) {
              src = new File(getCacheDir(), "ram-save");
              FileOps.writeBytes(src, data);
            } else {
              FileOps.writeString(file, text);
            }
            File arch = new File(archivePath);
            if (MapArch.isMap(arch.getName())) {
              MapBrowse.replace(arch, archiveInner, src);
            } else {
              ZipBrowse.replaceEntry(arch, archiveInner, src);
            }
            if (ramKey != null) {
              src.delete();
            }
            OpLog.info(EditorActivity.this, "save archive " + archiveInner);
          } else if (ramKey == null) {
            FileOps.writeString(file, text);
            OpLog.info(EditorActivity.this, "save " + file.getAbsolutePath());
          } else {
            File dir = AppSettings.incomingDir(EditorActivity.this);
            dir.mkdirs();
            File dst = new File(dir, TrashStore.uniqueName(dir, file.getName()));
            FileOps.writeBytes(dst, data);
            nextFile = dst;
            nextRam = null;
            OpLog.info(EditorActivity.this, "save " + dst.getAbsolutePath());
          }
          final File savedFile = nextFile;
          final String savedRam = nextRam;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              file = savedFile;
              ramKey = savedRam;
              ramLen = n;
              dirty = false;
              updateSaveBtn();
              updateStatus();
              Ui.toast(EditorActivity.this, archivePath != null ? "Saved in archive" : "Saved");
            }
          });
        } catch (Exception e) {
          OpLog.err(EditorActivity.this, "editor save", e);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(EditorActivity.this, "Save error");
            }
          });
        }
      }
    }, null);
  }
  @Override
  protected void onDestroy() {
    Ui.dismissOpen(this);
    if (editor != null && scrollL != null) {
      try {
        editor.getViewTreeObserver().removeOnScrollChangedListener(scrollL);
      } catch (Exception ignored) {}
    }
    undo.clear();
    redo.clear();
    super.onDestroy();
  }
  @Override
  public void onBackPressed() {
    if (dirty) {
      Ui.confirm(this, "Discard unsaved changes?", new Runnable() {
        @Override
        public void run() {
          finish();
        }
      });
      return;
    }
    super.onBackPressed();
  }
}
