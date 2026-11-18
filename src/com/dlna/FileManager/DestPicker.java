package com.dlna.FileManager;
import android.app.Activity;
import android.app.Dialog;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
final class DestPicker {
  interface OnDir {
    void on(File dir);
  }
  private DestPicker() {}
  static void show(Activity a, File start, String title, final OnDir cb) {
    final File[] cur = {start != null && start.isDirectory() ? start : Environment.getExternalStorageDirectory()};
    LinearLayout box = Ui.box(a, title);
    final TextView path = Ui.text(a, cur[0].getAbsolutePath(), Theme.ACCENT, 11, false);
    path.setSingleLine(true);
    path.setEllipsize(android.text.TextUtils.TruncateAt.START);
    box.addView(path);
    final LinearLayout list = new LinearLayout(a);
    list.setOrientation(LinearLayout.VERTICAL);
    ScrollView sc = new ScrollView(a);
    sc.addView(list);
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(a, 240));
    slp.topMargin = Theme.dp(a, 8);
    box.addView(sc, slp);
    final Dialog[] hd = new Dialog[1];
    final Runnable[] fill = new Runnable[1];
    fill[0] = new Runnable() {
      @Override
      public void run() {
        list.removeAllViews();
        path.setText(cur[0].getAbsolutePath());
        File[] kids = FileOps.list(cur[0], AppSettings.get().showHidden, 0, true);
        File[] extra = Probe.forPath(a, cur[0].getAbsolutePath());
        if (extra.length > 0) {
          kids = FileOps.merge(kids, extra);
        }
        int n = 0;
        for (int i = 0; i < kids.length; i++) {
          if (!kids[i].isDirectory()) {
            continue;
          }
          if (FileOps.skipListed(cur[0], kids[i])) {
            continue;
          }
          n++;
          final File d = kids[i];
          Button b = Ui.btn(a, d.getName(), Theme.TEXT);
          b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
          LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
          lp.topMargin = Theme.dp(a, 3);
          b.setLayoutParams(lp);
          b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              cur[0] = d;
              fill[0].run();
            }
          });
          list.addView(b);
        }
        if (n == 0) {
          list.addView(Ui.text(a, "No folders", Theme.TEXT_DIM, 13, false));
        }
      }
    };
    LinearLayout row = new LinearLayout(a);
    row.setPadding(0, Theme.dp(a, 10), 0, 0);
    Button up = Ui.btn(a, "Up", Theme.TEXT_DIM);
    up.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    up.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        File p = cur[0].getParentFile();
        if (p != null) {
          cur[0] = p;
          fill[0].run();
        }
      }
    });
    Button here = Ui.btn(a, "Here", Theme.GOOD);
    here.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    here.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (hd[0] != null) {
          hd[0].dismiss();
        }
        cb.on(cur[0]);
      }
    });
    Button cancel = Ui.btn(a, "Cancel", Theme.TEXT_DIM);
    cancel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (hd[0] != null) {
          hd[0].dismiss();
        }
      }
    });
    row.addView(up);
    row.addView(here);
    row.addView(cancel);
    box.addView(row);
    fill[0].run();
    hd[0] = Ui.showBox(a, box);
  }
}
