package com.dlna.FileManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.File;
final class PkgIcons {
  private static final SoftBmp CACHE = new SoftBmp(24);
  private PkgIcons() {}
  static boolean host(String dirPath) {
    if (dirPath == null) {
      return false;
    }
    if (dirPath.endsWith("/Android/data") || dirPath.endsWith("/Android/obb")) {
      return true;
    }
    if ("/data/data".equals(dirPath) || "/data/user/0".equals(dirPath)
        || "/data/app".equals(dirPath) || "/system/app".equals(dirPath)
        || "/system/priv-app".equals(dirPath) || "/product/app".equals(dirPath)
        || "/vendor/app".equals(dirPath)) {
      return true;
    }
    if (dirPath.startsWith("/data/app/") || dirPath.startsWith("/system/app/")
        || dirPath.startsWith("/system/priv-app/")) {
      return true;
    }
    return false;
  }
  static Bitmap of(Context c, File f) {
    if (c == null || f == null) {
      return null;
    }
    String pkg = pkgOf(f);
    if (pkg == null || pkg.length() == 0) {
      return null;
    }
    Bitmap hit = CACHE.get(pkg);
    if (hit != null) {
      return hit;
    }
    int sz = Theme.dp(c, 20);
    try {
      PackageManager pm = c.getPackageManager();
      ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
      Drawable d = pm.getApplicationIcon(ai);
      Bitmap bmp = toBitmap(d, sz);
      if (bmp != null) {
        CACHE.put(pkg, bmp);
      }
      return bmp;
    } catch (Exception e) {
      return null;
    }
  }
  static void clear() {
    CACHE.clear();
  }
  static String pkgOf(File f) {
    String n = f.getName();
    if (n.length() == 0) {
      return null;
    }
    int dash = n.indexOf('-');
    if (dash > 0 && n.indexOf('.') >= 0) {
      return n.substring(0, dash);
    }
    return n;
  }
  static Bitmap toBitmap(Drawable d, int sz) {
    if (d == null || sz <= 0) {
      return null;
    }
    Bitmap bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmp);
    if (d instanceof BitmapDrawable) {
      Bitmap b = ((BitmapDrawable) d).getBitmap();
      if (b != null && !b.isRecycled()) {
        android.graphics.RectF r = new android.graphics.RectF(0, 0, sz, sz);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG
          | android.graphics.Paint.ANTI_ALIAS_FLAG);
        c.drawBitmap(b, null, r, p);
        return bmp;
      }
    }
    d.setBounds(0, 0, sz, sz);
    d.draw(c);
    return bmp;
  }
}
