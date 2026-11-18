package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
final class MdDraw {
  private MdDraw() {}
  static boolean isMarkdownName(String name) {
    String e = FileOps.ext(name);
    return "md".equals(e) || "markdown".equals(e);
  }
  static void fill(Context c, LinearLayout box, String src, File dir) {
    if (src == null) {
      src = "";
    }
    src = src.replace("\r\n", "\n").replace('\r', '\n');
    String[] lines = splitKeep(src);
    int i = 0;
    while (i < lines.length) {
      String line = lines[i];
      if (isBlank(line)) {
        i++;
        continue;
      }
      if (isFence(line)) {
        i = addFence(c, box, lines, i);
        continue;
      }
      if (isHr(line)) {
        addHr(c, box);
        i++;
        continue;
      }
      int atx = headingAtx(line);
      if (atx > 0) {
        addHeading(c, box, atx, stripHeading(line));
        i++;
        continue;
      }
      if (i + 1 < lines.length && headingSetext(lines[i + 1]) > 0 && !isBlank(line)) {
        addHeading(c, box, headingSetext(lines[i + 1]), line.trim());
        i += 2;
        continue;
      }
      if (isQuote(line)) {
        i = addQuote(c, box, lines, i);
        continue;
      }
      if (listMarker(line) >= 0) {
        i = addList(c, box, lines, i);
        continue;
      }
      if (isTableRow(line) && i + 1 < lines.length && isTableSep(lines[i + 1])) {
        i = addTable(c, box, lines, i);
        continue;
      }
      if (isIndentedCode(line)) {
        i = addIndented(c, box, lines, i);
        continue;
      }
      i = addPara(c, box, lines, i, dir);
    }
  }
  static String[] splitKeep(String src) {
    ArrayList<String> out = new ArrayList<String>();
    int n = src.length();
    int s = 0;
    int i;
    for (i = 0; i < n; i++) {
      if (src.charAt(i) == '\n') {
        out.add(src.substring(s, i));
        s = i + 1;
      }
    }
    out.add(src.substring(s));
    return out.toArray(new String[out.size()]);
  }
  static boolean isBlank(String s) {
    return s == null || s.trim().length() == 0;
  }
  static String trimRight(String s) {
    int i = s.length();
    while (i > 0 && s.charAt(i - 1) <= ' ') {
      i--;
    }
    return s.substring(0, i);
  }
  static int countLead(String s, char ch) {
    int i = 0;
    while (i < s.length() && s.charAt(i) == ch) {
      i++;
    }
    return i;
  }
  static int indentWidth(String s) {
    int i = 0;
    int w = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == ' ') {
        w++;
      } else if (c == '\t') {
        w += 4;
      } else {
        break;
      }
      i++;
    }
    return w;
  }
  static String ltrim(String s) {
    int i = 0;
    while (i < s.length() && s.charAt(i) <= ' ') {
      i++;
    }
    return s.substring(i);
  }
  static int headingAtx(String line) {
    String t = ltrim(line);
    int n = countLead(t, '#');
    if (n < 1 || n > 6) {
      return 0;
    }
    if (n == t.length() || t.charAt(n) == ' ' || t.charAt(n) == '\t') {
      return n;
    }
    return 0;
  }
  static String stripHeading(String line) {
    String t = ltrim(line);
    int n = countLead(t, '#');
    t = ltrim(t.substring(n));
    int e = t.length();
    while (e > 0 && t.charAt(e - 1) == '#') {
      e--;
    }
    return ltrim(trimRight(t.substring(0, e)));
  }
  static int headingSetext(String line) {
    String t = line.trim();
    if (t.length() < 1) {
      return 0;
    }
    char ch = t.charAt(0);
    if (ch != '=' && ch != '-') {
      return 0;
    }
    int i;
    for (i = 0; i < t.length(); i++) {
      if (t.charAt(i) != ch) {
        return 0;
      }
    }
    return ch == '=' ? 1 : 2;
  }
  static boolean isFence(String line) {
    String t = ltrim(line);
    return fenceMark(t) >= 3;
  }
  static int fenceMark(String t) {
    if (t.startsWith("```")) {
      return countLead(t, '`');
    }
    if (t.startsWith("~~~")) {
      return countLead(t, '~');
    }
    return 0;
  }
  static boolean isHr(String line) {
    String t = line.trim();
    if (t.length() < 3) {
      return false;
    }
    char ch = t.charAt(0);
    if (ch != '-' && ch != '*' && ch != '_') {
      return false;
    }
    int n = 0;
    int i;
    for (i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      if (c == ch) {
        n++;
      } else if (c != ' ' && c != '\t') {
        return false;
      }
    }
    return n >= 3;
  }
  static boolean isQuote(String line) {
    return ltrim(line).startsWith(">");
  }
  static String quoteText(String line) {
    String t = ltrim(line);
    if (t.startsWith(">")) {
      t = t.substring(1);
      if (t.startsWith(" ")) {
        t = t.substring(1);
      }
    }
    return t;
  }
  static int listMarker(String line) {
    String t = ltrim(line);
    if (t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ")) {
      return 2;
    }
    int i = 0;
    while (i < t.length() && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
      i++;
    }
    if (i > 0 && i < t.length() && (t.charAt(i) == '.' || t.charAt(i) == ')') && i + 1 < t.length()
        && t.charAt(i + 1) == ' ') {
      return i + 2;
    }
    return -1;
  }
  static boolean listOrdered(String line) {
    String t = ltrim(line);
    return t.length() > 0 && t.charAt(0) >= '0' && t.charAt(0) <= '9';
  }
  static int taskState(String line) {
    String t = ltrim(line);
    int m = listMarker(line);
    if (m < 0) {
      return -1;
    }
    t = ltrim(t.substring(m));
    if (t.startsWith("[ ]")) {
      return 0;
    }
    if (t.startsWith("[x]") || t.startsWith("[X]")) {
      return 1;
    }
    return -1;
  }
  static String listBody(String line) {
    String t = ltrim(line);
    int m = listMarker(line);
    if (m < 0) {
      return t;
    }
    t = ltrim(t.substring(m));
    if (t.startsWith("[ ]") || t.startsWith("[x]") || t.startsWith("[X]")) {
      if (t.length() > 3) {
        t = ltrim(t.substring(3));
      } else {
        t = "";
      }
    }
    return t;
  }
  static boolean isTableRow(String line) {
    String t = line.trim();
    return t.indexOf('|') >= 0 && !isHr(line);
  }
  static boolean isTableSep(String line) {
    String t = line.trim();
    if (t.length() < 3) {
      return false;
    }
    int i;
    int dash = 0;
    for (i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      if (c == '-') {
        dash++;
      } else if (c != '|' && c != ':' && c != ' ' && c != '\t') {
        return false;
      }
    }
    return dash >= 3;
  }
  static String[] splitPipe(String line) {
    String t = line.trim();
    if (t.startsWith("|")) {
      t = t.substring(1);
    }
    if (t.endsWith("|")) {
      t = t.substring(0, t.length() - 1);
    }
    ArrayList<String> cells = new ArrayList<String>();
    StringBuilder cur = new StringBuilder();
    int i;
    boolean esc = false;
    for (i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      if (esc) {
        cur.append(c);
        esc = false;
        continue;
      }
      if (c == '\\') {
        esc = true;
        continue;
      }
      if (c == '|') {
        cells.add(cur.toString().trim());
        cur.setLength(0);
      } else {
        cur.append(c);
      }
    }
    cells.add(cur.toString().trim());
    return cells.toArray(new String[cells.size()]);
  }
  static boolean isIndentedCode(String line) {
    return indentWidth(line) >= 4 && !isBlank(line);
  }
  static boolean looksLikeHtml(String line) {
    String t = ltrim(line);
    return t.startsWith("<") && t.indexOf('>') > 1;
  }
  static String stripHtml(String s) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < s.length()) {
      int lt = s.indexOf('<', i);
      if (lt < 0) {
        out.append(s.substring(i));
        break;
      }
      out.append(s.substring(i, lt));
      int gt = s.indexOf('>', lt);
      if (gt < 0) {
        break;
      }
      i = gt + 1;
    }
    return out.toString();
  }
  static String unescape(String s) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        out.append(s.charAt(i + 1));
        i += 2;
        continue;
      }
      if (c == '&') {
        int semi = s.indexOf(';', i + 1);
        if (semi > i && semi - i < 10) {
          out.append(entity(s.substring(i, semi + 1)));
          i = semi + 1;
          continue;
        }
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }
  static String entity(String e) {
    if ("&amp;".equals(e)) {
      return "&";
    }
    if ("&lt;".equals(e)) {
      return "<";
    }
    if ("&gt;".equals(e)) {
      return ">";
    }
    if ("&quot;".equals(e)) {
      return "\"";
    }
    if ("&nbsp;".equals(e)) {
      return " ";
    }
    if (e.startsWith("&#x") || e.startsWith("&#X")) {
      try {
        int v = Integer.parseInt(e.substring(3, e.length() - 1), 16);
        return String.valueOf((char) v);
      } catch (Exception ignored) {}
    } else if (e.startsWith("&#")) {
      try {
        int v = Integer.parseInt(e.substring(2, e.length() - 1));
        return String.valueOf((char) v);
      } catch (Exception ignored) {}
    }
    return e;
  }
  static int findClose(String s, int from, String open, String close) {
    int i = from;
    int depth = 1;
    while (i < s.length()) {
      if (s.charAt(i) == '\\' && i + 1 < s.length()) {
        i += 2;
        continue;
      }
      if (s.startsWith(close, i)) {
        depth--;
        if (depth == 0) {
          return i;
        }
        i += close.length();
        continue;
      }
      if (s.startsWith(open, i)) {
        depth++;
        i += open.length();
        continue;
      }
      i++;
    }
    return -1;
  }
  static SpannableStringBuilder inline(String raw) {
    SpannableStringBuilder sb = new SpannableStringBuilder();
    if (raw == null) {
      return sb;
    }
    applyInline(sb, raw, 0, raw.length());
    return sb;
  }
  static void applyInline(SpannableStringBuilder sb, String s, int a, int b) {
    int i = a;
    while (i < b) {
      if (s.charAt(i) == '\\' && i + 1 < b) {
        sb.append(s.charAt(i + 1));
        i += 2;
        continue;
      }
      if (s.charAt(i) == '`') {
        int j = s.indexOf('`', i + 1);
        if (j > i && j < b) {
          int start = sb.length();
          sb.append(unescape(s.substring(i + 1, j)));
          sb.setSpan(new TypefaceSpan("monospace"), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          sb.setSpan(new ForegroundColorSpan(Theme.ACCENT), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          i = j + 1;
          continue;
        }
      }
      if (s.startsWith("~~", i) && i + 2 < b) {
        int j = s.indexOf("~~", i + 2);
        if (j > i && j < b) {
          int start = sb.length();
          applyInline(sb, s, i + 2, j);
          sb.setSpan(new StrikethroughSpan(), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          i = j + 2;
          continue;
        }
      }
      if (s.startsWith("**", i) || s.startsWith("__", i)) {
        String mk = s.substring(i, i + 2);
        int j = findClose(s, i + 2, mk, mk);
        if (j > i && j < b) {
          int start = sb.length();
          applyInline(sb, s, i + 2, j);
          sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          i = j + 2;
          continue;
        }
      }
      if (s.charAt(i) == '*' || s.charAt(i) == '_') {
        String mk = s.substring(i, i + 1);
        int j = findClose(s, i + 1, mk, mk);
        if (j > i && j < b) {
          int start = sb.length();
          applyInline(sb, s, i + 1, j);
          sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          i = j + 1;
          continue;
        }
      }
      if (s.startsWith("![", i)) {
        int br = s.indexOf(']', i + 2);
        if (br > i && br + 1 < b && s.charAt(br + 1) == '(') {
          int par = s.indexOf(')', br + 2);
          if (par > br && par < b) {
            int start = sb.length();
            sb.append(unescape(s.substring(i + 2, br)));
            sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = par + 1;
            continue;
          }
        }
      }
      if (s.charAt(i) == '[') {
        int br = s.indexOf(']', i + 1);
        if (br > i && br + 1 < b && s.charAt(br + 1) == '(') {
          int par = s.indexOf(')', br + 2);
          if (par > br && par < b) {
            String href = s.substring(br + 2, par).trim();
            int sp = href.indexOf(' ');
            if (sp > 0) {
              href = href.substring(0, sp);
            }
            int start = sb.length();
            applyInline(sb, s, i + 1, br);
            sb.setSpan(new URLSpan(href), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(Theme.ACCENT), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new UnderlineSpan(), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = par + 1;
            continue;
          }
        }
      }
      if (s.startsWith("http://", i) || s.startsWith("https://", i)) {
        int j = i;
        while (j < b && !isUrlEnd(s.charAt(j))) {
          j++;
        }
        String url = s.substring(i, j);
        int start = sb.length();
        sb.append(url);
        sb.setSpan(new URLSpan(url), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(Theme.ACCENT), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        i = j;
        continue;
      }
      if (s.charAt(i) == '`') {
        sb.append(s.charAt(i));
        i++;
        continue;
      }
      int amp = s.indexOf('&', i);
      int cut = b;
      int special = nextSpecial(s, i, b);
      if (special >= 0 && special < cut) {
        cut = special;
      }
      if (amp >= i && amp < cut) {
        int semi = s.indexOf(';', amp + 1);
        if (semi > amp && semi < cut && semi - amp < 10) {
          sb.append(unescape(s.substring(i, semi + 1)));
          i = semi + 1;
          continue;
        }
      }
      if (cut > i) {
        sb.append(unescape(s.substring(i, cut)));
        i = cut;
      } else {
        sb.append(s.charAt(i));
        i++;
      }
    }
  }
  static boolean isUrlEnd(char c) {
    return c <= ' ' || c == ')' || c == ']' || c == '<' || c == '>' || c == '"' || c == '\'';
  }
  static int nextSpecial(String s, int i, int b) {
    int best = -1;
    int[] marks = new int[] {
      s.indexOf('`', i), s.indexOf('*', i), s.indexOf('_', i), s.indexOf('[', i),
      s.indexOf('~', i), s.indexOf('\\', i), s.indexOf('!', i)
    };
    int k;
    for (k = 0; k < marks.length; k++) {
      int v = marks[k];
      if (v >= i && v < b && (best < 0 || v < best)) {
        best = v;
      }
    }
    int http = s.indexOf("http", i);
    if (http >= i && http < b && (best < 0 || http < best)) {
      best = http;
    }
    return best;
  }
  static float styleH(int level) {
    if (level <= 1) {
      return 1.55f;
    }
    if (level == 2) {
      return 1.35f;
    }
    if (level == 3) {
      return 1.2f;
    }
    if (level == 4) {
      return 1.1f;
    }
    return 1.0f;
  }
  static TextView tv(Context c, CharSequence text, float size, boolean bold) {
    TextView t = new TextView(c);
    t.setTextColor(Theme.TEXT);
    t.setTextSize(size);
    t.setText(text);
    if (bold) {
      t.setTypeface(Typeface.DEFAULT_BOLD);
    }
    t.setMovementMethod(LinkMovementMethod.getInstance());
    t.setLinkTextColor(Theme.ACCENT);
    int p = Theme.dp(c, 4);
    t.setPadding(0, p, 0, p);
    return t;
  }
  static void pad(View v, Context c, int l, int t, int r, int b) {
    v.setPadding(Theme.dp(c, l), Theme.dp(c, t), Theme.dp(c, r), Theme.dp(c, b));
  }
  static TextView codeTv(Context c, String body) {
    TextView t = new TextView(c);
    t.setTypeface(Typeface.MONOSPACE);
    t.setTextColor(Theme.TEXT);
    t.setTextSize(12);
    t.setText(body);
    t.setBackground(Theme.rounded(c, Theme.SURFACE, 8));
    int p = Theme.dp(c, 8);
    t.setPadding(p, p, p, p);
    return t;
  }
  static void addHr(Context c, LinearLayout box) {
    View v = new View(c);
    v.setBackgroundColor(Theme.SURFACE_HI);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(c, 1));
    lp.topMargin = Theme.dp(c, 8);
    lp.bottomMargin = Theme.dp(c, 8);
    box.addView(v, lp);
  }
  static void addHeading(Context c, LinearLayout box, int level, String text) {
    SpannableStringBuilder sb = inline(text);
    int start = 0;
    sb.setSpan(new RelativeSizeSpan(styleH(level)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    TextView t = tv(c, sb, 16, false);
    pad(t, c, 0, 8, 0, 4);
    box.addView(t);
  }
  static int addFence(Context c, LinearLayout box, String[] lines, int i) {
    String open = lines[i];
    int mark = fenceMark(ltrim(open));
    char ch = ltrim(open).charAt(0);
    i++;
    StringBuilder body = new StringBuilder();
    while (i < lines.length) {
      String t = ltrim(lines[i]);
      if (fenceMark(t) >= mark && t.length() > 0 && t.charAt(0) == ch) {
        i++;
        break;
      }
      if (body.length() > 0) {
        body.append('\n');
      }
      body.append(lines[i]);
      i++;
    }
    box.addView(codeTv(c, body.toString()));
    return i;
  }
  static int addIndented(Context c, LinearLayout box, String[] lines, int i) {
    StringBuilder body = new StringBuilder();
    while (i < lines.length && (isIndentedCode(lines[i]) || isBlank(lines[i]))) {
      if (isBlank(lines[i]) && (i + 1 >= lines.length || !isIndentedCode(lines[i + 1]))) {
        break;
      }
      String row = lines[i];
      if (indentWidth(row) >= 4) {
        int cut = 0;
        int w = 0;
        while (cut < row.length() && w < 4) {
          if (row.charAt(cut) == '\t') {
            w += 4;
          } else {
            w++;
          }
          cut++;
        }
        row = row.substring(cut);
      }
      if (body.length() > 0) {
        body.append('\n');
      }
      body.append(row);
      i++;
    }
    box.addView(codeTv(c, body.toString()));
    return i;
  }
  static int addQuote(Context c, LinearLayout box, String[] lines, int i) {
    LinearLayout inner = new LinearLayout(c);
    inner.setOrientation(LinearLayout.VERTICAL);
    inner.setBackground(quoteBar(c));
    pad(inner, c, 10, 4, 8, 4);
    StringBuilder para = new StringBuilder();
    while (i < lines.length && (isQuote(lines[i]) || (para.length() > 0 && !isBlank(lines[i]) && !isFence(lines[i])))) {
      if (isBlank(lines[i])) {
        if (para.length() > 0) {
          inner.addView(tv(c, inline(para.toString()), 14, false));
          para.setLength(0);
        }
        i++;
        continue;
      }
      if (para.length() > 0) {
        para.append(' ');
      }
      para.append(quoteText(lines[i]));
      i++;
    }
    if (para.length() > 0) {
      inner.addView(tv(c, inline(para.toString()), 14, false));
    }
    box.addView(inner);
    return i;
  }
  static GradientDrawable quoteBar(Context c) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(Theme.SURFACE);
    g.setCornerRadius(Theme.corner(c, 6));
    g.setStroke(Theme.dp(c, 3), Theme.ACCENT);
    return g;
  }
  static int addList(Context c, LinearLayout box, String[] lines, int i) {
    boolean ordered = listOrdered(lines[i]);
    int n = 1;
    while (i < lines.length && listMarker(lines[i]) >= 0) {
      boolean ord = listOrdered(lines[i]);
      if (ord != ordered) {
        break;
      }
      int task = taskState(lines[i]);
      String body = listBody(lines[i]);
      i++;
      while (i < lines.length && indentWidth(lines[i]) >= 2 && listMarker(lines[i]) < 0
          && !isFence(lines[i]) && !isHr(lines[i]) && !isBlank(lines[i])) {
        body = body + " " + ltrim(lines[i]);
        i++;
      }
      LinearLayout row = new LinearLayout(c);
      row.setOrientation(LinearLayout.HORIZONTAL);
      TextView mark = new TextView(c);
      mark.setTextColor(Theme.ACCENT);
      mark.setTextSize(14);
      if (task == 0) {
        mark.setText("[ ]  ");
      } else if (task == 1) {
        mark.setText("[x]  ");
      } else if (ordered) {
        mark.setText(n + ".  ");
      } else {
        mark.setText("\u2022  ");
      }
      row.addView(mark);
      TextView item = tv(c, inline(body), 14, false);
      item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      row.addView(item);
      box.addView(row);
      n++;
    }
    return i;
  }
  static int addTable(Context c, LinearLayout box, String[] lines, int i) {
    String[] head = splitPipe(lines[i]);
    i += 2;
    LinearLayout table = new LinearLayout(c);
    table.setOrientation(LinearLayout.VERTICAL);
    table.setBackground(Theme.outline(c, Theme.SURFACE, Theme.SURFACE_HI, 6));
    addTableRow(c, table, head, true);
    while (i < lines.length && isTableRow(lines[i]) && !isBlank(lines[i])) {
      addTableRow(c, table, splitPipe(lines[i]), false);
      i++;
    }
    box.addView(table);
    return i;
  }
  static void addTableRow(Context c, LinearLayout table, String[] cells, boolean head) {
    LinearLayout row = new LinearLayout(c);
    row.setOrientation(LinearLayout.HORIZONTAL);
    int k;
    for (k = 0; k < cells.length; k++) {
      TextView cell = tv(c, inline(cells[k]), 12, head);
      cell.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      pad(cell, c, 6, 4, 6, 4);
      row.addView(cell);
    }
    table.addView(row);
    View line = new View(c);
    line.setBackgroundColor(Theme.SURFACE_HI);
    table.addView(line, new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(c, 1)));
  }
  static int addPara(Context c, LinearLayout box, String[] lines, int i, File dir) {
    if (isImageOnly(lines[i])) {
      addImage(c, box, lines[i], dir);
      return i + 1;
    }
    StringBuilder para = new StringBuilder();
    while (i < lines.length && !isBlank(lines[i]) && !isFence(lines[i]) && !isHr(lines[i])
        && headingAtx(lines[i]) == 0 && listMarker(lines[i]) < 0 && !isQuote(lines[i])) {
      String row = trimRight(lines[i]);
      boolean hard = row.endsWith("  ");
      if (para.length() > 0) {
        para.append(hard ? '\n' : ' ');
      }
      para.append(ltrim(row));
      i++;
      if (hard) {
        break;
      }
    }
    String t = para.toString();
    if (looksLikeHtml(t)) {
      t = stripHtml(t);
    }
    box.addView(tv(c, inline(t), 14, false));
    return i;
  }
  static boolean isImageOnly(String line) {
    String t = line.trim();
    return t.startsWith("![") && t.endsWith(")") && t.indexOf("](") > 0;
  }
  static void addImage(Context c, LinearLayout box, String line, File dir) {
    String t = line.trim();
    int br = t.indexOf(']');
    int par = t.lastIndexOf(')');
    String alt = br > 2 ? t.substring(2, br) : "";
    String src = "";
    int open = t.indexOf("](");
    if (open >= 0 && par > open) {
      src = t.substring(open + 2, par).trim();
      int sp = src.indexOf(' ');
      if (sp > 0) {
        src = src.substring(0, sp);
      }
    }
    Bitmap bmp = decodeImg(dir, src);
    if (bmp != null) {
      ImageView iv = new ImageView(c);
      iv.setImageBitmap(bmp);
      iv.setAdjustViewBounds(true);
      iv.setMaxHeight(Theme.dp(c, 240));
      pad(iv, c, 0, 6, 0, 6);
      box.addView(iv);
      return;
    }
    TextView tAlt = tv(c, "[" + alt + "]", 13, false);
    tAlt.setTextColor(Theme.TEXT_DIM);
    tAlt.setGravity(Gravity.CENTER);
    box.addView(tAlt);
  }
  static Bitmap decodeImg(File dir, String src) {
    if (src == null || src.length() == 0 || src.startsWith("http")) {
      return null;
    }
    File f = src.startsWith("/") ? new File(src) : new File(dir, src);
    if (!f.isFile()) {
      return null;
    }
    try {
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inSampleSize = 2;
      o.inPreferredConfig = Bitmap.Config.RGB_565;
      return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    } catch (Throwable t) {
      return null;
    }
  }
}
