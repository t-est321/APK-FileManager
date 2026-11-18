package com.dlna.FileManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
final class TermShell {
  File cwd;
  private final Context ctx;
  private final ArrayList<String> hist = new ArrayList<String>();
  private int histAt = -1;
  TermShell(Context c, File start) {
    ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
    cwd = start != null ? start : Environment.getExternalStorageDirectory();
    if (cwd == null) {
      cwd = new File("/");
    }
  }
  String exec(String raw) {
    if (raw == null) {
      return "";
    }
    String line = raw.trim();
    if (line.length() == 0) {
      return "";
    }
    hist.add(line);
    if (hist.size() > 80) {
      hist.remove(0);
    }
    histAt = -1;
    ArrayList<String> args = split(line);
    if (args.size() == 0) {
      return "";
    }
    String cmd = args.get(0);
    try {
      if ("help".equals(cmd) || "?".equals(cmd) || "man".equals(cmd)) {
        return help(args.size() >= 2 ? args.get(1) : "");
      }
      if ("pwd".equals(cmd)) {
        return cwd.getAbsolutePath();
      }
      if ("cd".equals(cmd)) {
        return cd(args);
      }
      if ("cd..".equals(cmd) || "cd.".equals(cmd)) {
        ArrayList<String> a = new ArrayList<String>();
        a.add("cd");
        a.add("cd..".equals(cmd) ? ".." : ".");
        return cd(a);
      }
      if (cmd.startsWith("cd..")) {
        ArrayList<String> a = new ArrayList<String>();
        a.add("cd");
        a.add(".." + cmd.substring(4));
        return cd(a);
      }
      if ("ls".equals(cmd)) {
        return ls(args);
      }
      if ("cat".equals(cmd)) {
        return cat(args, 0, Integer.MAX_VALUE);
      }
      if ("head".equals(cmd)) {
        return cat(args, 0, 10);
      }
      if ("tail".equals(cmd)) {
        return cat(args, -10, Integer.MAX_VALUE);
      }
      if ("mkdir".equals(cmd)) {
        return mkdir(args);
      }
      if ("rmdir".equals(cmd)) {
        return rmdir(args);
      }
      if ("rm".equals(cmd)) {
        return rm(args);
      }
      if ("cp".equals(cmd)) {
        return cp(args);
      }
      if ("mv".equals(cmd)) {
        return mv(args);
      }
      if ("touch".equals(cmd)) {
        return touch(args);
      }
      if ("echo".equals(cmd)) {
        return echo(args);
      }
      if ("date".equals(cmd)) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
          .format(new java.util.Date());
      }
      if ("whoami".equals(cmd)) {
        return "uid=" + android.os.Process.myUid() + " pid=" + android.os.Process.myPid();
      }
      if ("uname".equals(cmd)) {
        return "Android " + Build.VERSION.RELEASE + " API " + Build.VERSION.SDK_INT
          + " " + Build.MANUFACTURER + " " + Build.MODEL;
      }
      if ("df".equals(cmd)) {
        return df();
      }
      if ("du".equals(cmd)) {
        return du(args);
      }
      if ("wc".equals(cmd)) {
        return wc(args);
      }
      if ("find".equals(cmd)) {
        return find(args);
      }
      if ("grep".equals(cmd)) {
        return grep(args);
      }
      if ("clear".equals(cmd)) {
        return "\u000c";
      }
      if ("history".equals(cmd)) {
        return history();
      }
      if ("log".equals(cmd)) {
        return OpLog.read(ctx);
      }
      if ("stat".equals(cmd)) {
        return stat(args);
      }
      if ("cpu".equals(cmd)) {
        return lsCpu();
      }
      if ("sys".equals(cmd)) {
        return catSys();
      }
      if ("id".equals(cmd)) {
        return "uid=" + android.os.Process.myUid() + " pid=" + android.os.Process.myPid()
          + " tid=" + android.os.Process.myTid();
      }
      if ("exit".equals(cmd) || "quit".equals(cmd)) {
        return "\u0004";
      }
      OpLog.err(ctx, "term unknown: " + cmd);
      return cmd + ": command not found. Type help.";
    } catch (Exception e) {
      OpLog.err(ctx, "term " + cmd, e);
      String m = e.getMessage();
      return cmd + ": " + (m == null ? e.getClass().getSimpleName() : m);
    }
  }
  String prev() {
    if (hist.size() == 0) {
      return "";
    }
    if (histAt < 0) {
      histAt = hist.size();
    }
    if (histAt > 0) {
      histAt--;
    }
    return hist.get(histAt);
  }
  private String help(String topic) {
    if (topic != null && topic.length() > 0) {
      String t = topic.toLowerCase(Locale.US);
      if ("ls".equals(t)) {
        return "ls [DIR]\nList files in DIR or the current directory.\nShows type (d/-), size and name.";
      }
      if ("cd".equals(t)) {
        return "cd [DIR]\nChange directory. Use .. for parent, ~ for shared storage, / for root.";
      }
      if ("rm".equals(t)) {
        return "rm [-r] FILE...\nDelete files. -r deletes directories recursively. Permanent, not trash.";
      }
      if ("cpu".equals(t)) {
        return "cpu\nList /sys/devices/system/cpu (cores, cpufreq, online, present).";
      }
    }
    return "FileManager shell  (built-in, not a real Linux /bin/sh)\n"
      + "Navigation\n"
      + "  pwd                 print working directory\n"
      + "  cd [DIR]            change directory  (..  ~  /  relative)\n"
      + "  ls [DIR]            list names, sizes and dir flag\n"
      + "  find [DIR]          walk tree (depth limited)\n"
      + "Files\n"
      + "  cat FILE            print text file (max 256 KB)\n"
      + "  head FILE           first 10 lines\n"
      + "  tail FILE           last 10 lines\n"
      + "  stat FILE           size, modified, r/w, type\n"
      + "  wc FILE             line and byte counts\n"
      + "  grep PATTERN FILE   case-insensitive search\n"
      + "  touch FILE          create or bump mtime\n"
      + "  mkdir DIR           create directory (parents too)\n"
      + "  rmdir DIR           remove empty directory\n"
      + "  rm [-r] FILE...     delete; -r recursive (not trash)\n"
      + "  cp SRC DST          copy file or folder\n"
      + "  mv SRC DST          move/rename\n"
      + "System\n"
      + "  date                local time\n"
      + "  whoami / id         uid and pid\n"
      + "  uname               Android version and device\n"
      + "  df                  storage used/free\n"
      + "  du [PATH]           recursive size\n"
      + "  cpu                 list /sys/devices/system/cpu\n"
      + "  sys                 print /proc/cpuinfo (trimmed)\n"
      + "  log                 app operation log\n"
      + "Session\n"
      + "  echo TEXT           print arguments\n"
      + "  history             previous commands\n"
      + "  clear               wipe the screen\n"
      + "  help [CMD]          this text, or one command\n"
      + "  exit / quit         close the terminal\n"
      + "Quotes:  echo \"hello world\"\n"
      + "There are no pipes, redirects, or glob expansion.";
  }
  private String stat(ArrayList<String> args) {
    if (args.size() < 2) {
      return "usage: stat FILE";
    }
    File f = resolve(args.get(1));
    if (f == null || !f.exists()) {
      return "stat: missing";
    }
    return f.getAbsolutePath()
      + "\ntype " + (f.isDirectory() ? "directory" : "file")
      + "\nsize " + f.length() + " (" + FileOps.humanSize(f.length()) + ")"
      + "\nmtime " + f.lastModified()
      + "\nread " + (f.canRead() ? "yes" : "no")
      + " write " + (f.canWrite() ? "yes" : "no")
      + " exec " + (f.canExecute() ? "yes" : "no");
  }
  private String lsCpu() {
    File dir = new File("/sys/devices/system/cpu");
    if (!dir.isDirectory()) {
      dir = new File("/sys/devices/cpu");
    }
    File[] kids = dir.listFiles();
    StringBuilder sb = new StringBuilder(dir.getAbsolutePath());
    if (kids == null || kids.length == 0) {
      return sb.append("\n(empty or unreadable)").toString();
    }
    for (int i = 0; i < kids.length; i++) {
      sb.append('\n').append(kids[i].getName());
    }
    return sb.toString();
  }
  private String catSys() {
    File f = new File("/proc/cpuinfo");
    try {
      if (!f.isFile()) {
        return "sys: /proc/cpuinfo missing";
      }
      String t = FileOps.readString(f);
      if (t.length() > 4000) {
        t = t.substring(0, 4000) + "\n...";
      }
      return t;
    } catch (Exception e) {
      return "sys: " + e.getMessage();
    }
  }
  private String cd(ArrayList<String> args) {
    File next;
    if (args.size() < 2 || "~".equals(args.get(1))) {
      next = Environment.getExternalStorageDirectory();
    } else {
      next = resolve(args.get(1));
    }
    if (next == null || (!next.isDirectory() && !FileOps.looksLikeDir(next))) {
      return "cd: no such directory";
    }
    cwd = next;
    return "";
  }
  private String ls(ArrayList<String> args) {
    File dir = args.size() >= 2 ? resolve(args.get(1)) : cwd;
    if (dir == null) {
      return "ls: missing";
    }
    File[] kids = FileOps.list(dir, true, 0, true);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < kids.length; i++) {
      if (i > 0) {
        sb.append('\n');
      }
      File f = kids[i];
      sb.append(FileOps.looksLikeDir(f) ? "d " : "- ");
      sb.append(String.format(Locale.US, "%10s  ", FileOps.humanSize(f.isFile() ? f.length() : 0)));
      sb.append(f.getName());
    }
    return sb.toString();
  }
  private String cat(ArrayList<String> args, int from, int maxLines) throws Exception {
    String name = args.size() > 0 ? args.get(0) : "cat";
    if (args.size() < 2) {
      return "usage: " + name + " FILE";
    }
    File f = resolve(args.get(1));
    if (f == null || !f.isFile()) {
      return name + ": not a file";
    }
    if (!f.canRead()) {
      return name + ": unreadable";
    }
    if (f.length() > 262144L) {
      return name + ": file too large";
    }
    String text = FileOps.readString(f);
    String[] lines = text.split("\n", -1);
    int start = from < 0 ? Math.max(0, lines.length + from) : from;
    int end = Math.min(lines.length, start + (maxLines == Integer.MAX_VALUE ? lines.length : maxLines));
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < end; i++) {
      if (i > start) {
        sb.append('\n');
      }
      sb.append(lines[i]);
    }
    return sb.toString();
  }
  private String mkdir(ArrayList<String> args) {
    if (args.size() < 2) {
      return "usage: mkdir DIR";
    }
    File f = resolve(args.get(1));
    if (f == null) {
      return "mkdir: missing";
    }
    if (FileOps.isProtected(f)) {
      return "mkdir: protected";
    }
    boolean ok = f.mkdirs() || f.isDirectory();
    OpLog.info(ctx, "mkdir " + (f == null ? "?" : f.getAbsolutePath()) + " ok=" + ok);
    return ok ? "" : "mkdir: failed";
  }
  private String rmdir(ArrayList<String> args) {
    if (args.size() < 2) {
      return "usage: rmdir DIR";
    }
    File f = resolve(args.get(1));
    if (f == null) {
      return "rmdir: missing";
    }
    if (FileOps.isProtected(f)) {
      return "rmdir: protected";
    }
    boolean ok = f.isDirectory() && f.delete();
    OpLog.info(ctx, "rmdir " + (f == null ? "?" : f.getAbsolutePath()) + " ok=" + ok);
    return ok ? "" : "rmdir: failed";
  }
  private String rm(ArrayList<String> args) {
    boolean rec = false;
    ArrayList<String> paths = new ArrayList<String>();
    for (int i = 1; i < args.size(); i++) {
      if ("-r".equals(args.get(i)) || "-rf".equals(args.get(i))) {
        rec = true;
      } else {
        paths.add(args.get(i));
      }
    }
    if (paths.size() == 0) {
      return "usage: rm [-r] FILE";
    }
    int n = 0;
    for (int i = 0; i < paths.size(); i++) {
      File f = resolve(paths.get(i));
      if (f == null) {
        continue;
      }
      if (FileOps.isProtected(f)) {
        OpLog.info(ctx, "rm protected " + f.getAbsolutePath());
        continue;
      }
      boolean ok = rec ? FileOps.deleteRec(f) : f.delete();
      if (ok) {
        n++;
      }
      OpLog.info(ctx, "rm " + f.getAbsolutePath() + " ok=" + ok);
    }
    return "removed " + n;
  }
  private String cp(ArrayList<String> args) throws Exception {
    if (args.size() < 3) {
      return "usage: cp SRC DST";
    }
    File src = resolve(args.get(1));
    File dst = resolve(args.get(2));
    if (src == null || dst == null || !src.exists()) {
      return "cp: missing";
    }
    if (dst.isDirectory()) {
      dst = new File(dst, src.getName());
    }
    if (FileOps.isProtected(dst) || FileOps.isProtected(dst.getParentFile())) {
      return "cp: protected";
    }
    FileOps.copyRec(src, dst);
    OpLog.info(ctx, "cp " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
    return "";
  }
  private String mv(ArrayList<String> args) {
    if (args.size() < 3) {
      return "usage: mv SRC DST";
    }
    File src = resolve(args.get(1));
    File dst = resolve(args.get(2));
    if (src == null || dst == null || !src.exists()) {
      return "mv: missing";
    }
    if (FileOps.isProtected(src)) {
      return "mv: protected";
    }
    if (dst.isDirectory()) {
      dst = new File(dst, src.getName());
    }
    if (FileOps.isProtected(dst) || FileOps.isProtected(dst.getParentFile())) {
      return "mv: protected";
    }
    boolean ok = FileOps.move(src, dst);
    OpLog.info(ctx, "mv " + src.getAbsolutePath() + " to " + dst.getAbsolutePath() + " ok=" + ok);
    return ok ? "" : "mv: failed";
  }
  private String touch(ArrayList<String> args) throws Exception {
    if (args.size() < 2) {
      return "usage: touch FILE";
    }
    File f = resolve(args.get(1));
    if (f == null) {
      return "touch: missing";
    }
    if (FileOps.isProtected(f) || FileOps.isProtected(f.getParentFile())) {
      return "touch: protected";
    }
    if (!f.exists()) {
      File par = f.getParentFile();
      if (par != null && !par.exists() && !par.mkdirs()) {
        return "touch: failed";
      }
      if (!f.createNewFile()) {
        return "touch: failed";
      }
    } else if (!f.setLastModified(System.currentTimeMillis())) {
      return "touch: failed";
    }
    OpLog.info(ctx, "touch " + f.getAbsolutePath());
    return "";
  }
  private String echo(ArrayList<String> args) {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < args.size(); i++) {
      if (i > 1) {
        sb.append(' ');
      }
      sb.append(args.get(i));
    }
    return sb.toString();
  }
  private String df() {
    File p = Environment.getExternalStorageDirectory();
    try {
      StatFs fs = new StatFs(p.getAbsolutePath());
      long bs;
      long tot;
      long av;
      if (Build.VERSION.SDK_INT >= 18) {
        bs = fs.getBlockSizeLong();
        tot = fs.getBlockCountLong();
        av = fs.getAvailableBlocksLong();
      } else {
        bs = fs.getBlockSize();
        tot = fs.getBlockCount();
        av = fs.getAvailableBlocks();
      }
      long total = bs * tot;
      long free = bs * av;
      return p.getAbsolutePath() + "  " + FileOps.humanSize(total - free) + " used / "
        + FileOps.humanSize(total) + "  free " + FileOps.humanSize(free);
    } catch (Exception e) {
      return "df: n/a";
    }
  }
  private String du(ArrayList<String> args) {
    File f = args.size() >= 2 ? resolve(args.get(1)) : cwd;
    if (f == null) {
      return "du: missing";
    }
    return FileOps.humanSize(FileOps.sizeOf(f)) + "\t" + f.getAbsolutePath();
  }
  private String wc(ArrayList<String> args) throws Exception {
    if (args.size() < 2) {
      return "usage: wc FILE";
    }
    File f = resolve(args.get(1));
    if (f == null || !f.isFile()) {
      return "wc: not a file";
    }
    String t = FileOps.readString(f);
    int lines = 0;
    for (int i = 0; i < t.length(); i++) {
      if (t.charAt(i) == '\n') {
        lines++;
      }
    }
    return lines + " " + t.length() + " " + f.getName();
  }
  private String find(ArrayList<String> args) {
    File dir = args.size() >= 2 ? resolve(args.get(1)) : cwd;
    StringBuilder sb = new StringBuilder();
    walkFind(dir, sb, 0);
    return sb.toString();
  }
  private void walkFind(File dir, StringBuilder sb, int depth) {
    if (dir == null || depth > 8) {
      return;
    }
    if (sb.length() > 0) {
      sb.append('\n');
    }
    sb.append(dir.getAbsolutePath());
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    int n = Math.min(kids.length, 80);
    for (int i = 0; i < n; i++) {
      if (kids[i].isDirectory()) {
        walkFind(kids[i], sb, depth + 1);
      } else {
        sb.append('\n').append(kids[i].getAbsolutePath());
      }
      if (sb.length() > 8000) {
        sb.append("\n...");
        return;
      }
    }
  }
  private String grep(ArrayList<String> args) throws Exception {
    if (args.size() < 3) {
      return "usage: grep PATTERN FILE";
    }
    String pat = args.get(1).toLowerCase(Locale.US);
    File f = resolve(args.get(2));
    if (f == null || !f.isFile()) {
      return "grep: not a file";
    }
    String[] lines = FileOps.readString(f).split("\n", -1);
    StringBuilder sb = new StringBuilder();
    int hits = 0;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].toLowerCase(Locale.US).contains(pat)) {
        if (hits > 0) {
          sb.append('\n');
        }
        sb.append(i + 1).append(':').append(lines[i]);
        hits++;
        if (hits >= 200) {
          break;
        }
      }
    }
    return sb.toString();
  }
  private String history() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hist.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(i + 1).append("  ").append(hist.get(i));
    }
    return sb.toString();
  }
  private File resolve(String p) {
    if (p == null || p.length() == 0) {
      return cwd;
    }
    if (".".equals(p)) {
      return cwd;
    }
    if ("..".equals(p)) {
      File par = cwd.getParentFile();
      return par != null ? par : cwd;
    }
    File f = p.startsWith("/") ? new File(p) : new File(cwd, p);
    return f;
  }
  private static ArrayList<String> split(String line) {
    ArrayList<String> out = new ArrayList<String>();
    StringBuilder cur = new StringBuilder();
    boolean q = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        q = !q;
      } else if ((c == ' ' || c == '\t') && !q) {
        if (cur.length() > 0) {
          out.add(cur.toString());
          cur.setLength(0);
        }
      } else {
        cur.append(c);
      }
    }
    if (cur.length() > 0) {
      out.add(cur.toString());
    }
    return out;
  }
}
