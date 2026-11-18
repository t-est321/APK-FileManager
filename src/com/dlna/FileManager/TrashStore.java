package com.dlna.FileManager;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
final class TrashStore {
  static final class Entry {
    String id;
    String name;
    String path;
    long deletedAt;
    long size;
    boolean isDir;
  }
  private TrashStore() {}
  static File trashDir(File filesDir) {
    return new File(filesDir, "trash");
  }
  static File binDir(File trashDir) {
    return new File(trashDir, "bin");
  }
  static File indexFile(File trashDir) {
    return new File(trashDir, "index.txt");
  }
  static File payload(File trashDir, String id) {
    return new File(binDir(trashDir), id);
  }
  static boolean isExpired(long deletedAt, long now) {
    int days = 30;
    try {
      days = AppSettings.get().trashDays();
    } catch (Exception ignored) {}
    if (days < 1) {
      days = 30;
    }
    return now - deletedAt > (long) days * 24L * 60L * 60L * 1000L;
  }
  private static List<Entry> readIndex(File trashDir) {
    List<Entry> out = new ArrayList<Entry>();
    File f = indexFile(trashDir);
    if (!f.exists()) {
      return out;
    }
    try {
      String[] lines = FileOps.readString(f).split("\n");
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i].trim();
        if (line.length() == 0) {
          continue;
        }
        JSONObject m = new JSONObject(line);
        Entry e = new Entry();
        e.id = m.optString("id", "");
        e.name = m.optString("name", "?");
        e.path = m.optString("path", "");
        e.deletedAt = m.optLong("deletedAt", 0L);
        e.size = m.optLong("size", 0L);
        e.isDir = m.optBoolean("isDir", false);
        if (e.id.length() > 0) {
          out.add(e);
        }
      }
    } catch (Exception ignored) {}
    return out;
  }
  private static void writeIndex(File trashDir, List<Entry> entries) {
    try {
      trashDir.mkdirs();
      StringBuilder sb = new StringBuilder(entries.size() * 96);
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        JSONObject m = new JSONObject();
        m.put("id", e.id);
        m.put("name", e.name);
        m.put("path", e.path);
        m.put("deletedAt", e.deletedAt);
        m.put("size", e.size);
        m.put("isDir", e.isDir);
        sb.append(m.toString());
        if (i < entries.size() - 1) {
          sb.append('\n');
        }
      }
      FileOps.writeString(indexFile(trashDir), sb.toString());
    } catch (Exception ignored) {}
  }
  private static int indexOf(List<Entry> entries, String id) {
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).id.equals(id)) {
        return i;
      }
    }
    return -1;
  }
  static synchronized boolean add(File trashDir, File src) {
    if (!src.exists()) {
      return false;
    }
    if (FileOps.under(src, trashDir)) {
      return false;
    }
    String id = Long.toHexString(System.currentTimeMillis()) + "_" + Long.toHexString(System.nanoTime() & 0xFFFFFFL);
    binDir(trashDir).mkdirs();
    File payload = payload(trashDir, id);
    if (!FileOps.move(src, payload)) {
      FileOps.deleteRec(payload);
      return false;
    }
    Entry e = new Entry();
    e.id = id;
    e.name = src.getName();
    e.path = src.getAbsolutePath();
    e.deletedAt = System.currentTimeMillis();
    e.size = FileOps.sizeOf(payload);
    e.isDir = payload.isDirectory();
    List<Entry> entries = readIndex(trashDir);
    entries.add(e);
    writeIndex(trashDir, entries);
    return true;
  }
  static List<Entry> list(File trashDir, long now) {
    purge(trashDir, now);
    List<Entry> out = readIndex(trashDir);
    Collections.sort(out, new Comparator<Entry>() {
      @Override
      public int compare(Entry a, Entry b) {
        return a.deletedAt < b.deletedAt ? 1 : (a.deletedAt > b.deletedAt ? -1 : 0);
      }
    });
    return out;
  }
  static synchronized int restore(File trashDir, String id) {
    List<Entry> entries = readIndex(trashDir);
    int idx = indexOf(entries, id);
    if (idx < 0) {
      return -1;
    }
    Entry e = entries.get(idx);
    if (e.path.length() == 0) {
      return -1;
    }
    File payload = payload(trashDir, id);
    if (!payload.exists()) {
      return -1;
    }
    File target = new File(e.path);
    if (target.exists()) {
      target = new File(target.getParentFile(), uniqueName(target.getParentFile(), target.getName()));
    }
    File parent = target.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    if (!FileOps.move(payload, target)) {
      return -1;
    }
    entries.remove(idx);
    writeIndex(trashDir, entries);
    return 1;
  }
  static synchronized void deleteForever(File trashDir, String id) {
    FileOps.deleteRec(payload(trashDir, id));
    List<Entry> entries = readIndex(trashDir);
    int idx = indexOf(entries, id);
    if (idx >= 0) {
      entries.remove(idx);
      writeIndex(trashDir, entries);
    }
  }
  static synchronized int empty(File trashDir) {
    int count = readIndex(trashDir).size();
    FileOps.emptyDir(trashDir);
    trashDir.mkdirs();
    binDir(trashDir).mkdirs();
    writeIndex(trashDir, new ArrayList<Entry>());
    return count;
  }
  static int purge(File trashDir, long now) {
    List<Entry> entries = readIndex(trashDir);
    List<Entry> keep = new ArrayList<Entry>();
    int removed = 0;
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      if (isExpired(e.deletedAt, now)) {
        FileOps.deleteRec(payload(trashDir, e.id));
        removed++;
      } else {
        keep.add(e);
      }
    }
    if (removed > 0) {
      writeIndex(trashDir, keep);
    }
    return removed;
  }
  static synchronized int migrate(File filesDir) {
    File old = trashDir(filesDir);
    if (!old.isDirectory() || indexFile(old).exists()) {
      return 0;
    }
    File bin = binDir(old);
    List<Entry> entries = new ArrayList<Entry>();
    File[] dirs = old.listFiles();
    if (dirs != null) {
      for (int i = 0; i < dirs.length; i++) {
        if ("bin".equals(dirs[i].getName()) || !dirs[i].isDirectory()) {
          continue;
        }
        try {
          JSONObject m = new JSONObject(FileOps.readString(new File(dirs[i], "meta.json")));
          Entry e = new Entry();
          e.id = dirs[i].getName();
          e.name = m.optString("name", "?");
          e.path = m.optString("path", "");
          e.deletedAt = m.optLong("deletedAt", 0L);
          e.size = m.optLong("size", 0L);
          e.isDir = m.optBoolean("isDir", false);
          File pl = new File(dirs[i], "payload");
          if (pl.exists() && e.id.length() > 0) {
            bin.mkdirs();
            if (FileOps.move(pl, new File(bin, e.id))) {
              entries.add(e);
            }
          }
        } catch (Exception ignored) {}
      }
    }
    File[] leftovers = old.listFiles();
    int moved = entries.size();
    if (leftovers != null) {
      for (int i = 0; i < leftovers.length; i++) {
        if (!"bin".equals(leftovers[i].getName()) && !"index.txt".equals(leftovers[i].getName())) {
          FileOps.deleteRec(leftovers[i]);
        }
      }
    }
    writeIndex(old, entries);
    return moved;
  }
  static synchronized int compact(File trashDir) {
    List<Entry> entries = readIndex(trashDir);
    List<Entry> keep = new ArrayList<Entry>();
    java.util.HashSet<String> ids = new java.util.HashSet<String>();
    int dropped = 0;
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      File pl = payload(trashDir, e.id);
      if (e.id.length() == 0 || !pl.exists()) {
        dropped++;
        continue;
      }
      keep.add(e);
      ids.add(e.id);
    }
    File bin = binDir(trashDir);
    File[] kids = bin.listFiles();
    int orphans = 0;
    if (kids != null) {
      for (int i = 0; i < kids.length; i++) {
        if (!ids.contains(kids[i].getName())) {
          FileOps.deleteRec(kids[i]);
          orphans++;
        }
      }
    }
    writeIndex(trashDir, keep);
    return dropped + orphans;
  }
  static String uniqueName(File parent, String base) {
    if (!new File(parent, base).exists()) {
      return base;
    }
    int dot = base.lastIndexOf('.');
    String stem = dot > 0 ? base.substring(0, dot) : base;
    String ext = dot > 0 ? base.substring(dot) : "";
    for (int i = 1; i < 1000; i++) {
      String cand = stem + " (" + i + ")" + ext;
      if (!new File(parent, cand).exists()) {
        return cand;
      }
    }
    return stem + " (" + System.currentTimeMillis() + ")" + ext;
  }
}
