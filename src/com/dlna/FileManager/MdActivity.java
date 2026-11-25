package com.dlna.FileManager;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
public final class MdActivity extends Activity {
  private File file;
  private LinearLayout root;
  @Override
  protected void onCreate(Bundle b) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(b);
    String path = getIntent().getStringExtra("path");
    file = new File(path == null ? "" : path);
    Ui.bar(this);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Theme.BG);
    TextView name = new TextView(this);
    name.setTextColor(Theme.TEXT_DIM);
    name.setTextSize(12);
    name.setPadding(Theme.dp(this, 12), Theme.dp(this, 10), Theme.dp(this, 12), Theme.dp(this, 6));
    name.setText(file.getName());
    root.addView(name);
    ScrollView sv = new ScrollView(this);
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    int p = Theme.dp(this, 12);
    box.setPadding(p, p, p, p);
    sv.addView(box);
    Ui.styleScroll(this, sv);
    root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    Button edit = Ui.btn(this, "Open as text", Theme.ACCENT);
    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    elp.setMargins(Theme.dp(this, 8), Theme.dp(this, 4), Theme.dp(this, 8), Theme.dp(this, 8));
    edit.setLayoutParams(elp);
    edit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(MdActivity.this, EditorActivity.class);
        i.putExtra("path", file.getAbsolutePath());
        startActivity(i);
      }
    });
    root.addView(edit);
    setContentView(root);
    String src = "";
    try {
      src = FileOps.readString(file);
    } catch (Exception ignored) {}
    MdDraw.fill(this, box, src, file.getParentFile());
  }
  @Override
  protected void onResume() {
    super.onResume();
    Ui.applyKeepScreen(this);
  }
  @Override
  protected void onDestroy() {
    Ui.dismissOpen(this);
    recycleBitmaps(root);
    root = null;
    super.onDestroy();
  }
  private static void recycleBitmaps(View v) {
    if (v == null) {
      return;
    }
    if (v instanceof ImageView) {
      ImageView iv = (ImageView) v;
      Drawable d = iv.getDrawable();
      iv.setImageDrawable(null);
      if (d instanceof BitmapDrawable) {
        Bitmap b = ((BitmapDrawable) d).getBitmap();
        if (b != null && !b.isRecycled()) {
          b.recycle();
        }
      }
      return;
    }
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      int i;
      for (i = 0; i < g.getChildCount(); i++) {
        recycleBitmaps(g.getChildAt(i));
      }
    }
  }
}
