package com.dlna.FileManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
final class MapBrowse {
  static final String SEP = "#/";
  static final class Node {
    String name;
    String inner;
    boolean dir;
    long size;
  }
  private static final HashMap<String, Node> META = new HashMap<String, Node>();
  private static File loaded;
  private static Object tree;
  private MapBrowse() {}
  static boolean isMapPath(String p) {
    return p != null && p.indexOf(SEP) >= 0;
  }
  static File mapOf(String p) {
    int i = p.indexOf(SEP);
    if (i >= 0) {
      return new File(p.substring(0, i));
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
    if (".".equals(s)) {
      return "";
    }
    return s;
  }
  static File root(File dat) {
    return new File(dat.getAbsolutePath() + SEP + ".");
  }
  static File child(File dat, String inner) {
    if (inner == null || inner.length() == 0) {
      return root(dat);
    }
    return new File(dat.getAbsolutePath() + SEP + inner);
  }
  static File parent(File cur) {
    String p = cur.getAbsolutePath();
    if (!isMapPath(p)) {
      return cur.getParentFile();
    }
    File dat = mapOf(p);
    String inner = innerOf(p);
    if (inner.length() == 0) {
      return dat.getParentFile();
    }
    int sl = inner.lastIndexOf('/');
    if (sl < 0) {
      return root(dat);
    }
    return child(dat, inner.substring(0, sl));
  }
  static String display(File cur) {
    String p = cur.getAbsolutePath();
    if (!isMapPath(p)) {
      return p;
    }
    File dat = mapOf(p);
    String inner = innerOf(p);
    return inner.length() == 0 ? dat.getAbsolutePath() : dat.getAbsolutePath() + "/" + inner;
  }
  static Node meta(String path) {
    return META.get(path);
  }
  static boolean isDir(File f) {
    String p = f.getAbsolutePath();
    if (!isMapPath(p)) {
      return false;
    }
    if (innerOf(p).length() == 0) {
      return true;
    }
    Node n = META.get(p);
    return n != null && n.dir;
  }
  static void clear() {
    META.clear();
    loaded = null;
    tree = null;
  }
  static Object tree(File dat) throws IOException {
    if (dat == null || !dat.isFile()) {
      throw new IOException("dat");
    }
    if (loaded != null && tree != null && loaded.getAbsolutePath().equals(dat.getAbsolutePath())) {
      return tree;
    }
    tree = MapArch.read(dat);
    loaded = dat;
    return tree;
  }
  static void remember(File dat, List<Node> nodes) {
    int i;
    for (i = 0; i < nodes.size(); i++) {
      Node n = nodes.get(i);
      String path = child(dat, n.inner).getAbsolutePath();
      if (META.size() >= 128) {
        META.remove(META.keySet().iterator().next());
      }
      META.put(path, n);
    }
  }
  static List<Node> list(File dat, String inner, boolean showHidden) throws IOException {
    Object node = walk(tree(dat), inner);
    ArrayList<Node> out = new ArrayList<Node>();
    if (!(node instanceof Map)) {
      return out;
    }
    Map<?, ?> map = (Map<?, ?>) node;
    Iterator<?> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
      String key = String.valueOf(e.getKey());
      if (key.length() == 0) {
        continue;
      }
      if (!showHidden && key.charAt(0) == '.') {
        continue;
      }
      Object val = e.getValue();
      Node n = new Node();
      boolean dir = val instanceof Map && !leafMap((Map<?, ?>) val);
      n.dir = dir;
      n.name = dir ? key : fileName(key);
      String pre = inner == null || inner.length() == 0 ? "" : inner + "/";
      n.inner = pre + n.name;
      n.size = dir ? 0L : sizeOf(val);
      out.add(n);
    }
    Collections.sort(out, new Comparator<Node>() {
      @Override
      public int compare(Node a, Node b) {
        if (a.dir != b.dir) {
          return a.dir ? -1 : 1;
        }
        return a.name.compareToIgnoreCase(b.name);
      }
    });
    return out;
  }
  static File materialize(File virt, File destDir) throws IOException {
    File dat = mapOf(virt.getAbsolutePath());
    String inner = innerOf(virt.getAbsolutePath());
    Object val = walk(tree(dat), inner);
    byte[] data = bytes(val);
    if (data == null) {
      data = new byte[0];
    }
    destDir.mkdirs();
    String name = inner;
    int sl = name.lastIndexOf('/');
    if (sl >= 0) {
      name = name.substring(sl + 1);
    }
    if (name.length() == 0) {
      name = "entry.txt";
    }
    name = name.replace('/', '_').replace('\\', '_');
    File out = new File(destDir, TrashStore.uniqueName(destDir, name));
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(out);
      fos.write(data);
      fos.flush();
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (Exception ignored) {}
      }
    }
    return out;
  }
  static void addEmpty(File dat, String inner, boolean dir) throws IOException {
    Object root = tree(dat);
    if (dir) {
      put(root, inner, new HashMap<String, Object>());
    } else {
      put(root, inner, "");
    }
    MapArch.writeObj(dat, root);
    loaded = dat;
    tree = root;
  }
  static void remove(File dat, String inner) throws IOException {
    Object root = tree(dat);
    if (!(root instanceof Map)) {
      throw new IOException("dat");
    }
    if (inner == null || inner.length() == 0 || ".".equals(inner)) {
      throw new IOException("inner");
    }
    String[] parts = split(inner);
    Map map = (Map) root;
    int i;
    for (i = 0; i < parts.length - 1; i++) {
      Object n = find(map, parts[i]);
      if (!(n instanceof Map)) {
        throw new IOException("key");
      }
      map = (Map) n;
    }
    String last = parts[parts.length - 1];
    String key = realKey(map, last);
    map.remove(key);
    MapArch.writeObj(dat, root);
    loaded = dat;
    tree = root;
  }
  static void replace(File dat, String inner, File src) throws IOException {
    Object root = tree(dat);
    Object val;
    try {
      val = FileOps.readString(src);
    } catch (Exception e) {
      throw new IOException("read");
    }
    put(root, inner, val);
    MapArch.writeObj(dat, root);
    loaded = dat;
    tree = root;
  }
  private static void put(Object root, String inner, Object val) throws IOException {
    if (!(root instanceof Map)) {
      throw new IOException("dat");
    }
    String[] parts = split(inner);
    Map map = (Map) root;
    int i;
    for (i = 0; i < parts.length - 1; i++) {
      Object n = find(map, parts[i]);
      if (!(n instanceof Map)) {
        throw new IOException("key");
      }
      map = (Map) n;
    }
    String last = parts[parts.length - 1];
    Object old = find(map, last);
    String key = realKey(map, last);
    if (old instanceof Map && leafMap((Map<?, ?>) old)) {
      ((Map) old).put("content", String.valueOf(val));
      map.put(key, old);
    } else {
      map.put(key, val);
    }
  }
  private static Object walk(Object root, String inner) {
    if (inner == null || inner.length() == 0 || ".".equals(inner)) {
      return root;
    }
    if (!(root instanceof Map)) {
      return null;
    }
    String[] parts = split(inner);
    Object cur = root;
    int i;
    for (i = 0; i < parts.length; i++) {
      if (!(cur instanceof Map)) {
        return null;
      }
      cur = find((Map) cur, parts[i]);
    }
    return cur;
  }
  private static Object find(Map map, String key) {
    if (map.containsKey(key)) {
      return map.get(key);
    }
    String stem = stripTxt(key);
    if (!stem.equals(key) && map.containsKey(stem)) {
      return map.get(stem);
    }
    Iterator it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry e = (Map.Entry) it.next();
      String k = String.valueOf(e.getKey());
      if (k.equals(key) || fileName(k).equals(key)) {
        return e.getValue();
      }
    }
    return null;
  }
  private static String realKey(Map map, String key) {
    if (map.containsKey(key)) {
      return key;
    }
    String stem = stripTxt(key);
    if (map.containsKey(stem)) {
      return stem;
    }
    Iterator it = map.keySet().iterator();
    while (it.hasNext()) {
      String k = String.valueOf(it.next());
      if (k.equals(key) || fileName(k).equals(key)) {
        return k;
      }
    }
    return stem;
  }
  private static String[] split(String inner) {
    if (inner == null || inner.length() == 0) {
      return new String[0];
    }
    ArrayList<String> p = new ArrayList<String>();
    int i = 0;
    while (i < inner.length()) {
      int sl = inner.indexOf('/', i);
      if (sl < 0) {
        p.add(inner.substring(i));
        break;
      }
      if (sl > i) {
        p.add(inner.substring(i, sl));
      }
      i = sl + 1;
    }
    return p.toArray(new String[p.size()]);
  }
  private static boolean leafMap(Map<?, ?> map) {
    if (map.containsKey("content") || map.containsKey("title") || map.containsKey("text")) {
      return true;
    }
    if (map.size() == 0 || map.size() > 12) {
      return false;
    }
    Iterator<?> it = map.values().iterator();
    while (it.hasNext()) {
      if (it.next() instanceof Map) {
        return false;
      }
    }
    return true;
  }
  private static String fileName(String key) {
    if (key.indexOf('.') >= 0) {
      return key;
    }
    return key + ".txt";
  }
  private static String stripTxt(String key) {
    if (key.endsWith(".txt") && key.length() > 4) {
      return key.substring(0, key.length() - 4);
    }
    return key;
  }
  private static long sizeOf(Object val) {
    byte[] b = bytes(val);
    return b == null ? 0L : b.length;
  }
  private static byte[] bytes(Object val) {
    if (val == null) {
      return new byte[0];
    }
    if (val instanceof byte[]) {
      return (byte[]) val;
    }
    if (val instanceof Map && leafMap((Map<?, ?>) val)) {
      Map<?, ?> m = (Map<?, ?>) val;
      Object title = m.get("title");
      Object content = m.get("content");
      if (content == null) {
        content = m.get("text");
      }
      StringBuilder sb = new StringBuilder();
      if (title != null) {
        sb.append(String.valueOf(title)).append('\n');
      }
      if (content != null) {
        sb.append(String.valueOf(content));
      }
      try {
        return sb.toString().getBytes("UTF-8");
      } catch (Exception e) {
        return sb.toString().getBytes();
      }
    }
    try {
      return String.valueOf(val).getBytes("UTF-8");
    } catch (Exception e) {
      return null;
    }
  }
}
