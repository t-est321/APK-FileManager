package com.dlna.FileManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import java.util.HashMap;
import java.util.Locale;
final class SvgDraw {
  private static final class Grad {
    float x1;
    float y1;
    float x2;
    float y2 = 1f;
    int c0 = 0xFFFFFFFF;
    int c1 = 0x1AFFFFFF;
  }
  private SvgDraw() {}
  static Bitmap render(String svg, int size) {
    return renderBox(svg, size, size, 0);
  }
  static Bitmap renderFit(String svg, int maxDim) {
    if (svg == null || svg.length() == 0 || maxDim <= 0) {
      return null;
    }
    int cap = 1280;
    int bg = 0xFFFFFFFF;
    try {
      AppSettings st = AppSettings.get();
      cap = st.svgRes();
      bg = st.svgBg();
    } catch (Exception ignored) {}
    if (cap < 64) {
      cap = 64;
    }
    maxDim = Math.min(maxDim, cap);
    float[] vb = viewBox(svg);
    float aw = vb[2] > 0f ? vb[2] : 24f;
    float ah = vb[3] > 0f ? vb[3] : 24f;
    int w;
    int h;
    if (aw >= ah) {
      w = maxDim;
      h = Math.max(1, Math.round(maxDim * ah / aw));
    } else {
      h = maxDim;
      w = Math.max(1, Math.round(maxDim * aw / ah));
    }
    return renderBox(svg, w, h, bg);
  }
  private static Bitmap renderBox(String svg, int w, int h, int bg) {
    if (svg == null || svg.length() == 0 || w <= 0 || h <= 0) {
      return null;
    }
    try {
      float[] vb = viewBox(svg);
      if (vb[2] <= 0f || vb[3] <= 0f) {
        return null;
      }
      boolean gradsOn = true;
      boolean rgb565 = false;
      try {
        AppSettings st = AppSettings.get();
        gradsOn = st.svgGrads;
        rgb565 = st.svgRgb565;
      } catch (Exception ignored) {}
      HashMap<String, Grad> grads = gradsOn ? parseGrads(svg) : new HashMap<String, Grad>();
      Bitmap b = Bitmap.createBitmap(w, h, rgb565 ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
      Canvas c = new Canvas(b);
      if (bg != 0) {
        c.drawColor(bg);
      }
      float sx = w / vb[2];
      float sy = h / vb[3];
      c.translate(-vb[0] * sx, -vb[1] * sy);
      c.scale(sx, sy);
      boolean drew = paintTree(c, svg, grads);
      if (!drew) {
        b.recycle();
        return null;
      }
      return b;
    } catch (Throwable t) {
      return null;
    }
  }
  private static float[] viewBox(String svg) {
    float[] vb = {0f, 0f, 24f, 24f};
    int vbAt = indexOf(svg, "viewBox");
    if (vbAt >= 0) {
      int eq = svg.indexOf('=', vbAt);
      int q1 = eq >= 0 ? nextQuote(svg, eq) : -1;
      int q2 = q1 >= 0 ? svg.indexOf(svg.charAt(q1), q1 + 1) : -1;
      if (q1 >= 0 && q2 > q1) {
        float[] n = readNums(svg.substring(q1 + 1, q2), 4);
        if (n[2] > 0f && n[3] > 0f) {
          vb[0] = n[0];
          vb[1] = n[1];
          vb[2] = n[2];
          vb[3] = n[3];
          return vb;
        }
      }
    }
    int svgAt = indexOf(svg, "<svg");
    if (svgAt >= 0) {
      int end = svg.indexOf('>', svgAt);
      if (end > svgAt) {
        String head = svg.substring(svgAt, end);
        float tw = attrFloat(head, "width", 0f);
        float th = attrFloat(head, "height", 0f);
        if (tw > 0f && th > 0f) {
          vb[2] = tw;
          vb[3] = th;
        }
      }
    }
    return vb;
  }
  private static boolean paintTree(Canvas c, String svg, HashMap<String, Grad> grads) {
    boolean drew = false;
    int i = 0;
    int n = svg.length();
    int gDepth = 0;
    while (i < n) {
      int lt = svg.indexOf('<', i);
      if (lt < 0) {
        break;
      }
      if (lt + 3 < n && svg.charAt(lt + 1) == '!' && svg.charAt(lt + 2) == '-' && svg.charAt(lt + 3) == '-') {
        int endc = svg.indexOf("--" + ">", lt + 4);
        i = endc < 0 ? n : endc + 3;
        continue;
      }
      int gt = svg.indexOf('>', lt);
      if (gt < 0) {
        break;
      }
      String tag = svg.substring(lt, gt);
      String name = tagName(tag);
      i = gt + 1;
      if (name.length() == 0) {
        continue;
      }
      if (name.charAt(0) == '/') {
        if ("g".equals(name.substring(1)) && gDepth > 0) {
          c.restore();
          gDepth--;
        }
        continue;
      }
      if (name.charAt(0) == '?' || name.charAt(0) == '!') {
        continue;
      }
      if ("defs".equals(name) || "clippath".equals(name) || "mask".equals(name)
          || "symbol".equals(name) || "style".equals(name) || "title".equals(name)
          || "desc".equals(name) || "lineargradient".equals(name) || "radialgradient".equals(name)
          || "metadata".equals(name)) {
        if (tag.endsWith("/")) {
          continue;
        }
        String close = "</" + name;
        int cl = indexOfFromIgnore(svg, close, i);
        int ni = cl < 0 ? n : svg.indexOf('>', cl) + 1;
        i = ni <= i ? n : ni;
        continue;
      }
      boolean pop = false;
      String tr = attr(tag, "transform");
      if ("g".equals(name) || (tr != null && tr.length() > 0)) {
        c.save();
        if ("g".equals(name)) {
          gDepth++;
        }
        pop = !"g".equals(name);
        if (tr != null && tr.length() > 0) {
          applyTransform(c, tr);
        }
      }
      Path path = shapePath(name, tag);
      if (path != null) {
        if (paintPath(c, path, tag, grads, name)) {
          drew = true;
        }
      }
      if (pop) {
        c.restore();
      }
    }
    return drew;
  }
  private static Path shapePath(String name, String tag) {
    if ("path".equals(name)) {
      String d = attr(tag, "d");
      return d == null ? null : parsePath(d);
    }
    if ("rect".equals(name)) {
      float x = attrFloat(tag, "x", 0f);
      float y = attrFloat(tag, "y", 0f);
      float w = attrFloat(tag, "width", 0f);
      float h = attrFloat(tag, "height", 0f);
      if (w <= 0f || h <= 0f) {
        return null;
      }
      float rx = attrFloat(tag, "rx", 0f);
      float ry = attrFloat(tag, "ry", rx);
      Path p = new Path();
      if (rx > 0f || ry > 0f) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
          p.addRoundRect(new RectF(x, y, x + w, y + h), rx, ry, Path.Direction.CW);
        } else {
          p.addRect(x, y, x + w, y + h, Path.Direction.CW);
        }
      } else {
        p.addRect(x, y, x + w, y + h, Path.Direction.CW);
      }
      return p;
    }
    if ("circle".equals(name)) {
      float cx = attrFloat(tag, "cx", 0f);
      float cy = attrFloat(tag, "cy", 0f);
      float r = attrFloat(tag, "r", 0f);
      if (r <= 0f) {
        return null;
      }
      Path p = new Path();
      p.addCircle(cx, cy, r, Path.Direction.CW);
      return p;
    }
    if ("ellipse".equals(name)) {
      float cx = attrFloat(tag, "cx", 0f);
      float cy = attrFloat(tag, "cy", 0f);
      float rx = attrFloat(tag, "rx", 0f);
      float ry = attrFloat(tag, "ry", 0f);
      if (rx <= 0f || ry <= 0f) {
        return null;
      }
      Path p = new Path();
      p.addOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW);
      return p;
    }
    if ("line".equals(name)) {
      Path p = new Path();
      p.moveTo(attrFloat(tag, "x1", 0f), attrFloat(tag, "y1", 0f));
      p.lineTo(attrFloat(tag, "x2", 0f), attrFloat(tag, "y2", 0f));
      return p;
    }
    if ("polyline".equals(name) || "polygon".equals(name)) {
      String pts = attr(tag, "points");
      if (pts == null || pts.length() == 0) {
        return null;
      }
      Path p = new Path();
      boolean first = true;
      int k = 0;
      while (k < pts.length()) {
        float[] a = readOne(pts, k);
        if ((int) a[1] == k) {
          break;
        }
        float[] b = readOne(pts, (int) a[1]);
        if ((int) b[1] == (int) a[1]) {
          break;
        }
        if (first) {
          p.moveTo(a[0], b[0]);
          first = false;
        } else {
          p.lineTo(a[0], b[0]);
        }
        k = (int) b[1];
      }
      if (first) {
        return null;
      }
      if ("polygon".equals(name)) {
        p.close();
      }
      return p;
    }
    return null;
  }
  private static boolean paintPath(Canvas c, Path path, String tag, HashMap<String, Grad> grads, String name) {
    boolean drew = false;
    float opac = attrFloat(tag, "opacity", 1f);
    float fillOp = attrFloat(tag, "fill-opacity", 1f) * opac;
    String fillV = attr(tag, "fill");
    boolean lineOnly = "line".equals(name) || "polyline".equals(name);
    if (fillV == null) {
      fillV = lineOnly ? "none" : "#000000";
    }
    Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    fill.setStyle(Paint.Style.FILL);
    if (!"none".equalsIgnoreCase(fillV)) {
      fill.setShader(null);
      Grad g = urlGrad(fillV, grads);
      if (g != null) {
        fill.setShader(new LinearGradient(g.x1, g.y1, g.x2, g.y2, g.c0, g.c1, Shader.TileMode.CLAMP));
        fill.setColor(0xFFFFFFFF);
      } else {
        fill.setColor(withAlpha(colorOf(fillV, 0xFF000000), fillOp));
      }
      c.drawPath(path, fill);
      drew = true;
    }
    String strokeV = attr(tag, "stroke");
    if (strokeV != null && !"none".equalsIgnoreCase(strokeV)) {
      Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
      stroke.setStyle(Paint.Style.STROKE);
      stroke.setStrokeCap(Paint.Cap.ROUND);
      stroke.setStrokeJoin(Paint.Join.ROUND);
      float sw = attrFloat(tag, "stroke-width", 1f);
      stroke.setStrokeWidth(sw);
      float sop = attrFloat(tag, "stroke-opacity", 1f) * opac;
      stroke.setColor(withAlpha(colorOf(strokeV, 0xFF000000), sop));
      c.drawPath(path, stroke);
      drew = true;
    }
    return drew;
  }
  private static void applyTransform(Canvas c, String tr) {
    int i = 0;
    int n = tr.length();
    while (i < n) {
      while (i < n) {
        char ch = tr.charAt(i);
        if (ch == ' ' || ch == ',' || ch == '\n' || ch == '\t') {
          i++;
        } else {
          break;
        }
      }
      int p = tr.indexOf('(', i);
      if (p < 0) {
        break;
      }
      String fn = tr.substring(i, p).trim().toLowerCase(Locale.US);
      int q = tr.indexOf(')', p);
      if (q < 0) {
        break;
      }
      float[] v = readNums(tr.substring(p + 1, q), 6);
      if ("translate".equals(fn)) {
        c.translate(v[0], v[1]);
      } else if ("scale".equals(fn)) {
        int cn = countNums(tr.substring(p + 1, q));
        c.scale(v[0], cn < 2 ? v[0] : v[1]);
      } else if ("rotate".equals(fn)) {
        if (countNums(tr.substring(p + 1, q)) >= 3) {
          c.rotate(v[0], v[1], v[2]);
        } else {
          c.rotate(v[0]);
        }
      } else if ("matrix".equals(fn)) {
        Matrix m = new Matrix();
        m.setValues(new float[]{v[0], v[2], v[4], v[1], v[3], v[5], 0f, 0f, 1f});
        c.concat(m);
      }
      i = q + 1;
    }
  }
  private static int countNums(String s) {
    int k = 0;
    int i = 0;
    while (i < s.length()) {
      float[] n = readOne(s, i);
      if ((int) n[1] == i) {
        break;
      }
      k++;
      i = (int) n[1];
    }
    return k;
  }
  private static String tagName(String tag) {
    int i = 1;
    int n = tag.length();
    while (i < n && (tag.charAt(i) == ' ' || tag.charAt(i) == '\n' || tag.charAt(i) == '\t')) {
      i++;
    }
    int a = i;
    while (i < n) {
      char ch = tag.charAt(i);
      if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '/' || ch == '>') {
        break;
      }
      i++;
    }
    if (i <= a) {
      return "";
    }
    return tag.substring(a, i).toLowerCase(Locale.US);
  }
  private static HashMap<String, Grad> parseGrads(String svg) {
    HashMap<String, Grad> out = new HashMap<String, Grad>();
    int from = 0;
    String low = svg.toLowerCase(Locale.US);
    while (true) {
      int p = low.indexOf("<lineargradient", from);
      if (p < 0) {
        break;
      }
      int endTag = svg.indexOf('>', p);
      if (endTag < 0) {
        break;
      }
      int close = low.indexOf("</lineargradient", endTag);
      if (close < 0) {
        close = endTag;
      }
      String head = svg.substring(p, endTag);
      String body = svg.substring(endTag + 1, close);
      Grad g = new Grad();
      String id = attr(head, "id");
      g.x1 = attrFloat(head, "x1", 0f);
      g.y1 = attrFloat(head, "y1", 0f);
      g.x2 = attrFloat(head, "x2", 0f);
      g.y2 = attrFloat(head, "y2", 1f);
      int stops = 0;
      int sp = 0;
      String bodyLow = body.toLowerCase(Locale.US);
      while (true) {
        int st = bodyLow.indexOf("<stop", sp);
        if (st < 0) {
          break;
        }
        int se = body.indexOf('>', st);
        if (se < 0) {
          break;
        }
        String stop = body.substring(st, se);
        String col = attr(stop, "stop-color");
        float op = attrFloat(stop, "stop-opacity", 1f);
        int argb = withAlpha(colorOf(col == null ? "#ffffff" : col, 0xFFFFFFFF), op);
        if (stops == 0) {
          g.c0 = argb;
        }
        g.c1 = argb;
        stops++;
        sp = se + 1;
      }
      if (id != null && id.length() > 0) {
        out.put(id, g);
      }
      from = close + 1;
    }
    return out;
  }
  private static Grad urlGrad(String fill, HashMap<String, Grad> grads) {
    if (fill == null) {
      return null;
    }
    String v = fill.trim();
    int a = v.indexOf("url(#");
    if (a < 0) {
      return null;
    }
    int b = v.indexOf(')', a);
    if (b < 0) {
      return null;
    }
    return grads.get(v.substring(a + 5, b).trim());
  }
  private static String quotedAttr(String tag, String key) {
    int at = indexOf(tag, key);
    if (at < 0) {
      return null;
    }
    int q1 = nextQuote(tag, at);
    int q2 = q1 >= 0 ? tag.indexOf(tag.charAt(q1), q1 + 1) : -1;
    if (q1 < 0 || q2 <= q1) {
      return null;
    }
    return tag.substring(q1 + 1, q2).trim();
  }
  private static String attr(String tag, String key) {
    String v = quotedAttr(tag, key);
    if (v != null) {
      return v;
    }
    if ("style".equals(key)) {
      return null;
    }
    String st = quotedAttr(tag, "style");
    if (st == null || st.length() == 0) {
      return null;
    }
    String[] parts = st.split(";");
    for (int i = 0; i < parts.length; i++) {
      String p = parts[i].trim();
      int c = p.indexOf(':');
      if (c <= 0) {
        continue;
      }
      if (p.substring(0, c).trim().equalsIgnoreCase(key)) {
        return p.substring(c + 1).trim();
      }
    }
    return null;
  }
  private static float attrFloat(String tag, String key, float def) {
    String v = attr(tag, key);
    if (v == null || v.length() == 0) {
      return def;
    }
    if (v.endsWith("px") || v.endsWith("PX")) {
      v = v.substring(0, v.length() - 2).trim();
    }
    if (v.endsWith("%")) {
      try {
        return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
      } catch (Exception e) {
        return def;
      }
    }
    try {
      return Float.parseFloat(v);
    } catch (Exception e) {
      return def;
    }
  }
  private static int withAlpha(int color, float opac) {
    if (opac >= 0.999f) {
      return color;
    }
    if (opac <= 0f) {
      return color & 0x00FFFFFF;
    }
    int a = Color.alpha(color);
    int na = Math.max(0, Math.min(255, Math.round(a * opac)));
    return (na << 24) | (color & 0x00FFFFFF);
  }
  private static int colorOf(String v, int def) {
    if (v == null) {
      return def;
    }
    String s = v.trim();
    if ("none".equalsIgnoreCase(s)) {
      return 0;
    }
    if (s.length() > 0 && s.charAt(0) == '#') {
      try {
        String h = s.substring(1);
        if (h.length() == 3) {
          int r = Integer.parseInt(h.substring(0, 1), 16);
          int g = Integer.parseInt(h.substring(1, 2), 16);
          int b = Integer.parseInt(h.substring(2, 3), 16);
          return 0xFF000000 | (r * 17) << 16 | (g * 17) << 8 | (b * 17);
        }
        if (h.length() == 6) {
          return 0xFF000000 | Integer.parseInt(h, 16);
        }
        if (h.length() == 8) {
          return (int) Long.parseLong(h, 16);
        }
      } catch (Exception ignored) {}
    }
    if (s.toLowerCase(Locale.US).startsWith("rgb")) {
      int a = s.indexOf('(');
      int b = s.indexOf(')');
      if (a >= 0 && b > a) {
        float[] n = readNums(s.substring(a + 1, b), 4);
        int r = clamp255(n[0]);
        int g = clamp255(n[1]);
        int bl = clamp255(n[2]);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
      }
    }
    String low = s.toLowerCase(Locale.US);
    if ("black".equals(low)) {
      return 0xFF000000;
    }
    if ("white".equals(low)) {
      return 0xFFFFFFFF;
    }
    if ("red".equals(low)) {
      return 0xFFFF0000;
    }
    if ("green".equals(low)) {
      return 0xFF008000;
    }
    if ("blue".equals(low)) {
      return 0xFF0000FF;
    }
    if ("yellow".equals(low)) {
      return 0xFFFFFF00;
    }
    if ("gray".equals(low) || "grey".equals(low)) {
      return 0xFF808080;
    }
    if ("orange".equals(low)) {
      return 0xFFFFA500;
    }
    if ("purple".equals(low)) {
      return 0xFF800080;
    }
    if ("navy".equals(low)) {
      return 0xFF000080;
    }
    if ("teal".equals(low)) {
      return 0xFF008080;
    }
    if ("aqua".equals(low) || "cyan".equals(low)) {
      return 0xFF00FFFF;
    }
    if ("lime".equals(low)) {
      return 0xFF00FF00;
    }
    if ("maroon".equals(low)) {
      return 0xFF800000;
    }
    if ("silver".equals(low)) {
      return 0xFFC0C0C0;
    }
    if ("fuchsia".equals(low) || "magenta".equals(low)) {
      return 0xFFFF00FF;
    }
    return def;
  }
  private static int clamp255(float v) {
    int n = Math.round(v);
    if (n < 0) {
      return 0;
    }
    if (n > 255) {
      return 255;
    }
    return n;
  }
  private static Path parsePath(String d) {
    Path path = new Path();
    int i = 0;
    int n = d.length();
    char cmd = 0;
    float cx = 0f;
    float cy = 0f;
    float sx = 0f;
    float sy = 0f;
    float c2x = 0f;
    float c2y = 0f;
    boolean has = false;
    while (i < n) {
      char ch = d.charAt(i);
      if (ch == ',' || ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
        i++;
        continue;
      }
      if (isCmd(ch)) {
        cmd = ch;
        i++;
        continue;
      }
      if (cmd == 0) {
        i++;
        continue;
      }
      int at = i;
      boolean rel = cmd >= 'a' && cmd <= 'z';
      char op = rel ? (char) (cmd - 32) : cmd;
      if (op == 'Z') {
        path.close();
        cx = sx;
        cy = sy;
        cmd = 0;
        continue;
      }
      if (op == 'M' || op == 'L' || op == 'T') {
        float[] xy = readPair(d, i);
        i = (int) xy[2];
        if (i <= at) {
          i = at + 1;
        }
        float x = rel ? cx + xy[0] : xy[0];
        float y = rel ? cy + xy[1] : xy[1];
        if (op == 'M') {
          path.moveTo(x, y);
          sx = x;
          sy = y;
          cmd = rel ? 'l' : 'L';
        } else {
          path.lineTo(x, y);
        }
        cx = x;
        cy = y;
        has = true;
      } else if (op == 'H') {
        float[] v = readOne(d, i);
        i = (int) v[1];
        cx = rel ? cx + v[0] : v[0];
        path.lineTo(cx, cy);
        has = true;
      } else if (op == 'V') {
        float[] v = readOne(d, i);
        i = (int) v[1];
        cy = rel ? cy + v[0] : v[0];
        path.lineTo(cx, cy);
        has = true;
      } else if (op == 'C') {
        float[] a = readPair(d, i);
        i = (int) a[2];
        float[] b = readPair(d, i);
        i = (int) b[2];
        float[] e = readPair(d, i);
        i = (int) e[2];
        float x1 = rel ? cx + a[0] : a[0];
        float y1 = rel ? cy + a[1] : a[1];
        float x2 = rel ? cx + b[0] : b[0];
        float y2 = rel ? cy + b[1] : b[1];
        float x = rel ? cx + e[0] : e[0];
        float y = rel ? cy + e[1] : e[1];
        path.cubicTo(x1, y1, x2, y2, x, y);
        c2x = x2;
        c2y = y2;
        cx = x;
        cy = y;
        has = true;
      } else if (op == 'Q') {
        float[] a = readPair(d, i);
        i = (int) a[2];
        float[] e = readPair(d, i);
        i = (int) e[2];
        float x1 = rel ? cx + a[0] : a[0];
        float y1 = rel ? cy + a[1] : a[1];
        float x = rel ? cx + e[0] : e[0];
        float y = rel ? cy + e[1] : e[1];
        path.quadTo(x1, y1, x, y);
        cx = x;
        cy = y;
        has = true;
      } else if (op == 'S') {
        float[] b = readPair(d, i);
        i = (int) b[2];
        float[] e = readPair(d, i);
        i = (int) e[2];
        float x1 = cx * 2f - c2x;
        float y1 = cy * 2f - c2y;
        float x2 = rel ? cx + b[0] : b[0];
        float y2 = rel ? cy + b[1] : b[1];
        float x = rel ? cx + e[0] : e[0];
        float y = rel ? cy + e[1] : e[1];
        path.cubicTo(x1, y1, x2, y2, x, y);
        c2x = x2;
        c2y = y2;
        cx = x;
        cy = y;
        has = true;
      } else if (op == 'A') {
        float[] rx = readOne(d, i);
        i = (int) rx[1];
        float[] ry = readOne(d, i);
        i = (int) ry[1];
        float[] rot = readOne(d, i);
        i = (int) rot[1];
        float[] laf = readOne(d, i);
        i = (int) laf[1];
        float[] swf = readOne(d, i);
        i = (int) swf[1];
        float[] e = readPair(d, i);
        i = (int) e[2];
        float x = rel ? cx + e[0] : e[0];
        float y = rel ? cy + e[1] : e[1];
        path.lineTo(x, y);
        cx = x;
        cy = y;
        has = true;
      } else {
        i++;
      }
      if (i <= at) {
        i = at + 1;
      }
    }
    return has ? path : null;
  }
  private static boolean isCmd(char c) {
    return "MmLlHhVvCcQqSsTtAaZz".indexOf(c) >= 0;
  }
  private static float[] readPair(String s, int i) {
    float[] a = readOne(s, i);
    float[] b = readOne(s, (int) a[1]);
    return new float[]{a[0], b[0], b[1]};
  }
  private static float[] readOne(String s, int i) {
    int n = s.length();
    while (i < n) {
      char c = s.charAt(i);
      if (c == ',' || c == ' ' || c == '\n' || c == '\r' || c == '\t') {
        i++;
        continue;
      }
      break;
    }
    int start = i;
    if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
      i++;
    }
    boolean dot = false;
    boolean exp = false;
    while (i < n) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') {
        i++;
      } else if (c == '.' && !dot) {
        dot = true;
        i++;
      } else if ((c == 'e' || c == 'E') && !exp) {
        exp = true;
        i++;
        if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
          i++;
        }
      } else {
        break;
      }
    }
    float v = 0f;
    if (i > start) {
      try {
        v = Float.parseFloat(s.substring(start, i));
      } catch (Exception ignored) {}
    }
    return new float[]{v, i};
  }
  private static float[] readNums(String s, int max) {
    float[] out = new float[max];
    int i = 0;
    int k = 0;
    while (k < max && i < s.length()) {
      float[] n = readOne(s, i);
      if ((int) n[1] == i) {
        break;
      }
      out[k++] = n[0];
      i = (int) n[1];
    }
    return out;
  }
  private static int nextQuote(String s, int from) {
    int a = s.indexOf('"', from);
    int b = s.indexOf('\'', from);
    if (a < 0) {
      return b;
    }
    if (b < 0) {
      return a;
    }
    return a < b ? a : b;
  }
  private static int indexOf(String s, String k) {
    return s.toLowerCase(Locale.US).indexOf(k.toLowerCase(Locale.US));
  }
  private static int indexOfFromIgnore(String s, String k, int from) {
    return s.toLowerCase(Locale.US).indexOf(k.toLowerCase(Locale.US), from);
  }
}
