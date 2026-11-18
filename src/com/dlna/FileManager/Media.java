package com.dlna.FileManager;
import java.io.File;
import java.io.FileInputStream;
final class Media {
  static final int NONE = 0;
  static final int IMAGE = 1;
  static final int VIDEO = 2;
  static final int AUDIO = 3;
  private Media() {}
  static int of(String name) {
    String e = FileOps.ext(name);
    if (e.length() == 0) {
      return NONE;
    }
    int mapped = AppSettings.get().mapExt(e);
    if (mapped == FileGlyph.IMG) {
      return IMAGE;
    }
    if (mapped == FileGlyph.VIDEO) {
      return VIDEO;
    }
    if (mapped == FileGlyph.AUDIO) {
      return AUDIO;
    }
    String tag = "|" + e + "|";
    if ("|png|jpg|jpeg|gif|webp|bmp|svg|".contains(tag)) {
      return IMAGE;
    }
    if ("|mp4|mkv|avi|webm|3gp|mov|flv|ts|m2ts|mpg|mpeg|wmv|".contains(tag)) {
      return VIDEO;
    }
    if ("|mp3|wav|ogg|flac|m4a|aac|opus|amr|mid|".contains(tag)) {
      return AUDIO;
    }
    return NONE;
  }
  static int ofFile(File f) {
    if (f == null) {
      return NONE;
    }
    int n = of(f.getName());
    if (n != NONE) {
      return n;
    }
    return sniff(f);
  }
  static boolean gifHeader(File f) {
    byte[] b = head(f, 6);
    if (b == null || b.length < 6) {
      return false;
    }
    return b[0] == 'G' && b[1] == 'I' && b[2] == 'F'
      && b[3] == '8' && (b[4] == '7' || b[4] == '9') && b[5] == 'a';
  }
  static int sniff(File f) {
    byte[] b = head(f, 16);
    if (b == null || b.length < 3) {
      return NONE;
    }
    if ((b[0] & 255) == 0xFF && (b[1] & 255) == 0xD8 && (b[2] & 255) == 0xFF) {
      return IMAGE;
    }
    if (b.length >= 8 && (b[0] & 255) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
      return IMAGE;
    }
    if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
      return IMAGE;
    }
    if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') {
      return IMAGE;
    }
    if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
        && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
      return IMAGE;
    }
    return NONE;
  }
  private static byte[] head(File f, int n) {
    if (f == null || !f.isFile() || n <= 0) {
      return null;
    }
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
      byte[] b = new byte[n];
      int got = 0;
      while (got < n) {
        int r = in.read(b, got, n - got);
        if (r <= 0) {
          break;
        }
        got += r;
      }
      if (got <= 0) {
        return null;
      }
      if (got == n) {
        return b;
      }
      byte[] cut = new byte[got];
      System.arraycopy(b, 0, cut, 0, got);
      return cut;
    } catch (Exception e) {
      return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {}
      }
    }
  }
}
