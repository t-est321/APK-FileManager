package com.dlna.FileManager;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public final class TrashActivity extends Activity {
  private final List<TrashStore.Entry> entries = new ArrayList<TrashStore.Entry>();
  private File trashDir;
  private TextView header;
  private ListView list;
  private BaseAdapter adapter;
  private final SimpleDateFormat fmt =
    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(savedInstanceState);
    trashDir = TrashStore.trashDir(getFilesDir());
    trashDir.mkdirs();
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Theme.BG);
    header = new TextView(this);
    header.setTextColor(Theme.TEXT_DIM);
    header.setTextSize(11);
    header.setPadding(Theme.dp(this, 12), Theme.dp(this, 10), Theme.dp(this, 12), Theme.dp(this, 6));
    root.addView(header);
    list = new ListView(this);
    Ui.styleList(this, list);
    FrameLayout wrap = new FrameLayout(this);
    wrap.addView(list, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    ScrollRail rail = new ScrollRail(this);
    FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
      Theme.dp(this, 10), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
    rlp.rightMargin = Theme.dp(this, 2);
    wrap.addView(rail, rlp);
    rail.attach(list);
    root.addView(wrap, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    LinearLayout row = new LinearLayout(this);
    row.setPadding(Theme.dp(this, 4), Theme.dp(this, 4), Theme.dp(this, 4), Theme.dp(this, 6));
    Button empty = Ui.btn(this, "Empty Trash", Theme.DANGER);
    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    elp.setMargins(Theme.dp(this, 2), 0, Theme.dp(this, 2), 0);
    empty.setLayoutParams(elp);
    empty.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        emptyTrash();
      }
    });
    row.addView(empty);
    Button back = Ui.btn(this, "Back", Theme.ACCENT);
    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    blp.setMargins(Theme.dp(this, 2), 0, Theme.dp(this, 2), 0);
    back.setLayoutParams(blp);
    back.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    row.addView(back);
    root.addView(row);
    adapter = new BaseAdapter() {
      @Override
      public int getCount() { return entries.size(); }
      @Override
      public Object getItem(int p) { return entries.get(p); }
      @Override
      public long getItemId(int p) { return p; }
      @Override
      public View getView(int p, View v, ViewGroup g) {
        TrashStore.Entry e = entries.get(p);
        long leftMs = (long) AppSettings.get().trashDays() * 24L * 60L * 60L * 1000L
          - (System.currentTimeMillis() - e.deletedAt);
        long leftDays = leftMs / (24L * 60L * 60L * 1000L) + 1;
        int kind = e.isDir ? FileGlyph.DIR : FileGlyph.forName(e.name);
        String sub = e.path + "  |  " + FileOps.humanSize(e.size)
          + "  |  left " + leftDays + "d  |  " + fmt.format(new Date(e.deletedAt));
        View row = Ui.row(TrashActivity.this, v, kind, e.name, sub, Theme.TEXT, false);
        if (AppSettings.get().showIcons && !e.isDir
            && (kind == FileGlyph.IMG || kind == FileGlyph.VIDEO || kind == FileGlyph.APK)) {
          Thumbs.bind(TrashActivity.this, Ui.iconView(row), TrashStore.payload(trashDir, e.id).getAbsolutePath(), kind);
        }
        return row;
      }
    };
    list.setAdapter(adapter);
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
        entryMenu(entries.get(pos));
      }
    });
    setContentView(root);
    Ui.bar(this);
  }
  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }
  private void refresh() {
    Ui.bg(this, "Scanning trash...", new Runnable() {
      @Override
      public void run() {
        final List<TrashStore.Entry> fresh = TrashStore.list(trashDir, System.currentTimeMillis());
        long total = 0;
        for (int i = 0; i < fresh.size(); i++) {
          total += fresh.get(i).size;
        }
        final long totalSize = total;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            entries.clear();
            entries.addAll(fresh);
            adapter.notifyDataSetChanged();
            header.setText("Trash (" + entries.size() + " items, " + FileOps.humanSize(totalSize)
              + ", auto-delete after 30 days)");
          }
        });
      }
    }, null);
  }
  private void entryMenu(final TrashStore.Entry e) {
    Ui.menu(this, e.name, new String[]{"Restore", "Delete forever"},
      new int[]{Theme.ACCENT, Theme.DANGER}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            restore(e);
          } else {
            confirmDelete(e);
          }
        }
      });
  }
  private void restore(final TrashStore.Entry e) {
    Ui.bg(this, "Restoring...", new Runnable() {
      @Override
      public void run() {
        final int r = TrashStore.restore(trashDir, e.id);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Ui.toast(TrashActivity.this,
              r > 0 ? "Restored: " + e.name : "Restore failed: " + e.name);
            refresh();
          }
        });
      }
    }, null);
  }
  private void confirmDelete(final TrashStore.Entry e) {
    Runnable act = new Runnable() {
      @Override
      public void run() {
        TrashStore.deleteForever(trashDir, e.id);
        Ui.toast(TrashActivity.this, "Deleted: " + e.name);
        refresh();
      }
    };
    if (AppSettings.get().confirmPermanent) {
      Ui.confirm(this, "Delete forever (no recovery)?", act);
    } else {
      act.run();
    }
  }
  private void emptyTrash() {
    Runnable act = new Runnable() {
      @Override
      public void run() {
        Ui.bg(TrashActivity.this, "Emptying...", new Runnable() {
          @Override
          public void run() {
            final int[] n = {TrashStore.empty(trashDir)};
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                Ui.toast(TrashActivity.this, "Removed " + n[0] + " items");
                refresh();
              }
            });
          }
        }, null);
      }
    };
    Ui.confirm(this, "Empty trash completely (no recovery)?", act);
  }
}
