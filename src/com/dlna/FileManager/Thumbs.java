package com.dlna.FileManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
final class Thumbs {
  private static int target() {
    int n = 192;
    try {
      n = AppSettings.get().thumbPx();
    } catch (Exception ignored) {}
    if (n < 48) {
      n = 48;
    }
    return n;
  }
  private static final SoftBmp CACHE = new SoftBmp(24);
  private static Handler handler;
  private static ThreadPoolExecutor exec;
  private Thumbs() {}
  private static synchronized void ensureInit() {
    if (handler == null) {
      handler = new Handler(Looper.getMainLooper());
    }
    if (exec == null || exec.isShutdown()) {
      exec = new ThreadPoolExecutor(1, 1, 20L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(8), new ThreadPoolExecutor.DiscardOldestPolicy());
      exec.allowCoreThreadTimeOut(true);
    }
  }
  static synchronized void clear() {
    CACHE.clear();
    if (exec != null) {
      exec.shutdownNow();
      exec = null;
    }
    if (handler != null) {
      handler.removeCallbacksAndMessages(null);
      handler = null;
    }
  }
  static void bind(Context c, ImageView iv, String path, int kind) {
    if (AppSettings.get().optNoThumbs) {
      return;
    }
    final String p = path;
    final int k = kind;
    Bitmap cached = CACHE.get(p);
    if (cached != null) {
      iv.setTag(p);
      iv.setImageBitmap(cached);
      return;
    }
    iv.setTag(p);
    iv.setImageBitmap(Icons.of(k, k == FileGlyph.DIR ? Icons.dirTint() : Icons.tintFor(k)));
    if (k != FileGlyph.IMG && k != FileGlyph.VIDEO && k != FileGlyph.APK) {
      return;
    }
    ensureInit();
    final ThreadPoolExecutor pool = exec;
    final Handler h = handler;
    if (pool == null || h == null) {
      return;
    }
    final WeakReference<ImageView> ref = new WeakReference<ImageView>(iv);
    final WeakReference<Context> cref = new WeakReference<Context>(c);
    try {
      pool.execute(new Runnable() {
        @Override
        public void run() {
          Context ctx = cref.get();
          final Bitmap d = ctx == null ? null : decode(ctx, p, k);
          if (d == null) {
            return;
          }
          CACHE.put(p, d);
          Handler ui = handler;
          if (ui == null) {
            return;
          }
          ui.post(new Runnable() {
            @Override
            public void run() {
              ImageView v = ref.get();
              if (v != null && p.equals(v.getTag())) {
                v.setImageBitmap(d);
              }
            }
          });
        }
      });
    } catch (Exception ignored) {}
  }
  private static Bitmap decode(Context c, String path, int kind) {
    Bitmap raw = null;
    if (kind == FileGlyph.APK) {
      raw = apkIcon(c, path);
      if (raw == null) {
        return null;
      }
    } else if (kind == FileGlyph.IMG) {
      String low = path.toLowerCase(java.util.Locale.US);
      if (low.endsWith(".svg")) {
        try {
          return SvgDraw.render(FileOps.readString(new java.io.File(path)), target());
        } catch (Exception e) {
          return null;
        }
      }
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      decodePath(path, bounds);
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null;
      }
      int cap = target() * 2;
      try {
        if (AppSettings.get().imgCompress) {
          cap = Math.max(48, target());
        }
      } catch (Exception ignored) {}
      int sample = 1;
      while (bounds.outWidth / sample > cap || bounds.outHeight / sample > cap) {
        sample *= 2;
      }
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inSampleSize = sample;
      o.inPreferredConfig = Bitmap.Config.RGB_565;
      try {
        raw = decodePath(path, o);
      } catch (OutOfMemoryError e) {
        return null;
      }
    } else {
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      java.io.FileInputStream fis = null;
      try {
        fis = new java.io.FileInputStream(path);
        mmr.setDataSource(fis.getFD());
        raw = mmr.getFrameAtTime(0);
      } catch (Exception e) {
        try {
          mmr.setDataSource(path);
          raw = mmr.getFrameAtTime(0);
        } catch (Exception e2) {
          return null;
        }
      } finally {
        if (fis != null) {
          try {
            fis.close();
          } catch (Exception ignored) {}
        }
        try {
          mmr.release();
        } catch (Exception ignored) {}
      }
    }
    if (raw == null) {
      return null;
    }
    int w = raw.getWidth();
    int h = raw.getHeight();
    if (w > target() || h > target()) {
      float s = target() / (float) Math.max(w, h);
      Bitmap scaled = Bitmap.createScaledBitmap(raw, Math.max(1, Math.round(w * s)),
        Math.max(1, Math.round(h * s)), true);
      if (scaled != raw) {
        raw.recycle();
      }
      return scaled;
    }
    return raw;
  }
  private static Bitmap decodePath(String path, BitmapFactory.Options o) {
    java.io.FileInputStream in = null;
    try {
      in = new java.io.FileInputStream(path);
      return BitmapFactory.decodeStream(in, null, o);
    } catch (Exception e) {
      return BitmapFactory.decodeFile(path, o);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static Bitmap apkIcon(Context c, String path) {
    try {
      PackageManager pm = c.getPackageManager();
      PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
      if (pi == null || pi.applicationInfo == null) {
        return null;
      }
      pi.applicationInfo.sourceDir = path;
      pi.applicationInfo.publicSourceDir = path;
      Drawable d = pi.applicationInfo.loadIcon(pm);
      return PkgIcons.toBitmap(d, target());
    } catch (Throwable t) {
      return null;
    }
  }
}
