package com.dlna.FileManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
final class Incoming {
  private Incoming() {}
  static File save(Context c, Uri uri) {
    if (c == null || uri == null) {
      return null;
    }
    File dir = AppSettings.incomingDir(c);
    dir.mkdirs();
    String name = queryName(c, uri);
    if (name == null || name.length() == 0) {
      name = "incoming_" + System.currentTimeMillis();
    }
    name = name.replace('\\', '_').replace('/', '_');
    File dst = new File(dir, TrashStore.uniqueName(dir, name));
    InputStream in = null;
    FileOutputStream out = null;
    try {
      String scheme = uri.getScheme();
      if ("file".equals(scheme)) {
        String p = uri.getPath();
        if (p == null) {
          return null;
        }
        File src = new File(p);
        if (!src.isFile()) {
          return null;
        }
        in = new FileInputStream(src);
      } else {
        in = c.getContentResolver().openInputStream(uri);
      }
      if (in == null) {
        return null;
      }
      out = new FileOutputStream(dst);
      byte[] buf = new byte[16384];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
      out.flush();
      closeQuiet(out);
      out = null;
      File hit = FileOps.existingCrc(dir, dst);
      if (hit != null) {
        dst.delete();
        return hit;
      }
      return dst;
    } catch (Exception e) {
      OpLog.err(c, "incoming", e);
      return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
      if (out != null) {
        try {
          out.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static void closeQuiet(java.io.Closeable c) {
    if (c == null) {
      return;
    }
    try {
      c.close();
    } catch (Exception ignored) {}
  }
  private static String queryName(Context c, Uri uri) {
    Cursor cur = null;
    try {
      cur = c.getContentResolver().query(uri, null, null, null, null);
      if (cur != null && cur.moveToFirst()) {
        int idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (idx >= 0) {
          return cur.getString(idx);
        }
      }
    } catch (Exception ignored) {
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
    String last = uri.getLastPathSegment();
    return last;
  }
}
