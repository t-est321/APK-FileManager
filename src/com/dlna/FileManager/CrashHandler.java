package com.dlna.FileManager;
import android.content.Context;
import java.io.PrintWriter;
import java.io.StringWriter;
final class CrashHandler {
  private CrashHandler() {}
  static void install(Context c) {
    if (c == null) {
      return;
    }
    final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
    Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
    if (prev instanceof Guard) {
      return;
    }
    Thread.setDefaultUncaughtExceptionHandler(new Guard(app, prev));
  }
  private static final class Guard implements Thread.UncaughtExceptionHandler {
    private final Context app;
    private final Thread.UncaughtExceptionHandler prev;
    Guard(Context app, Thread.UncaughtExceptionHandler prev) {
      this.app = app;
      this.prev = prev;
    }
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      try {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String body = "crash thread=" + (t == null ? "?" : t.getName()) + "\n" + sw.toString();
        OpLog.writeFatal(app, true, body, e);
        OpLog.add(app, "C", body, e);
      } catch (Throwable ignored) {}
      if (prev != null) {
        prev.uncaughtException(t, e);
      } else {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
      }
    }
  }
}
