package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
final class ToastUI {
  private ToastUI() {}
  static void showT(Context c, String m) {
    Toast t = Toast.makeText(c, m, Toast.LENGTH_SHORT);
    View v = t.getView();
    if (v != null) {
      GradientDrawable s = new GradientDrawable();
      boolean dark = true;
      try {
        dark = AppSettings.get().darkTheme;
      } catch (Exception ignored) {}
      s.setColor(dark ? 0xDD333333 : 0xEEF0F2F5);
      s.setCornerRadius(35f);
      v.setBackground(s);
      TextView tv = (TextView) v.findViewById(android.R.id.message);
      if (tv != null) {
        tv.setTextColor(dark ? Color.WHITE : 0xFF121A22);
        tv.setGravity(Gravity.CENTER);
        v.setPadding(30, 2, 30, 2);
      }
    }
    t.show();
  }
}
