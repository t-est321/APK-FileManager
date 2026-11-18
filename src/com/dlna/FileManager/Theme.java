package com.dlna.FileManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
final class Theme {
  static final int[][] ACCENTS = {
    {0xFF6CB6FF, 0xFF173047, 0xFF2E7BC7, 0xFFD8E9FB},
    {0xFF7BE09A, 0xFF163828, 0xFF3FA45B, 0xFFDDF1E3},
    {0xFFF4C96A, 0xFF3F3010, 0xFFB8891A, 0xFFF7EAD0},
    {0xFFD1A3F5, 0xFF322044, 0xFF8E5BC7, 0xFFEBE0F7},
    {0xFFF47A7A, 0xFF3E1C1C, 0xFFD95353, 0xFFF9DCDC}
  };
  static int BG = 0xFF000000;
  static int SURFACE = 0xFF141C26;
  static int SURFACE_HI = 0xFF243140;
  static int TEXT = 0xFFF3F7FB;
  static int TEXT_DIM = 0xFF8FA3B6;
  static int ACCENT = ACCENTS[0][0];
  static int ACCENT_SOFT = ACCENTS[0][1];
  static int STROKE = 0xFF173047;
  static int SEL = 0xFF173047;
  static final int DANGER = 0xFFF47A7A;
  static final int GOOD = 0xFF7BE09A;
  private Theme() {}
  static void applyAccent(int idx, boolean dark) {
    boolean preset = idx >= 0 && idx < ACCENTS.length;
    if (!preset && idx != ACCENTS.length) {
      idx = 0;
      preset = true;
    }
    int[] set = ACCENTS[preset ? idx : 0];
    int custom = 0xFF6CB6FF;
    try {
      custom = AppSettings.get().customAccent;
    } catch (Exception ignored) {}
    if (custom == 0) {
      custom = 0xFF6CB6FF;
    }
    if (dark) {
      BG = 0xFF000000;
      SURFACE = 0xFF141C26;
      SURFACE_HI = 0xFF243140;
      TEXT = 0xFFF3F7FB;
      TEXT_DIM = 0xFF8FA3B6;
      if (preset) {
        ACCENT = set[0];
        ACCENT_SOFT = set[1];
      } else {
        ACCENT = custom;
        ACCENT_SOFT = mix(custom, 0xFF000000, 45);
      }
    } else {
      BG = 0xFFF4F6F9;
      SURFACE = 0xFFFFFFFF;
      SURFACE_HI = 0xFFE7EDF4;
      TEXT = 0xFF121A22;
      TEXT_DIM = 0xFF5A6C7E;
      if (preset) {
        ACCENT = set[2];
        ACCENT_SOFT = set[3];
      } else {
        ACCENT = custom;
        ACCENT_SOFT = mix(custom, 0xFFFFFFFF, 40);
      }
    }
    SEL = ACCENT_SOFT;
    STROKE = ACCENT_SOFT;
    try {
      AppSettings st = AppSettings.get();
      if (st.useCustomChrome) {
        if (dark) {
          paintChrome(st.customBg, st.customText, st.customSurface, st.customStroke, st.customSel);
        } else {
          paintChrome(st.customBgL, st.customTextL, st.customSurfaceL, st.customStrokeL, st.customSelL);
        }
      }
    } catch (Exception ignored) {}
  }
  private static int opaque(int c) {
    if (c == 0) {
      return 0;
    }
    return 0xFF000000 | (c & 0xFFFFFF);
  }
  private static void paintChrome(int bg, int text, int surface, int stroke, int sel) {
    bg = opaque(bg);
    text = opaque(text);
    surface = opaque(surface);
    stroke = opaque(stroke);
    sel = opaque(sel);
    if (bg != 0) {
      BG = bg;
    }
    if (text != 0) {
      TEXT = text;
      TEXT_DIM = mix(text, BG, 42);
    }
    if (surface != 0) {
      SURFACE = surface;
      SURFACE_HI = mix(surface, TEXT, 14);
    }
    if (stroke != 0) {
      STROKE = stroke;
    }
    if (sel != 0) {
      SEL = sel;
      ACCENT_SOFT = sel;
    }
  }
  static int mix(int a, int b, int pctB) {
    int ar = (a >> 16) & 255;
    int ag = (a >> 8) & 255;
    int ab = a & 255;
    int br = (b >> 16) & 255;
    int bg = (b >> 8) & 255;
    int bb = b & 255;
    int r = ar + (br - ar) * pctB / 100;
    int g = ag + (bg - ag) * pctB / 100;
    int bl = ab + (bb - ab) * pctB / 100;
    return 0xFF000000 | (r << 16) | (g << 8) | bl;
  }
  static int parseHex(String t, int fallback) {
    if (t == null) {
      return fallback;
    }
    t = t.trim();
    if (t.length() > 0 && t.charAt(0) == '#') {
      t = t.substring(1);
    }
    try {
      if (t.length() == 6) {
        return 0xFF000000 | Integer.parseInt(t, 16);
      }
      if (t.length() == 8) {
        return (int) Long.parseLong(t, 16);
      }
    } catch (Exception ignored) {}
    return fallback;
  }
  static String toHex(int c) {
    int v = c & 0xFFFFFF;
    String h = Integer.toHexString(v).toUpperCase();
    while (h.length() < 6) {
      h = "0" + h;
    }
    return h;
  }
  static int dp(Context c, int v) {
    return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
  }
  static boolean compact(Context c) {
    return c.getResources().getDisplayMetrics().widthPixels < dp(c, 400);
  }
  static float corner(Context c, int rDp) {
    if (AppSettings.get().optNoRound) {
      return 0f;
    }
    return dp(c, rDp);
  }
  static GradientDrawable rounded(Context c, int color, int rDp) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(corner(c, rDp));
    return g;
  }
  static GradientDrawable outline(Context c, int color, int strokeColor, int rDp) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(corner(c, rDp));
    g.setStroke(dp(c, 1), strokeColor);
    return g;
  }
}
