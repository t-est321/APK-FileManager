package com.dlna.FileManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
final class ZipBrowse {
  static final String SEP = "!/";
  static final class Node {
    String name;
    String inner;
    boolean dir;
    long size;
    long time;
  }
  private static final HashMap<String, Node> META = new HashMap<String, Node>();
  private static final int META_MAX = 128;
  private ZipBrowse() {}
  static boolean isZipPath(String p) {
    if (p == null || p.length() == 0) {
      return false;
    }
    if (p.indexOf(SEP) >= 0) {
      return true;
    }
    return p.endsWith("!") && FileOps.isArchive(p.substring(0, p.length() - 1));
  }
  static File zipOf(String p) {
    int i = p.indexOf(SEP);
    if (i >= 0) {
      return new File(p.substring(0, i));
    }
    if (p.endsWith("!")) {
      return new File(p.substring(0, p.length() - 1));
    }
    return new File(p);
  }
  static String innerOf(String p) {
    int i = p.indexOf(SEP);
    if (i < 0) {
      return "";
    }
    String s = p.substring(i + SEP.length());
    while (s.startsWith("/")) {
      s = s.substring(1);
    }
    if (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    if (".".equals(s)) {
      return "";
    }
    return s;
  }
  static File root(File zip) {
    return new File(zip.getAbsolutePath() + SEP + ".");
  }
  static void clear() {
    META.clear();
  }
  static File child(File zip, String inner) {
    if (inner == null || inner.length() == 0) {
      return root(zip);
    }
    return new File(zip.getAbsolutePath() + SEP + inner);
  }
  static File parent(File cur) {
    String p = cur.getAbsolutePath();
    if (!isZipPath(p)) {
      return cur.getParentFile();
    }
    File zip = zipOf(p);
    String inner = innerOf(p);
    if (inner.length() == 0) {
      return zip.getParentFile();
    }
    int sl = inner.lastIndexOf('/');
    if (sl < 0) {
      return root(zip);
    }
    return child(zip, inner.substring(0, sl));
  }
  static String display(File cur) {
    String p = cur.getAbsolutePath();
    if (!isZipPath(p)) {
      return p;
    }
    File zip = zipOf(p);
    String inner = innerOf(p);
    return inner.length() == 0 ? zip.getAbsolutePath() : zip.getAbsolutePath() + "/" + inner;
  }
  static Node meta(String path) {
    return META.get(path);
  }
  static boolean isDir(File f) {
    String p = f.getAbsolutePath();
    if (!isZipPath(p)) {
      return false;
    }
    if (innerOf(p).length() == 0) {
      return true;
    }
    Node n = META.get(p);
    return n != null && n.dir;
  }
  static List<Node> list(File zip, String inner, boolean showHidden) throws IOException {
    if (TarOps.tarName(zip.getName())) {
      return kidsFromNames(tarNames(zip), inner, showHidden);
    }
    String prefix = inner == null ? "" : inner;
    if (prefix.startsWith("/")) {
      prefix = prefix.substring(1);
    }
    if (prefix.length() > 0 && prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    String pre = prefix.length() == 0 ? "" : prefix + "/";
    HashMap<String, Node> kids = new HashMap<String, Node>();
    ZipFile zf = new ZipFile(zip);
    try {
      Enumeration<? extends ZipEntry> en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = en.nextElement();
        String name = norm(e.getName());
        if (name.length() == 0) {
          continue;
        }
        if (pre.length() > 0 && !name.equals(prefix) && !name.startsWith(pre)) {
          continue;
        }
        String rest = pre.length() == 0 ? name : (name.equals(prefix) ? "" : name.substring(pre.length()));
        if (rest.length() == 0) {
          continue;
        }
        int sl = rest.indexOf('/');
        boolean dir;
        String child;
        if (sl >= 0) {
          child = rest.substring(0, sl);
          dir = true;
        } else {
          child = rest;
          dir = e.isDirectory();
        }
        if (child.length() == 0) {
          continue;
        }
        if (!showHidden && child.charAt(0) == '.') {
          continue;
        }
        Node n = kids.get(child);
        if (n == null) {
          n = new Node();
          n.name = child;
          n.inner = pre + child;
          n.dir = dir;
          n.size = dir ? 0L : e.getSize();
          n.time = e.getTime();
          kids.put(child, n);
        } else if (dir) {
          n.dir = true;
        }
      }
    } finally {
      zf.close();
    }
    List<Node> out = new ArrayList<Node>(kids.values());
    Collections.sort(out, nodeCmp());
    return out;
  }
  static List<Node> search(File zip, String inner, String query, boolean glob, boolean hidden, int max)
      throws IOException {
    ArrayList<Node> out = new ArrayList<Node>();
    searchWalk(zip, inner, query, glob, hidden, max, out);
    return out;
  }
  private static void searchWalk(File zip, String inner, String query, boolean glob, boolean hidden,
      int max, List<Node> out) throws IOException {
    if (out.size() >= max) {
      return;
    }
    List<Node> kids = list(zip, inner, hidden);
    for (int i = 0; i < kids.size(); i++) {
      if (out.size() >= max) {
        return;
      }
      Node n = kids.get(i);
      if (FileOps.matches(n.name, query, glob) || FileOps.matches(n.inner, query, glob)) {
        out.add(n);
      }
      if (n.dir) {
        searchWalk(zip, n.inner, query, glob, hidden, max, out);
      }
    }
  }
  static void remember(File zip, List<Node> nodes) {
    ArrayList<String> drop = new ArrayList<String>();
    String head = zip.getAbsolutePath() + SEP;
    for (String k : META.keySet()) {
      if (k.startsWith(head)) {
        drop.add(k);
      }
    }
    for (int i = 0; i < drop.size(); i++) {
      META.remove(drop.get(i));
    }
    Node root = new Node();
    root.name = zip.getName();
    root.inner = "";
    root.dir = true;
    META.put(root(zip).getAbsolutePath(), root);
    for (int i = 0; i < nodes.size(); i++) {
      Node n = nodes.get(i);
      META.put(child(zip, n.inner).getAbsolutePath(), n);
    }
    if (META.size() > META_MAX) {
      ArrayList<String> keys = new ArrayList<String>(META.keySet());
      int i;
      for (i = 0; i < keys.size() && META.size() > META_MAX / 2; i++) {
        String k = keys.get(i);
        if (!k.startsWith(head) && !k.equals(root(zip).getAbsolutePath())) {
          META.remove(k);
        }
      }
    }
  }
  static int extract(File zip, String[] inners, File dest) throws IOException {
    if (TarOps.tarName(zip.getName())) {
      return TarOps.extract(zip, inners, dest);
    }
    dest.mkdirs();
    String root = dest.getCanonicalPath();
    ZipFile zf = new ZipFile(zip);
    int count = 0;
    try {
      for (int i = 0; i < inners.length; i++) {
        count += extractInner(zf, inners[i], dest, root);
      }
    } finally {
      zf.close();
    }
    return count;
  }
  private static int extractInner(ZipFile zf, String inner, File dest, String root) throws IOException {
    String want = norm(inner);
    int count = 0;
    Enumeration<? extends ZipEntry> en = zf.entries();
    while (en.hasMoreElements()) {
      ZipEntry e = en.nextElement();
      String name = norm(e.getName());
      boolean hit;
      if (want.length() == 0) {
        hit = true;
      } else if (name.equals(want)) {
        hit = true;
      } else {
        hit = name.startsWith(want + "/");
      }
      if (!hit) {
        continue;
      }
      File out = new File(dest, name);
      String canon = out.getCanonicalPath();
      if (!canon.equals(root) && !canon.startsWith(root + File.separator)) {
        continue;
      }
      if (e.isDirectory() || name.endsWith("/")) {
        out.mkdirs();
        continue;
      }
      File parent = out.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      InputStream in = zf.getInputStream(e);
      try {
        FileOutputStream os = new FileOutputStream(out);
        try {
          byte[] buf = new byte[8192];
          int n;
          while ((n = in.read(buf)) > 0) {
            os.write(buf, 0, n);
          }
        } finally {
          os.close();
        }
      } finally {
        in.close();
      }
      count++;
    }
    return count;
  }
  static int deleteEntries(File zip, String[] inners) throws IOException {
    if (TarOps.tarName(zip.getName())) {
      return TarOps.deleteEntries(zip, inners);
    }
    File tmp = new File(zip.getAbsolutePath() + ".tmp");
    java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zip));
    java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(tmp));
    int kept = 0;
    try {
      ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = in.getNextEntry()) != null) {
        String name = norm(e.getName());
        if (dropped(name, inners)) {
          in.closeEntry();
          continue;
        }
        out.putNextEntry(new ZipEntry(e.getName()));
        int n;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
        out.closeEntry();
        in.closeEntry();
        kept++;
      }
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {}
      out.close();
    }
    replace(zip, tmp);
    return kept;
  }
  static int renameEntry(File zip, String from, String to) throws IOException {
    if (TarOps.tarName(zip.getName())) {
      return TarOps.renameEntry(zip, from, to);
    }
    File tmp = new File(zip.getAbsolutePath() + ".tmp");
    java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zip));
    java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(tmp));
    int kept = 0;
    try {
      ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = in.getNextEntry()) != null) {
        String name = norm(e.getName());
        String nn = name;
        if (name.equals(from) || name.startsWith(from + "/")) {
          nn = to + name.substring(from.length());
        }
        out.putNextEntry(new ZipEntry(e.isDirectory() || name.endsWith("/") ? (nn.endsWith("/") ? nn : nn + "/") : nn));
        int n;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
        out.closeEntry();
        in.closeEntry();
        kept++;
      }
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {}
      out.close();
    }
    replace(zip, tmp);
    return kept;
  }
  static int replaceEntry(File zip, String inner, File src) throws IOException {
    if (src == null || !src.isFile()) {
      throw new IOException("replace");
    }
    if (TarOps.tarName(zip.getName())) {
      return TarOps.replaceEntry(zip, inner, src);
    }
    String want = norm(inner);
    if (want.length() == 0) {
      throw new IOException("replace");
    }
    File tmp = new File(zip.getAbsolutePath() + ".tmp");
    ZipFile zf = new ZipFile(zip);
    java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(tmp));
    int kept = 0;
    boolean found = false;
    try {
      byte[] buf = new byte[8192];
      Enumeration<? extends ZipEntry> en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = en.nextElement();
        String name = norm(e.getName());
        if (name.equals(want) && !e.isDirectory()) {
          found = true;
          kept += putFile(out, src, e.getName());
          continue;
        }
        out.putNextEntry(new ZipEntry(e.getName()));
        if (!e.isDirectory()) {
          InputStream in = zf.getInputStream(e);
          try {
            int n;
            while ((n = in.read(buf)) > 0) {
              out.write(buf, 0, n);
            }
          } finally {
            in.close();
          }
        }
        out.closeEntry();
        kept++;
      }
      if (!found) {
        kept += putFile(out, src, want);
      }
    } finally {
      try {
        zf.close();
      } catch (Exception ignored) {}
      out.close();
    }
    replace(zip, tmp);
    return kept;
  }
  static int addFiles(File zip, File[] srcs, String prefix) throws IOException {
    if (TarOps.tarName(zip.getName())) {
      return TarOps.addFiles(zip, srcs, prefix);
    }
    File tmp = new File(zip.getAbsolutePath() + ".tmp");
    java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zip));
    java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(tmp));
    int kept = 0;
    try {
      ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = in.getNextEntry()) != null) {
        out.putNextEntry(new ZipEntry(e.getName()));
        int n;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
        out.closeEntry();
        in.closeEntry();
        kept++;
      }
      String pre = prefix == null ? "" : prefix;
      if (pre.length() > 0 && !pre.endsWith("/")) {
        pre = pre + "/";
      }
      for (int i = 0; i < srcs.length; i++) {
        if (srcs[i] != null && srcs[i].exists()) {
          kept += putFile(out, srcs[i], pre + srcs[i].getName());
        }
      }
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {}
      out.close();
    }
    replace(zip, tmp);
    return kept;
  }
  private static int putFile(java.util.zip.ZipOutputStream out, File f, String name) throws IOException {
    if (f.isDirectory()) {
      String n = name.endsWith("/") ? name : name + "/";
      out.putNextEntry(new ZipEntry(n));
      out.closeEntry();
      int c = 1;
      File[] kids = f.listFiles();
      if (kids != null) {
        for (int i = 0; i < kids.length; i++) {
          c += putFile(out, kids[i], n + kids[i].getName());
        }
      }
      return c;
    }
    out.putNextEntry(new ZipEntry(name));
    java.io.FileInputStream in = new java.io.FileInputStream(f);
    try {
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
    } finally {
      in.close();
    }
    out.closeEntry();
    return 1;
  }
  private static void replace(File zip, File tmp) throws IOException {
    if (!zip.delete() && zip.exists()) {
      tmp.delete();
      throw new IOException("replace");
    }
    if (!tmp.renameTo(zip)) {
      FileOps.copyRec(tmp, zip);
      tmp.delete();
    }
  }
  private static boolean dropped(String name, String[] drop) {
    if (drop == null) {
      return false;
    }
    for (int i = 0; i < drop.length; i++) {
      String w = drop[i];
      if (w == null || w.length() == 0) {
        continue;
      }
      if (name.equals(w) || name.startsWith(w + "/")) {
        return true;
      }
    }
    return false;
  }
  private static List<Node> tarNames(File tar) throws IOException {
    List<TarOps.Entry> all = TarOps.list(tar);
    List<Node> nodes = new ArrayList<Node>();
    for (int i = 0; i < all.size(); i++) {
      TarOps.Entry e = all.get(i);
      Node n = new Node();
      n.name = e.name;
      n.inner = e.name.endsWith("/") ? e.name.substring(0, e.name.length() - 1) : e.name;
      n.dir = e.dir;
      n.size = e.size;
      n.time = e.time;
      nodes.add(n);
    }
    return nodes;
  }
  private static List<Node> kidsFromNames(List<Node> all, String inner, boolean showHidden) {
    String prefix = inner == null ? "" : inner;
    if (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    String pre = prefix.length() == 0 ? "" : prefix + "/";
    HashMap<String, Node> kids = new HashMap<String, Node>();
    for (int i = 0; i < all.size(); i++) {
      String name = all.get(i).inner;
      if (pre.length() > 0 && !name.equals(prefix) && !name.startsWith(pre)) {
        continue;
      }
      String rest = pre.length() == 0 ? name : (name.equals(prefix) ? "" : name.substring(pre.length()));
      if (rest.length() == 0) {
        continue;
      }
      int sl = rest.indexOf('/');
      boolean dir = sl >= 0 || all.get(i).dir;
      String child = sl >= 0 ? rest.substring(0, sl) : rest;
      if (child.length() == 0) {
        continue;
      }
      if (!showHidden && child.charAt(0) == '.') {
        continue;
      }
      Node n = kids.get(child);
      if (n == null) {
        n = new Node();
        n.name = child;
        n.inner = pre + child;
        n.dir = dir;
        n.size = dir ? 0L : all.get(i).size;
        n.time = all.get(i).time;
        kids.put(child, n);
      } else if (dir) {
        n.dir = true;
      }
    }
    List<Node> out = new ArrayList<Node>(kids.values());
    Collections.sort(out, nodeCmp());
    return out;
  }
  private static Comparator<Node> nodeCmp() {
    return new Comparator<Node>() {
      @Override
      public int compare(Node a, Node b) {
        AppSettings s = AppSettings.get();
        return FileOps.cmp(a.name, b.name, a.time, b.time, a.size, b.size, a.dir, b.dir,
          s.sortMode, s.foldersFirst);
      }
    };
  }
  private static String norm(String name) {
    if (name == null) {
      return "";
    }
    String s = name.replace('\\', '/');
    while (s.startsWith("/")) {
      s = s.substring(1);
    }
    if (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }
}
