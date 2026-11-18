package com.dlna.FileManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
final class Icons {
  private static final int SZ = 96;
  private static final SoftBmp CACHE = new SoftBmp(32);
  private Icons() {}
  static int tintFor(int kind) {
    switch (kind) {
      case FileGlyph.IMG: return 0xFF7BD88F;
      case FileGlyph.VIDEO: return 0xFFF26D6D;
      case FileGlyph.AUDIO: return 0xFFC792EA;
      case FileGlyph.ARCH: return 0xFFF2C14E;
      case FileGlyph.CODE: return 0xFF5AB0F8;
      case FileGlyph.APK: return 0xFF8BE0C9;
      case FileGlyph.PDF: return 0xFFF28B6D;
      case FileGlyph.TEXT: return 0xFFAFC3D9;
      default: return 0xFF8FA6BD;
    }
  }
  static int dirTint() {
    AppSettings s = AppSettings.get();
    int idx = s.folderColor;
    if (idx >= 0 && idx < Theme.ACCENTS.length) {
      int[] set = Theme.ACCENTS[idx];
      return set[s.darkTheme ? 0 : 2];
    }
    int c = s.customFolder;
    return c == 0 ? 0xFF2E7BC7 : c;
  }
  static synchronized Bitmap of(int kind, int tint) {
    boolean grad = AppSettings.get().iconGradient;
    String key = kind + ":" + tint + ":" + (grad ? "g" : "f");
    Bitmap cached = CACHE.get(key);
    if (cached != null) {
      return cached;
    }
    Bitmap b = Bitmap.createBitmap(SZ, SZ, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    draw(kind, tint, c);
    CACHE.put(key, b);
    return b;
  }
  static void clear() {
    CACHE.clear();
  }
  private static Paint fill(int tint, Shader sh) {
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setStyle(Paint.Style.FILL);
    if (sh != null) {
      p.setShader(sh);
    } else {
      p.setColor(tint);
    }
    return p;
  }
  private static Shader fade(int tint, float y0, float y1) {
    if (!AppSettings.get().iconGradient) {
      return null;
    }
    int bottom = Color.argb(26, Color.red(tint), Color.green(tint), Color.blue(tint));
    return new LinearGradient(0, y0, 0, y1, tint, bottom, Shader.TileMode.CLAMP);
  }
  private static void strokeOut(Canvas c, Path p) {
    Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    s.setStyle(Paint.Style.STROKE);
    s.setStrokeWidth(3f);
    s.setColor(0x66FFFFFF);
    c.drawPath(p, s);
  }
  private static void draw(int kind, int tint, Canvas c) {
    if (kind == FileGlyph.DIR || kind == FileGlyph.LINK) {
      drawFolder(c, tint);
      if (kind == FileGlyph.LINK) {
        drawLinkBadge(c);
      }
      return;
    }
    if (kind == FileGlyph.UP) {
      drawUp(c, tint);
      return;
    }
    drawPage(c, tint);
    Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
    g.setColor(0xE6FFFFFF);
    g.setStyle(Paint.Style.FILL);
    g.setStrokeWidth(4f);
    g.setStrokeCap(Paint.Cap.ROUND);
    g.setStrokeJoin(Paint.Join.ROUND);
    switch (kind) {
      case FileGlyph.IMG:
        c.drawCircle(62, 36, 5, g);
        Path mt = new Path();
        mt.moveTo(34, 68);
        mt.lineTo(46, 52);
        mt.lineTo(54, 61);
        mt.lineTo(60, 54);
        mt.lineTo(66, 68);
        mt.close();
        c.drawPath(mt, g);
        break;
      case FileGlyph.VIDEO:
        Path tri = new Path();
        tri.moveTo(42, 40);
        tri.lineTo(62, 52);
        tri.lineTo(42, 64);
        tri.close();
        c.drawPath(tri, g);
        break;
      case FileGlyph.AUDIO:
        g.setStyle(Paint.Style.STROKE);
        c.drawLine(56, 36, 56, 60, g);
        c.drawLine(42, 40, 56, 36, g);
        g.setStyle(Paint.Style.FILL);
        c.drawOval(new RectF(44, 58, 58, 68), g);
        break;
      case FileGlyph.ARCH:
        g.setStyle(Paint.Style.STROKE);
        g.setStrokeWidth(3f);
        c.drawRect(new RectF(38, 44, 62, 58), g);
        c.drawRect(new RectF(38, 58, 62, 72), g);
        c.drawLine(50, 36, 50, 50, g);
        c.drawLine(50, 54, 50, 68, g);
        break;
      case FileGlyph.CODE:
        g.setStyle(Paint.Style.STROKE);
        Path l = new Path();
        l.moveTo(46, 44);
        l.lineTo(36, 54);
        l.lineTo(46, 64);
        c.drawPath(l, g);
        Path r = new Path();
        r.moveTo(54, 44);
        r.lineTo(64, 54);
        r.lineTo(54, 64);
        c.drawPath(r, g);
        break;
      case FileGlyph.APK:
        c.drawRoundRect(new RectF(40, 44, 60, 64), 8, 8, g);
        g.setStyle(Paint.Style.STROKE);
        c.drawLine(44, 44, 40, 34, g);
        c.drawLine(56, 44, 60, 34, g);
        break;
      case FileGlyph.PDF:
        g.setStyle(Paint.Style.STROKE);
        g.setStrokeWidth(3f);
        c.drawLine(40, 46, 60, 46, g);
        c.drawLine(40, 54, 60, 54, g);
        c.drawLine(40, 62, 52, 62, g);
        break;
      case FileGlyph.TEXT:
        g.setStyle(Paint.Style.STROKE);
        g.setStrokeWidth(3f);
        c.drawLine(40, 42, 60, 42, g);
        c.drawLine(40, 50, 60, 50, g);
        c.drawLine(40, 58, 60, 58, g);
        c.drawLine(40, 66, 52, 66, g);
        break;
      default:
        break;
    }
  }
  private static void drawFolder(Canvas c, int tint) {
    Path p = new Path();
    p.moveTo(12, 30);
    p.lineTo(38, 30);
    p.lineTo(46, 38);
    p.lineTo(84, 38);
    p.lineTo(84, 76);
    p.lineTo(12, 76);
    p.close();
    c.drawPath(p, fill(tint, fade(tint, 30, 76)));
    strokeOut(c, p);
    Paint tab = fill(tint, null);
    tab.setColor(Color.argb(230, Color.red(tint), Color.green(tint), Color.blue(tint)));
    c.drawRoundRect(new RectF(12, 24, 44, 34), 4, 4, tab);
  }
  private static void drawLinkBadge(Canvas c) {
    Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
    b.setColor(0xF0FFFFFF);
    c.drawCircle(72, 68, 14, b);
    Paint a = new Paint(Paint.ANTI_ALIAS_FLAG);
    a.setColor(0xFF2E7BC7);
    a.setStyle(Paint.Style.STROKE);
    a.setStrokeWidth(3.5f);
    a.setStrokeCap(Paint.Cap.ROUND);
    c.drawCircle(68, 64, 5, a);
    c.drawCircle(76, 72, 5, a);
    c.drawLine(71, 67, 73, 69, a);
  }
  private static void drawUp(Canvas c, int tint) {
    Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
    g.setColor(tint);
    g.setStyle(Paint.Style.STROKE);
    g.setStrokeWidth(9f);
    g.setStrokeCap(Paint.Cap.ROUND);
    g.setStrokeJoin(Paint.Join.ROUND);
    Path p = new Path();
    p.moveTo(24, 60);
    p.lineTo(48, 34);
    p.lineTo(72, 60);
    c.drawPath(p, g);
    c.drawLine(48, 36, 48, 74, g);
  }
  private static void drawPage(Canvas c, int tint) {
    Path p = new Path();
    p.moveTo(30, 16);
    p.lineTo(58, 16);
    p.lineTo(72, 30);
    p.lineTo(72, 80);
    p.lineTo(30, 80);
    p.close();
    c.drawPath(p, fill(tint, fade(tint, 16, 80)));
    strokeOut(c, p);
    Paint fold = fill(tint, null);
    fold.setColor(Color.argb(140, Color.red(tint), Color.green(tint), Color.blue(tint)));
    Path f = new Path();
    f.moveTo(58, 16);
    f.lineTo(58, 30);
    f.lineTo(72, 30);
    f.close();
    c.drawPath(f, fold);
  }
}
