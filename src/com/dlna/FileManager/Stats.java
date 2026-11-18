package com.dlna.FileManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;
final class Stats {
  static final class FolderScan {
    int files;
    int dirs;
    long size;
    long ms;
  }
  private Stats() {}
  static String build(Context c) {
    Runtime rt = Runtime.getRuntime();
    long heapTotal = rt.totalMemory();
    long heapFree = rt.freeMemory();
    StringBuilder sb = new StringBuilder(256);
    sb.append("[ Heap ]\n");
    sb.append("Used: ").append(FileOps.humanSize(heapTotal - heapFree)).append('\n');
    sb.append("Free: ").append(FileOps.humanSize(heapFree)).append('\n');
    sb.append("Total: ").append(FileOps.humanSize(heapTotal));
    sb.append("\n\n[ Storage ]\n");
    sb.append(storageLine("External", Environment.getExternalStorageDirectory()));
    sb.append(storageLine("Internal", c.getFilesDir()));
    File appDir = c.getFilesDir().getParentFile();
    File settingsFile = AppSettings.file(c);
    sb.append("\n[ App Data ]\n");
    sb.append("Path: ").append(appDir.getAbsolutePath()).append('\n');
    sb.append("Settings: ").append(FileOps.humanSize(settingsFile.exists() ? settingsFile.length() : 0));
    sb.append("\n\n[ Public Export ]\n");
    File pub = AppSettings.publicSettings();
    sb.append(pub.getAbsolutePath()).append('\n');
    sb.append(pub.exists() ? "present, " + FileOps.humanSize(pub.length()) : "not exported yet");
    sb.append("\n\n[ Package ]\n");
    sb.append(c.getPackageName());
    return sb.toString();
  }
  static FolderScan scan(File dir) {
    FolderScan s = new FolderScan();
    long t0 = System.currentTimeMillis();
    walk(dir, s, 0);
    s.ms = System.currentTimeMillis() - t0;
    return s;
  }
  private static void walk(File dir, FolderScan s, int depth) {
    if (dir == null || FileOps.cancelled()) {
      return;
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    for (int i = 0; i < kids.length; i++) {
      if (FileOps.cancelled()) {
        return;
      }
      File f = kids[i];
      boolean d = f.isDirectory();
      if (d) {
        s.dirs++;
      } else {
        s.files++;
        s.size += f.length();
      }
      if (d && depth < 6) {
        walk(f, s, depth + 1);
      }
    }
  }
  private static String storageLine(String label, File path) {
    try {
      StatFs fs = new StatFs(path.getAbsolutePath());
      long total = (long) fs.getBlockSize() * fs.getBlockCount();
      long free = (long) fs.getBlockSize() * fs.getAvailableBlocks();
      return label + ": " + FileOps.humanSize(total - free) + " used / "
        + FileOps.humanSize(total) + " total\n";
    } catch (Exception e) {
      return label + ": n/a\n";
    }
  }
}
