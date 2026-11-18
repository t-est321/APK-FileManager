package com.dlna.FileManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
final class OpLog {
  private static final long MAX_BYTES = 395264L;
  private static final Object OPS_LOCK = new Object();
  private static Handler logH;
  private OpLog() {}
  private static synchronized Handler logHandler() {
    if (logH == null) {
      HandlerThread t = new HandlerThread("fm-log");
      t.start();
      logH = new Handler(t.getLooper());
    }
    return logH;
  }
  static File file(Context c) {
    return new File(c.getCacheDir(), "ops.log");
  }
  static void info(Context c, String msg) {
    add(c, "I", msg, null);
  }
  static void err(Context c, String msg) {
    add(c, "E", msg, null);
  }
  static void err(Context c, String msg, Throwable t) {
    add(c, "E", msg, t);
  }
  static void add(Context c, String level, String msg, Throwable t) {
    if (c == null) {
      return;
    }
    boolean skipOps = false;
    try {
      skipOps = AppSettings.get().optNoLog;
    } catch (Exception ignored) {}
    String extra = "";
    if (t != null) {
      extra = ": " + t.getClass().getSimpleName();
      if (t.getMessage() != null) {
        extra = extra + " " + t.getMessage();
      }
    }
    String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
      + " [" + level + "] " + (msg == null ? "" : msg) + extra + "\n";
    if ("E".equals(level) || "C".equals(level)) {
      writeFatal(c, "C".equals(level), line, t);
    }
    if (skipOps) {
      return;
    }
    Context app = c.getApplicationContext();
    if (app == null) {
      app = c;
    }
    final Context ctx = app;
    final String toWrite = line;
    logHandler().post(new Runnable() {
      @Override
      public void run() {
        writeOps(ctx, toWrite);
      }
    });
  }
  private static void writeOps(Context c, String line) {
    synchronized (OPS_LOCK) {
      try {
        File f = file(c);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
          parent.mkdirs();
        }
        if (f.isFile() && f.length() > MAX_BYTES) {
          f.delete();
        }
        FileOutputStream os = new FileOutputStream(f, true);
        try {
          os.write(line.getBytes("UTF-8"));
        } finally {
          os.close();
        }
      } catch (Exception ignored) {}
    }
  }
  static void clearCrashLogs(Context c) {
    if (c == null) {
      return;
    }
    File dir = new File(c.getFilesDir(), "log");
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    for (int i = 0; i < kids.length; i++) {
      String n = kids[i].getName();
      if (n.startsWith("CrashLog_") || n.startsWith("ErrorLog_")) {
        kids[i].delete();
      }
    }
  }
  static void writeFatal(Context c, boolean crash, String line, Throwable t) {
    if (c == null) {
      return;
    }
    String stamp = new SimpleDateFormat("yyyy.MM.dd-HH-mm-ss", Locale.US).format(new Date());
    String name = (crash ? "CrashLog_" : "ErrorLog_") + stamp + ".log";
    StringBuilder sb = new StringBuilder();
    sb.append(line);
    if (!line.endsWith("\n")) {
      sb.append('\n');
    }
    if (t != null) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      sb.append(sw.toString());
    }
    writeOne(new File(c.getFilesDir(), "log"), name, sb.toString());
  }
  private static boolean writeOne(File dir, String name, String body) {
    if (dir == null) {
      return false;
    }
    try {
      if (!dir.exists() && !dir.mkdirs()) {
        if (!dir.exists()) {
          return false;
        }
      }
      FileOps.writeString(new File(dir, name), body);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  static String read(Context c) {
    try {
      File f = file(c);
      if (!f.isFile()) {
        return "(empty log)";
      }
      return FileOps.readString(f);
    } catch (Exception e) {
      return "(log read error)";
    }
  }
  static void clear(Context c) {
    try {
      file(c).delete();
    } catch (Exception ignored) {}
  }
}
