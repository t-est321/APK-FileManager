package com.dlna.FileManager;
final class SvgIcons {
  private SvgIcons() {}
  static String hex(int argb) {
    return String.format("#%06X", Integer.valueOf(argb & 0xFFFFFF));
  }
  static String of(int kind, int tint) {
    String c = hex(tint);
    boolean grad = AppSettings.get().iconGradient;
    float y0 = kind == FileGlyph.DIR ? 24f : 16f;
    float y1 = kind == FileGlyph.DIR ? 76f : 80f;
    String fill = c;
    String defs = "";
    if (grad && kind != FileGlyph.UP) {
      defs = "<linearGradient id=\"g\" x1=\"0\" y1=\"" + y0 + "\" x2=\"0\" y2=\"" + y1 + "\">"
        + "<stop offset=\"0\" stop-color=\"" + c + "\"/>"
        + "<stop offset=\"1\" stop-color=\"" + c + "\" stop-opacity=\"0.10\"/>"
        + "</linearGradient>";
      fill = "url(#g)";
    }
    if (kind == FileGlyph.DIR) {
      return svg(defs
        + "<path fill=\"" + fill + "\" d=\"M12 30 L38 30 L46 38 L84 38 L84 76 L12 76 Z\"/>"
        + "<path fill=\"" + c + "\" fill-opacity=\"0.90\" d=\"M12 24h32v10H12z\"/>");
    }
    if (kind == FileGlyph.UP) {
      return svg("<path fill=\"none\" stroke=\"" + c + "\" stroke-width=\"9\" "
        + "d=\"M24 60 L48 34 L72 60 M48 36 L48 74\"/>");
    }
    String page = "<path fill=\"" + fill + "\" d=\"M30 16 L58 16 L72 30 L72 80 L30 80 Z\"/>"
      + "<path fill=\"" + c + "\" fill-opacity=\"0.55\" d=\"M58 16 L58 30 L72 30 Z\"/>";
    String mark;
    if (kind == FileGlyph.IMG) {
      mark = "<path fill=\"#ffffff\" d=\"M52 31 L62 31 L62 41 L52 41 Z\"/>"
        + "<path fill=\"#ffffff\" d=\"M34 68 L46 52 L54 61 L60 54 L66 68 Z\"/>";
    } else if (kind == FileGlyph.VIDEO) {
      mark = "<path fill=\"#ffffff\" d=\"M42 40 L62 52 L42 64 Z\"/>";
    } else if (kind == FileGlyph.AUDIO) {
      mark = "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"4\" d=\"M42 40 L56 36 L56 60\"/>"
        + "<path fill=\"#ffffff\" d=\"M44 58h14v10H44z\"/>";
    } else if (kind == FileGlyph.ARCH) {
      mark = "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"3\" "
        + "d=\"M38 44h24v14H38z M38 58h24v14H38z M50 36v32\"/>";
    } else if (kind == FileGlyph.CODE) {
      mark = "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"4\" "
        + "d=\"M46 44 L36 54 L46 64 M54 44 L64 54 L54 64\"/>";
    } else if (kind == FileGlyph.APK) {
      mark = "<path fill=\"#ffffff\" d=\"M40 44h20v20H40z\"/>"
        + "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"4\" d=\"M44 44 L40 34 M56 44 L60 34\"/>";
    } else if (kind == FileGlyph.PDF) {
      mark = "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"3\" "
        + "d=\"M40 46h20 M40 54h20 M40 62h12\"/>";
    } else if (kind == FileGlyph.TEXT) {
      mark = "<path fill=\"none\" stroke=\"#ffffff\" stroke-width=\"3\" "
        + "d=\"M40 42h20 M40 50h20 M40 58h20 M40 66h12\"/>";
    } else {
      mark = "";
    }
    return svg(defs + page + mark);
  }
  private static String svg(String inner) {
    return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 96 96\">" + inner + "</svg>";
  }
}
