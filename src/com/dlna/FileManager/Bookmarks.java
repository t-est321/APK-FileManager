package com.dlna.FileManager;
import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
final class Bookmarks {
  static final class Item {
    String name;
    String path;
  }
  private Bookmarks() {}
  static File file(Context c) {
    return new File(c.getFilesDir(), "bookmarks.txt");
  }
  static List<Item> load(Context c) {
    List<Item> out = new ArrayList<Item>();
    try {
      String[] lines = FileOps.readString(file(c)).split("\n");
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i].trim();
        if (line.length() == 0) {
          continue;
        }
        Item it = new Item();
        int tab = line.indexOf('\t');
        if (tab > 0) {
          it.name = line.substring(0, tab);
          it.path = line.substring(tab + 1);
        } else {
          it.path = line;
          it.name = new File(line).getName();
          if (it.name.length() == 0) {
            it.name = line;
          }
        }
        out.add(it);
      }
    } catch (Exception ignored) {}
    return out;
  }
  static void save(Context c, List<Item> items) {
    try {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < items.size(); i++) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(items.get(i).name).append('\t').append(items.get(i).path);
      }
      FileOps.writeString(file(c), sb.toString());
    } catch (Exception ignored) {}
  }
  static boolean add(Context c, File dir) {
    if (dir == null) {
      return false;
    }
    String p = dir.getAbsolutePath();
    List<Item> all = load(c);
    if (indexOf(all, p) >= 0) {
      return false;
    }
    Item it = new Item();
    it.path = p;
    it.name = dir.getName();
    if (it.name.length() == 0) {
      it.name = p;
    }
    all.add(it);
    save(c, all);
    return true;
  }
  static boolean remove(Context c, String path) {
    List<Item> all = load(c);
    int idx = indexOf(all, path);
    if (idx < 0) {
      return false;
    }
    all.remove(idx);
    save(c, all);
    return true;
  }
  static boolean has(Context c, File dir) {
    return dir != null && indexOf(load(c), dir.getAbsolutePath()) >= 0;
  }
  static void update(Context c, String oldPath, String name, String path) {
    List<Item> all = load(c);
    int idx = indexOf(all, oldPath);
    if (idx < 0) {
      return;
    }
    all.get(idx).name = name;
    all.get(idx).path = path;
    save(c, all);
  }
  static int indexOf(List<Item> items, String path) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).path.equals(path)) {
        return i;
      }
    }
    return -1;
  }
}
