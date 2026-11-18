package com.dlna.FileManager;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
final class MapArch {
  private MapArch() {}
  static boolean isMap(String name) {
    return "dat".equals(FileOps.ext(name));
  }
  static File archivesDir() {
    File d = new File(AppSettings.publicDir(), "HashMap/Archives");
    if (!d.exists()) {
      d.mkdirs();
    }
    return d;
  }
  static File extractedDir() {
    File d = new File(AppSettings.publicDir(), "HashMap/Extracted");
    if (!d.exists()) {
      d.mkdirs();
    }
    return d;
  }
  static int pack(File[] srcs, File dest) throws IOException {
    HashMap<String, byte[]> files = new HashMap<String, byte[]>();
    int i;
    for (i = 0; i < srcs.length; i++) {
      File s = srcs[i];
      if (s == null) {
        continue;
      }
      if (s.isFile()) {
        files.put(s.getName(), readAll(s));
      } else if (s.isDirectory()) {
        putTree(files, s, s.getName());
      }
    }
    HashMap<String, Object> root = new HashMap<String, Object>();
    root.put("archive", files);
    File parent = dest.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(dest));
      out.writeObject(root);
      out.flush();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception ignored) {}
      }
    }
    return files.size();
  }
  static int unpack(File src, File destDir) throws IOException {
    Object raw = load(src);
    if (raw == null) {
      throw new IOException("dat");
    }
    if (!destDir.exists() && !destDir.mkdirs()) {
      throw new IOException("mkdir " + destDir);
    }
    int n = dump(raw, destDir, true);
    pruneEmpty(destDir);
    return n;
  }
  static Object read(File src) throws IOException {
    return load(src);
  }
  static void writeObj(File dest, Object root) throws IOException {
    File parent = dest.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(dest));
      out.writeObject(root);
      out.flush();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static Object load(File src) throws IOException {
    try {
      Object o = readObj(src);
      if (o != null) {
        return o;
      }
    } catch (Exception ignored) {}
    return Ser.parse(src);
  }
  private static void pruneEmpty(File dir) {
    if (dir == null || !dir.isDirectory()) {
      return;
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    int i;
    for (i = 0; i < kids.length; i++) {
      File k = kids[i];
      if (k.isDirectory()) {
        pruneEmpty(k);
        File[] inner = k.listFiles();
        if (inner == null || inner.length == 0) {
          k.delete();
        }
      }
    }
  }
  private static int dump(Object raw, File destDir, boolean root) throws IOException {
    if (raw instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) raw;
      int n = 0;
      Iterator<?> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
        String key = keyStr(e.getKey());
        if (!safeKey(key)) {
          continue;
        }
        Object val = e.getValue();
        if (val instanceof Map) {
          if (((Map<?, ?>) val).isEmpty()) {
            continue;
          }
          boolean flatten = root && ("archive".equals(key) || "bin".equals(key));
          if (flatten) {
            n += dump(val, destDir, false);
          } else if (isRecord((Map<?, ?>) val)) {
            n += writeFile(destDir, fileName(key), recordBytes((Map<?, ?>) val));
          } else {
            File sub = new File(destDir, key);
            if (!FileOps.under(sub, destDir) && !sub.getAbsolutePath().equals(destDir.getAbsolutePath())) {
              continue;
            }
            if (!sub.exists() && !sub.mkdirs()) {
              continue;
            }
            n += dump(val, sub, false);
          }
        } else {
          byte[] data = toBytes(val);
          if (data == null) {
            continue;
          }
          n += writeFile(destDir, key, data);
        }
      }
      return n;
    }
    byte[] one = toBytes(raw);
    if (one == null) {
      return 0;
    }
    return writeFile(destDir, "data.bin", one);
  }
  private static boolean isRecord(Map<?, ?> map) {
    if (map.containsKey("content") || map.containsKey("title") || map.containsKey("text")) {
      return true;
    }
    if (map.size() == 0) {
      return false;
    }
    Iterator<?> it = map.values().iterator();
    while (it.hasNext()) {
      Object v = it.next();
      if (v instanceof Map) {
        return false;
      }
    }
    return map.size() <= 12;
  }
  private static byte[] recordBytes(Map<?, ?> map) {
    StringBuilder sb = new StringBuilder();
    Object title = map.get("title");
    Object content = map.get("content");
    if (content == null) {
      content = map.get("text");
    }
    if (title != null) {
      sb.append(String.valueOf(title)).append('\n');
    }
    if (content != null) {
      sb.append(String.valueOf(content));
    }
    if (sb.length() == 0) {
      Iterator<?> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
        sb.append(keyStr(e.getKey())).append('=').append(String.valueOf(e.getValue())).append('\n');
      }
    }
    try {
      return sb.toString().getBytes("UTF-8");
    } catch (Exception e) {
      return sb.toString().getBytes();
    }
  }
  private static String fileName(String key) {
    if (key.indexOf('.') >= 0) {
      return key;
    }
    return key + ".txt";
  }
  private static String keyStr(Object k) {
    return k == null ? "null" : String.valueOf(k);
  }
  private static int writeFile(File destDir, String key, byte[] data) throws IOException {
    if (data == null || !safeKey(key)) {
      return 0;
    }
    File out = new File(destDir, key);
    if (!FileOps.under(out, destDir)) {
      return 0;
    }
    File par = out.getParentFile();
    if (par != null && !par.exists()) {
      par.mkdirs();
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(out);
      fos.write(data);
      fos.flush();
      return 1;
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static byte[] toBytes(Object val) {
    if (val == null) {
      return null;
    }
    if (val instanceof byte[]) {
      return (byte[]) val;
    }
    if (val instanceof char[]) {
      try {
        return new String((char[]) val).getBytes("UTF-8");
      } catch (Exception e) {
        return null;
      }
    }
    if (val instanceof String || val instanceof Number || val instanceof Boolean) {
      try {
        return String.valueOf(val).getBytes("UTF-8");
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }
  private static void putTree(HashMap<String, byte[]> map, File dir, String prefix) throws IOException {
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    int i;
    for (i = 0; i < kids.length; i++) {
      File k = kids[i];
      String name = prefix + "/" + k.getName();
      if (k.isDirectory()) {
        putTree(map, k, name);
      } else if (k.isFile()) {
        map.put(name, readAll(k));
      }
    }
  }
  private static byte[] readAll(File f) throws IOException {
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
      return out.toByteArray();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static Object readObj(File f) throws IOException {
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(f));
      return in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("dat");
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
  private static boolean safeKey(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    if (key.charAt(0) == '/' || key.charAt(0) == '\\') {
      return false;
    }
    return key.indexOf("..") < 0;
  }
  private static final class Ser {
    private static final byte TC_NULL = 0x70;
    private static final byte TC_REFERENCE = 0x71;
    private static final byte TC_CLASSDESC = 0x72;
    private static final byte TC_OBJECT = 0x73;
    private static final byte TC_STRING = 0x74;
    private static final byte TC_ARRAY = 0x75;
    private static final byte TC_CLASS = 0x76;
    private static final byte TC_BLOCKDATA = 0x77;
    private static final byte TC_ENDBLOCKDATA = 0x78;
    private static final byte TC_RESET = 0x79;
    private static final byte TC_BLOCKDATALONG = 0x7A;
    private static final byte TC_LONGSTRING = 0x7C;
    private static final byte TC_PROXYCLASSDESC = 0x7D;
    private static final byte TC_ENUM = 0x7E;
    private static final byte SC_WRITE_METHOD = 0x01;
    private static final byte SC_SERIALIZABLE = 0x02;
    private static final byte SC_EXTERNALIZABLE = 0x04;
    private static final byte SC_BLOCK_DATA = 0x08;
    private final DataInputStream in;
    private final ArrayList<Object> handles = new ArrayList<Object>();
    private Ser(InputStream raw) {
      in = new DataInputStream(raw);
    }
    static Object parse(File f) throws IOException {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(f);
        Ser s = new Ser(new BufferedInputStream(fis));
        if (s.in.readShort() != (short) 0xACED) {
          throw new IOException("magic");
        }
        s.in.readShort();
        return s.readContent();
      } finally {
        if (fis != null) {
          try {
            fis.close();
          } catch (Exception ignored) {}
        }
      }
    }
    private int newHandle(Object o) {
      handles.add(o);
      return handles.size() - 1;
    }
    private Object handleAt(int ref) {
      int i = ref - 0x7E0000;
      if (i < 0 || i >= handles.size()) {
        return null;
      }
      return handles.get(i);
    }
    private Object readContent() throws IOException {
      byte tc = in.readByte();
      return readTc(tc);
    }
    private Object readTc(byte tc) throws IOException {
      if (tc == TC_NULL) {
        return null;
      }
      if (tc == TC_REFERENCE) {
        return handleAt(in.readInt());
      }
      if (tc == TC_STRING) {
        return readUtf(false);
      }
      if (tc == TC_LONGSTRING) {
        return readUtf(true);
      }
      if (tc == TC_RESET) {
        handles.clear();
        return readContent();
      }
      if (tc == TC_BLOCKDATA) {
        int n = in.readUnsignedByte();
        skip(n);
        return readContent();
      }
      if (tc == TC_BLOCKDATALONG) {
        int n = in.readInt();
        skip(n);
        return readContent();
      }
      if (tc == TC_ENDBLOCKDATA) {
        return null;
      }
      if (tc == TC_CLASSDESC || tc == TC_PROXYCLASSDESC) {
        Cls c = readClassDesc(tc);
        return c;
      }
      if (tc == TC_CLASS) {
        Cls c = readClassDesc(in.readByte());
        newHandle(c);
        return c;
      }
      if (tc == TC_ARRAY) {
        return readArray();
      }
      if (tc == TC_OBJECT) {
        return readNewObject();
      }
      if (tc == TC_ENUM) {
        return readEnum();
      }
      throw new IOException("tc " + (tc & 0xFF));
    }
    private String readUtf(boolean lng) throws IOException {
      int n = lng ? (int) in.readLong() : in.readUnsignedShort();
      byte[] b = new byte[n];
      in.readFully(b);
      String s;
      try {
        s = new String(b, "UTF-8");
      } catch (Exception e) {
        s = new String(b);
      }
      newHandle(s);
      return s;
    }
    private Cls readClassDesc(byte tc) throws IOException {
      if (tc == TC_NULL) {
        return null;
      }
      if (tc == TC_REFERENCE) {
        Object o = handleAt(in.readInt());
        return o instanceof Cls ? (Cls) o : null;
      }
      if (tc == TC_PROXYCLASSDESC) {
        Cls c = new Cls();
        c.name = "proxy";
        newHandle(c);
        int n = in.readInt();
        int i;
        for (i = 0; i < n; i++) {
          in.readUTF();
        }
        skipAnn();
        c.parent = readClassDesc(in.readByte());
        return c;
      }
      if (tc != TC_CLASSDESC) {
        throw new IOException("cls");
      }
      Cls c = new Cls();
      c.name = in.readUTF();
      c.uid = in.readLong();
      c.flags = in.readByte();
      int nf = in.readUnsignedShort();
      c.fields = new Field[nf];
      newHandle(c);
      int i;
      for (i = 0; i < nf; i++) {
        Field f = new Field();
        f.type = (char) in.readByte();
        f.name = in.readUTF();
        if (f.type == 'L' || f.type == '[') {
          Object t = readContent();
          f.typeName = t == null ? "" : String.valueOf(t);
        }
        c.fields[i] = f;
      }
      skipAnn();
      c.parent = readClassDesc(in.readByte());
      return c;
    }
    private void skipAnn() throws IOException {
      while (true) {
        in.mark(1);
        int b = in.read();
        if (b < 0) {
          return;
        }
        if ((byte) b == TC_ENDBLOCKDATA) {
          return;
        }
        in.reset();
        readContent();
      }
    }
    private Object readArray() throws IOException {
      Cls c = readClassDesc(in.readByte());
      int n = in.readInt();
      Object box = new Object();
      newHandle(box);
      if (c != null && "[B".equals(c.name)) {
        byte[] arr = new byte[n];
        in.readFully(arr);
        return arr;
      }
      if (c != null && c.name != null && c.name.startsWith("[") && c.name.length() == 2) {
        char t = c.name.charAt(1);
        int i;
        for (i = 0; i < n; i++) {
          readPrim(t);
        }
        return new byte[0];
      }
      ArrayList<Object> list = new ArrayList<Object>();
      int i;
      for (i = 0; i < n; i++) {
        list.add(readContent());
      }
      return list;
    }
    private Object readEnum() throws IOException {
      readClassDesc(in.readByte());
      Object name = readContent();
      newHandle(name);
      return name;
    }
    private Object readNewObject() throws IOException {
      Cls c = readClassDesc(in.readByte());
      LinkedHashMap<String, Object> rec = new LinkedHashMap<String, Object>();
      newHandle(rec);
      fillObject(c, rec);
      if (c != null && isMapClass(c.name)) {
        return mapFrom(c, rec);
      }
      return rec;
    }
    private boolean isMapClass(String n) {
      return "java.util.HashMap".equals(n) || "java.util.LinkedHashMap".equals(n)
        || "java.util.Hashtable".equals(n) || "java.util.TreeMap".equals(n)
        || "java.util.concurrent.ConcurrentHashMap".equals(n);
    }
    private Object mapFrom(Cls c, LinkedHashMap<String, Object> rec) throws IOException {
      LinkedHashMap<Object, Object> map = new LinkedHashMap<Object, Object>();
      if (c != null && (c.flags & SC_WRITE_METHOD) != 0) {
        readMapBody(map);
      }
      return map;
    }
    private void readMapBody(Map<Object, Object> map) throws IOException {
      byte tc = in.readByte();
      if (tc != TC_BLOCKDATA && tc != TC_BLOCKDATALONG) {
        if (tc == TC_ENDBLOCKDATA) {
          return;
        }
        Object first = readTc(tc);
        if (first != null) {
          map.put("0", first);
        }
        drainBlock(map);
        return;
      }
      int n = tc == TC_BLOCKDATA ? in.readUnsignedByte() : in.readInt();
      int size = 0;
      if (n >= 8) {
        in.readInt();
        size = in.readInt();
        n -= 8;
      } else if (n >= 4) {
        size = in.readInt();
        n -= 4;
      }
      skip(n);
      int i;
      for (i = 0; i < size; i++) {
        Object k = readContent();
        Object v = readContent();
        if (k != null) {
          map.put(k, v);
        }
      }
      expectEnd();
    }
    private void drainBlock(Map<Object, Object> map) throws IOException {
      while (true) {
        byte tc = in.readByte();
        if (tc == TC_ENDBLOCKDATA) {
          return;
        }
        Object o = readTc(tc);
        if (o != null) {
          map.put(String.valueOf(map.size()), o);
        }
      }
    }
    private void expectEnd() throws IOException {
      byte tc = in.readByte();
      if (tc != TC_ENDBLOCKDATA) {
        readTc(tc);
        while (true) {
          byte n = in.readByte();
          if (n == TC_ENDBLOCKDATA) {
            return;
          }
          readTc(n);
        }
      }
    }
    private void fillObject(Cls c, LinkedHashMap<String, Object> rec) throws IOException {
      if (c == null) {
        return;
      }
      fillObject(c.parent, rec);
      if ((c.flags & SC_EXTERNALIZABLE) != 0) {
        if ((c.flags & SC_BLOCK_DATA) != 0) {
          skipBlock();
        }
        return;
      }
      if ((c.flags & SC_SERIALIZABLE) == 0) {
        return;
      }
      int i;
      if (c.fields != null) {
        for (i = 0; i < c.fields.length; i++) {
          Field f = c.fields[i];
          rec.put(f.name, readField(f));
        }
      }
      if ((c.flags & SC_WRITE_METHOD) != 0 && !isMapClass(c.name)) {
        skipBlock();
      }
    }
    private void skipBlock() throws IOException {
      while (true) {
        byte tc = in.readByte();
        if (tc == TC_ENDBLOCKDATA) {
          return;
        }
        if (tc == TC_BLOCKDATA) {
          skip(in.readUnsignedByte());
        } else if (tc == TC_BLOCKDATALONG) {
          skip(in.readInt());
        } else {
          readTc(tc);
        }
      }
    }
    private Object readField(Field f) throws IOException {
      if (f.type == 'L' || f.type == '[') {
        return readContent();
      }
      return readPrim(f.type);
    }
    private Object readPrim(char t) throws IOException {
      if (t == 'B') {
        return Byte.valueOf(in.readByte());
      }
      if (t == 'C') {
        return Character.valueOf(in.readChar());
      }
      if (t == 'D') {
        return Double.valueOf(in.readDouble());
      }
      if (t == 'F') {
        return Float.valueOf(in.readFloat());
      }
      if (t == 'I') {
        return Integer.valueOf(in.readInt());
      }
      if (t == 'J') {
        return Long.valueOf(in.readLong());
      }
      if (t == 'S') {
        return Short.valueOf(in.readShort());
      }
      if (t == 'Z') {
        return Boolean.valueOf(in.readBoolean());
      }
      return null;
    }
    private void skip(int n) throws IOException {
      while (n > 0) {
        int k = (int) in.skip(n);
        if (k <= 0) {
          if (in.read() < 0) {
            return;
          }
          n--;
        } else {
          n -= k;
        }
      }
    }
    private static final class Cls {
      String name;
      long uid;
      byte flags;
      Field[] fields;
      Cls parent;
    }
    private static final class Field {
      char type;
      String name;
      String typeName;
    }
  }
}
