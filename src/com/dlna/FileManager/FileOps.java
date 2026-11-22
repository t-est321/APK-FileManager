package com.dlna.FileManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
final class FileOps {
  static volatile boolean cancelOp;
  static volatile long progDone;
  static volatile long progTotal;
  interface Prog {
    void on(long done, long total);
  }
  static volatile Prog progCb;
  private static final String[] ROOT_FALLBACK = {
    "/acct", "/bin", "/bugreports", "/cache", "/config", "/d",
    "/data", "/dev", "/etc", "/init", "/lost+found",
    "/metadata", "/mnt", "/odm", "/oem", "/postinstall", "/proc", "/product",
    "/sbin", "/sdcard", "/storage", "/sys", "/system", "/vendor"
  };
  static void setCancel(boolean v) {
    cancelOp = v;
  }
  static boolean cancelled() {
    return cancelOp || Thread.currentThread().isInterrupted();
  }
  static void beginProg(long total) {
    progDone = 0L;
    progTotal = total < 0L ? 0L : total;
    fireProg();
  }
  static void addProg(long n) {
    if (n <= 0L) {
      fireProg();
      return;
    }
    progDone += n;
    fireProg();
  }
  static void clearProg() {
    progCb = null;
    progDone = 0L;
    progTotal = 0L;
  }
  private static void fireProg() {
    Prog p = progCb;
    if (p != null) {
      p.on(progDone, progTotal);
    }
  }
  static boolean isProtected(File f) {
    AppSettings s = AppSettings.get();
    if (s == null || !s.protectOn || f == null) {
      return false;
    }
    String[] paths = s.protectPaths();
    for (int i = 0; i < paths.length; i++) {
      if (paths[i] == null || paths[i].length() == 0) {
        continue;
      }
      if (under(f, new File(paths[i]))) {
        return true;
      }
    }
    return false;
  }
  static String[] rootHints() {
    return ROOT_FALLBACK;
  }
  private FileOps() {}
  static String ext(String name) {
    int dot = name.lastIndexOf('.');
    if (dot <= 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(Locale.US);
  }
  static boolean extListed(String csv, String name) {
    String e = ext(name);
    if (e.length() == 0 || csv == null || csv.length() == 0) {
      return false;
    }
    return ("," + csv + ",").contains("," + e + ",");
  }
  static boolean isArchive(String name) {
    if (name == null) {
      return false;
    }
    String n = name.toLowerCase(Locale.US);
    if (n.endsWith(".tar.gz") || n.endsWith(".tgz")) {
      return true;
    }
    String e = ext(name);
    return "zip".equals(e) || "jar".equals(e) || "apk".equals(e) || "xapk".equals(e) || "tar".equals(e);
  }
  static boolean isLink(File f) {
    if (f == null) {
      return false;
    }
    try {
      String a = f.getAbsolutePath();
      String c = f.getCanonicalPath();
      return c != null && !c.equals(a);
    } catch (Exception e) {
      return false;
    }
  }
  static String relTime(long ms) {
    if (ms <= 0L) {
      return "";
    }
    long d = System.currentTimeMillis() - ms;
    if (d < 0L) {
      d = 0L;
    }
    if (d < 60000L) {
      return (d / 1000L) + "s ago";
    }
    if (d < 3600000L) {
      return (d / 60000L) + "m ago";
    }
    if (d < 86400000L) {
      return (d / 3600000L) + "h ago";
    }
    if (d < 604800000L) {
      return (d / 86400000L) + "d ago";
    }
    return (d / 604800000L) + "w ago";
  }
  static String sha256File(File f, long max) {
    if (f == null || !f.isFile()) {
      return "";
    }
    java.security.MessageDigest md;
    FileInputStream in = null;
    try {
      md = java.security.MessageDigest.getInstance("SHA-256");
      in = new FileInputStream(f);
      byte[] buf = new byte[8192];
      long left = max <= 0L ? f.length() : max;
      int n;
      while (left > 0L && (n = in.read(buf, 0, (int) Math.min(buf.length, left))) > 0) {
        md.update(buf, 0, n);
        left -= n;
      }
      byte[] d = md.digest();
      StringBuilder sb = new StringBuilder(d.length * 2);
      int i;
      for (i = 0; i < d.length; i++) {
        int v = d[i] & 255;
        if (v < 16) {
          sb.append('0');
        }
        sb.append(Integer.toHexString(v));
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
  static String sha256Hex(String s) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] d = md.digest((s == null ? "" : s).getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder(d.length * 2);
      for (int i = 0; i < d.length; i++) {
        int v = d[i] & 255;
        if (v < 16) {
          sb.append('0');
        }
        sb.append(Integer.toHexString(v));
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }
  static String readStringLimit(File f, long max) throws IOException {
    FileInputStream in = new FileInputStream(f);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[65536];
      long left = max < 0L ? Long.MAX_VALUE : max;
      int n;
      while (left > 0L && (n = in.read(buf, 0, (int) Math.min(buf.length, left))) > 0) {
        out.write(buf, 0, n);
        left -= n;
      }
      return new String(out.toByteArray(), "UTF-8");
    } finally {
      in.close();
    }
  }
  static String hexDump(File f, long max) throws IOException {
    FileInputStream in = new FileInputStream(f);
    try {
      StringBuilder sb = new StringBuilder();
      byte[] row = new byte[16];
      long off = 0L;
      long left = max < 0L ? Long.MAX_VALUE : max;
      while (left > 0L) {
        int n = in.read(row, 0, (int) Math.min(16L, left));
        if (n <= 0) {
          break;
        }
        sb.append(String.format(Locale.US, "%08X  ", Long.valueOf(off)));
        int i;
        for (i = 0; i < 16; i++) {
          if (i == 8) {
            sb.append(' ');
          }
          if (i < n) {
            sb.append(String.format(Locale.US, "%02X ", Integer.valueOf(row[i] & 255)));
          } else {
            sb.append("   ");
          }
        }
        sb.append(' ');
        for (i = 0; i < n; i++) {
          int b = row[i] & 255;
          sb.append(b >= 32 && b < 127 ? (char) b : '.');
        }
        sb.append('\n');
        off += n;
        left -= n;
      }
      return sb.toString();
    } finally {
      in.close();
    }
  }
  static boolean canList(File dir) {
    String[] n = dir.list();
    return n != null;
  }
  static boolean isRootHint(File f) {
    if (f == null) {
      return false;
    }
    String p = f.getAbsolutePath();
    if ("/".equals(p)) {
      return true;
    }
    for (int i = 0; i < ROOT_FALLBACK.length; i++) {
      String h = ROOT_FALLBACK[i];
      if (p.equals(h) || p.startsWith(h + "/")) {
        return true;
      }
    }
    return false;
  }
  static boolean looksLikeDir(File f) {
    if (f.isDirectory()) {
      return true;
    }
    if (f.isFile()) {
      return false;
    }
    String p = f.getAbsolutePath();
    if (ZipBrowse.isZipPath(p)) {
      return ZipBrowse.isDir(f);
    }
    if (MapBrowse.isMapPath(p)) {
      return MapBrowse.isDir(f);
    }
    return isRootHint(f);
  }
  static int unzip(File zip, File dest, boolean overwrite) throws IOException {
    if (overwrite && dest.exists()) {
      deleteRec(dest);
    }
    dest.mkdirs();
    int count = 0;
    String root = dest.getCanonicalPath();
    ZipInputStream in = new ZipInputStream(new FileInputStream(zip));
    try {
      ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = in.getNextEntry()) != null) {
        String name = e.getName();
        if (name.length() == 0) {
          in.closeEntry();
          continue;
        }
        File out = new File(dest, name);
        String canon = out.getCanonicalPath();
        if (!canon.equals(root) && !canon.startsWith(root + File.separator)) {
          in.closeEntry();
          continue;
        }
        if (e.isDirectory() || name.endsWith("/")) {
          out.mkdirs();
          in.closeEntry();
          continue;
        }
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
          parent.mkdirs();
        }
        if (out.exists() && !overwrite) {
          in.closeEntry();
          continue;
        }
        FileOutputStream os = new FileOutputStream(out);
        try {
          int n;
          while ((n = in.read(buf)) > 0) {
            os.write(buf, 0, n);
          }
        } finally {
          os.close();
        }
        count++;
        in.closeEntry();
      }
    } finally {
      in.close();
    }
    return count;
  }
  static int zipTo(File[] srcs, File zip) throws IOException {
    File parent = zip.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
    int count = 0;
    try {
      for (int i = 0; i < srcs.length; i++) {
        if (srcs[i] != null && srcs[i].exists() && !nestIntoSelf(srcs[i], zip)) {
          count += addZip(zos, srcs[i], srcs[i].getName());
        }
      }
    } finally {
      zos.close();
    }
    return count;
  }
  private static int addZip(ZipOutputStream zos, File f, String name) throws IOException {
    if (f.isDirectory()) {
      zos.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
      zos.closeEntry();
      File[] kids = f.listFiles();
      int n = 1;
      if (kids != null) {
        for (int i = 0; i < kids.length; i++) {
          n += addZip(zos, kids[i], name + "/" + kids[i].getName());
        }
      }
      return n;
    }
    zos.putNextEntry(new ZipEntry(name));
    FileInputStream in = new FileInputStream(f);
    try {
      byte[] buf = new byte[8192];
      int r;
      while ((r = in.read(buf)) > 0) {
        zos.write(buf, 0, r);
        addProg(r);
      }
    } finally {
      in.close();
    }
    zos.closeEntry();
    return 1;
  }
  static boolean under(File f, File dir) {
    if (f == null || dir == null) {
      return false;
    }
    String a = f.getAbsolutePath();
    String b = dir.getAbsolutePath();
    return a.equals(b) || a.startsWith(b + "/");
  }
  static boolean isCacheRoot(File dir) {
    return dir != null && "/cache".equals(dir.getAbsolutePath());
  }
  static String mime(String name) {
    String e = ext(name);
    if ("html".equals(e) || "htm".equals(e)) {
      return "text/html";
    }
    if ("md".equals(e) || "markdown".equals(e)) {
      return "text/markdown";
    }
    if ("svg".equals(e)) {
      return "image/svg+xml";
    }
    if ("css".equals(e)) {
      return "text/css";
    }
    if ("js".equals(e) || "mjs".equals(e)) {
      return "application/javascript";
    }
    if ("json".equals(e)) {
      return "application/json";
    }
    if ("xml".equals(e)) {
      return "text/xml";
    }
    int k = FileGlyph.forName(name);
    if (k == FileGlyph.IMG) {
      if ("jpg".equals(e) || "jpeg".equals(e)) {
        return "image/jpeg";
      }
      if ("gif".equals(e)) {
        return "image/gif";
      }
      if ("webp".equals(e)) {
        return "image/webp";
      }
      if ("svg".equals(e)) {
        return "image/svg+xml";
      }
      if ("bmp".equals(e)) {
        return "image/bmp";
      }
      return "image/png";
    }
    if (k == FileGlyph.VIDEO) {
      return "video/mp4";
    }
    if (k == FileGlyph.AUDIO) {
      return "audio/mpeg";
    }
    if (k == FileGlyph.APK) {
      return "application/vnd.android.package-archive";
    }
    if (k == FileGlyph.PDF) {
      return "application/pdf";
    }
    if (k == FileGlyph.ARCH) {
      return "application/zip";
    }
    if (k == FileGlyph.TEXT || k == FileGlyph.CODE) {
      return "text/plain";
    }
    return "application/octet-stream";
  }
  static File[] list(final File dir, final boolean showHidden, final int sortMode, final boolean foldersFirst) {
    if (isCacheRoot(dir)) {
      return new File[0];
    }
    File[] arr = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        if (!showHidden && f.isHidden()) {
          return false;
        }
        return !skipListed(dir, f);
      }
    });
    String p = dir.getAbsolutePath();
    if ("/".equals(p)) {
      arr = merge(arr == null ? new File[0] : arr, probeRoot(showHidden));
    }
    if ("/storage/emulated".equals(p)) {
      arr = merge(arr == null ? new File[0] : arr, new File[]{new File("/storage/emulated/0")});
    }
    if (arr == null) {
      arr = new File[0];
    }
    Arrays.sort(arr, new Comparator<File>() {
      @Override
      public int compare(File a, File b) {
        return cmp(a.getName(), b.getName(), a.lastModified(), b.lastModified(),
          a.length(), b.length(), looksLikeDir(a), looksLikeDir(b), sortMode, foldersFirst);
      }
    });
    return arr;
  }
  static int cmp(String na, String nb, long ta, long tb, long sa, long sb,
      boolean da, boolean db, int sortMode, boolean foldersFirst) {
    if (foldersFirst && da != db) {
      return da ? -1 : 1;
    }
    int c = 0;
    if (sortMode == 1 || sortMode == 4) {
      long x = sortMode == 1 ? tb : ta;
      long y = sortMode == 1 ? ta : tb;
      c = x < y ? -1 : (x > y ? 1 : 0);
    } else if (sortMode == 2 || sortMode == 5) {
      long x = sortMode == 2 ? sb : sa;
      long y = sortMode == 2 ? sa : sb;
      c = x < y ? -1 : (x > y ? 1 : 0);
    } else if (sortMode == 3) {
      c = nb.compareToIgnoreCase(na);
    } else if (sortMode == 6) {
      c = ext(na).compareToIgnoreCase(ext(nb));
    } else if (sortMode == 7) {
      c = FileGlyph.forName(na) - FileGlyph.forName(nb);
    }
    if (c != 0) {
      return c;
    }
    return na.compareToIgnoreCase(nb);
  }
  static boolean skipListed(File dir, File f) {
    if (dir == null || f == null) {
      return false;
    }
    String name = f.getName();
    if (name.length() == 0 || ".".equals(name) || "..".equals(name)) {
      return true;
    }
    String p = dir.getAbsolutePath();
    String fp = f.getAbsolutePath();
    if (fp.equals(p) || fp.equals(p + "/.") || fp.equals(p + "/..")) {
      return true;
    }
    if ("/".equals(p) && name.indexOf('_') >= 0) {
      return true;
    }
    if ("/storage".equals(p) && "0".equals(name) && new File(dir, "emulated").exists()) {
      return true;
    }
    if ("/storage".equals(p) && "/storage/emulated/0".equals(fp)) {
      return true;
    }
    return false;
  }
  static String childCounts(File dir, boolean hidden) {
    if (dir == null) {
      return "";
    }
    String p = dir.getAbsolutePath();
    if ("/proc".equals(p) || "/sys".equals(p) || "/dev".equals(p) || isCacheRoot(dir)) {
      return "";
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return "";
    }
    int folders = 0;
    int files = 0;
    for (int i = 0; i < kids.length; i++) {
      File k = kids[i];
      if (skipListed(dir, k)) {
        continue;
      }
      if (!hidden && k.isHidden()) {
        continue;
      }
      if (looksLikeDir(k)) {
        folders++;
      } else {
        files++;
      }
    }
    return folders + " folders, " + files + " files";
  }
  private static File[] probeRoot(boolean showHidden) {
    List<File> out = new ArrayList<File>();
    for (int i = 0; i < ROOT_FALLBACK.length; i++) {
      File f = new File(ROOT_FALLBACK[i]);
      if (skipListed(new File("/"), f)) {
        continue;
      }
      if (showHidden || !f.isHidden()) {
        out.add(f);
      }
    }
    return out.toArray(new File[out.size()]);
  }
  static File[] merge(File[] a, File[] b) {
    List<File> out = new ArrayList<File>();
    java.util.HashSet<String> seen = new java.util.HashSet<String>();
    addAll(out, seen, a);
    addAll(out, seen, b);
    return out.toArray(new File[out.size()]);
  }
  private static void addAll(List<File> out, java.util.HashSet<String> seen, File[] arr) {
    if (arr == null) {
      return;
    }
    for (int i = 0; i < arr.length; i++) {
      String p = arr[i].getAbsolutePath();
      if (seen.add(p)) {
        out.add(arr[i]);
      }
    }
  }
  static boolean move(File src, File dst) {
    if (src.renameTo(dst)) {
      return true;
    }
    try {
      copyRec(src, dst);
      deleteRec(src);
      return true;
    } catch (IOException e) {
      deleteRec(dst);
      return false;
    }
  }
  static void copyRec(File src, File dst) throws IOException {
    if (cancelled()) {
      throw new IOException("cancelled");
    }
    if (src == null || dst == null) {
      throw new IOException("copy");
    }
    if (same(src, dst) || (src.isDirectory() && under(dst, src))) {
      throw new IOException("nested copy");
    }
    if (src.isDirectory()) {
      if (!dst.exists() && !dst.mkdirs()) {
        throw new IOException("mkdir " + dst);
      }
      File[] kids = src.listFiles();
      if (kids == null) {
        return;
      }
      for (int i = 0; i < kids.length; i++) {
        copyRec(kids[i], new File(dst, kids[i].getName()));
      }
      return;
    }
    File parent = dst.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    FileChannel in = new FileInputStream(src).getChannel();
    try {
      FileChannel out = new FileOutputStream(dst).getChannel();
      try {
        long pos = 0;
        long size = in.size();
        while (pos < size) {
          if (cancelled()) {
            throw new IOException("cancelled");
          }
          long w = in.transferTo(pos, size - pos, out);
          if (w <= 0L) {
            break;
          }
          pos += w;
          addProg(w);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }
  static boolean deleteRec(File f) {
    if (cancelled()) {
      return false;
    }
    if (f.isDirectory()) {
      File[] kids = f.listFiles();
      if (kids != null) {
        for (int i = 0; i < kids.length; i++) {
          deleteRec(kids[i]);
        }
      }
    }
    return f.delete();
  }
  static int emptyDir(File dir) {
    if (dir == null || !dir.isDirectory()) {
      return 0;
    }
    int n = 0;
    File[] kids = dir.listFiles();
    if (kids == null) {
      return 0;
    }
    for (int i = 0; i < kids.length; i++) {
      if (deleteRec(kids[i])) {
        n++;
      }
    }
    return n;
  }
  static long sizeOf(File f) {
    if (cancelled()) {
      return 0L;
    }
    if (!f.isDirectory()) {
      return f.length();
    }
    long sum = 0;
    File[] kids = f.listFiles();
    if (kids != null) {
      for (int i = 0; i < kids.length; i++) {
        sum += sizeOf(kids[i]);
      }
    }
    return sum;
  }
  static String humanSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    double v = bytes;
    String[] units = {"KB", "MB", "GB", "TB"};
    int u = -1;
    do {
      v /= 1024.0;
      u++;
    } while (v >= 1024.0 && u < 3);
    return String.format(Locale.US, "%.1f %s", Double.valueOf(v), units[u]);
  }
  static String readString(File f) throws IOException {
    FileInputStream in = new FileInputStream(f);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
      return new String(out.toByteArray(), "UTF-8");
    } finally {
      in.close();
    }
  }
  static void writeString(File f, String s) throws IOException {
    writeBytes(f, s.getBytes("UTF-8"));
  }
  static void writeBytes(File f, byte[] b) throws IOException {
    File parent = f.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    FileOutputStream out = new FileOutputStream(f);
    try {
      out.write(b);
    } finally {
      out.close();
    }
  }
  static boolean isTextFile(File f) {
    if (f.length() > AppSettings.get().openLimit()) {
      return false;
    }
    try {
      FileInputStream in = new FileInputStream(f);
      try {
        byte[] buf = new byte[4096];
        int n = in.read(buf);
        if (n <= 0) {
          return true;
        }
        int ctrl = 0;
        for (int i = 0; i < n; i++) {
          int b = buf[i] & 255;
          if (b == 0) {
            return false;
          }
          if (b < 9 || (b > 13 && b < 32)) {
            ctrl++;
          }
        }
        return ctrl * 10 < n;
      } finally {
        in.close();
      }
    } catch (IOException e) {
      return false;
    }
  }
  static boolean copyName(File src, File destDir) {
    try {
      if (!destDir.exists() && !destDir.mkdirs()) {
        return false;
      }
      File dst = new File(destDir, src.getName());
      if (dst.exists()) {
        dst = new File(destDir, TrashStore.uniqueName(destDir, src.getName()));
      }
      writeString(dst, src.getName());
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  static boolean same(File a, File b) {
    if (a == null || b == null) {
      return false;
    }
    return a.getAbsolutePath().equals(b.getAbsolutePath());
  }
  static boolean nestIntoSelf(File src, File dest) {
    if (src == null || dest == null) {
      return false;
    }
    if (same(src, dest)) {
      return true;
    }
    return src.isDirectory() && under(dest, src);
  }
  static File[] pruneNested(File[] srcs) {
    if (srcs == null || srcs.length <= 1) {
      return srcs == null ? new File[0] : srcs;
    }
    ArrayList<File> out = new ArrayList<File>();
    for (int i = 0; i < srcs.length; i++) {
      File a = srcs[i];
      if (a == null) {
        continue;
      }
      boolean nested = false;
      for (int j = 0; j < srcs.length; j++) {
        if (i == j || srcs[j] == null) {
          continue;
        }
        if (!same(a, srcs[j]) && under(a, srcs[j])) {
          nested = true;
          break;
        }
      }
      if (!nested) {
        out.add(a);
      }
    }
    return out.toArray(new File[out.size()]);
  }
  static final class Tally {
    int files;
    int dirs;
    long bytes;
  }
  static Tally tallyAll(File[] srcs) {
    Tally t = new Tally();
    if (srcs == null) {
      return t;
    }
    for (int i = 0; i < srcs.length; i++) {
      addTally(t, srcs[i]);
    }
    return t;
  }
  private static void addTally(Tally t, File f) {
    if (f == null || cancelled()) {
      return;
    }
    if (f.isDirectory()) {
      t.dirs++;
      File[] kids = f.listFiles();
      if (kids == null) {
        return;
      }
      for (int i = 0; i < kids.length; i++) {
        addTally(t, kids[i]);
      }
      return;
    }
    t.files++;
    t.bytes += f.length();
  }
  static String tallyLine(String verb, Tally t) {
    if (t == null) {
      return verb;
    }
    return verb + "  " + t.dirs + " folders, " + t.files + " files  (" + humanSize(t.bytes) + ")";
  }
  static String rel(File base, File f) {
    if (base == null || f == null) {
      return f == null ? "" : f.getName();
    }
    String b = base.getAbsolutePath();
    String a = f.getAbsolutePath();
    if (a.equals(b)) {
      return f.getName();
    }
    String prefix = b.endsWith("/") ? b : b + "/";
    if (a.startsWith(prefix)) {
      return a.substring(prefix.length());
    }
    return f.getName();
  }
  static List<File> search(File dir, String query, boolean glob, boolean hidden, int max) {
    ArrayList<File> out = new ArrayList<File>();
    HashSet<String> seen = new HashSet<String>();
    walkSearch(dir, query, glob, hidden, max, 0, out, seen);
    return out;
  }
  private static void walkSearch(File dir, String query, boolean glob, boolean hidden,
      int max, int depth, List<File> out, HashSet<String> seen) {
    if (dir == null || depth > 12 || out.size() >= max || cancelled()) {
      return;
    }
    String key = dir.getAbsolutePath();
    if (!seen.add(key)) {
      return;
    }
    if (isCacheRoot(dir)) {
      return;
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    for (int i = 0; i < kids.length; i++) {
      if (out.size() >= max || cancelled()) {
        return;
      }
      File f = kids[i];
      if (skipListed(dir, f)) {
        continue;
      }
      if (!hidden && f.isHidden()) {
        continue;
      }
      if (matches(f.getName(), query, glob) || matches(rel(dir, f), query, glob)) {
        out.add(f);
      }
      if (f.isDirectory()) {
        String p = f.getAbsolutePath();
        if ("/proc".equals(p) || "/sys".equals(p) || "/dev".equals(p)) {
          continue;
        }
        walkSearch(f, query, glob, hidden, max, depth + 1, out, seen);
      }
    }
  }
  static boolean matches(String name, String query, boolean glob) {
    if (query == null || query.length() == 0) {
      return true;
    }
    String n = name.toLowerCase(Locale.US);
    String q = query.toLowerCase(Locale.US);
    if (!glob) {
      return n.indexOf(q) >= 0;
    }
    return globAt(n, 0, q, 0);
  }
  private static boolean globAt(String s, int si, String p, int pi) {
    while (pi < p.length()) {
      char c = p.charAt(pi);
      if (c == '*') {
        while (pi + 1 < p.length() && p.charAt(pi + 1) == '*') {
          pi++;
        }
        if (pi == p.length() - 1) {
          return true;
        }
        for (int k = si; k <= s.length(); k++) {
          if (globAt(s, k, p, pi + 1)) {
            return true;
          }
        }
        return false;
      }
      if (si >= s.length()) {
        return false;
      }
      if (c != '?' && c != s.charAt(si)) {
        return false;
      }
      si++;
      pi++;
    }
    return si == s.length();
  }
  static int deleteEmptyDirs(File dir, File keep) {
    if (dir == null || !dir.isDirectory()) {
      return 0;
    }
    if (keep != null && dir.getAbsolutePath().equals(keep.getAbsolutePath())) {
      File[] kids = dir.listFiles();
      int n = 0;
      if (kids != null) {
        for (int i = 0; i < kids.length; i++) {
          n += deleteEmptyDirs(kids[i], keep);
        }
      }
      return n;
    }
    int n = 0;
    File[] kids = dir.listFiles();
    if (kids != null) {
      for (int i = 0; i < kids.length; i++) {
        if (kids[i].isDirectory()) {
          n += deleteEmptyDirs(kids[i], keep);
        }
      }
    }
    File[] left = dir.listFiles();
    if (left != null && left.length == 0 && dir.delete()) {
      n++;
    }
    return n;
  }
  static long crc32(File f) {
    if (f == null || !f.isFile()) {
      return -1L;
    }
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
      CRC32 c = new CRC32();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
        c.update(buf, 0, n);
      }
      return c.getValue();
    } catch (Exception e) {
      return -1L;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
  static File existingCrc(File dir, File src) {
    if (dir == null || src == null || !src.isFile()) {
      return null;
    }
    long len = src.length();
    long crc = crc32(src);
    if (crc < 0L) {
      return null;
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return null;
    }
    String skip = src.getAbsolutePath();
    int i;
    for (i = 0; i < kids.length; i++) {
      File k = kids[i];
      if (!k.isFile() || k.length() != len) {
        continue;
      }
      if (skip.equals(k.getAbsolutePath())) {
        continue;
      }
      if (crc32(k) == crc) {
        return k;
      }
    }
    return null;
  }
  static File copyFileDedup(File src, File dir) throws IOException {
    if (src == null || !src.isFile()) {
      throw new IOException("copy");
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("mkdir " + dir);
    }
    File hit = existingCrc(dir, src);
    if (hit != null) {
      return hit;
    }
    File dst = new File(dir, TrashStore.uniqueName(dir, src.getName()));
    copyRec(src, dst);
    return dst;
  }
}
