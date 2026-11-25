package com.dlna.FileManager;
import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
public final class MainActivity extends Activity {
  private final List<File> files = new ArrayList<File>();
  private final List<String> labels = new ArrayList<String>();
  private boolean hasUp;
  private final Set<String> selected = new HashSet<String>();
  private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
  private File currentDir;
  private BaseAdapter adapter;
  private ListView listView;
  private TextView pathView;
  private TextView hintView;
  private TextView emptyView;
  private LinearLayout actionsBlock;
  private int accentAtEntry = -1;
  private boolean themeAtEntry = true;
  private int folderColorAtEntry = 0;
  private EditText search;
  private Button globChip;
  private String query = "";
  private boolean globOn = false;
  private final List<Tab> tabs = new ArrayList<Tab>();
  private int tabAt = 0;
  private LinearLayout tabRow;
  private LinearLayout navRow;
  private LinearLayout markRow;
  private HorizontalScrollView markSv;
  private Button selMenu;
  private TextView pullView;
  private float pullY = -1f;
  private View navBarView;
  private View tabBarView;
  private View findRow;
  private View railView;
  private boolean tabsReady = false;
  private static final HashMap<String, int[]> scrollAt = new HashMap<String, int[]>();
  private static final int SCROLL_MAX = 32;
  private int lastTapPos = -1;
  private int skipSelectPos = -1;
  private long skipSelectAt = 0L;
  private Handler tapH;
  private Runnable tabsFlush;
  private static final class Tab {
    File dir;
    String name;
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(savedInstanceState);
    tapH = new Handler();
    CrashHandler.install(this);
    currentDir = Environment.getExternalStorageDirectory();
    Tab first = new Tab();
    first.dir = currentDir;
    first.name = "";
    tabs.add(first);
    if (savedInstanceState != null) {
      String keep = savedInstanceState.getString("dir");
      if (keep != null && new File(keep).isDirectory()) {
        currentDir = new File(keep);
      }
    }
    buildUi();
    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
    }
    TrashStore.migrate(getFilesDir());
    TrashStore.trashDir(getFilesDir()).mkdirs();
    loadTabs();
    setContentView(buildRoot());
    Ui.bar(this);
    applyChrome();
    refresh();
    handleIncoming(getIntent());
  }
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIncoming(intent);
  }
  private void handleIncoming(Intent intent) {
    if (intent == null) {
      return;
    }
    String act = intent.getAction();
    Uri uri = intent.getData();
    if (uri == null && intent.getExtras() != null) {
      Object extra = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (extra instanceof Uri) {
        uri = (Uri) extra;
      }
    }
    if (uri == null) {
      return;
    }
    if (!Intent.ACTION_VIEW.equals(act) && !Intent.ACTION_SEND.equals(act)
        && !Intent.ACTION_EDIT.equals(act)) {
      return;
    }
    final Uri src = uri;
    Ui.bg(this, "Loading...", new Runnable() {
      @Override
      public void run() {
        if ("file".equals(src.getScheme())) {
          String p = src.getPath();
          if (p != null) {
            final File local = new File(p);
            if (local.isFile()) {
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  openFile(local);
                }
              });
              return;
            }
          }
        }
        final String name = Incoming.nameOf(MainActivity.this, src);
        final byte[] ram = Incoming.read(MainActivity.this, src, ZipBrowse.ramLimit());
        final String key = ram == null ? null : ZipBrowse.holdRam(name, ram);
        if (key != null) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (openFromRam(name, key, null, null)) {
                return;
              }
              incomingSaveOpen(src);
            }
          });
          return;
        }
        incomingSaveFromBg(src);
      }
    }, null);
  }
  private void incomingSaveOpen(final Uri src) {
    Ui.bg(this, "Loading...", new Runnable() {
      @Override
      public void run() {
        incomingSaveFromBg(src);
      }
    }, null);
  }
  private void incomingSaveFromBg(Uri src) {
    File got = Incoming.save(this, src);
    if (got == null || !got.isFile()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Ui.toast(MainActivity.this, "Open failed");
        }
      });
      return;
    }
    final File f = got;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        openFile(f);
      }
    });
  }
  @Override
  public void onRequestPermissionsResult(int code, String[] p, int[] g) {
    if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) {
      refresh();
    } else {
      Ui.toast(this, "Storage permission denied");
    }
  }
  @Override
  protected void onSaveInstanceState(Bundle out) {
    if (currentDir != null) {
      out.putString("dir", currentDir.getAbsolutePath());
    }
    super.onSaveInstanceState(out);
  }
  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    if (res == RESULT_OK) {
      AppSettings s = AppSettings.get();
      if (accentAtEntry >= 0 && (accentAtEntry != s.accentColor
          || themeAtEntry != s.darkTheme || folderColorAtEntry != s.folderColor)) {
        recreate();
        return;
      }
      applyKeepScreenOn();
      Ui.styleList(this, listView);
      loadTabsKeep();
      rebuildNav();
      rebuildMarks();
      rebuildTabs();
      applyChrome();
      adapter.notifyDataSetChanged();
      refresh();
    }
  }
  private LinearLayout buildRoot() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Theme.BG);
    int m = Theme.dp(this, 8);
    HorizontalScrollView navSv = new HorizontalScrollView(this);
    navSv.setHorizontalScrollBarEnabled(false);
    navSv.setPadding(m, m, m, 0);
    navRow = new LinearLayout(this);
    navSv.addView(navRow);
    navBarView = navSv;
    root.addView(navSv);
    markSv = new HorizontalScrollView(this);
    markSv.setHorizontalScrollBarEnabled(false);
    markSv.setPadding(m, Theme.dp(this, 4), m, 0);
    markRow = new LinearLayout(this);
    markSv.addView(markRow);
    root.addView(markSv);
    rebuildNav();
    rebuildMarks();
    HorizontalScrollView hsv = new HorizontalScrollView(this);
    hsv.setHorizontalScrollBarEnabled(false);
    tabRow = new LinearLayout(this);
    hsv.addView(tabRow);
    tabBarView = hsv;
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    tlp.setMargins(m, Theme.dp(this, 4), m, 0);
    root.addView(hsv, tlp);
    rebuildTabs();
    LinearLayout find = new LinearLayout(this);
    find.setPadding(m, Theme.dp(this, 4), m, 0);
    find.setGravity(Gravity.CENTER_VERTICAL);
    search.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    find.addView(search);
    LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    glp.leftMargin = Theme.dp(this, 4);
    globChip.setLayoutParams(glp);
    find.addView(globChip);
    findRow = find;
    root.addView(find);
    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    pp.setMargins(m, m, m, 0);
    root.addView(pathView, pp);
    FrameLayout wrap = new FrameLayout(this);
    wrap.addView(listView, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    emptyView.setVisibility(View.GONE);
    wrap.addView(emptyView, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
    ScrollRail rail = new ScrollRail(this);
    FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
      Theme.dp(this, 10), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
    rlp.rightMargin = Theme.dp(this, 2);
    rlp.topMargin = Theme.dp(this, 6);
    rlp.bottomMargin = Theme.dp(this, 6);
    wrap.addView(rail, rlp);
    rail.attach(listView);
    railView = rail;
    pullView = new TextView(this);
    pullView.setText("\u21bb  Pull to refresh");
    pullView.setTextColor(Theme.ACCENT);
    pullView.setTextSize(13);
    pullView.setGravity(Gravity.CENTER);
    pullView.setBackground(Theme.rounded(this, Theme.SURFACE, 14));
    pullView.setPadding(Theme.dp(this, 16), Theme.dp(this, 8), Theme.dp(this, 16), Theme.dp(this, 8));
    pullView.setVisibility(View.GONE);
    FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
      Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    plp.topMargin = Theme.dp(this, 8);
    wrap.addView(pullView, plp);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(wrap, lp);
    LinearLayout hintRow = new LinearLayout(this);
    hintRow.setGravity(Gravity.CENTER_VERTICAL);
    hintRow.addView(hintView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    selMenu = Ui.btn(this, "\u22ee", Theme.ACCENT);
    selMenu.setVisibility(View.GONE);
    selMenu.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectionMenu();
      }
    });
    hintRow.addView(selMenu);
    root.addView(hintRow);
    root.addView(actionsBlock);
    return root;
  }
  private Button navChip(String label, final File dir) {
    Button b = Ui.chip(this, label);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins(Theme.dp(this, 2), 0, Theme.dp(this, 2), 0);
    b.setLayoutParams(lp);
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        go(dir);
      }
    });
    return b;
  }
  private void rebuildNav() {
    if (navRow == null) {
      return;
    }
    navRow.removeAllViews();
    List<AppSettings.NavItem> items = AppSettings.get().navItems();
    for (int i = 0; i < items.size(); i++) {
      AppSettings.NavItem n = items.get(i);
      if (!n.on) {
        continue;
      }
      navRow.addView(navChip(n.label, AppSettings.resolveNav(this, n.path)));
    }
  }
  private void rebuildMarks() {
    if (markRow == null) {
      return;
    }
    markRow.removeAllViews();
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins(Theme.dp(this, 2), 0, Theme.dp(this, 2), 0);
    List<Bookmarks.Item> all = Bookmarks.load(this);
    for (int i = 0; i < all.size(); i++) {
      final Bookmarks.Item it = all.get(i);
      Button b = Ui.chip(this, it.name);
      b.setLayoutParams(lp);
      b.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          File t = new File(it.path);
          if (t.isFile() && !ZipBrowse.isZipPath(it.path)) {
            go(t.getParentFile() != null ? t.getParentFile() : t);
            openFile(t);
          } else {
            go(t);
          }
        }
      });
      b.setOnLongClickListener(new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          bookmarkChipMenu(it);
          return true;
        }
      });
      markRow.addView(b);
    }
    if (markSv != null) {
      markSv.setVisibility(all.isEmpty() ? View.GONE : View.VISIBLE);
    }
  }
  private void buildUi() {
    pathView = new TextView(this);
    pathView.setTextColor(Theme.ACCENT);
    pathView.setTextSize(11);
    pathView.setTypeface(Typeface.MONOSPACE);
    pathView.setSingleLine(true);
    pathView.setEllipsize(android.text.TextUtils.TruncateAt.START);
    pathView.setBackground(Theme.rounded(this, Theme.SURFACE, 10));
    int ph = Theme.dp(this, 10);
    int pv = Theme.dp(this, 7);
    pathView.setPadding(ph, pv, ph, pv);
    pathView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        askPath();
      }
    });
    pathView.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        AppSettings st = AppSettings.get();
        accentAtEntry = st.accentColor;
        themeAtEntry = st.darkTheme;
        folderColorAtEntry = st.folderColor;
        startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 3);
        return true;
      }
    });
    search = new EditText(this);
    search.setHint("Search");
    search.setHintTextColor(Theme.TEXT_DIM);
    search.setTextColor(Theme.TEXT);
    search.setSingleLine(true);
    search.setTextSize(13);
    search.setBackground(Theme.rounded(this, Theme.SURFACE, 10));
    int sp = Theme.dp(this, 10);
    search.setPadding(sp, Theme.dp(this, 8), sp, Theme.dp(this, 8));
    search.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
      @Override
      public void onTextChanged(CharSequence s, int a, int b, int c) {}
      @Override
      public void afterTextChanged(Editable s) {
        query = s.toString().trim();
        refreshKeepSel();
      }
    });
    globChip = Ui.chip(this, "* glob");
    globChip.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        globOn = !globOn;
        globChip.setTextColor(globOn ? Theme.GOOD : Theme.ACCENT);
        refreshKeepSel();
      }
    });
    hintView = new TextView(this);
    hintView.setTextColor(Theme.TEXT_DIM);
    hintView.setTextSize(10);
    hintView.setPadding(Theme.dp(this, 12), Theme.dp(this, 2), Theme.dp(this, 12), Theme.dp(this, 2));
    emptyView = new TextView(this);
    emptyView.setTextColor(Theme.TEXT_DIM);
    emptyView.setTextSize(13);
    emptyView.setGravity(Gravity.CENTER);
    emptyView.setText("This folder is empty");
    listView = new ListView(this);
    Ui.styleList(this, listView);
    listView.setFastScrollEnabled(false);
    adapter = new BaseAdapter() {
      @Override
      public int getCount() { return files.size(); }
      @Override
      public Object getItem(int p) { return rowName(p); }
      @Override
      public long getItemId(int p) { return p; }
      @Override
      public View getView(int p, View v, ViewGroup g) {
        File f = files.get(p);
        String name = rowName(p);
        boolean sel = selected.contains(f.getAbsolutePath());
        boolean isUp = isUp(p);
        ZipBrowse.Node zn = ZipBrowse.meta(f.getAbsolutePath());
        MapBrowse.Node mn = zn == null ? MapBrowse.meta(f.getAbsolutePath()) : null;
        boolean isDir = isUp || (zn != null ? zn.dir : (mn != null ? mn.dir : FileOps.looksLikeDir(f)));
        int kind = isUp ? FileGlyph.UP : (isDir ? (FileOps.isLink(f) ? FileGlyph.LINK : FileGlyph.DIR) : FileGlyph.of(f));
        String sub;
        if (isUp) {
          sub = Lang.t("parent directory");
        } else {
          long mod = zn != null ? zn.time : f.lastModified();
          long sz = zn != null ? zn.size : (mn != null ? mn.size : f.length());
          if (mod < 0L) {
            mod = 0L;
          }
          String date;
          if (AppSettings.get().relDate) {
            date = FileOps.relTime(mod);
          } else {
            date = fmt.format(new Date(mod));
            long dayMs = 7L * 24L * 60L * 60L * 1000L;
            if (mod < System.currentTimeMillis() - dayMs) {
              date = date.substring(0, 10);
            }
          }
          sub = isDir ? date : FileOps.humanSize(sz) + "  |  " + date;
          if (isDir && AppSettings.get().showFolderCounts) {
            String cc = FileOps.childCounts(f, AppSettings.get().showHidden);
            if (cc.length() > 0) {
              sub = sub + "  |  " + cc;
            }
          }
        }
        int titleColor = sel ? Theme.ACCENT : (isDir ? Theme.TEXT : Theme.TEXT_DIM);
        View row = Ui.row(MainActivity.this, v, kind, isUp ? ".." : name, sub, titleColor, sel);
        if (AppSettings.get().showIcons && !AppSettings.get().optNoPkgIcons && isDir && !isUp
            && PkgIcons.host(currentDir.getAbsolutePath())) {
          android.graphics.Bitmap pkg = PkgIcons.of(MainActivity.this, f);
          ImageView badge = Ui.pkgView(row);
          if (pkg != null) {
            badge.setImageBitmap(pkg);
            badge.setVisibility(View.VISIBLE);
          } else {
            badge.setVisibility(View.GONE);
          }
        }
        if (AppSettings.get().showIcons && !AppSettings.get().optNoThumbs && !isDir
            && (kind == FileGlyph.IMG || kind == FileGlyph.VIDEO || kind == FileGlyph.APK)) {
          Thumbs.bind(MainActivity.this, Ui.iconView(row), f.getAbsolutePath(), kind);
        }
        if (!isUp) {
          TextView dots = Ui.dotsView(row);
          dots.setVisibility(View.VISIBLE);
          final File target = f;
          dots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
              fileMenu(target);
            }
          });
        }
        return row;
      }
    };
    listView.setAdapter(adapter);
    listView.setOnTouchListener(new View.OnTouchListener() {
      private final GestureDetector gd = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
          return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
          int pos = listView.pointToPosition((int) e.getX(), (int) e.getY());
          if (pos >= 0 && !hitDots(e, pos)) {
            long now = android.os.SystemClock.uptimeMillis();
            lastTapPos = pos;
            final int p = pos;
            final long t = now;
            tapH.postDelayed(new Runnable() {
              @Override
              public void run() {
                if (p == skipSelectPos && skipSelectAt >= t) {
                  return;
                }
                handleSingle(p);
              }
            }, 75);
          }
          return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
          int pos = listView.pointToPosition((int) e.getX(), (int) e.getY());
          if (pos < 0) {
            pos = lastTapPos;
          }
          if (pos >= 0 && !hitDots(e, pos)) {
            long now = android.os.SystemClock.uptimeMillis();
            skipSelectPos = pos;
            skipSelectAt = now;
            lastTapPos = -1;
            handleDouble(pos);
          }
          return true;
        }
      });
      @Override
      public boolean onTouch(View v, MotionEvent e) {
        gd.onTouchEvent(e);
        handlePull(e);
        return false;
      }
    });
    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
        toggleSelect(pos);
        return true;
      }
    });
    actionsBlock = new LinearLayout(this);
    actionsBlock.setOrientation(LinearLayout.VERTICAL);
    int ap = Theme.dp(this, 4);
    actionsBlock.setPadding(ap, Theme.dp(this, 2), ap, Theme.dp(this, Theme.compact(this) ? 4 : 6));
    rebuildActions();
  }
  private Button actBtn(String label, int color, View.OnClickListener l) {
    Button b = Ui.btn(this, label, color);
    boolean compact = Theme.compact(this);
    LinearLayout.LayoutParams lp = compact
      ? new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
      : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    lp.setMargins(Theme.dp(this, 2), Theme.dp(this, compact ? 0 : 2), Theme.dp(this, 2), Theme.dp(this, compact ? 0 : 2));
    b.setLayoutParams(lp);
    b.setOnClickListener(l);
    return b;
  }
  private void rebuildActions() {
    actionsBlock.removeAllViews();
    AppSettings st = AppSettings.get();
    ArrayList<Button> all = new ArrayList<Button>();
    all.add(actBtn("New File", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) { createEntry(false); }
    }));
    all.add(actBtn("New Folder", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) { createEntry(true); }
    }));
    if (!st.hideNotes) {
      all.add(actBtn("Notes", Theme.GOOD, new View.OnClickListener() {
        @Override
        public void onClick(View v) { openNotes(); }
      }));
    }
    all.add(actBtn("Copy", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) { copySelected(); }
    }));
    all.add(actBtn("Move", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) { moveSelected(); }
    }));
    all.add(actBtn("Refresh", Theme.TEXT, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        refresh();
        Ui.toast(MainActivity.this, "Refreshed");
      }
    }));
    all.add(actBtn("Delete", Theme.DANGER, new View.OnClickListener() {
      @Override
      public void onClick(View v) { deleteSelected(); }
    }));
    all.add(actBtn("Trash", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, TrashActivity.class));
      }
    }));
    if (!st.hideTerm) {
      all.add(actBtn("Term", Theme.GOOD, new View.OnClickListener() {
        @Override
        public void onClick(View v) { openTerm(); }
      }));
    }
    if (!st.hideStats) {
      all.add(actBtn("Stats", Theme.TEXT, new View.OnClickListener() {
        @Override
        public void onClick(View v) { showStats(); }
      }));
    }
    if (!st.hideLog) {
      all.add(actBtn("Log", Theme.TEXT, new View.OnClickListener() {
        @Override
        public void onClick(View v) { showLog(); }
      }));
    }
    all.add(actBtn("Settings", Theme.ACCENT, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AppSettings s = AppSettings.get();
        accentAtEntry = s.accentColor;
        themeAtEntry = s.darkTheme;
        folderColorAtEntry = s.folderColor;
        startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 3);
      }
    }));
    Button[] btns = all.toArray(new Button[all.size()]);
    if (Theme.compact(this)) {
      HorizontalScrollView hsv = new HorizontalScrollView(this);
      hsv.setHorizontalScrollBarEnabled(false);
      hsv.setFillViewport(false);
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.CENTER_VERTICAL);
      for (int i = 0; i < btns.length; i++) {
        row.addView(btns[i]);
      }
      hsv.addView(row);
      actionsBlock.addView(hsv);
      return;
    }
    int cols = 4;
    LinearLayout row = null;
    for (int i = 0; i < btns.length; i++) {
      if (i % cols == 0) {
        row = new LinearLayout(this);
        actionsBlock.addView(row);
      }
      row.addView(btns[i]);
    }
  }
  private void openTerm() {
    Intent i = new Intent(this, TermActivity.class);
    i.putExtra("cwd", currentDir == null ? "/" : currentDir.getAbsolutePath());
    startActivity(i);
  }
  private void openNotes() {
    File n = AppSettings.notesFile(this);
    try {
      if (!n.exists()) {
        FileOps.writeString(n, "");
      }
    } catch (Exception ignored) {}
    openEditor(n);
  }
  private void showLog() {
    String text = OpLog.read(this);
    if (text.length() > 4000) {
      text = text.substring(text.length() - 4000);
    }
    Ui.alert(this, "Operation log", text, true);
  }
  private void refresh() {
    labels.clear();
    files.clear();
    hasUp = false;
    selected.clear();
    if (currentDir == null) {
      currentDir = Environment.getExternalStorageDirectory();
    }
    AppSettings s = AppSettings.get();
    boolean listed;
    String dirPath = ZipBrowse.display(currentDir);
    if (MapBrowse.isMapPath(currentDir.getAbsolutePath())
        || (currentDir.isFile() && MapArch.isMap(currentDir.getName()))) {
      File dat = MapBrowse.isMapPath(currentDir.getAbsolutePath())
        ? MapBrowse.mapOf(currentDir.getAbsolutePath()) : currentDir;
      String inner = MapBrowse.isMapPath(currentDir.getAbsolutePath())
        ? MapBrowse.innerOf(currentDir.getAbsolutePath()) : "";
      if (currentDir.isFile() && MapArch.isMap(currentDir.getName())) {
        currentDir = MapBrowse.root(currentDir);
        dat = MapBrowse.mapOf(currentDir.getAbsolutePath());
        inner = "";
      }
      File parent = MapBrowse.parent(currentDir);
      boolean searching = query.length() > 0;
      if (parent != null && !searching) {
        hasUp = true;
        files.add(parent);
      }
      listed = dat.isFile();
      try {
        List<MapBrowse.Node> nodes = MapBrowse.list(dat, inner, s.showHidden);
        MapBrowse.remember(dat, nodes);
        int mi;
        for (mi = 0; mi < nodes.size(); mi++) {
          MapBrowse.Node n = nodes.get(mi);
          if (searching) {
            if (!FileOps.matches(n.name, query, globOn) && !FileOps.matches(n.inner, query, globOn)) {
              continue;
            }
            labels.add(n.inner.length() > 0 ? n.inner : n.name);
          }
          files.add(MapBrowse.child(dat, n.inner));
        }
      } catch (Exception e) {
        listed = false;
      }
      dirPath = MapBrowse.display(currentDir);
    } else if (!AppSettings.get().optNoArchiveBrowse && (ZipBrowse.isZipPath(currentDir.getAbsolutePath())
        || (currentDir.isFile() && FileOps.isArchive(currentDir.getName())))) {
      File zip = ZipBrowse.isZipPath(currentDir.getAbsolutePath())
        ? ZipBrowse.zipOf(currentDir.getAbsolutePath()) : currentDir;
      String inner = ZipBrowse.isZipPath(currentDir.getAbsolutePath())
        ? ZipBrowse.innerOf(currentDir.getAbsolutePath()) : "";
      if (currentDir.isFile() && FileOps.isArchive(currentDir.getName())) {
        currentDir = ZipBrowse.root(currentDir);
        zip = ZipBrowse.zipOf(currentDir.getAbsolutePath());
        inner = "";
      }
      File parent = ZipBrowse.parent(currentDir);
      boolean searching = query.length() > 0;
      if (parent != null && !searching) {
        hasUp = true;
        files.add(parent);
      }
      listed = zip.isFile();
      try {
        List<ZipBrowse.Node> nodes = searching
          ? ZipBrowse.search(zip, inner, query, globOn, s.showHidden, 800)
          : ZipBrowse.list(zip, inner, s.showHidden);
        ZipBrowse.remember(zip, nodes);
        for (int i = 0; i < nodes.size(); i++) {
          ZipBrowse.Node n = nodes.get(i);
          files.add(ZipBrowse.child(zip, n.inner));
          if (searching) {
            labels.add(n.inner.length() > 0 ? n.inner : n.name);
          }
        }
      } catch (Exception e) {
        listed = false;
      }
      dirPath = ZipBrowse.display(currentDir);
    } else {
      ZipBrowse.clear();
      File parent = currentDir.getParentFile();
      boolean searching = query.length() > 0;
      if (currentDir.getAbsolutePath().length() > 1 && parent != null && !searching) {
        hasUp = true;
        files.add(parent);
      }
      listed = FileOps.canList(currentDir);
      dirPath = currentDir.getAbsolutePath();
      if (searching) {
        List<File> found = FileOps.search(currentDir, query, globOn, s.showHidden, 800);
        listed = true;
        for (int i = 0; i < found.size(); i++) {
          File f = found.get(i);
          files.add(f);
          labels.add(FileOps.rel(currentDir, f));
        }
      } else {
        File[] arr = FileOps.list(currentDir, s.showHidden, s.sortMode, s.foldersFirst);
        File[] probed;
        if (s.optNoProbe && Probe.isSystemPath(dirPath)) {
          probed = new File[0];
        } else {
          probed = Probe.forPath(this, dirPath);
        }
        if (FileOps.isCacheRoot(currentDir)) {
          arr = new File[0];
          listed = false;
          probed = new File[0];
        }
        if (probed.length > 0) {
          arr = FileOps.merge(arr, probed);
          listed = true;
        }
        for (int i = 0; i < arr.length; i++) {
          if (FileOps.skipListed(currentDir, arr[i])) {
            continue;
          }
          files.add(arr[i]);
        }
      }
    }
    pathView.setText(dirPath);
    int count = files.size() - (hasUp ? 1 : 0);
    if (count == 0) {
      emptyView.setText(listed ? "This folder is empty"
        : (ZipBrowse.isZipPath(currentDir.getAbsolutePath())
          ? "Error to get data" : "Error to get permission"));
      emptyView.setVisibility(View.VISIBLE);
    } else {
      emptyView.setVisibility(View.GONE);
    }
    adapter.notifyDataSetChanged();
    updateHint(listed);
    restoreScroll();
  }
  private void rememberScroll() {
    if (listView == null || currentDir == null) {
      return;
    }
    String k = currentDir.getAbsolutePath();
    View ch = listView.getChildAt(0);
    scrollAt.put(k, new int[]{listView.getFirstVisiblePosition(), ch == null ? 0 : ch.getTop()});
    if (scrollAt.size() > SCROLL_MAX) {
      scrollAt.remove(scrollAt.keySet().iterator().next());
    }
  }
  private void restoreScroll() {
    if (listView == null || currentDir == null) {
      return;
    }
    int[] at = scrollAt.get(currentDir.getAbsolutePath());
    if (at == null || at.length < 2) {
      return;
    }
    final int f = at[0];
    final int t = at[1];
    listView.post(new Runnable() {
      @Override
      public void run() {
        listView.setSelectionFromTop(f, t);
      }
    });
  }
  private void refreshKeepSel() {
    Set<String> keep = new HashSet<String>(selected);
    refresh();
    selected.clear();
    selected.addAll(keep);
    adapter.notifyDataSetChanged();
    updateHint(true);
  }
  private void updateHint(boolean listed) {
    if (selMenu != null) {
      selMenu.setVisibility(selected.isEmpty() ? View.GONE : View.VISIBLE);
    }
    if (!selected.isEmpty()) {
      long bytes = 0L;
      java.util.Iterator<String> it = selected.iterator();
      while (it.hasNext()) {
        File sf = new File(it.next());
        if (sf.isFile()) {
          bytes += sf.length();
        }
      }
      hintView.setText("Selected: " + selected.size() + "  |  " + FileOps.humanSize(bytes)
        + "  |  tap \u22ee for commands");
      return;
    }
    int count = files.size() - (hasUp ? 1 : 0);
    if (count == 0) {
      hintView.setText(listed ? "This folder is empty"
        : (ZipBrowse.isZipPath(currentDir.getAbsolutePath())
          ? "Error to get data" : "Error to get permission"));
    } else {
      hintView.setText(count + " items  |  tap selects  |  double-tap opens  |  \u22ee");
    }
  }
  private boolean hitDots(MotionEvent e, int pos) {
    if (pos < 0 || pos >= files.size() || isUp(pos)) {
      return false;
    }
    View row = listView.getChildAt(pos - listView.getFirstVisiblePosition());
    if (row == null) {
      return false;
    }
    return e.getX() >= (float) (row.getRight() - Theme.dp(this, 36));
  }
  private void askPath() {
    String cur = currentDir == null ? "/" : ZipBrowse.display(currentDir);
    Ui.input(this, "Go to path:", cur, new Ui.OnText() {
      @Override
      public void on(String t) {
        if (t.length() == 0) {
          return;
        }
        go(new File(t));
      }
    });
  }
  private boolean blocked(File f) {
    if (FileOps.isProtected(f)) {
      Ui.toast(this, "Protected path");
      return true;
    }
    return false;
  }
  private void go(File dir) {
    if (dir == null) {
      return;
    }
    rememberScroll();
    if (!AppSettings.get().optNoArchiveBrowse && dir.isFile() && FileOps.isArchive(dir.getName())) {
      dir = ZipBrowse.root(dir);
    }
    if (dir.isFile() && MapArch.isMap(dir.getName())) {
      dir = MapBrowse.root(dir);
    }
    String p = dir.getAbsolutePath();
    if (!ZipBrowse.isZipPath(p) && !MapBrowse.isMapPath(p) && !dir.exists() && !FileOps.looksLikeDir(dir)) {
      Ui.toast(this, "Does not exist");
      return;
    }
    if (ZipBrowse.isZipPath(p) && !ZipBrowse.zipOf(p).isFile()) {
      Ui.toast(this, "Archive not found");
      return;
    }
    if (MapBrowse.isMapPath(p) && !MapBrowse.mapOf(p).isFile()) {
      Ui.toast(this, "Archive not found");
      return;
    }
    currentDir = dir;
    if (!ZipBrowse.isZipPath(p)) {
      ZipBrowse.clear();
    }
    if (!MapBrowse.isMapPath(p)) {
      MapBrowse.clear();
    }
    if (tabAt >= 0 && tabAt < tabs.size()) {
      tabs.get(tabAt).dir = dir;
    }
    OpLog.info(this, "go " + dir.getAbsolutePath());
    refresh();
    rebuildTabs();
    rebuildActions();
    saveTabs();
  }
  private void handleSingle(int pos) {
    if (pos < 0 || pos >= files.size()) {
      return;
    }
    File f = files.get(pos);
    if (isUp(pos)) {
      go(f);
      return;
    }
    toggleSelect(pos);
  }
  private void handleDouble(int pos) {
    if (pos < 0 || pos >= files.size()) {
      return;
    }
    File f = files.get(pos);
    if (FileOps.looksLikeDir(f) || isUp(pos) || ZipBrowse.isDir(f) || MapBrowse.isDir(f)) {
      go(f);
      return;
    }
    if (!AppSettings.get().optNoArchiveBrowse && f.isFile() && FileOps.isArchive(f.getName())) {
      go(ZipBrowse.root(f));
      return;
    }
    if (f.isFile() && MapArch.isMap(f.getName())) {
      go(MapBrowse.root(f));
      return;
    }
    openFile(f);
  }
  private void toggleSelect(int pos) {
    File f = files.get(pos);
    if (isUp(pos)) {
      return;
    }
    String path = f.getAbsolutePath();
    if (selected.contains(path)) {
      selected.remove(path);
    } else {
      selected.add(path);
    }
    adapter.notifyDataSetChanged();
    updateHint(true);
  }
  private void openFile(File f) {
    OpLog.info(this, "open " + f.getAbsolutePath());
    if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
      if (ZipBrowse.isDir(f)) {
        go(f);
        return;
      }
      openZipEntry(f);
      return;
    }
    if (!AppSettings.get().optNoArchiveBrowse && f.isFile() && FileOps.isArchive(f.getName())) {
      go(ZipBrowse.root(f));
      return;
    }
    if (MapBrowse.isMapPath(f.getAbsolutePath())) {
      if (MapBrowse.isDir(f)) {
        go(f);
        return;
      }
      try {
        File out = MapBrowse.materialize(f, AppSettings.zipOpenDir(this));
        openEditor(out, MapBrowse.mapOf(f.getAbsolutePath()), MapBrowse.innerOf(f.getAbsolutePath()));
      } catch (Exception e) {
        Ui.toast(this, "Open failed");
      }
      return;
    }
    if (MapArch.isMap(f.getName()) && f.isFile()) {
      go(MapBrowse.root(f));
      return;
    }
    AppSettings s = AppSettings.get();
    if (openSpecial(f, false)) {
      return;
    }
    long lim = s.openLimit();
    if (f.length() > lim) {
      Ui.toast(this, "File too big (" + FileOps.humanSize(f.length())
        + ", limit " + FileOps.humanSize(lim) + ")");
      return;
    }
    if (FileOps.extListed(s.extraTextExts, f.getName())) {
      openEditor(f);
      return;
    }
    if (!FileOps.isTextFile(f) && !s.openBinary) {
      Ui.toast(this, "Binary file");
      return;
    }
    openEditor(f);
  }
  private boolean openSpecial(File f, boolean sniff) {
    AppSettings s = AppSettings.get();
    if (MdDraw.isMarkdownName(f.getName())) {
      Intent m = new Intent(this, MdActivity.class);
      m.putExtra("path", f.getAbsolutePath());
      startActivity(m);
      return true;
    }
    String ext = FileOps.ext(f.getName());
    if (!s.optNoHtml && ("html".equals(ext) || "htm".equals(ext))) {
      Intent h = new Intent(this, HtmlActivity.class);
      h.putExtra("path", f.getAbsolutePath());
      startActivity(h);
      return true;
    }
    int kind = sniff ? Media.ofFile(f) : Media.of(f.getName());
    if (s.mediaPreview && kind != Media.NONE) {
      Intent i = new Intent(this, ViewerActivity.class);
      i.putExtra("path", f.getAbsolutePath());
      startActivity(i);
      return true;
    }
    return false;
  }
  static void clearScroll() {
    scrollAt.clear();
  }
  private boolean isUp(int p) {
    return hasUp && p == 0;
  }
  private String rowName(int p) {
    if (p < 0 || p >= files.size()) {
      return "";
    }
    if (isUp(p)) {
      return "..";
    }
    if (labels.size() == files.size()) {
      return labels.get(p);
    }
    return files.get(p).getName();
  }
  private void openEditor(File f) {
    openEditor(f, null, null);
  }
  private void openEditor(File f, File zip, String inner) {
    openEditor(f, zip, inner, null);
  }
  private void openEditor(File f, File zip, String inner, String ramKey) {
    Intent i = new Intent(this, EditorActivity.class);
    i.putExtra("path", f.getAbsolutePath());
    if (zip != null && inner != null && inner.length() > 0) {
      i.putExtra("archive", zip.getAbsolutePath());
      i.putExtra("inner", inner);
    }
    if (ramKey != null && ramKey.length() > 0) {
      i.putExtra("ram", ramKey);
    }
    startActivity(i);
  }
  private boolean openFromRam(String name, String ramKey, File zip, String inner) {
    byte[] ram = ZipBrowse.getRam(ramKey);
    if (name == null || ramKey == null || ram == null) {
      return false;
    }
    AppSettings s = AppSettings.get();
    if (ram.length > s.openLimit()) {
      return false;
    }
    if (MdDraw.isMarkdownName(name)) {
      return false;
    }
    String ext = FileOps.ext(name);
    if (!s.optNoHtml && ("html".equals(ext) || "htm".equals(ext))) {
      return false;
    }
    int kind = Media.of(name);
    if (kind == Media.VIDEO || kind == Media.AUDIO) {
      return false;
    }
    if (kind == Media.IMAGE) {
      if (!s.mediaPreview) {
        return false;
      }
      Intent i = new Intent(this, ViewerActivity.class);
      i.putExtra("path", name);
      i.putExtra("ram", ramKey);
      i.putExtra("asImage", true);
      startActivity(i);
      return true;
    }
    if (!FileOps.extListed(s.extraTextExts, name) && !FileOps.isTextBytes(ram) && !s.openBinary) {
      return false;
    }
    openEditor(new File(name), zip, inner, ramKey);
    return true;
  }
  private void createEntry(final boolean folder) {
    Ui.input(this, folder ? "New folder name:" : "New file name:", "", new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0 || name.indexOf('/') >= 0 || ".".equals(name) || "..".equals(name)) {
          Ui.toast(MainActivity.this, "Invalid name");
          return;
        }
        String p = currentDir.getAbsolutePath();
        if (ZipBrowse.isZipPath(p)) {
          createZipEntry(name, folder);
          return;
        }
        if (MapBrowse.isMapPath(p)) {
          createMapEntry(name, folder);
          return;
        }
        File target = new File(currentDir, name);
        if (blocked(currentDir) || blocked(target)) {
          return;
        }
        if (target.exists()) {
          Ui.toast(MainActivity.this, "Already exists");
          return;
        }
        boolean ok = folder ? target.mkdir() : createFile(target);
        Ui.toast(MainActivity.this, ok ? "Created: " + name : "Create failed");
        refresh();
      }
    });
  }
  private void createZipEntry(String name, boolean folder) {
    File zip = ZipBrowse.zipOf(currentDir.getAbsolutePath());
    String inner = ZipBrowse.innerOf(currentDir.getAbsolutePath());
    try {
      List<ZipBrowse.Node> nodes = ZipBrowse.list(zip, inner, true);
      int i;
      for (i = 0; i < nodes.size(); i++) {
        if (name.equals(nodes.get(i).name)) {
          Ui.toast(this, "Already exists");
          return;
        }
      }
      File cache = new File(getCacheDir(), "fm_new");
      FileOps.emptyDir(cache);
      cache.mkdirs();
      File tmp = new File(cache, name);
      boolean made = folder ? tmp.mkdir() : createFile(tmp);
      if (!made) {
        Ui.toast(this, "Create failed");
        return;
      }
      ZipBrowse.addFiles(zip, new File[]{tmp}, inner);
      FileOps.deleteRec(tmp);
      Ui.toast(this, "Created: " + name);
      refresh();
    } catch (Exception e) {
      Ui.toast(this, "Create failed");
    }
  }
  private void createMapEntry(String name, boolean folder) {
    File dat = MapBrowse.mapOf(currentDir.getAbsolutePath());
    String inner = MapBrowse.innerOf(currentDir.getAbsolutePath());
    String path = inner.length() == 0 ? name : inner + "/" + name;
    try {
      List<MapBrowse.Node> nodes = MapBrowse.list(dat, inner, true);
      int i;
      for (i = 0; i < nodes.size(); i++) {
        if (name.equals(nodes.get(i).name)) {
          Ui.toast(this, "Already exists");
          return;
        }
      }
      MapBrowse.addEmpty(dat, path, folder);
      Ui.toast(this, "Created: " + name);
      refresh();
    } catch (Exception e) {
      Ui.toast(this, "Create failed");
    }
  }
  static boolean createFile(File f) {
    try {
      return f.createNewFile();
    } catch (IOException e) {
      return false;
    }
  }
  private void renameFile(final File src) {
    if (blocked(src)) {
      return;
    }
    Ui.input(this, "New name:", src.getName(), new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0 || name.indexOf('/') >= 0 || ".".equals(name) || "..".equals(name)) {
          Ui.toast(MainActivity.this, "Invalid name");
          return;
        }
        File dst = new File(src.getParentFile(), name);
        if (dst.exists()) {
          Ui.toast(MainActivity.this, "Name already taken");
          return;
        }
        Ui.toast(MainActivity.this, src.renameTo(dst) ? "Renamed" : "Rename failed");
        refresh();
      }
    });
  }
  private void copyClip(String text) {
    if (text == null) {
      text = "";
    }
    try {
      android.content.ClipboardManager cm =
        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      cm.setPrimaryClip(ClipData.newPlainText("fm", text));
      Ui.toast(this, "Copied");
    } catch (Exception e) {
      Ui.toast(this, "Copy failed");
    }
  }
  private void renameBatch() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    Ui.menu(this, Lang.t("Rename batch"),
      new String[]{"Replace name", "Find / replace", "Prefix", "Suffix", "Extension"},
      new int[]{Theme.ACCENT, Theme.ACCENT, Theme.TEXT, Theme.TEXT, Theme.GOOD},
      new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 1) {
            Ui.input(MainActivity.this, Lang.t("Find"), "", new Ui.OnText() {
              @Override
              public void on(String find) {
                Ui.input(MainActivity.this, Lang.t("Replace with"), "", new Ui.OnText() {
                  @Override
                  public void on(String repl) {
                    batchRename(1, find, repl);
                  }
                });
              }
            });
            return;
          }
          final int mode = w;
          String title = w == 0 ? "New name" : (w == 2 ? "Prefix" : (w == 3 ? "Suffix" : "Extension"));
          Ui.input(MainActivity.this, Lang.t(title), "", new Ui.OnText() {
            @Override
            public void on(String text) {
              batchRename(mode, text, "");
            }
          });
        }
      });
  }
  private void batchRename(int mode, String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    if (a.indexOf('/') >= 0 || b.indexOf('/') >= 0) {
      Ui.toast(this, "Invalid name");
      return;
    }
    File[] srcs = selectedFiles();
    int n = 0;
    int seq = 0;
    int i;
    for (i = 0; i < srcs.length; i++) {
      File src = srcs[i];
      if (src == null || blocked(src)) {
        continue;
      }
      File parent = src.getParentFile();
      if (parent == null) {
        continue;
      }
      String old = src.getName();
      String next = batchName(mode, old, a, b, srcs.length, seq);
      if (next == null || next.length() == 0 || next.equals(old) || next.indexOf('/') >= 0) {
        continue;
      }
      seq++;
      File dst = new File(parent, TrashStore.uniqueName(parent, next));
      if (src.renameTo(dst)) {
        n++;
      }
    }
    Ui.toast(this, "Renamed " + n);
    refresh();
  }
  private static String batchName(int mode, String old, String a, String b, int total, int seq) {
    int dot = old.lastIndexOf('.');
    String stem = dot > 0 ? old.substring(0, dot) : old;
    String ext = dot > 0 ? old.substring(dot) : "";
    if (mode == 0) {
      if (total <= 1) {
        return a;
      }
      return a + "-" + (seq + 1) + ext;
    }
    if (mode == 1) {
      if (a.length() == 0) {
        return old;
      }
      return old.replace(a, b);
    }
    if (mode == 2) {
      return a + old;
    }
    if (mode == 3) {
      return stem + a + ext;
    }
    String ne = a;
    if (ne.startsWith(".")) {
      ne = ne.substring(1);
    }
    return stem + (ne.length() == 0 ? "" : "." + ne);
  }
  private File[] selectedFiles() {
    File[] out = new File[selected.size()];
    int i = 0;
    for (String p : selected) {
      out[i++] = new File(p);
    }
    return out;
  }
  private void copySelected() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    copyItems(selectedFiles());
  }
  private void moveSelected() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    moveItems(selectedFiles());
  }
  private void copyItems(final File[] srcs) {
    if (srcs.length > 0 && ZipBrowse.isZipPath(srcs[0].getAbsolutePath())) {
      copyArchiveTo(srcs[0]);
      return;
    }
    if (srcs.length > 0 && MapBrowse.isMapPath(srcs[0].getAbsolutePath())) {
      copyMapTo(srcs);
      return;
    }
    DestPicker.show(this, currentDir, "Copy " + srcs.length + " item(s) to", new DestPicker.OnDir() {
      @Override
      public void on(File dest) {
        transfer(srcs, dest, false);
      }
    });
  }
  private void moveItems(final File[] srcs) {
    if (srcs.length > 0 && ZipBrowse.isZipPath(srcs[0].getAbsolutePath())) {
      moveArchiveTo(srcs);
      return;
    }
    if (srcs.length > 0 && MapBrowse.isMapPath(srcs[0].getAbsolutePath())) {
      moveMapTo(srcs);
      return;
    }
    DestPicker.show(this, currentDir, "Move " + srcs.length + " item(s) to", new DestPicker.OnDir() {
      @Override
      public void on(File dest) {
        transfer(srcs, dest, true);
      }
    });
  }
  private void copyMapTo(final File[] srcs) {
    DestPicker.show(this, Environment.getExternalStorageDirectory(), "Copy from archive to", new DestPicker.OnDir() {
      @Override
      public void on(File dir) {
        writeMapOut(srcs, dir, false);
      }
    });
  }
  private void moveMapTo(final File[] srcs) {
    DestPicker.show(this, Environment.getExternalStorageDirectory(), "Move from archive to", new DestPicker.OnDir() {
      @Override
      public void on(File dir) {
        writeMapOut(srcs, dir, true);
      }
    });
  }
  private void writeMapOut(final File[] srcs, final File dir, final boolean drop) {
    Ui.bg(this, drop ? "Moving..." : "Copying...", new Runnable() {
      @Override
      public void run() {
        int ok = 0;
        int i;
        for (i = 0; i < srcs.length; i++) {
          try {
            MapBrowse.materialize(srcs[i], dir);
            if (drop) {
              MapBrowse.remove(MapBrowse.mapOf(srcs[i].getAbsolutePath()),
                MapBrowse.innerOf(srcs[i].getAbsolutePath()));
            }
            ok++;
          } catch (Exception e) {
            OpLog.err(MainActivity.this, "map out", e);
          }
        }
        final int n = ok;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Ui.toast(MainActivity.this, (drop ? "Moved " : "Copied ") + n);
          }
        });
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void moveArchiveTo(final File[] srcs) {
    File zip = ZipBrowse.zipOf(srcs[0].getAbsolutePath());
    final String[] inners = new String[srcs.length];
    int i;
    for (i = 0; i < srcs.length; i++) {
      inners[i] = ZipBrowse.innerOf(srcs[i].getAbsolutePath());
    }
    final File arch = zip;
    DestPicker.show(this, Environment.getExternalStorageDirectory(), "Move from archive to", new DestPicker.OnDir() {
      @Override
      public void on(File dir) {
        extractZipTo(arch, inners, dir, true);
      }
    });
  }
  private void transfer(final File[] srcs, final File dest, final boolean move) {
    if (blocked(dest)) {
      return;
    }
    final File trash = TrashStore.trashDir(getFilesDir());
    if (FileOps.under(dest, trash)) {
      for (int i = 0; i < srcs.length; i++) {
        if (FileOps.under(srcs[i], trash) || "trash".equalsIgnoreCase(srcs[i].getName())) {
          Ui.toast(this, "Stopped: trash inside trash");
          return;
        }
      }
    }
    boolean clash = destClash(srcs, dest);
    if (AppSettings.get().confirmOverwrite && clash) {
      Ui.confirm(this, "Overwrite existing files?",
        new Runnable() {
          @Override
          public void run() {
            transferRun(srcs, dest, move, true);
          }
        },
        new Runnable() {
          @Override
          public void run() {
            transferRun(srcs, dest, move, false);
          }
        });
      return;
    }
    transferRun(srcs, dest, move, false);
  }
  private static boolean destClash(File[] srcs, File dest) {
    if (srcs == null || dest == null) {
      return false;
    }
    int i;
    for (i = 0; i < srcs.length; i++) {
      File s = srcs[i];
      if (s == null) {
        continue;
      }
      File d = new File(dest, s.getName());
      if (d.exists() && !FileOps.same(s, d)) {
        return true;
      }
    }
    return false;
  }
  private void transferRun(final File[] srcs, final File dest, final boolean move, final boolean overwrite) {
    final File trash = TrashStore.trashDir(getFilesDir());
    Ui.bg(this, move ? "Moving..." : "Copying...", new Ui.Work() {
      @Override
      public void run(Ui.Progress p) {
        File[] list = FileOps.pruneNested(srcs);
        FileOps.Tally tally = FileOps.tallyAll(list);
        FileOps.beginProg(tally.bytes);
        p.msg(FileOps.tallyLine(move ? "Moving" : "Copying", tally));
        int ok = 0;
        int fail = 0;
        int namesOnly = 0;
        boolean stopped = false;
        for (int i = 0; i < list.length; i++) {
          File src = list[i];
          if (stopped) {
            fail++;
            continue;
          }
          if (FileOps.under(dest, trash) && (FileOps.under(src, trash)
              || "trash".equalsIgnoreCase(src.getName()))) {
            stopped = true;
            fail++;
            continue;
          }
          if (!src.exists()) {
            fail++;
            continue;
          }
          if (move && FileOps.isProtected(src)) {
            fail++;
            continue;
          }
          if (FileOps.nestIntoSelf(src, dest)) {
            fail++;
            continue;
          }
          File parent = src.getParentFile();
          if (move && parent != null && FileOps.same(parent, dest)) {
            fail++;
            continue;
          }
          File dst;
          if (overwrite) {
            dst = new File(dest, src.getName());
            if (dst.exists() && !FileOps.same(src, dst)) {
              FileOps.deleteRec(dst);
            }
          } else {
            dst = new File(dest, TrashStore.uniqueName(dest, src.getName()));
          }
          if (FileOps.same(src, dst) || FileOps.nestIntoSelf(src, dst)) {
            fail++;
            continue;
          }
          boolean done;
          if (move) {
            done = FileOps.move(src, dst);
          } else {
            try {
              FileOps.copyRec(src, dst);
              done = true;
            } catch (IOException e) {
              FileOps.deleteRec(dst);
              done = false;
            }
          }
          if (done) {
            ok++;
          } else if (FileOps.copyName(src, dest)) {
            namesOnly++;
          } else {
            fail++;
          }
        }
        OpLog.info(MainActivity.this, (move ? "move " : "copy ") + ok + "/" + (ok + fail)
          + (namesOnly > 0 ? " names=" + namesOnly : ""));
        String msg = (move ? "Moved: " : "Copied: ") + ok;
        if (namesOnly > 0) {
          msg = msg + "  Names: " + namesOnly;
        }
        if (fail > 0) {
          msg = msg + "  Failed: " + fail;
        }
        final String out = msg;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Ui.toast(MainActivity.this, out);
          }
        });
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void fileMenu(final File f) {
    boolean bulk = selected.size() > 1 && selected.contains(f.getAbsolutePath());
    if (bulk) {
      if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
        ArrayList<String> zit = new ArrayList<String>();
        ArrayList<Integer> zcol = new ArrayList<Integer>();
        zit.add("Extract");
        zcol.add(Integer.valueOf(Theme.GOOD));
        zit.add("Copy to...");
        zcol.add(Integer.valueOf(Theme.ACCENT));
        if (!AppSettings.get().hideInfo) {
          zit.add("Info");
          zcol.add(Integer.valueOf(Theme.TEXT));
        }
        final String[] zitems = zit.toArray(new String[zit.size()]);
        int[] zcolors = new int[zcol.size()];
        int zi;
        for (zi = 0; zi < zcol.size(); zi++) {
          zcolors[zi] = zcol.get(zi).intValue();
        }
        Ui.menu(this, selected.size() + " items", zitems, zcolors, new Ui.OnPick() {
          @Override
          public void onPick(int w) {
            String cmd = zitems[w];
            if ("Extract".equals(cmd)) {
              extractZipSelected();
            } else if ("Copy to...".equals(cmd)) {
              copyArchiveTo(f);
            } else {
              showInfo(f);
            }
          }
        });
        return;
      }
      ArrayList<String> bit = new ArrayList<String>();
      ArrayList<Integer> bcol = new ArrayList<Integer>();
      bit.add("Copy");
      bcol.add(Integer.valueOf(Theme.ACCENT));
      bit.add("Move");
      bcol.add(Integer.valueOf(Theme.ACCENT));
      if (!AppSettings.get().hideCopyPath) {
        bit.add("Copy path");
        bcol.add(Integer.valueOf(Theme.ACCENT));
      }
      bit.add("Rename batch");
      bcol.add(Integer.valueOf(Theme.ACCENT));
      bit.add("Pack to ZIP archive");
      bcol.add(Integer.valueOf(Theme.GOOD));
      bit.add("Pack to TAR archive");
      bcol.add(Integer.valueOf(Theme.GOOD));
      bit.add("Pack to TAR.GZ archive");
      bcol.add(Integer.valueOf(Theme.GOOD));
      bit.add("Pack to HashMap archive");
      bcol.add(Integer.valueOf(Theme.GOOD));
      bit.add("Delete");
      bcol.add(Integer.valueOf(Theme.DANGER));
      if (!AppSettings.get().hideInfo) {
        bit.add("Info");
        bcol.add(Integer.valueOf(Theme.TEXT));
      }
      final String[] bitems = bit.toArray(new String[bit.size()]);
      int[] bcolors = new int[bcol.size()];
      for (int bi = 0; bi < bcol.size(); bi++) {
        bcolors[bi] = bcol.get(bi).intValue();
      }
      Ui.menu(this, selected.size() + " items", bitems, bcolors, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          String cmd = bitems[w];
          if ("Copy".equals(cmd)) {
            copyItems(selectedFiles());
          } else if ("Move".equals(cmd)) {
            moveItems(selectedFiles());
          } else if ("Copy path".equals(cmd)) {
            copyClip(f.getParent());
          } else if ("Rename batch".equals(cmd)) {
            renameBatch();
          } else if ("Pack to ZIP archive".equals(cmd)) {
            zipSelected();
          } else if ("Pack to TAR archive".equals(cmd)) {
            tarSelected(false);
          } else if ("Pack to TAR.GZ archive".equals(cmd)) {
            tarSelected(true);
          } else if ("Pack to HashMap archive".equals(cmd)) {
            mapSelected();
          } else if ("Delete".equals(cmd)) {
            deleteSelected();
          } else {
            showInfo(f);
          }
        }
      });
      return;
    }
    boolean zipEntry = ZipBrowse.isZipPath(f.getAbsolutePath());
    boolean arch = f.isFile() && FileOps.isArchive(f.getName());
    boolean hmap = f.isFile() && MapArch.isMap(f.getName());
    ArrayList<String> it = new ArrayList<String>();
    ArrayList<Integer> col = new ArrayList<Integer>();
    AppSettings st = AppSettings.get();
    it.add("Open");
    col.add(Integer.valueOf(Theme.ACCENT));
    if (!st.hideShare) {
      it.add("Share");
      col.add(Integer.valueOf(Theme.ACCENT));
    }
    if (!f.isDirectory() && !ZipBrowse.isDir(f)) {
      it.add("Open as...");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Open as image");
      col.add(Integer.valueOf(Theme.TEXT));
      it.add("Open as archive");
      col.add(Integer.valueOf(Theme.GOOD));
    }
    if (Media.of(f.getName()) == Media.IMAGE && !ZipBrowse.isDir(f)) {
      it.add("Open as text");
      col.add(Integer.valueOf(Theme.TEXT));
    }
    if ("apk".equals(FileOps.ext(f.getName())) && !zipEntry) {
      it.add("Install");
      col.add(Integer.valueOf(Theme.GOOD));
    }
    if (arch) {
      it.add("Browse archive");
      col.add(Integer.valueOf(Theme.GOOD));
    }
    it.add("Marks");
    col.add(Integer.valueOf(Theme.GOOD));
    if (!st.hideInfo) {
      it.add("Info");
      col.add(Integer.valueOf(Theme.TEXT));
    }
    if (!st.hideCopyPath) {
      it.add("Copy path");
      col.add(Integer.valueOf(Theme.ACCENT));
    }
    if (!st.hideCopyName) {
      it.add("Copy name");
      col.add(Integer.valueOf(Theme.ACCENT));
    }
    if (!zipEntry) {
      it.add("Rename");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Copy");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Move");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Pack to ZIP archive");
      col.add(Integer.valueOf(Theme.GOOD));
      it.add("Pack to TAR archive");
      col.add(Integer.valueOf(Theme.GOOD));
      it.add("Pack to TAR.GZ archive");
      col.add(Integer.valueOf(Theme.GOOD));
      it.add("Pack to HashMap archive");
      col.add(Integer.valueOf(Theme.GOOD));
    }
    if (arch || zipEntry || hmap) {
      it.add(hmap ? "Unpack" : "Extract");
      col.add(Integer.valueOf(Theme.GOOD));
    }
    if (zipEntry) {
      it.add("Copy to...");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Add file");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Rename");
      col.add(Integer.valueOf(Theme.ACCENT));
      it.add("Delete");
      col.add(Integer.valueOf(Theme.DANGER));
    }
    if (!zipEntry) {
      it.add("Delete");
      col.add(Integer.valueOf(Theme.DANGER));
    }
    final String[] items = it.toArray(new String[it.size()]);
    int[] colors = new int[col.size()];
    for (int i = 0; i < col.size(); i++) {
      colors[i] = col.get(i).intValue();
    }
    Ui.menu(this, f.getName(), items, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        onFileCmd(f, items[w]);
      }
    });
  }
  private void onFileCmd(File f, String cmd) {
    if ("Open".equals(cmd)) {
      if (f.isDirectory() || ZipBrowse.isDir(f)) {
        go(f);
      } else {
        openFile(f);
      }
    } else if ("Open as...".equals(cmd)) {
      openAs(f);
    } else if ("Share".equals(cmd)) {
      shareItem(f);
    } else if ("Open as image".equals(cmd)) {
      openAsImage(f);
    } else if ("Open as archive".equals(cmd)) {
      openAsArchive(f);
    } else if ("Open as text".equals(cmd)) {
      if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
        File out = materialize(f);
        if (out != null) {
          openEditor(out, ZipBrowse.zipOf(f.getAbsolutePath()), ZipBrowse.innerOf(f.getAbsolutePath()));
        }
      } else {
        openEditor(f);
      }
    } else if ("Copy to...".equals(cmd)) {
      copyArchiveTo(f);
    } else if ("Browse archive".equals(cmd)) {
      go(ZipBrowse.root(f));
    } else if ("Marks".equals(cmd)) {
      markItem(f);
    } else if ("Install".equals(cmd)) {
      installApk(f);
    } else if ("Info".equals(cmd)) {
      showInfo(f);
    } else if ("Copy path".equals(cmd)) {
      copyClip(f.getAbsolutePath());
    } else if ("Copy name".equals(cmd)) {
      copyClip(f.getName());
    } else if ("Rename batch".equals(cmd)) {
      renameBatch();
    } else if ("Rename".equals(cmd)) {
      if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
        archiveRename(f);
      } else {
        renameFile(f);
      }
    } else if ("Copy".equals(cmd)) {
      copyItems(new File[]{f});
    } else if ("Move".equals(cmd)) {
      moveItems(new File[]{f});
    } else if ("Zip".equals(cmd) || "Pack to ZIP archive".equals(cmd)) {
      selected.clear();
      selected.add(f.getAbsolutePath());
      zipSelected();
    } else if ("Tar".equals(cmd) || "Pack to TAR archive".equals(cmd)) {
      selected.clear();
      selected.add(f.getAbsolutePath());
      tarSelected(false);
    } else if ("Tar.gz".equals(cmd) || "Pack to TAR.GZ archive".equals(cmd)) {
      selected.clear();
      selected.add(f.getAbsolutePath());
      tarSelected(true);
    } else if ("Pack to HashMap archive".equals(cmd)) {
      selected.clear();
      selected.add(f.getAbsolutePath());
      mapSelected();
    } else if ("Unpack".equals(cmd)) {
      unpackMap(f);
    } else if ("Add file".equals(cmd)) {
      archiveAdd(f);
    } else if ("Extract".equals(cmd)) {
      if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
        extractZipEntry(f);
      } else {
        extractFile(f);
      }
    } else if ("Delete".equals(cmd)) {
      if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
        archiveDelete(f);
      } else {
        selected.clear();
        selected.add(f.getAbsolutePath());
        deleteSelected();
      }
    }
  }
  private void shareItem(File src) {
    File f = src;
    if (ZipBrowse.isZipPath(src.getAbsolutePath())) {
      f = materialize(src);
    }
    if (f == null) {
      Ui.toast(this, "Share failed");
      return;
    }
    if (f.isDirectory()) {
      File zip = new File(AppSettings.openAsDir(this), f.getName() + ".zip");
      try {
        FileOps.zipTo(new File[]{f}, zip);
        f = zip;
      } catch (Exception e) {
        Ui.toast(this, "Share folder failed");
        return;
      }
    }
    if (!f.isFile()) {
      Ui.toast(this, "Share failed");
      return;
    }
    File send = shareCopy(f);
    Uri uri = InstallProvider.uriFor(send);
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType(FileOps.mime(send.getName()));
    i.putExtra(Intent.EXTRA_STREAM, uri);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if (Build.VERSION.SDK_INT >= 16) {
      i.setClipData(ClipData.newRawUri(send.getName(), uri));
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    try {
      startActivity(Intent.createChooser(i, "Share"));
    } catch (Exception e) {
      Ui.toast(this, "Share failed");
    }
  }
  private void openAsImage(File src) {
    if (ZipBrowse.isZipPath(src.getAbsolutePath())) {
      File zip = ZipBrowse.zipOf(src.getAbsolutePath());
      String inner = ZipBrowse.innerOf(src.getAbsolutePath());
      byte[] ram = ZipBrowse.viewBytes(zip, inner);
      if (ram != null) {
        String name = new File(inner).getName();
        if (openFromRam(name, ZipBrowse.ramKey(zip, inner), zip, inner)) {
          return;
        }
      }
    }
    File f = materialize(src);
    if (f == null || !f.isFile()) {
      Ui.toast(this, "Cannot open image");
      return;
    }
    Intent i = new Intent(this, ViewerActivity.class);
    i.putExtra("path", f.getAbsolutePath());
    i.putExtra("asImage", true);
    startActivity(i);
  }
  private void openAsArchive(File src) {
    File f = materialize(src);
    if (f == null || !f.isFile()) {
      Ui.toast(this, "Not an archive");
      return;
    }
    go(ZipBrowse.root(f));
  }
  private void markItem(File f) {
    if (Bookmarks.has(this, f)) {
      Ui.toast(this, "Already marked");
      return;
    }
    Bookmarks.add(this, f);
    Ui.toast(this, "Marked");
    rebuildMarks();
  }
  private File materialize(File f) {
    if (ZipBrowse.isZipPath(f.getAbsolutePath())) {
      return extractZipToFile(f);
    }
    return f;
  }
  private void openAs(File src) {
    final File f = materialize(src);
    if (f == null || !f.isFile()) {
      Ui.toast(this, "Open as failed");
      return;
    }
    File share = shareCopy(f);
    final Uri uri = InstallProvider.uriFor(share);
    String mime = FileOps.mime(share.getName());
    ArrayList<ResolveInfo> all = new ArrayList<ResolveInfo>();
    HashSet<String> seen = new HashSet<String>();
    String any = "*" + "/" + "*";
    collectViewers(all, seen, uri, mime);
    collectViewers(all, seen, uri, any);
    collectViewers(all, seen, Uri.fromFile(share), mime);
    collectViewers(all, seen, Uri.fromFile(share), any);
    if (all.isEmpty()) {
      Ui.toast(this, "No apps can open this file");
      return;
    }
    String[] labels = new String[all.size()];
    int[] colors = new int[all.size()];
    final ResolveInfo[] pick = new ResolveInfo[all.size()];
    final File send = share;
    PackageManager pm = getPackageManager();
    for (int i = 0; i < all.size(); i++) {
      ResolveInfo ri = all.get(i);
      pick[i] = ri;
      CharSequence label = ri.loadLabel(pm);
      labels[i] = (label != null ? label.toString() : ri.activityInfo.packageName)
        + "  (" + ri.activityInfo.packageName + ")";
      colors[i] = Theme.ACCENT;
    }
    Ui.menu(this, "Open as...", labels, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        if (w < 0 || w >= pick.length) {
          return;
        }
        launchViewer(pick[w], send, uri);
      }
    });
  }
  private File shareCopy(File f) {
    File dir = AppSettings.openAsDir(this);
    if (f.isDirectory()) {
      dir.mkdirs();
      File dst = new File(dir, f.getName());
      if (dst.getAbsolutePath().equals(f.getAbsolutePath())) {
        return f;
      }
      try {
        FileOps.copyRec(f, dst);
        return dst;
      } catch (Exception e) {
        return f;
      }
    }
    try {
      return FileOps.copyFileDedup(f, dir);
    } catch (Exception e) {
      return f;
    }
  }
  private void launchViewer(ResolveInfo ri, File f, Uri uri) {
    String pkg = ri.activityInfo.packageName;
    String cls = ri.activityInfo.name;
    String type = FileOps.mime(f.getName());
    try {
      grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Exception ignored) {}
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setComponent(new ComponentName(pkg, cls));
    i.setDataAndType(uri, type);
    i.putExtra(Intent.EXTRA_STREAM, uri);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    i.addCategory(Intent.CATEGORY_DEFAULT);
    if (Build.VERSION.SDK_INT >= 16) {
      i.setClipData(ClipData.newRawUri(f.getName(), uri));
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    try {
      startActivity(i);
      return;
    } catch (Exception ignored) {}
    try {
      Intent file = new Intent(Intent.ACTION_VIEW);
      file.setComponent(new ComponentName(pkg, cls));
      file.setDataAndType(Uri.fromFile(f), type);
      file.addCategory(Intent.CATEGORY_DEFAULT);
      startActivity(file);
    } catch (Exception e2) {
      Ui.toast(this, "Open failed");
    }
  }
  private void collectViewers(ArrayList<ResolveInfo> out, HashSet<String> seen, Uri uri, String mime) {
    try {
      Intent probe = new Intent(Intent.ACTION_VIEW);
      probe.setDataAndType(uri, mime);
      probe.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      List<ResolveInfo> list = getPackageManager().queryIntentActivities(probe, 0);
      String self = getPackageName();
      for (int i = 0; i < list.size(); i++) {
        ResolveInfo ri = list.get(i);
        if (ri.activityInfo == null) {
          continue;
        }
        if (self.equals(ri.activityInfo.packageName)) {
          continue;
        }
        String key = ri.activityInfo.packageName + "/" + ri.activityInfo.name;
        if (seen.add(key)) {
          out.add(ri);
        }
      }
    } catch (Exception ignored) {}
  }
  private void installApk(File f) {
    try {
      if (Build.VERSION.SDK_INT >= 26) {
        PackageManager pm = getPackageManager();
        if (!pm.canRequestPackageInstalls()) {
          Intent perm = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
          perm.setData(Uri.parse("package:" + getPackageName()));
          startActivity(perm);
        }
      }
      File dir = AppSettings.installDir(this);
      File dst = f;
      if (f.isFile()) {
        try {
          dst = FileOps.copyFileDedup(f, dir);
        } catch (IOException e) {
          dst = f;
        }
      }
      if (Build.VERSION.SDK_INT >= 24) {
        Uri uri = InstallProvider.uriFor(dst);
        Intent inst = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        inst.setDataAndType(uri, "application/vnd.android.package-archive");
        inst.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(inst);
        return;
      }
      Intent view = new Intent(Intent.ACTION_VIEW);
      view.setDataAndType(Uri.fromFile(dst), "application/vnd.android.package-archive");
      startActivity(view);
    } catch (Exception e) {
      Ui.toast(this, "Install failed");
    }
  }
  private void showInfo(File f) {
    if (selected.size() > 1 && selected.contains(f.getAbsolutePath())) {
      showInfoMany(selectedFiles());
      return;
    }
    final File target = f;
    final boolean dir = f.isDirectory() || ZipBrowse.isDir(f) || MapBrowse.isDir(f) || FileOps.looksLikeDir(f);
    if (!dir) {
      StringBuilder sb = new StringBuilder();
      sb.append("This file\n");
      sb.append("Path: ").append(f.getAbsolutePath()).append('\n');
      boolean virt = ZipBrowse.isZipPath(f.getAbsolutePath()) || MapBrowse.isMapPath(f.getAbsolutePath());
      if (virt) {
        ZipBrowse.Node zn = ZipBrowse.meta(f.getAbsolutePath());
        MapBrowse.Node mn = zn == null ? MapBrowse.meta(f.getAbsolutePath()) : null;
        long sz = zn != null ? zn.size : (mn != null ? mn.size : 0L);
        sb.append("Archive entry\n");
        sb.append("Size: ").append(FileOps.humanSize(sz)).append('\n');
        appendMem(sb);
        Ui.alert(this, f.getName(), sb.toString(), true);
        return;
      }
      sb.append("Size: ").append(FileOps.humanSize(f.length())).append('\n');
      sb.append("Modified: ").append(fmt.format(new Date(f.lastModified()))).append('\n');
      sb.append("Readable: ").append(f.canRead() ? "yes" : "no").append('\n');
      sb.append("Writable: ").append(f.canWrite() ? "yes" : "no");
      appendMem(sb);
      if (AppSettings.get().optNoSha256) {
        Ui.alert(this, f.getName(), sb.toString(), true);
        return;
      }
      final String[] out = new String[1];
      out[0] = sb.toString();
      Ui.bg(this, "Hash...", new Runnable() {
        @Override
        public void run() {
          long cap = 4194304L;
          String hx = FileOps.sha256File(target, cap);
          if (hx.length() == 0) {
            return;
          }
          String extra = "\nSHA-256: " + hx;
          if (target.length() > cap) {
            extra = extra + "  (first 4 MB)";
          }
          out[0] = out[0] + extra;
        }
      }, new Runnable() {
        @Override
        public void run() {
          Ui.alert(MainActivity.this, target.getName(), out[0], true);
        }
      });
      return;
    }
    final String[] out = new String[1];
    Ui.bg(this, "Calculating...", new Runnable() {
      @Override
      public void run() {
        long t0 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("This folder\n");
        sb.append("Path: ").append(target.getAbsolutePath()).append('\n');
        if (ZipBrowse.isZipPath(target.getAbsolutePath())) {
          ZipBrowse.Node zn = ZipBrowse.meta(target.getAbsolutePath());
          sb.append("Archive entry\n");
          if (zn != null) {
            sb.append("Size: ").append(FileOps.humanSize(zn.size)).append('\n');
          }
          sb.append("Time: ").append(System.currentTimeMillis() - t0).append(" ms");
          appendMem(sb);
          out[0] = sb.toString();
          return;
        }
        Stats.FolderScan sc = Stats.scan(target);
        sb.append("Size: ").append(FileOps.humanSize(sc.size)).append('\n');
        sb.append("Files: ").append(sc.files).append('\n');
        sb.append("Folders: ").append(sc.dirs).append('\n');
        sb.append("Readable: ").append(target.canRead() ? "yes" : "no").append('\n');
        sb.append("Writable: ").append(target.canWrite() ? "yes" : "no").append('\n');
        sb.append("Listable: ").append(FileOps.canList(target) ? "yes" : "no").append('\n');
        sb.append("Time: ").append(sc.ms).append(" ms");
        appendMem(sb);
        out[0] = sb.toString();
      }
    }, new Runnable() {
      @Override
      public void run() {
        Ui.alert(MainActivity.this, target.getName(), out[0] == null ? "" : out[0], true);
      }
    });
  }
  private static void appendMem(StringBuilder sb) {
    if (AppSettings.get().optNoHeap) {
      return;
    }
    sb.append('\n').append(Stats.memLine());
  }
  private void showInfoMany(final File[] srcs) {
    final String[] out = new String[1];
    Ui.bg(this, "Calculating...", new Runnable() {
      @Override
      public void run() {
        long t0 = System.currentTimeMillis();
        File[] list = FileOps.pruneNested(srcs);
        FileOps.Tally t = FileOps.tallyAll(list);
        StringBuilder sb = new StringBuilder();
        sb.append("Selected ").append(list.length).append(" items\n");
        sb.append("Files: ").append(t.files).append('\n');
        sb.append("Folders: ").append(t.dirs).append('\n');
        sb.append("Size: ").append(FileOps.humanSize(t.bytes)).append('\n');
        sb.append("Time: ").append(System.currentTimeMillis() - t0).append(" ms");
        out[0] = sb.toString();
      }
    }, new Runnable() {
      @Override
      public void run() {
        Ui.alert(MainActivity.this, selected.size() + " items", out[0] == null ? "" : out[0], true);
      }
    });
  }
  private void copyArchiveTo(final File f) {
    DestPicker.show(this, currentDir, "Copy from archive to", new DestPicker.OnDir() {
      @Override
      public void on(File dir) {
        File[] srcs;
        if (selected.size() > 1 && selected.contains(f.getAbsolutePath())) {
          srcs = selectedFiles();
        } else {
          srcs = new File[]{f};
        }
        File zip = ZipBrowse.zipOf(srcs[0].getAbsolutePath());
        String[] inners = new String[srcs.length];
        for (int i = 0; i < srcs.length; i++) {
          inners[i] = ZipBrowse.innerOf(srcs[i].getAbsolutePath());
        }
        extractZipTo(zip, inners, dir);
      }
    });
  }
  private void extractFile(final File f) {
    if (!FileOps.isArchive(f.getName())) {
      Ui.toast(this, "Not a zip archive (zip/jar/apk)");
      return;
    }
    Ui.menu(this, "Extract " + f.getName(),
      new String[]{"Default (Extracted/" + stemOf(f.getName()) + ")", "Choose folder"},
      new int[]{Theme.ACCENT, Theme.GOOD}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            extractInto(f, AppSettings.extractDir());
          } else {
            DestPicker.show(MainActivity.this, currentDir, "Extract to", new DestPicker.OnDir() {
              @Override
              public void on(File dir) {
                extractInto(f, dir);
              }
            });
          }
        }
      });
  }
  static String stemOf(String name) {
    String low = name.toLowerCase(Locale.US);
    if (low.endsWith(".tar.gz")) {
      return name.substring(0, name.length() - 7);
    }
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }
  private File extractZipToFile(File f) {
    File zip = ZipBrowse.zipOf(f.getAbsolutePath());
    String inner = ZipBrowse.innerOf(f.getAbsolutePath());
    File dest = new File(AppSettings.zipOpenDir(this), stemOf(zip.getName()));
    File out = new File(dest, inner);
    try {
      ZipBrowse.extract(zip, new String[]{inner}, dest);
    } catch (Exception e) {
      return null;
    }
    return out.exists() ? out : dest;
  }
  private void openZipEntry(File f) {
    File zip = ZipBrowse.zipOf(f.getAbsolutePath());
    String inner = ZipBrowse.innerOf(f.getAbsolutePath());
    byte[] ram = ZipBrowse.viewBytes(zip, inner);
    if (ram != null) {
      String name = new File(inner).getName();
      if (openFromRam(name, ZipBrowse.ramKey(zip, inner), zip, inner)) {
        return;
      }
    }
    File out = extractZipToFile(f);
    if (out == null || !out.exists()) {
      Ui.toast(this, "Extract failed");
      return;
    }
    if (out.isDirectory()) {
      go(out);
      return;
    }
    AppSettings s = AppSettings.get();
    if (openSpecial(out, true)) {
      return;
    }
    long lim = s.openLimit();
    if (out.length() > lim) {
      Ui.toast(this, "File too big (" + FileOps.humanSize(out.length())
        + ", limit " + FileOps.humanSize(lim) + ")");
      return;
    }
    if (FileOps.extListed(s.extraTextExts, out.getName()) || FileOps.isTextFile(out) || s.openBinary) {
      openEditor(out, zip, inner);
      return;
    }
    Ui.toast(this, "Binary file");
  }
  private void extractZipEntry(File f) {
    extractZipInners(ZipBrowse.zipOf(f.getAbsolutePath()),
      new String[]{ZipBrowse.innerOf(f.getAbsolutePath())});
  }
  private void extractZipSelected() {
    File[] srcs = selectedFiles();
    if (srcs.length == 0) {
      return;
    }
    File zip = ZipBrowse.zipOf(srcs[0].getAbsolutePath());
    String[] inners = new String[srcs.length];
    for (int i = 0; i < srcs.length; i++) {
      inners[i] = ZipBrowse.innerOf(srcs[i].getAbsolutePath());
    }
    extractZipInners(zip, inners);
  }
  private void extractZipInners(final File zip, final String[] inners) {
    Ui.menu(this, "Extract to",
      new String[]{"Default (Extracted/" + stemOf(zip.getName()) + ")", "Choose folder"},
      new int[]{Theme.ACCENT, Theme.GOOD}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            extractZipTo(zip, inners, new File(AppSettings.extractDir(), stemOf(zip.getName())));
          } else {
            DestPicker.show(MainActivity.this, currentDir, "Extract to", new DestPicker.OnDir() {
              @Override
              public void on(File dir) {
                extractZipTo(zip, inners, dir);
              }
            });
          }
        }
      });
  }
  private void extractZipTo(File zip, String[] inners, File dest) {
    extractZipTo(zip, inners, dest, false);
  }
  private void extractZipTo(final File zip, final String[] inners, final File dest, final boolean drop) {
    Ui.bg(this, drop ? "Moving..." : "Extracting...", new Runnable() {
      @Override
      public void run() {
        try {
          final int n = ZipBrowse.extract(zip, inners, dest);
          if (drop) {
            ZipBrowse.deleteEntries(zip, inners);
          }
          OpLog.info(MainActivity.this, "extract " + n + " to " + dest.getAbsolutePath());
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, (drop ? "Moved " : "Copied/extracted ") + n + " to " + dest.getAbsolutePath());
            }
          });
        } catch (Exception e) {
          OpLog.err(MainActivity.this, "extract", e);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, drop ? "Move failed" : "Extract failed");
            }
          });
        }
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void extractInto(final File zip, File base) {
    final File dest = new File(base, stemOf(zip.getName()));
    Runnable go = new Runnable() {
      @Override
      public void run() {
        doExtract(zip, dest, true);
      }
    };
    if (dest.exists()) {
      Ui.confirm(this, "Folder already exists:\n" + dest.getAbsolutePath()
        + "\nReplace files inside?", go);
    } else {
      go.run();
    }
  }
  private void mapSelected() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    String base = "archive";
    if (selected.size() == 1) {
      base = stemOf(new File(selected.iterator().next()).getName());
    }
    final File dir = MapArch.archivesDir();
    File dest = new File(dir, base + ".dat");
    if (dest.exists()) {
      dest = new File(dir, base + "_" + System.currentTimeMillis() + ".dat");
    }
    final File out = dest;
    final File[] srcs = selectedFiles();
    Ui.bg(this, "Creating HashMap...", new Ui.Work() {
      @Override
      public void run(Ui.Progress p) {
        try {
          File[] list = FileOps.pruneNested(srcs);
          FileOps.Tally t = FileOps.tallyAll(list);
          FileOps.beginProg(t.bytes);
          p.msg(FileOps.tallyLine("Packing HashMap", t));
          final int n = MapArch.pack(list, out);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "HashMap " + n + " files\n" + out.getAbsolutePath());
            }
          });
        } catch (Exception e) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "HashMap pack failed");
            }
          });
        }
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void unpackMap(final File f) {
    if (f == null || !MapArch.isMap(f.getName())) {
      Ui.toast(this, "Not a HashMap archive");
      return;
    }
    final File dest = new File(MapArch.extractedDir(), stemOf(f.getName()));
    Ui.bg(this, "Unpacking HashMap...", new Runnable() {
      @Override
      public void run() {
        try {
          final int n = MapArch.unpack(f, dest);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Unpacked " + n + " files\n" + dest.getAbsolutePath());
            }
          });
        } catch (Exception e) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "HashMap unpack failed");
            }
          });
        }
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void zipSelected() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    String base = "archive";
    if (selected.size() == 1) {
      base = stemOf(new File(selected.iterator().next()).getName());
    }
    final File dir = AppSettings.archiveDir();
    dir.mkdirs();
    File zip = new File(dir, base + ".zip");
    if (zip.exists()) {
      zip = new File(dir, base + "_" + System.currentTimeMillis() + ".zip");
    }
    final File dest = zip;
    final File[] srcs = selectedFiles();
    if (dest.exists()) {
      Ui.confirm(this, "Archive exists. Replace?\n" + dest.getAbsolutePath(), new Runnable() {
        @Override
        public void run() {
          doZip(srcs, dest);
        }
      });
    } else {
      doZip(srcs, dest);
    }
  }
  private void tarSelected(boolean gzip) {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    String base = "archive";
    if (selected.size() == 1) {
      base = stemOf(new File(selected.iterator().next()).getName());
    }
    final File dir = AppSettings.archiveDir();
    dir.mkdirs();
    String ext = gzip ? ".tar.gz" : ".tar";
    File tar = new File(dir, base + ext);
    if (tar.exists()) {
      tar = new File(dir, base + "_" + System.currentTimeMillis() + ext);
    }
    final File dest = tar;
    final File[] srcs = selectedFiles();
    final boolean gz = gzip;
    Ui.bg(this, "Creating tar...", new Ui.Work() {
      @Override
      public void run(Ui.Progress p) {
        try {
          File[] list = FileOps.pruneNested(srcs);
          FileOps.Tally t = FileOps.tallyAll(list);
          FileOps.beginProg(t.bytes);
          p.msg(FileOps.tallyLine(gz ? "Packing tar.gz" : "Packing tar", t));
          final int n = TarOps.create(list, dest, gz);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Tar " + n + " entries  " + dest.getName());
            }
          });
        } catch (Exception e) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Tar failed");
            }
          });
        }
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void archiveDelete(final File f) {
    Ui.confirm(this, "Delete from archive:\\n" + ZipBrowse.innerOf(f.getAbsolutePath()), new Runnable() {
      @Override
      public void run() {
        try {
          File zip = ZipBrowse.zipOf(f.getAbsolutePath());
          ZipBrowse.deleteEntries(zip, new String[]{ZipBrowse.innerOf(f.getAbsolutePath())});
          Ui.toast(MainActivity.this, "Removed from archive");
          go(currentDir);
        } catch (Exception e) {
          Ui.toast(MainActivity.this, "Edit failed");
        }
      }
    });
  }
  private void archiveRename(final File f) {
    final String inner = ZipBrowse.innerOf(f.getAbsolutePath());
    Ui.input(this, "New name in archive:", new File(inner).getName(), new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0 || name.indexOf('/') >= 0) {
          Ui.toast(MainActivity.this, "Invalid name");
          return;
        }
        int sl = inner.lastIndexOf('/');
        String to = sl < 0 ? name : inner.substring(0, sl + 1) + name;
        try {
          ZipBrowse.renameEntry(ZipBrowse.zipOf(f.getAbsolutePath()), inner, to);
          Ui.toast(MainActivity.this, "Renamed in archive");
          go(currentDir);
        } catch (Exception e) {
          Ui.toast(MainActivity.this, "Rename failed");
        }
      }
    });
  }
  private void archiveAdd(final File f) {
    File zip = ZipBrowse.zipOf(f.getAbsolutePath());
    String inner = ZipBrowse.innerOf(f.getAbsolutePath());
    final String prefix = ZipBrowse.isDir(f) ? inner : (inner.indexOf('/') < 0 ? "" : inner.substring(0, inner.lastIndexOf('/')));
    final File arch = zip;
    File start = Environment.getExternalStorageDirectory();
    FilePick.show(this, start, "Add file to archive", "", new FilePick.OnFile() {
      @Override
      public void on(File file) {
        try {
          ZipBrowse.addFiles(arch, new File[]{file}, prefix);
          Ui.toast(MainActivity.this, "Added to archive");
          go(currentDir);
        } catch (Exception e) {
          Ui.toast(MainActivity.this, "Add failed");
        }
      }
    });
  }
  private void doZip(final File[] srcs, final File dest) {
    Ui.bg(this, "Creating zip...", new Ui.Work() {
      @Override
      public void run(Ui.Progress p) {
        try {
          File[] list = FileOps.pruneNested(srcs);
          FileOps.Tally t = FileOps.tallyAll(list);
          FileOps.beginProg(t.bytes);
          p.msg(FileOps.tallyLine("Zipping", t));
          final int n = FileOps.zipTo(list, dest);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Zipped " + n + " entries\n" + dest.getAbsolutePath());
            }
          });
        } catch (Exception e) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Zip failed");
            }
          });
        }
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void doExtract(final File zip, final File dest, final boolean overwrite) {
    Ui.bg(this, "Extracting...", new Runnable() {
      @Override
      public void run() {
        try {
          if (overwrite && dest.exists()) {
            FileOps.deleteRec(dest);
          }
          final int n = ZipBrowse.extract(zip, new String[]{""}, dest);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Extracted " + n + " files to " + dest.getAbsolutePath());
            }
          });
        } catch (Exception e) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Ui.toast(MainActivity.this, "Extract failed");
            }
          });
        }
      }
    }, null);
  }
  private void deleteSelected() {
    if (selected.isEmpty()) {
      Ui.toast(this, "Nothing selected");
      return;
    }
    File[] srcs = selectedFiles();
    if (srcs.length > 0 && ZipBrowse.isZipPath(srcs[0].getAbsolutePath())) {
      deleteZipSelected(srcs);
      return;
    }
    if (srcs.length > 0 && MapBrowse.isMapPath(srcs[0].getAbsolutePath())) {
      deleteMapSelected(srcs);
      return;
    }
    Runnable act = new Runnable() {
      @Override
      public void run() {
        doDelete();
      }
    };
    if (AppSettings.get().confirmDelete) {
      Ui.confirm(this, "Move " + selected.size() + " item(s) to Trash?", act);
    } else {
      act.run();
    }
  }
  private void deleteZipSelected(final File[] srcs) {
    Runnable act = new Runnable() {
      @Override
      public void run() {
        try {
          File zip = ZipBrowse.zipOf(srcs[0].getAbsolutePath());
          String[] inners = new String[srcs.length];
          int i;
          for (i = 0; i < srcs.length; i++) {
            inners[i] = ZipBrowse.innerOf(srcs[i].getAbsolutePath());
          }
          ZipBrowse.deleteEntries(zip, inners);
          selected.clear();
          Ui.toast(MainActivity.this, "Removed from archive");
          refresh();
        } catch (Exception e) {
          Ui.toast(MainActivity.this, "Delete failed");
        }
      }
    };
    if (AppSettings.get().confirmDelete) {
      Ui.confirm(this, "Delete " + srcs.length + " item(s) from archive?", act);
    } else {
      act.run();
    }
  }
  private void deleteMapSelected(final File[] srcs) {
    Runnable act = new Runnable() {
      @Override
      public void run() {
        int ok = 0;
        int i;
        for (i = 0; i < srcs.length; i++) {
          try {
            MapBrowse.remove(MapBrowse.mapOf(srcs[i].getAbsolutePath()),
              MapBrowse.innerOf(srcs[i].getAbsolutePath()));
            ok++;
          } catch (Exception e) {
            OpLog.err(MainActivity.this, "map del", e);
          }
        }
        selected.clear();
        Ui.toast(MainActivity.this, "Removed " + ok);
        refresh();
      }
    };
    if (AppSettings.get().confirmDelete) {
      Ui.confirm(this, "Delete " + srcs.length + " item(s) from archive?", act);
    } else {
      act.run();
    }
  }
  private void doDelete() {
    final File trashDir = TrashStore.trashDir(getFilesDir());
    Ui.bg(this, "Trashing...", new Ui.Work() {
      @Override
      public void run(Ui.Progress p) {
        File[] list = FileOps.pruneNested(selectedFiles());
        FileOps.Tally tally = FileOps.tallyAll(list);
        FileOps.beginProg(tally.bytes);
        p.msg(FileOps.tallyLine("Trashing", tally));
        int ok = 0;
        int fail = 0;
        for (int i = 0; i < list.length; i++) {
          if (FileOps.isProtected(list[i])) {
            fail++;
            continue;
          }
          if (TrashStore.add(trashDir, list[i])) {
            ok++;
          } else {
            fail++;
          }
        }
        OpLog.info(MainActivity.this, "trash " + ok + " fail=" + fail);
        final String msg = "Trashed: " + ok + (fail > 0 ? "  Failed: " + fail : "");
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Ui.toast(MainActivity.this, msg);
          }
        });
      }
    }, new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }
  private void showStats() {
    Ui.bg(this, "Stats...", new Runnable() {
      @Override
      public void run() {
        final String t = Stats.build(MainActivity.this);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (isFinishing()) {
              return;
            }
            Ui.alert(MainActivity.this, "Statistics", t, true);
          }
        });
      }
    }, null);
  }
  private void applyKeepScreenOn() {
    if (listView != null) {
      listView.setKeepScreenOn(AppSettings.get().keepScreenOn);
    }
    Ui.applyKeepScreen(this);
  }
  private void applyChrome() {
    AppSettings s = AppSettings.get();
    vis(navBarView, !s.hideNav);
    if (s.hideMarks) {
      vis(markSv, false);
    } else if (markRow != null) {
      vis(markSv, markRow.getChildCount() > 0);
    }
    vis(tabBarView, !s.hideTabs);
    vis(findRow, !s.hideSearch);
    vis(globChip, !s.hideGlob && !s.hideSearch);
    vis(pathView, !s.hidePath);
    vis(railView, !s.hideRail);
    rebuildActions();
  }
  private static void vis(View v, boolean on) {
    if (v != null) {
      v.setVisibility(on ? View.VISIBLE : View.GONE);
    }
  }
  private void rebuildTabs() {
    if (tabRow == null) {
      return;
    }
    tabRow.removeAllViews();
    for (int i = 0; i < tabs.size(); i++) {
      final int idx = i;
      File d = tabs.get(i).dir;
      String label;
      if (d == null) {
        label = "/";
      } else if (ZipBrowse.isZipPath(d.getAbsolutePath())) {
        String inner = ZipBrowse.innerOf(d.getAbsolutePath());
        label = inner.length() == 0 ? ZipBrowse.zipOf(d.getAbsolutePath()).getName() : new File(inner).getName();
      } else if (MapBrowse.isMapPath(d.getAbsolutePath())) {
        String inner = MapBrowse.innerOf(d.getAbsolutePath());
        label = inner.length() == 0 ? MapBrowse.mapOf(d.getAbsolutePath()).getName() : new File(inner).getName();
      } else {
        label = d.getAbsolutePath().length() <= 1 ? "/" : d.getName();
      }
      if (tabs.get(i).name != null && tabs.get(i).name.length() > 0) {
        label = tabs.get(i).name;
      }
      if (label.length() == 0) {
        label = "/";
      }
      Button b = Ui.chip(this, label);
      if (i == tabAt) {
        b.setTextColor(Theme.GOOD);
      }
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(Theme.dp(this, 2), 0, Theme.dp(this, 2), 0);
      b.setLayoutParams(lp);
      b.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          switchTab(idx);
        }
      });
      b.setOnLongClickListener(new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          tabChipMenu(idx);
          return true;
        }
      });
      tabRow.addView(b);
    }
    Button plus = Ui.chip(this, "+");
    plus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        addTab();
      }
    });
    tabRow.addView(plus);
  }
  private void addTab() {
    Tab t = new Tab();
    t.dir = currentDir;
    t.name = "";
    tabs.add(t);
    tabAt = tabs.size() - 1;
    rebuildTabs();
    saveTabs();
  }
  private void tabChipMenu(final int idx) {
    if (idx < 0 || idx >= tabs.size()) {
      return;
    }
    Ui.menu(this, tabLabel(tabs.get(idx)),
      new String[]{"Edit name and path", "Move left", "Move right", "Close"},
      new int[]{Theme.ACCENT, Theme.TEXT, Theme.TEXT, Theme.DANGER},
      new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            editTab(idx);
          } else if (w == 1) {
            moveTab(idx, -1);
          } else if (w == 2) {
            moveTab(idx, 1);
          } else {
            askCloseTab(idx);
          }
        }
      });
  }
  private String tabLabel(Tab t) {
    if (t.name != null && t.name.length() > 0) {
      return t.name;
    }
    if (t.dir == null) {
      return "/";
    }
    String n = t.dir.getName();
    return n.length() == 0 ? "/" : n;
  }
  private void editTab(final int idx) {
    final Tab t = tabs.get(idx);
    String curName = t.name == null ? "" : t.name;
    String curPath = t.dir == null ? "/" : t.dir.getAbsolutePath();
    Ui.input(this, "Tab name (empty = folder name):", curName, new Ui.OnText() {
      @Override
      public void on(String name) {
        final String nm = name;
        Ui.input(MainActivity.this, "Tab path:", curPath, new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              return;
            }
            t.name = nm;
            t.dir = new File(path);
            if (idx == tabAt) {
              currentDir = t.dir;
              refresh();
            }
            rebuildTabs();
            saveTabs();
          }
        });
      }
    });
  }
  private void moveTab(int idx, int delta) {
    int to = idx + delta;
    if (to < 0 || to >= tabs.size()) {
      return;
    }
    Tab t = tabs.remove(idx);
    tabs.add(to, t);
    if (tabAt == idx) {
      tabAt = to;
    } else if (idx < tabAt && to >= tabAt) {
      tabAt--;
    } else if (idx > tabAt && to <= tabAt) {
      tabAt++;
    }
    rebuildTabs();
    saveTabs();
  }
  private void switchTab(int idx) {
    if (idx < 0 || idx >= tabs.size()) {
      return;
    }
    if (tabAt >= 0 && tabAt < tabs.size()) {
      tabs.get(tabAt).dir = currentDir;
    }
    tabAt = idx;
    File d = tabs.get(tabAt).dir;
    currentDir = d != null ? d : Environment.getExternalStorageDirectory();
    refresh();
    rebuildTabs();
  }
  private void askCloseTab(final int idx) {
    if (tabs.size() <= 1 || idx < 0 || idx >= tabs.size()) {
      Ui.toast(this, "Last tab");
      return;
    }
    if (AppSettings.get().optNoConfirmTab) {
      closeTab(idx);
      return;
    }
    Ui.confirm(this, Lang.t("Close this tab?"), new Runnable() {
      @Override
      public void run() {
        closeTab(idx);
      }
    });
  }
  private void closeTab(int idx) {
    if (tabs.size() <= 1 || idx < 0 || idx >= tabs.size()) {
      Ui.toast(this, "Last tab");
      return;
    }
    tabs.remove(idx);
    if (idx < tabAt) {
      tabAt--;
    }
    if (tabAt >= tabs.size()) {
      tabAt = tabs.size() - 1;
    }
    if (tabAt < 0) {
      tabAt = 0;
    }
    File d = tabs.get(tabAt).dir;
    currentDir = d != null ? d : Environment.getExternalStorageDirectory();
    refresh();
    rebuildTabs();
    saveTabs();
  }
  private void handlePull(MotionEvent e) {
    if (listView == null || pullView == null) {
      return;
    }
    int act = e.getActionMasked();
    boolean atTop = listView.getFirstVisiblePosition() == 0;
    View ch = listView.getChildAt(0);
    if (ch != null && ch.getTop() < -Theme.dp(this, 4)) {
      atTop = false;
    }
    if (act == MotionEvent.ACTION_DOWN) {
      pullY = atTop ? e.getY() : -1f;
      return;
    }
    int fire = Theme.dp(this, 72);
    if (act == MotionEvent.ACTION_MOVE && pullY >= 0f && atTop) {
      float d = e.getY() - pullY;
      if (d > Theme.dp(this, 28)) {
        pullView.setVisibility(View.VISIBLE);
        pullView.setText(d > fire ? "\u21bb  Release to refresh" : "\u21bb  Pull to refresh");
      } else {
        pullView.setVisibility(View.GONE);
      }
      return;
    }
    if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
      boolean go = pullView.getVisibility() == View.VISIBLE && pullY >= 0f
        && (e.getY() - pullY) > fire;
      pullView.setVisibility(View.GONE);
      pullY = -1f;
      if (go && act == MotionEvent.ACTION_UP) {
        OpLog.info(this, "refresh pull");
        refresh();
        Ui.toast(this, "Refreshed");
      }
    }
  }
  private void selectionMenu() {
    Ui.menu(this, "Selection",
      new String[]{"Select all", "Invert selection", "Clear selection"},
      new int[]{Theme.ACCENT, Theme.GOOD, Theme.DANGER}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            selectAll();
          } else if (w == 1) {
            invertSelection();
          } else {
            selected.clear();
            adapter.notifyDataSetChanged();
            updateHint(true);
          }
        }
      });
  }
  private void selectAll() {
    selected.clear();
    for (int i = 0; i < files.size(); i++) {
      if (!isUp(i)) {
        selected.add(files.get(i).getAbsolutePath());
      }
    }
    adapter.notifyDataSetChanged();
    updateHint(true);
    OpLog.info(this, "select all " + selected.size());
  }
  private void invertSelection() {
    HashSet<String> next = new HashSet<String>();
    for (int i = 0; i < files.size(); i++) {
      if (isUp(i)) {
        continue;
      }
      String p = files.get(i).getAbsolutePath();
      if (!selected.contains(p)) {
        next.add(p);
      }
    }
    selected.clear();
    selected.addAll(next);
    adapter.notifyDataSetChanged();
    updateHint(true);
  }
  private void bookmarkChipMenu(final Bookmarks.Item it) {
    Ui.menu(this, it.name, new String[]{"Edit", "Unmark"},
      new int[]{Theme.ACCENT, Theme.DANGER}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            editBookmark(it);
          } else {
            askUnmark(it);
          }
        }
      });
  }
  private void askUnmark(final Bookmarks.Item it) {
    Runnable go = new Runnable() {
      @Override
      public void run() {
        Bookmarks.remove(MainActivity.this, it.path);
        Ui.toast(MainActivity.this, "Unmark");
        rebuildMarks();
      }
    };
    if (AppSettings.get().optNoConfirmTab) {
      go.run();
      return;
    }
    Ui.confirm(this, Lang.t("Remove this bookmark?"), go);
  }
  private void editBookmark(final Bookmarks.Item it) {
    Ui.input(this, "Bookmark name:", it.name, new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0) {
          name = it.name;
        }
        final String nm = name;
        Ui.input(MainActivity.this, "Bookmark path:", it.path, new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              path = it.path;
            }
            Bookmarks.update(MainActivity.this, it.path, nm, path);
            Ui.toast(MainActivity.this, "Bookmark updated");
            rebuildMarks();
          }
        });
      }
    });
  }
  private void loadTabs() {
    loadTabsKeep();
    if (tabs.size() > 0) {
      int at = AppSettings.get().lastTab;
      if (at < 0 || at >= tabs.size()) {
        at = 0;
      }
      tabAt = at;
      if (tabs.get(tabAt).dir != null) {
        currentDir = tabs.get(tabAt).dir;
      }
    }
  }
  private void loadTabsKeep() {
    if (!AppSettings.tabsFilePresent(this)) {
      tabsReady = true;
      return;
    }
    java.util.List<AppSettings.TabItem> items = AppSettings.readTabs(this);
    if (items.size() == 0) {
      tabsReady = false;
      OpLog.err(this, "tabs file unreadable, keep current");
      return;
    }
    String cur = currentDir == null ? "" : currentDir.getAbsolutePath();
    List<Tab> loaded = new ArrayList<Tab>();
    int keep = 0;
    for (int i = 0; i < items.size(); i++) {
      AppSettings.TabItem it = items.get(i);
      Tab t = new Tab();
      t.dir = new File(it.path == null ? "/" : it.path);
      t.name = it.name;
      loaded.add(t);
      if (cur.equals(it.path)) {
        keep = i;
      }
    }
    tabs.clear();
    tabs.addAll(loaded);
    tabAt = keep;
    if (tabAt < 0 || tabAt >= tabs.size()) {
      tabAt = 0;
    }
    if (tabs.get(tabAt).dir != null) {
      currentDir = tabs.get(tabAt).dir;
    }
    tabsReady = true;
  }
  private void saveTabs() {
    if (AppSettings.holdSave || !tabsReady || tabs.size() == 0) {
      return;
    }
    if (tapH == null) {
      writeTabsNow();
      return;
    }
    if (tabsFlush != null) {
      tapH.removeCallbacks(tabsFlush);
    }
    tabsFlush = new Runnable() {
      @Override
      public void run() {
        writeTabsNow();
      }
    };
    tapH.postDelayed(tabsFlush, 2000);
  }
  private void writeTabsNow() {
    if (AppSettings.holdSave || !tabsReady || tabs.size() == 0) {
      return;
    }
    java.util.List<AppSettings.TabItem> items = new ArrayList<AppSettings.TabItem>();
    for (int i = 0; i < tabs.size(); i++) {
      Tab t = tabs.get(i);
      AppSettings.TabItem it = new AppSettings.TabItem();
      it.name = t.name == null ? "" : t.name;
      it.path = t.dir == null ? "/" : t.dir.getAbsolutePath();
      items.add(it);
    }
    AppSettings.get().lastTab = tabAt;
    AppSettings.writeTabs(this, items);
  }
  private void flushTabs() {
    if (tapH != null && tabsFlush != null) {
      tapH.removeCallbacks(tabsFlush);
    }
    writeTabsNow();
  }
  @Override
  protected void onPause() {
    flushTabs();
    super.onPause();
  }
  @Override
  protected void onDestroy() {
    flushTabs();
    Ui.dismissOpen(this);
    if (listView != null) {
      listView.setAdapter(null);
    }
    ZipBrowse.clear();
    MapBrowse.clear();
    Thumbs.clear();
    Icons.clear();
    PkgIcons.clear();
    ExtIcons.clear();
    if (tapH != null) {
      tapH.removeCallbacksAndMessages(null);
    }
    super.onDestroy();
  }
  @Override
  public void onBackPressed() {
    if (!selected.isEmpty()) {
      selected.clear();
      adapter.notifyDataSetChanged();
      updateHint(true);
      return;
    }
    File parent;
    if (ZipBrowse.isZipPath(currentDir.getAbsolutePath())) {
      parent = ZipBrowse.parent(currentDir);
    } else if (MapBrowse.isMapPath(currentDir.getAbsolutePath())) {
      parent = MapBrowse.parent(currentDir);
    } else {
      parent = currentDir.getParentFile();
    }
    if (AppSettings.get().backGoesUp && parent != null && currentDir.getAbsolutePath().length() > 1) {
      go(parent);
      return;
    }
    super.onBackPressed();
  }
}
