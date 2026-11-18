package com.dlna.FileManager;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
final class TarOps {
  static final class Entry {
    String name;
    boolean dir;
    long size;
    long time;
  }
  private TarOps() {}
  static boolean gzipName(String name) {
    String n = name.toLowerCase();
    return n.endsWith(".tar.gz") || n.endsWith(".tgz");
  }
  static boolean tarName(String name) {
    String n = name.toLowerCase();
    return n.endsWith(".tar") || gzipName(name);
  }
  static List<Entry> list(File tar) throws IOException {
    InputStream raw = new BufferedInputStream(new FileInputStream(tar), 8192);
    InputStream in = gzipName(tar.getName()) ? (InputStream) new GZIPInputStream(raw) : raw;
    List<Entry> out = new ArrayList<Entry>();
    try {
      byte[] hdr = new byte[512];
      String pending = null;
      while (true) {
        if (!readFull(in, hdr)) {
          break;
        }
        if (zeroBlock(hdr)) {
          break;
        }
        String name = parseName(hdr);
        long size = parseOctal(hdr, 124, 12);
        char type = (char) (hdr[156] & 255);
        if (type == 'L') {
          pending = readString(in, size);
          skip(in, pad(size) - size);
          continue;
        }
        if (skipType(type)) {
          skip(in, pad(size));
          pending = null;
          continue;
        }
        if (pending != null && pending.length() > 0) {
          name = pending;
          pending = null;
        }
        if (name.length() == 0) {
          skip(in, pad(size));
          continue;
        }
        Entry e = new Entry();
        e.name = name;
        e.dir = type == '5' || name.endsWith("/");
        e.size = e.dir ? 0L : size;
        e.time = parseOctal(hdr, 136, 12) * 1000L;
        out.add(e);
        skip(in, pad(size));
      }
    } finally {
      in.close();
    }
    return out;
  }
  static int extract(File tar, String[] inners, File dest) throws IOException {
    dest.mkdirs();
    String root = dest.getCanonicalPath();
    InputStream raw = new BufferedInputStream(new FileInputStream(tar), 8192);
    InputStream in = gzipName(tar.getName()) ? (InputStream) new GZIPInputStream(raw) : raw;
    int count = 0;
    try {
      byte[] hdr = new byte[512];
      byte[] buf = new byte[8192];
      String pending = null;
      while (true) {
        if (!readFull(in, hdr)) {
          break;
        }
        if (zeroBlock(hdr)) {
          break;
        }
        String name = parseName(hdr);
        long size = parseOctal(hdr, 124, 12);
        char type = (char) (hdr[156] & 255);
        if (type == 'L') {
          pending = readString(in, size);
          skip(in, pad(size) - size);
          continue;
        }
        if (skipType(type)) {
          skip(in, pad(size));
          pending = null;
          continue;
        }
        if (pending != null && pending.length() > 0) {
          name = pending;
          pending = null;
        }
        boolean hit = want(name, inners);
        if (hit && name.length() > 0) {
          File out = new File(dest, name);
          String canon = out.getCanonicalPath();
          if (canon.equals(root) || canon.startsWith(root + File.separator)) {
            if (type == '5' || name.endsWith("/")) {
              out.mkdirs();
            } else {
              File parent = out.getParentFile();
              if (parent != null && !parent.exists()) {
                parent.mkdirs();
              }
              FileOutputStream os = new FileOutputStream(out);
              try {
                copyN(in, os, size, buf);
              } finally {
                os.close();
              }
              FileOps.addProg(size);
              count++;
              long extra = pad(size) - size;
              if (extra > 0) {
                skip(in, extra);
              }
              continue;
            }
          }
        }
        skip(in, pad(size));
      }
    } finally {
      in.close();
    }
    return count;
  }
  static int create(File[] srcs, File tar, boolean gzip) throws IOException {
    File parent = tar.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    OutputStream raw = new BufferedOutputStream(new FileOutputStream(tar), 8192);
    OutputStream out = gzip ? (OutputStream) new GZIPOutputStream(raw) : raw;
    int n = 0;
    try {
      for (int i = 0; i < srcs.length; i++) {
        if (srcs[i] != null && srcs[i].exists() && !FileOps.nestIntoSelf(srcs[i], tar)) {
          n += put(out, srcs[i], srcs[i].getName());
        }
      }
      out.write(new byte[1024]);
      out.flush();
      if (gzip) {
        ((GZIPOutputStream) out).finish();
      }
    } finally {
      out.close();
    }
    return n;
  }
  static int deleteEntries(File tar, String[] drop) throws IOException {
    return rewrite(tar, drop, null, null, null, null);
  }
  static int renameEntry(File tar, String from, String to) throws IOException {
    return rewrite(tar, null, from, to, null, null);
  }
  static int addFiles(File tar, File[] srcs, String prefix) throws IOException {
    return rewrite(tar, null, null, null, srcs, prefix, null, null);
  }
  static int replaceEntry(File tar, String inner, File src) throws IOException {
    if (src == null || !src.isFile() || inner == null || inner.length() == 0) {
      throw new IOException("replace");
    }
    return rewrite(tar, null, null, null, null, null, inner, src);
  }
  private static int rewrite(File tar, String[] drop, String from, String to,
      File[] add, String prefix) throws IOException {
    return rewrite(tar, drop, from, to, add, prefix, null, null);
  }
  private static int rewrite(File tar, String[] drop, String from, String to,
      File[] add, String prefix, String replaceInner, File replaceSrc) throws IOException {
    File tmp = new File(tar.getAbsolutePath() + ".tmp");
    boolean gz = gzipName(tar.getName());
    InputStream rawIn = new BufferedInputStream(new FileInputStream(tar), 8192);
    InputStream in = gz ? (InputStream) new GZIPInputStream(rawIn) : rawIn;
    OutputStream rawOut = new BufferedOutputStream(new FileOutputStream(tmp), 8192);
    OutputStream out = gz ? (OutputStream) new GZIPOutputStream(rawOut) : rawOut;
    int kept = 0;
    boolean replaced = false;
    String want = replaceInner == null ? "" : replaceInner;
    while (want.startsWith("/")) {
      want = want.substring(1);
    }
    if (want.endsWith("/")) {
      want = want.substring(0, want.length() - 1);
    }
    try {
      byte[] hdr = new byte[512];
      byte[] buf = new byte[8192];
      String pending = null;
      while (true) {
        if (!readFull(in, hdr)) {
          break;
        }
        if (zeroBlock(hdr)) {
          break;
        }
        String name = parseName(hdr);
        long size = parseOctal(hdr, 124, 12);
        char type = (char) (hdr[156] & 255);
        if (type == 'L') {
          pending = readString(in, size);
          skip(in, pad(size) - size);
          continue;
        }
        if (skipType(type)) {
          skip(in, pad(size));
          pending = null;
          continue;
        }
        if (pending != null && pending.length() > 0) {
          name = pending;
          pending = null;
        }
        if (dropped(name, drop) || (from != null && name.equals(from) && to == null)) {
          skip(in, pad(size));
          continue;
        }
        String base = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        if (replaceSrc != null && want.length() > 0 && base.equals(want) && type != '5') {
          skip(in, pad(size));
          kept += put(out, replaceSrc, name.endsWith("/") ? base : name);
          replaced = true;
          continue;
        }
        if (from != null && to != null && (name.equals(from) || name.startsWith(from + "/"))) {
          name = to + name.substring(from.length());
        }
        writeLongIfNeeded(out, name);
        putName(hdr, name);
        checksum(hdr);
        out.write(hdr);
        copyN(in, out, pad(size), buf);
        kept++;
      }
      if (replaceSrc != null && !replaced && want.length() > 0) {
        kept += put(out, replaceSrc, want);
      }
      if (add != null) {
        String pre = prefix == null ? "" : prefix;
        if (pre.length() > 0 && !pre.endsWith("/")) {
          pre = pre + "/";
        }
        for (int i = 0; i < add.length; i++) {
          if (add[i] != null && add[i].exists()) {
            kept += put(out, add[i], pre + add[i].getName());
          }
        }
      }
      out.write(new byte[1024]);
      out.flush();
      if (gz) {
        ((GZIPOutputStream) out).finish();
      }
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {}
      out.close();
    }
    if (!tar.delete() && tar.exists()) {
      tmp.delete();
      throw new IOException("replace");
    }
    if (!tmp.renameTo(tar)) {
      FileOps.copyRec(tmp, tar);
      tmp.delete();
    }
    return kept;
  }
  private static int put(OutputStream out, File f, String name) throws IOException {
    if (FileOps.cancelled()) {
      throw new IOException("cancelled");
    }
    if (f.isDirectory()) {
      String n = name.endsWith("/") ? name : name + "/";
      writeLongIfNeeded(out, n);
      out.write(header(n, 0L, f.lastModified() / 1000L, '5'));
      int c = 1;
      File[] kids = f.listFiles();
      if (kids != null) {
        for (int i = 0; i < kids.length; i++) {
          c += put(out, kids[i], n + kids[i].getName());
        }
      }
      return c;
    }
    long size = f.length();
    writeLongIfNeeded(out, name);
    out.write(header(name, size, f.lastModified() / 1000L, '0'));
    FileInputStream in = new FileInputStream(f);
    try {
      byte[] buf = new byte[8192];
      long left = size;
      while (left > 0) {
        int n = in.read(buf, 0, (int) Math.min(buf.length, left));
        if (n <= 0) {
          break;
        }
        out.write(buf, 0, n);
        FileOps.addProg(n);
        left -= n;
      }
    } finally {
      in.close();
    }
    long extra = pad(size) - size;
    if (extra > 0) {
      out.write(new byte[(int) extra]);
    }
    return 1;
  }
  private static void writeLongIfNeeded(OutputStream out, String name) throws IOException {
    byte[] b = utf8(name);
    if (fitsUstar(b)) {
      return;
    }
    out.write(header("././@LongLink", b.length + 1L, 0L, 'L'));
    out.write(b);
    out.write(0);
    long extra = pad(b.length + 1L) - (b.length + 1L);
    if (extra > 0) {
      out.write(new byte[(int) extra]);
    }
  }
  private static byte[] header(String name, long size, long mtime, char type) {
    byte[] h = new byte[512];
    putName(h, name);
    putOctal(h, 100, type == '5' ? 0755 : 0644, 8);
    putOctal(h, 108, 0, 8);
    putOctal(h, 116, 0, 8);
    putOctal(h, 124, size, 12);
    putOctal(h, 136, mtime, 12);
    h[156] = (byte) type;
    h[257] = 'u';
    h[258] = 's';
    h[259] = 't';
    h[260] = 'a';
    h[261] = 'r';
    h[262] = 0;
    h[263] = '0';
    h[264] = '0';
    checksum(h);
    return h;
  }
  private static void putName(byte[] h, String name) {
    byte[] b = utf8(name);
    if (b.length <= 99) {
      System.arraycopy(b, 0, h, 0, b.length);
      return;
    }
    int slash = splitPrefix(b);
    if (slash > 0) {
      System.arraycopy(b, 0, h, 345, slash);
      System.arraycopy(b, slash + 1, h, 0, b.length - slash - 1);
      return;
    }
    System.arraycopy(b, 0, h, 0, 99);
  }
  private static boolean fitsUstar(byte[] b) {
    if (b.length <= 99) {
      return true;
    }
    return splitPrefix(b) > 0;
  }
  private static int splitPrefix(byte[] b) {
    int i = b.length - 1;
    while (i >= 0) {
      if (b[i] == '/' && (b.length - i - 1) <= 99 && i <= 155) {
        return i;
      }
      i--;
    }
    return -1;
  }
  private static void putOctal(byte[] h, int off, long v, int len) {
    int i = 0;
    while (i < len) {
      h[off + i] = 0;
      i++;
    }
    if (len <= 1) {
      return;
    }
    String s = Long.toOctalString(v < 0L ? 0L : v);
    int digits = len - 1;
    if (s.length() > digits) {
      s = s.substring(s.length() - digits);
    }
    int pad = digits - s.length();
    for (i = 0; i < pad; i++) {
      h[off + i] = (byte) '0';
    }
    for (i = 0; i < s.length(); i++) {
      h[off + pad + i] = (byte) s.charAt(i);
    }
    h[off + len - 1] = 0;
  }
  private static void checksum(byte[] h) {
    int i;
    for (i = 148; i < 156; i++) {
      h[i] = 0x20;
    }
    int sum = 0;
    for (i = 0; i < 512; i++) {
      sum += h[i] & 255;
    }
    String s = Long.toOctalString(sum);
    while (s.length() < 6) {
      s = "0" + s;
    }
    if (s.length() > 6) {
      s = s.substring(s.length() - 6);
    }
    for (i = 0; i < 6; i++) {
      h[148 + i] = (byte) s.charAt(i);
    }
    h[154] = 0;
    h[155] = 0x20;
  }
  private static String parseName(byte[] h) {
    String name = cstr(h, 0, 100);
    String prefix = cstr(h, 345, 155);
    if (prefix.length() == 0) {
      return name;
    }
    if (name.length() == 0) {
      return prefix;
    }
    return prefix + "/" + name;
  }
  private static String cstr(byte[] h, int off, int len) {
    int n = 0;
    while (n < len && off + n < h.length && h[off + n] != 0) {
      n++;
    }
    try {
      return new String(h, off, n, "UTF-8");
    } catch (Exception e) {
      return new String(h, off, n);
    }
  }
  private static byte[] utf8(String s) {
    try {
      return s.getBytes("UTF-8");
    } catch (Exception e) {
      return s.getBytes();
    }
  }
  private static long parseOctal(byte[] h, int off, int len) {
    long v = 0L;
    for (int i = 0; i < len; i++) {
      int c = h[off + i] & 255;
      if (c == 0 || c == 0x20) {
        continue;
      }
      if (c < '0' || c > '7') {
        break;
      }
      v = (v << 3) + (c - '0');
    }
    return v;
  }
  private static boolean skipType(char type) {
    return type == 'K' || type == 'x' || type == 'g' || type == 'X' || type == 'S';
  }
  private static boolean want(String name, String[] inners) {
    if (inners == null || inners.length == 0) {
      return true;
    }
    String n = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    for (int i = 0; i < inners.length; i++) {
      String w = inners[i];
      if (w == null || w.length() == 0) {
        return true;
      }
      if (n.equals(w) || n.startsWith(w + "/")) {
        return true;
      }
    }
    return false;
  }
  private static boolean dropped(String name, String[] drop) {
    if (drop == null) {
      return false;
    }
    String n = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    for (int i = 0; i < drop.length; i++) {
      String w = drop[i];
      if (w == null || w.length() == 0) {
        continue;
      }
      if (n.equals(w) || n.startsWith(w + "/")) {
        return true;
      }
    }
    return false;
  }
  private static long pad(long size) {
    long r = size % 512L;
    return r == 0 ? size : size + (512L - r);
  }
  private static boolean zeroBlock(byte[] h) {
    for (int i = 0; i < h.length; i++) {
      if (h[i] != 0) {
        return false;
      }
    }
    return true;
  }
  private static boolean readFull(InputStream in, byte[] buf) throws IOException {
    int off = 0;
    while (off < buf.length) {
      int n = in.read(buf, off, buf.length - off);
      if (n < 0) {
        return false;
      }
      off += n;
    }
    return true;
  }
  private static String readString(InputStream in, long size) throws IOException {
    if (size <= 0L) {
      return "";
    }
    int n = (int) Math.min(size, 65536L);
    byte[] b = new byte[n];
    int off = 0;
    while (off < n) {
      int r = in.read(b, off, n - off);
      if (r < 0) {
        break;
      }
      off += r;
    }
    if (size > n) {
      skip(in, size - n);
    }
    int len = off;
    if (len > 0 && b[len - 1] == 0) {
      len--;
    }
    try {
      return new String(b, 0, len, "UTF-8");
    } catch (Exception e) {
      return new String(b, 0, len);
    }
  }
  private static void skip(InputStream in, long n) throws IOException {
    long left = n;
    while (left > 0) {
      long s = in.skip(left);
      if (s <= 0) {
        if (in.read() < 0) {
          break;
        }
        s = 1;
      }
      left -= s;
    }
  }
  private static void copyN(InputStream in, OutputStream out, long n, byte[] buf) throws IOException {
    long left = n;
    while (left > 0) {
      int r = in.read(buf, 0, (int) Math.min(buf.length, left));
      if (r < 0) {
        break;
      }
      out.write(buf, 0, r);
      left -= r;
    }
  }
}
