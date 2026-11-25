package com.dlna.FileManager;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
public final class TermActivity extends Activity {
  private TermShell sh;
  private TextView out;
  private EditText in;
  private ScrollView sc;
  private final Runnable jumpRun = new Runnable() {
    @Override
    public void run() {
      if (sc != null) {
        sc.fullScroll(View.FOCUS_DOWN);
      }
    }
  };
  @Override
  protected void onCreate(Bundle b) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(b);
    String cwd = getIntent().getStringExtra("cwd");
    sh = new TermShell(this, cwd == null ? new File("/") : new File(cwd));
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Theme.BG);
    LinearLayout top = new LinearLayout(this);
    top.setGravity(Gravity.CENTER_VERTICAL);
    top.setBackgroundColor(Theme.SURFACE);
    int m = Theme.dp(this, Theme.compact(this) ? 6 : 8);
    top.setPadding(m, m / 2, m, m / 2);
    TextView title = new TextView(this);
    title.setText("Terminal");
    title.setTextColor(Theme.TEXT);
    title.setTextSize(14);
    top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    Button prev = Ui.btn(this, "Prev", Theme.TEXT);
    prev.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String p = sh.prev();
        if (p.length() > 0) {
          in.setText(p);
          in.setSelection(p.length());
        }
      }
    });
    top.addView(prev);
    Button clear = Ui.btn(this, "Clear", Theme.TEXT_DIM);
    clear.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        out.setText("");
        prompt();
      }
    });
    top.addView(clear);
    root.addView(top);
    sc = new ScrollView(this);
    Ui.styleScroll(this, sc);
    out = new TextView(this);
    out.setTypeface(Typeface.MONOSPACE);
    out.setTextColor(Theme.GOOD);
    out.setTextSize(12);
    out.setPadding(m, m, m, m);
    out.setTextIsSelectable(true);
    sc.addView(out);
    root.addView(sc, new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(m, m / 2, m, m);
    row.setBackgroundColor(Theme.SURFACE);
    in = new EditText(this);
    in.setSingleLine(true);
    in.setTextColor(Theme.TEXT);
    in.setHint("command");
    in.setHintTextColor(Theme.TEXT_DIM);
    in.setTypeface(Typeface.MONOSPACE);
    in.setImeOptions(EditorInfo.IME_ACTION_GO);
    in.setBackground(Theme.rounded(this, Theme.SURFACE_HI, 8));
    int p = Theme.dp(this, 8);
    in.setPadding(p, p, p, p);
    in.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int a, KeyEvent e) {
        if (e != null && e.getAction() != KeyEvent.ACTION_DOWN) {
          return true;
        }
        if (a != EditorInfo.IME_ACTION_GO && a != EditorInfo.IME_ACTION_DONE
            && a != EditorInfo.IME_NULL && a != EditorInfo.IME_ACTION_UNSPECIFIED) {
          return false;
        }
        runLine();
        return true;
      }
    });
    row.addView(in, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    Button go = Ui.btn(this, "Run", Theme.ACCENT);
    go.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        runLine();
      }
    });
    LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    glp.leftMargin = Theme.dp(this, 6);
    go.setLayoutParams(glp);
    row.addView(go);
    root.addView(row);
    setContentView(root);
    Ui.bar(this);
    CrashHandler.install(this);
    out.setText("FileManager shell. Type help or help ls.\n");
    prompt();
    OpLog.info(this, "term open " + sh.cwd.getAbsolutePath());
  }
  @Override
  protected void onDestroy() {
    if (sc != null) {
      sc.removeCallbacks(jumpRun);
    }
    Ui.dismissOpen(this);
    super.onDestroy();
  }
  private void prompt() {
    out.append(sh.cwd.getAbsolutePath() + " $ ");
    jump();
  }
  private void runLine() {
    String line = in.getText().toString();
    in.setText("");
    out.append(line + "\n");
    String res = sh.exec(line);
    if ("\u0004".equals(res)) {
      finish();
      return;
    }
    if ("\u000c".equals(res)) {
      out.setText("");
      prompt();
      return;
    }
    if (res != null && res.length() > 0) {
      out.append(res);
      if (!res.endsWith("\n")) {
        out.append("\n");
      }
    }
    prompt();
  }
  private void jump() {
    sc.post(jumpRun);
  }
}
