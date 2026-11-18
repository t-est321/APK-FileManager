package com.dlna.FileManager;
import android.graphics.Bitmap;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
final class SoftBmp {
  private final LinkedHashMap<String, WeakReference<Bitmap>> map;
  SoftBmp(final int max) {
    map = new LinkedHashMap<String, WeakReference<Bitmap>>(max, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, WeakReference<Bitmap>> e) {
        return size() > max;
      }
    };
  }
  private static void drop(WeakReference<Bitmap> r) {
    if (r == null) {
      return;
    }
    Bitmap b = r.get();
    if (b != null && !b.isRecycled()) {
      b.recycle();
    }
  }
  synchronized Bitmap get(String key) {
    WeakReference<Bitmap> r = map.get(key);
    if (r == null) {
      return null;
    }
    Bitmap b = r.get();
    if (b == null || b.isRecycled()) {
      map.remove(key);
      return null;
    }
    return b;
  }
  synchronized void put(String key, Bitmap b) {
    if (key == null || b == null || b.isRecycled()) {
      return;
    }
    map.put(key, new WeakReference<Bitmap>(b));
  }
  synchronized void clear() {
    Iterator<WeakReference<Bitmap>> it = map.values().iterator();
    while (it.hasNext()) {
      drop(it.next());
    }
    map.clear();
  }
}
