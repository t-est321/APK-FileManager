package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.File;
final class ExtIcons {
  private static final int SZ = 96;
  private static final SoftBmp CACHE = new SoftBmp(32);
  private ExtIcons() {}
  static File dir(Context c) {
    return new File(c.getFilesDir(), "ext_icons");
  }
  static File svgFile(Context c, String ext) {
    return new File(dir(c), ext + ".svg");
  }
  static boolean save(Context c, String ext, File src) {
    String e = AppSettings.normExts(ext);
    if (e.length() == 0 || src == null || !src.isFile()) {
      return false;
    }
    dir(c).mkdirs();
    File dst = svgFile(c, e);
    try {
      FileOps.copyRec(src, dst);
      CACHE.clear();
      return dst.exists();
    } catch (Exception ex) {
      return false;
    }
  }
  static Bitmap get(Context c, String ext) {
    if (ext == null || ext.length() == 0) {
      return null;
    }
    String e = ext.toLowerCase(java.util.Locale.US);
    Bitmap cached = CACHE.get(e);
    if (cached != null) {
      return cached;
    }
    File f = svgFile(c, e);
    if (!f.isFile()) {
      return null;
    }
    try {
      Bitmap b = SvgDraw.render(FileOps.readString(f), SZ);
      if (b != null) {
        CACHE.put(e, b);
      }
      return b;
    } catch (Exception ex) {
      return null;
    }
  }
  static void clear() {
    CACHE.clear();
  }
}
