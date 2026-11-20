package com.dlna.FileManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
final class Ui {
  interface OnText {
    void on(String text);
  }
  interface OnPick {
    void onPick(int which);
  }
  private static final ArrayList<WeakReference<Dialog>> OPEN = new ArrayList<WeakReference<Dialog>>();
  private Ui() {}
  static void dismissOpen() {
    int i;
    for (i = 0; i < OPEN.size(); i++) {
      Dialog d = OPEN.get(i).get();
      if (d != null && d.isShowing()) {
        try {
          d.dismiss();
        } catch (Exception ignored) {}
      }
    }
    OPEN.clear();
  }
  private static void pruneOpen() {
    int i = 0;
    while (i < OPEN.size()) {
      Dialog d = OPEN.get(i).get();
      if (d == null || !d.isShowing()) {
        OPEN.remove(i);
      } else {
        i++;
      }
    }
  }
  static void applyTheme(Activity a) {
    AppSettings s = AppSettings.get();
    Theme.applyAccent(s.accentColor, s.darkTheme);
    if (s.darkTheme) {
      a.setTheme(android.R.style.Theme_NoTitleBar);
    } else {
      a.setTheme(android.R.style.Theme_Light_NoTitleBar);
    }
  }
  static void bar(Activity a) {
    AppSettings s = AppSettings.get();
    Theme.applyAccent(s.accentColor, s.darkTheme);
    Window w = a.getWindow();
    if (w != null) {
      w.setBackgroundDrawable(new ColorDrawable(Theme.BG));
    }
    if (Build.VERSION.SDK_INT >= 21 && w != null) {
      w.setStatusBarColor(Theme.BG);
      w.setNavigationBarColor(Theme.BG);
    }
    if (Build.VERSION.SDK_INT >= 23 && w != null) {
      View decor = w.getDecorView();
      int fl = decor.getSystemUiVisibility();
      if (s.darkTheme) {
        fl = fl & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        fl = fl | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      decor.setSystemUiVisibility(fl);
    }
    applyKeepScreen(a);
  }
  static void applyKeepScreen(Activity a) {
    Window w = a.getWindow();
    if (w == null) {
      return;
    }
    if (AppSettings.get().keepScreenOn) {
      w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }
  static void toast(Context c, String msg) {
    ToastUI.showT(c, msg);
  }
  static void styleScroll(Context c, View v) {
    try {
      v.setVerticalScrollBarEnabled(false);
      v.setHorizontalScrollBarEnabled(false);
    } catch (Throwable ignored) {}
  }
  static void styleList(Context c, ListView lv) {
    lv.setSelector(new ColorDrawable(Color.TRANSPARENT));
    lv.setDivider(new ColorDrawable(Theme.BG));
    lv.setDividerHeight(Theme.dp(c, 3));
    lv.setPadding(Theme.dp(c, 6), Theme.dp(c, 4), Theme.dp(c, 6), Theme.dp(c, 4));
    lv.setClipToPadding(false);
    lv.setCacheColorHint(Theme.BG);
    lv.setBackgroundColor(Theme.BG);
    styleScroll(c, lv);
  }
  static LinearLayout box(Context c, String title) {
    LinearLayout root = new LinearLayout(c);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackground(Theme.rounded(c, Theme.SURFACE, 14));
    int p = Theme.dp(c, 16);
    root.setPadding(p, p, p, Theme.dp(c, 10));
    if (title != null) {
      TextView t = text(c, title, Theme.TEXT, 16, true);
      t.setPadding(0, 0, 0, Theme.dp(c, 10));
      root.addView(t);
    }
    return root;
  }
  static TextView text(Context c, String s, int color, int sp, boolean bold) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextColor(color);
    t.setTextSize(sp);
    if (bold) {
      t.setTypeface(Typeface.DEFAULT_BOLD);
    }
    return t;
  }
  static Dialog showBox(Context c, View root) {
    pruneOpen();
    Dialog d = new Dialog(c);
    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
    FrameLayout outer = new FrameLayout(c);
    int m = Theme.dp(c, Theme.compact(c) ? 12 : 28);
    outer.setPadding(m, 0, m, 0);
    outer.addView(root, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
    d.setContentView(outer);
    d.show();
    OPEN.add(new WeakReference<Dialog>(d));
    Window w = d.getWindow();
    if (w != null) {
      w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    return d;
  }
  private static LinearLayout btnRow(Context c, final Dialog[] hd, String[] labels,
      int[] colors, final OnPick cb) {
    LinearLayout r = new LinearLayout(c);
    r.setPadding(0, Theme.dp(c, 10), 0, 0);
    for (int i = 0; i < labels.length; i++) {
      final int idx = i;
      Button b = btn(c, Lang.t(labels[i]), colors[i]);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      lp.setMargins(Theme.dp(c, 2), 0, Theme.dp(c, 2), 0);
      b.setLayoutParams(lp);
      b.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (hd[0] != null) {
            hd[0].dismiss();
          }
          if (cb != null) {
            cb.onPick(idx);
          }
        }
      });
      r.addView(b);
    }
    return r;
  }
  static void alert(Context c, String title, String msg) {
    alert(c, title, msg, false);
  }
  static void alert(Context c, String title, String msg, boolean mono) {
    LinearLayout b = box(c, title);
    TextView m = text(c, msg, Theme.TEXT_DIM, mono ? 12 : 14, false);
    if (mono) {
      m.setTypeface(Typeface.MONOSPACE);
      m.setTextColor(Theme.TEXT);
    }
    final int maxH = Math.max(Theme.dp(c, 200),
      (c.getResources().getDisplayMetrics().heightPixels * 3) / 5);
    ScrollView sc = new ScrollView(c) {
      @Override
      protected void onMeasure(int w, int h) {
        super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
      }
    };
    sc.setFillViewport(false);
    styleScroll(c, sc);
    sc.addView(m);
    b.addView(sc, new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    final Dialog[] hd = new Dialog[1];
    b.addView(btnRow(c, hd, new String[]{"OK"}, new int[]{Theme.ACCENT}, null));
    hd[0] = showBox(c, b);
  }
  static void confirm(Context c, String msg, final Runnable yes) {
    confirm(c, msg, yes, null);
  }
  static void confirm(Context c, String msg, final Runnable yes, final Runnable no) {
    LinearLayout b = box(c, null);
    b.addView(text(c, msg, Theme.TEXT_DIM, 14, false));
    final Dialog[] hd = new Dialog[1];
    b.addView(btnRow(c, hd, new String[]{"Yes", "No"},
      new int[]{Theme.ACCENT, Theme.TEXT_DIM}, new OnPick() {
        @Override
        public void onPick(int which) {
          if (which == 0) {
            if (yes != null) {
              yes.run();
            }
          } else if (no != null) {
            no.run();
          }
        }
      }));
    hd[0] = showBox(c, b);
  }
  static void menu(Context c, String title, String[] items, int[] colors, final OnPick cb) {
    LinearLayout b = box(c, title);
    final Dialog[] hd = new Dialog[1];
    LinearLayout list = new LinearLayout(c);
    list.setOrientation(LinearLayout.VERTICAL);
    for (int i = 0; i < items.length; i++) {
      final int idx = i;
      int color = colors != null && i < colors.length ? colors[i] : Theme.ACCENT;
      Button it = btn(c, Lang.t(items[idx]), color);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, Theme.dp(c, 3), 0, Theme.dp(c, 3));
      it.setLayoutParams(lp);
      it.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
      int ph = Theme.dp(c, 14);
      it.setPadding(ph, Theme.dp(c, 8), ph, Theme.dp(c, 8));
      it.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (hd[0] != null) {
            hd[0].dismiss();
          }
          cb.onPick(idx);
        }
      });
      list.addView(it);
    }
    final int maxH = Math.max(Theme.dp(c, 240),
      (c.getResources().getDisplayMetrics().heightPixels * 3) / 5);
    ScrollView sc = new ScrollView(c) {
      @Override
      protected void onMeasure(int w, int h) {
        super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
      }
    };
    sc.setFillViewport(false);
    styleScroll(c, sc);
    sc.addView(list);
    b.addView(sc, new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    b.addView(btnRow(c, hd, new String[]{"Cancel"}, new int[]{Theme.TEXT_DIM}, null));
    hd[0] = showBox(c, b);
  }
  static void askPass(Context c, final Runnable ok) {
    if (!AppSettings.get().hasLockPass()) {
      ok.run();
      return;
    }
    input(c, "Lock password", "", new OnText() {
      @Override
      public void on(String t) {
        if (AppSettings.get().checkLockPass(t)) {
          ok.run();
        } else {
          toast(c, "Wrong password");
        }
      }
    });
  }
  static void input(Context c, String title, String def, final OnText cb) {
    LinearLayout b = box(c, title);
    final EditText et = new EditText(c);
    et.setSingleLine(true);
    et.setTextColor(Theme.TEXT);
    et.setBackground(Theme.rounded(c, Theme.SURFACE_HI, 8));
    et.setText(def);
    et.setSelection(def.length());
    int p = Theme.dp(c, 12);
    et.setPadding(p, Theme.dp(c, 10), p, Theme.dp(c, 10));
    b.addView(et);
    final Dialog[] hd = new Dialog[1];
    b.addView(btnRow(c, hd, new String[]{"OK", "Cancel"},
      new int[]{Theme.ACCENT, Theme.TEXT_DIM}, new OnPick() {
        @Override
        public void onPick(int which) {
          if (which == 0) {
            cb.on(et.getText().toString().trim());
          }
        }
      }));
    hd[0] = showBox(c, b);
  }
  interface Work {
    void run(Progress p);
  }
  static final class Progress {
    private final WeakReference<Activity> aref;
    private final WeakReference<TextView> tvRef;
    private final WeakReference<TextView> pctRef;
    private final WeakReference<ProgressBar> barRef;
    Progress(Activity a, TextView tv, TextView pct, ProgressBar bar) {
      this.aref = new WeakReference<Activity>(a);
      this.tvRef = new WeakReference<TextView>(tv);
      this.pctRef = new WeakReference<TextView>(pct);
      this.barRef = new WeakReference<ProgressBar>(bar);
    }
    void msg(final String s) {
      final Activity a = aref.get();
      final TextView tv = tvRef.get();
      if (a == null || tv == null || s == null) {
        return;
      }
      a.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          tv.setText(s);
        }
      });
    }
    void set(final long done, final long total) {
      final Activity a = aref.get();
      if (a == null) {
        return;
      }
      final TextView pct = pctRef.get();
      final ProgressBar bar = barRef.get();
      a.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          int p;
          if (total <= 0L) {
            p = done > 0L ? 100 : 0;
          } else {
            long v = (done * 100L) / total;
            if (v < 0L) {
              v = 0L;
            }
            if (v > 100L) {
              v = 100L;
            }
            p = (int) v;
          }
          if (pct != null) {
            pct.setText(p + "%");
          }
          if (bar != null) {
            bar.setFraction(p / 100f);
          }
        }
      });
    }
  }
  static void bg(final Activity a, String msg, final Runnable work, final Runnable done) {
    bg(a, msg, new Work() {
      @Override
      public void run(Progress p) {
        work.run();
      }
    }, done);
  }
  static void bg(final Activity a, String msg, final Work work, final Runnable done) {
    LinearLayout box = box(a, null);
    final TextView tv = text(a, msg, Theme.TEXT, 14, false);
    box.addView(tv);
    LinearLayout row = new LinearLayout(a);
    row.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    rlp.topMargin = Theme.dp(a, 12);
    row.setLayoutParams(rlp);
    final ProgressBar bar = new ProgressBar(a);
    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
      0, Theme.dp(a, 8), 1f);
    row.addView(bar, blp);
    final TextView pct = text(a, "0%", Theme.ACCENT, 12, true);
    pct.setPadding(Theme.dp(a, 8), 0, 0, 0);
    row.addView(pct);
    box.addView(row);
    final boolean[] cancelled = {false};
    Button cancel = btn(a, "Cancel", Theme.DANGER);
    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    clp.topMargin = Theme.dp(a, 10);
    cancel.setLayoutParams(clp);
    box.addView(cancel);
    FileOps.setCancel(false);
    FileOps.clearProg();
    final Dialog d = showBox(a, box);
    d.setCancelable(false);
    bar.startAnim();
    final WeakReference<Activity> aref = new WeakReference<Activity>(a);
    final Progress pg = new Progress(a, tv, pct, bar);
    final Thread th = new Thread(new Runnable() {
      @Override
      public void run() {
        FileOps.progCb = new FileOps.Prog() {
          @Override
          public void on(long done, long total) {
            pg.set(done, total);
          }
        };
        try {
          work.run(pg);
        } catch (Exception e) {
          Activity act = aref.get();
          if (act != null) {
            OpLog.err(act, "bg work", e);
          }
        } finally {
          FileOps.setCancel(false);
          FileOps.clearProg();
          final Activity ui = aref.get();
          if (ui == null) {
            try {
              d.dismiss();
            } catch (Exception ignored) {}
            return;
          }
          ui.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              bar.stopAnim();
              boolean gone;
              if (Build.VERSION.SDK_INT >= 17) {
                gone = ui.isFinishing() || ui.isDestroyed();
              } else {
                gone = ui.isFinishing();
              }
              try {
                d.dismiss();
              } catch (Exception ignored) {}
              if (!gone && !cancelled[0] && done != null) {
                done.run();
              }
            }
          });
        }
      }
    }, "fm-bg");
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        cancelled[0] = true;
        FileOps.setCancel(true);
        FileOps.clearProg();
        th.interrupt();
        bar.stopAnim();
        try {
          d.dismiss();
        } catch (Exception ignored) {}
        Activity act = aref.get();
        if (act != null) {
          OpLog.info(act, "bg cancelled: " + msg);
          Ui.toast(act, "Cancelled");
        }
      }
    });
    th.start();
  }
  static Button btn(Context c, String label, int textColor) {
    Button b = new Button(c);
    b.setText(label);
    b.setTextColor(textColor);
    b.setTextSize(13);
    b.setAllCaps(false);
    b.setBackground(Theme.rounded(c, Theme.SURFACE_HI, 12));
    boolean small = Theme.compact(c);
    b.setMinHeight(Theme.dp(c, small ? 38 : 42));
    b.setMinimumHeight(0);
    b.setMinWidth(0);
    int ph = Theme.dp(c, 10);
    b.setPadding(ph, Theme.dp(c, small ? 6 : 7), ph, Theme.dp(c, small ? 6 : 7));
    if (Build.VERSION.SDK_INT >= 21) {
      b.setStateListAnimator(null);
      b.setElevation(0f);
    }
    return b;
  }
  static Button chip(Context c, String label) {
    Button b = new Button(c);
    b.setText(label);
    b.setTextColor(Theme.ACCENT);
    b.setTextSize(12);
    b.setAllCaps(false);
    b.setBackground(Theme.outline(c, Theme.SURFACE, Theme.STROKE, 12));
    b.setMinHeight(Theme.dp(c, Theme.compact(c) ? 36 : 32));
    b.setMinimumHeight(0);
    int ph = Theme.dp(c, 10);
    b.setPadding(ph, Theme.dp(c, Theme.compact(c) ? 6 : 4), ph, Theme.dp(c, Theme.compact(c) ? 6 : 4));
    if (Build.VERSION.SDK_INT >= 21) {
      b.setStateListAnimator(null);
      b.setElevation(0f);
    }
    return b;
  }
  static ImageView iconView(View row) {
    FrameLayout box = (FrameLayout) ((LinearLayout) row).getChildAt(0);
    return (ImageView) box.getChildAt(0);
  }
  static ImageView pkgView(View row) {
    return (ImageView) ((LinearLayout) row).getChildAt(1);
  }
  static TextView dotsView(View row) {
    LinearLayout r = (LinearLayout) row;
    return (TextView) r.getChildAt(r.getChildCount() - 1);
  }
  static View row(Context c, View convert, int kind, String title, String sub,
      int titleColor, boolean sel) {
    LinearLayout r;
    FrameLayout iconBox;
    ImageView iv;
    TextView tv;
    TextView sv;
    TextView dots;
    ImageView pkg;
    if (convert instanceof LinearLayout && ((LinearLayout) convert).getChildCount() >= 4) {
      r = (LinearLayout) convert;
      iconBox = (FrameLayout) r.getChildAt(0);
      iv = (ImageView) iconBox.getChildAt(0);
      iv.setTag(null);
      pkg = (ImageView) r.getChildAt(1);
      LinearLayout col = (LinearLayout) r.getChildAt(2);
      tv = (TextView) col.getChildAt(0);
      sv = (TextView) col.getChildAt(1);
      dots = (TextView) r.getChildAt(3);
    } else {
      r = new LinearLayout(c);
      r.setOrientation(LinearLayout.HORIZONTAL);
      r.setGravity(Gravity.CENTER_VERTICAL);
      int ph = Theme.dp(c, Theme.compact(c) ? 10 : 12);
      int pv = Theme.dp(c, Theme.compact(c) ? 10 : 9);
      r.setPadding(ph, pv, Theme.dp(c, 8), pv);
      iconBox = new FrameLayout(c);
      iv = new ImageView(c);
      iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
      int sz = Theme.dp(c, 38);
      int ip = Theme.dp(c, 4);
      iconBox.setPadding(ip, ip, ip, ip);
      iconBox.addView(iv, new FrameLayout.LayoutParams(sz, sz));
      LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(sz + ip * 2, sz + ip * 2);
      blp.setMargins(0, 0, Theme.dp(c, 6), 0);
      r.addView(iconBox, blp);
      pkg = new ImageView(c);
      pkg.setScaleType(ImageView.ScaleType.FIT_CENTER);
      int pz = Theme.dp(c, 20);
      LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(pz, pz);
      plp.setMargins(0, 0, Theme.dp(c, 8), 0);
      r.addView(pkg, plp);
      LinearLayout col = new LinearLayout(c);
      col.setOrientation(LinearLayout.VERTICAL);
      tv = new TextView(c);
      tv.setSingleLine(true);
      tv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
      col.addView(tv);
      sv = new TextView(c);
      sv.setSingleLine(true);
      sv.setEllipsize(android.text.TextUtils.TruncateAt.END);
      col.addView(sv);
      r.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      dots = new TextView(c);
      dots.setText("\u22ee");
      dots.setTextColor(Theme.TEXT_DIM);
      dots.setTextSize(22);
      dots.setGravity(Gravity.CENTER);
      dots.setClickable(true);
      dots.setFocusable(true);
      int dw = Theme.dp(c, 36);
      r.addView(dots, new LinearLayout.LayoutParams(dw, LinearLayout.LayoutParams.MATCH_PARENT));
    }
    AppSettings st = AppSettings.get();
    int tSp = AppSettings.LIST_SP[st.listFont];
    int tint = kind == FileGlyph.UP ? Theme.ACCENT
      : (kind == FileGlyph.DIR || kind == FileGlyph.LINK ? Icons.dirTint() : Icons.tintFor(kind));
    Bitmap custom = null;
    if (kind != FileGlyph.DIR && kind != FileGlyph.LINK && kind != FileGlyph.UP) {
      custom = ExtIcons.get(c, FileOps.ext(title));
    }
    iv.setImageBitmap(custom != null ? custom : Icons.of(kind, tint));
    iconBox.setBackground(Theme.rounded(c, Theme.SURFACE_HI, 10));
    iconBox.setVisibility(st.showIcons ? View.VISIBLE : View.GONE);
    pkg.setImageBitmap(null);
    pkg.setVisibility(View.GONE);
    tv.setText(title);
    tv.setTextSize(tSp);
    tv.setTextColor(titleColor);
    sv.setText(sub);
    sv.setTextSize(Math.max(10, tSp - 3));
    sv.setTextColor(Theme.TEXT_DIM);
    sv.setVisibility(st.showDetails && sub.length() > 0 ? View.VISIBLE : View.GONE);
    r.setBackground(sel ? Theme.rounded(c, Theme.SEL, 14) : Theme.rounded(c, Theme.SURFACE, 12));
    dots.setVisibility(View.GONE);
    dots.setOnClickListener(null);
    return r;
  }
}
