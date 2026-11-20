package com.dlna.FileManager;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.io.File;
public final class ViewerActivity extends Activity {
  private static final int MAX_DIM = 2048;
  private VideoView video;
  private GifView gif;
  private TextView nameBar;
  private LinearLayout videoBar;
  private Button playBtn;
  private TextView timeView;
  private VideoSeek seek;
  private final Handler progress = new Handler();
  private boolean prepared;
  private int duration;
  private Bitmap imageBmp;
  private File[] imgFiles;
  private int imgAt;
  private int imgGen;
  private FrameLayout imgStage;
  private boolean wasPlaying = false;
  private int position = 0;
  private final Runnable progressTick = new Runnable() {
    @Override
    public void run() {
      updateTime();
      progress.postDelayed(this, 250);
    }
  };
  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    String path = getIntent().getStringExtra("path");
    final File f = new File(path == null ? "" : path);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.BLACK);
    nameBar = new TextView(this);
    nameBar.setText(f.getName() + "  |  " + FileOps.humanSize(f.length()));
    nameBar.setTextColor(Theme.TEXT);
    nameBar.setTextSize(12);
    nameBar.setSingleLine(true);
    nameBar.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
    nameBar.setBackgroundColor(Theme.SURFACE);
    nameBar.setPadding(Theme.dp(this, 14), Theme.dp(this, 10), Theme.dp(this, 14), Theme.dp(this, 8));
    root.addView(nameBar);
    final FrameLayout stage = new FrameLayout(this);
    stage.setBackgroundColor(Color.BLACK);
    root.addView(stage, new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    setContentView(root);
    Ui.bar(this);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    int kind = getIntent().getBooleanExtra("asImage", false) ? Media.IMAGE : Media.ofFile(f);
    if (kind == Media.IMAGE) {
      imgStage = stage;
      fillImgList(f);
      if ("gif".equals(FileOps.ext(f.getName())) || Media.gifHeader(f)) {
        gif = new GifView(this, f.getAbsolutePath());
        if (gif.ready()) {
          ZoomPane zp = new ZoomPane(this);
          zp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleBars(); }
          });
          zp.addView(gif, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
          stage.addView(zp, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
          gif.startMovie();
          Ui.toast(this, "GIF  pinch + drag");
        } else {
          gif = null;
          loadImage(stage, f);
        }
      } else {
        loadImage(stage, f);
      }
      addImgNav(stage);
    } else if (kind == Media.VIDEO || kind == Media.AUDIO) {
      buildVideoBar(root);
      showVideo(stage, f);
    } else {
      Ui.toast(this, "Unsupported type");
      OpLog.err(this, "viewer unsupported " + f.getName());
      finish();
    }
  }
  private void fillImgList(File f) {
    File dir = f.getParentFile();
    imgAt = 0;
    imgFiles = new File[]{f};
    if (dir == null) {
      return;
    }
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    java.util.ArrayList<File> out = new java.util.ArrayList<File>();
    int i;
    for (i = 0; i < kids.length; i++) {
      File k = kids[i];
      if (k.isFile() && Media.of(k.getName()) == Media.IMAGE) {
        out.add(k);
      }
    }
    if (out.size() == 0) {
      return;
    }
    java.util.Collections.sort(out, new java.util.Comparator<File>() {
      @Override
      public int compare(File a, File b) {
        return a.getName().compareToIgnoreCase(b.getName());
      }
    });
    imgFiles = out.toArray(new File[out.size()]);
    for (i = 0; i < imgFiles.length; i++) {
      if (imgFiles[i].getAbsolutePath().equals(f.getAbsolutePath())) {
        imgAt = i;
        break;
      }
    }
  }
  private void addImgNav(final FrameLayout stage) {
    if (imgFiles == null || imgFiles.length < 2) {
      return;
    }
    LinearLayout nav = new LinearLayout(this);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    nav.setGravity(Gravity.CENTER);
    Button prev = imgNavBtn("<", new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        goImg(-1);
      }
    });
    Button next = imgNavBtn(">", new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        goImg(1);
      }
    });
    nav.addView(prev);
    nav.addView(next);
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    lp.bottomMargin = Theme.dp(this, 16);
    stage.addView(nav, lp);
    nav.bringToFront();
  }
  private Button imgNavBtn(String label, View.OnClickListener l) {
    Button b = Ui.btn(this, label, Theme.TEXT);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    p.setMargins(Theme.dp(this, 8), Theme.dp(this, 4), Theme.dp(this, 8), Theme.dp(this, 4));
    b.setLayoutParams(p);
    b.setMinWidth(Theme.dp(this, 56));
    b.setOnClickListener(l);
    b.setClickable(true);
    b.setFocusable(true);
    return b;
  }
  private void goImg(int d) {
    if (imgFiles == null || imgFiles.length == 0 || imgStage == null) {
      return;
    }
    imgAt = (imgAt + d + imgFiles.length) % imgFiles.length;
    imgGen++;
    if (gif != null) {
      gif.stopMovie();
      gif = null;
    }
    File nf = imgFiles[imgAt];
    nameBar.setText(nf.getName() + "  |  " + FileOps.humanSize(nf.length()));
    int i;
    for (i = imgStage.getChildCount() - 1; i >= 0; i--) {
      View ch = imgStage.getChildAt(i);
      if (ch instanceof LinearLayout) {
        continue;
      }
      imgStage.removeViewAt(i);
    }
    if ("gif".equals(FileOps.ext(nf.getName())) || Media.gifHeader(nf)) {
      gif = new GifView(this, nf.getAbsolutePath());
      if (gif.ready()) {
        ZoomPane zp = new ZoomPane(this);
        zp.addView(gif, new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imgStage.addView(zp, 0, new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        int gi;
        for (gi = 0; gi < imgStage.getChildCount(); gi++) {
          if (imgStage.getChildAt(gi) instanceof LinearLayout) {
            imgStage.getChildAt(gi).bringToFront();
          }
        }
        gif.startMovie();
        return;
      }
    }
    loadImage(imgStage, nf);
  }
  private void loadImage(final FrameLayout stage, File f) {
    final String path = f.getAbsolutePath();
    final int gen = ++imgGen;
    final Bitmap[] out = new Bitmap[1];
    Ui.bg(this, "Loading image...", new Runnable() {
      @Override
      public void run() {
        out[0] = decode(path);
      }
    }, new Runnable() {
      @Override
      public void run() {
        Bitmap bmp = out[0];
        if (gen != imgGen) {
          if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
          }
          return;
        }
        if (bmp == null) {
          Ui.toast(ViewerActivity.this, "Cannot decode image");
          OpLog.err(ViewerActivity.this, "decode fail " + path);
          finish();
          return;
        }
        ImageView iv = new ImageView(ViewerActivity.this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        if (imageBmp != null && imageBmp != bmp && !imageBmp.isRecycled()) {
          imageBmp.recycle();
        }
        imageBmp = bmp;
        iv.setImageBitmap(bmp);
        ZoomPane zp = new ZoomPane(ViewerActivity.this);
        zp.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) { toggleBars(); }
        });
        zp.addView(iv, new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        stage.addView(zp, 0, new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        int i;
        for (i = 0; i < stage.getChildCount(); i++) {
          if (stage.getChildAt(i) instanceof LinearLayout) {
            stage.getChildAt(i).bringToFront();
          }
        }
        Ui.toast(ViewerActivity.this, bmp.getWidth() + " x " + bmp.getHeight() + "  pinch + drag");
      }
    });
  }
  private static Bitmap decode(String path) {
    if (path != null) {
      String low = path.toLowerCase(java.util.Locale.US);
      if (low.endsWith(".svg")) {
        try {
          int cap = MAX_DIM;
          try {
            cap = AppSettings.get().svgRes();
          } catch (Exception ignored) {}
          return SvgDraw.renderFit(FileOps.readString(new File(path)), cap);
        } catch (Exception e) {
          return null;
        }
      }
    }
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    decodeStream(path, bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
      return null;
    }
    int cap = MAX_DIM;
    try {
      if (AppSettings.get().imgCompress) {
        cap = 1024;
      }
    } catch (Exception ignored) {}
    int sample = 1;
    while (bounds.outWidth / sample > cap || bounds.outHeight / sample > cap) {
      sample *= 2;
    }
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inSampleSize = sample;
    try {
      Bitmap bmp = decodeStream(path, o);
      if (bmp != null) {
        return bmp;
      }
    } catch (OutOfMemoryError e) {
      o.inSampleSize = sample * 2;
      try {
        return decodeStream(path, o);
      } catch (OutOfMemoryError e2) {
        return null;
      }
    }
    return BitmapFactory.decodeFile(path, o);
  }
  private static Bitmap decodeStream(String path, BitmapFactory.Options o) {
    java.io.FileInputStream in = null;
    try {
      in = new java.io.FileInputStream(path);
      return BitmapFactory.decodeStream(in, null, o);
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
  private void buildVideoBar(LinearLayout root) {
    videoBar = new LinearLayout(this);
    videoBar.setOrientation(LinearLayout.VERTICAL);
    videoBar.setBackgroundColor(Theme.SURFACE);
    int m = Theme.dp(this, 8);
    videoBar.setPadding(m, Theme.dp(this, 4), m, Theme.dp(this, 8));
    LinearLayout top = new LinearLayout(this);
    top.setGravity(Gravity.CENTER_VERTICAL);
    timeView = new TextView(this);
    timeView.setTextColor(Theme.TEXT_DIM);
    timeView.setTextSize(12);
    timeView.setTypeface(android.graphics.Typeface.MONOSPACE);
    timeView.setText("0:00 / 0:00");
    top.addView(timeView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    videoBar.addView(top);
    seek = new VideoSeek(this);
    seek.setMax(1);
    seek.setListener(new VideoSeek.OnSeek() {
      @Override
      public void onSeek(int v, boolean fromUser) {
        if (fromUser && video != null && prepared) {
          try {
            video.seekTo(v);
          } catch (Exception e) {
            OpLog.err(ViewerActivity.this, "seek", e);
          }
        }
      }
      @Override
      public void onStart() {
        if (video != null && video.isPlaying()) {
          video.pause();
        }
      }
      @Override
      public void onStop() {
        if (video != null && prepared && playBtn != null && "Pause".equals(playBtn.getText().toString())) {
          try {
            video.start();
          } catch (Exception ignored) {}
        }
        updateTime();
      }
    });
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    slp.topMargin = Theme.dp(this, 4);
    videoBar.addView(seek, slp);
    LinearLayout controls = new LinearLayout(this);
    controls.addView(ctrlBtn("-10s", new View.OnClickListener() {
      @Override
      public void onClick(View v) { skip(-10000); }
    }));
    playBtn = Ui.btn(this, "Pause", Theme.ACCENT);
    playBtn.setLayoutParams(weightLp());
    playBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { togglePlay(); }
    });
    controls.addView(playBtn);
    controls.addView(ctrlBtn("+10s", new View.OnClickListener() {
      @Override
      public void onClick(View v) { skip(10000); }
    }));
    videoBar.addView(controls);
    videoBar.setVisibility(View.GONE);
    root.addView(videoBar);
  }
  private LinearLayout.LayoutParams weightLp() {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    lp.setMargins(Theme.dp(this, 2), Theme.dp(this, 2), Theme.dp(this, 2), Theme.dp(this, 2));
    return lp;
  }
  private Button ctrlBtn(String label, View.OnClickListener l) {
    Button b = Ui.btn(this, label, Theme.TEXT);
    b.setLayoutParams(weightLp());
    b.setOnClickListener(l);
    return b;
  }
  private void skip(int deltaMs) {
    if (video == null || !prepared || duration <= 0) {
      return;
    }
    int next = video.getCurrentPosition() + deltaMs;
    if (next < 0) {
      next = 0;
    }
    if (next > duration) {
      next = duration;
    }
    try {
      video.seekTo(next);
    } catch (Exception e) {
      OpLog.err(this, "skip", e);
    }
    updateTime();
  }
  private void togglePlay() {
    if (video == null || !prepared) {
      return;
    }
    try {
      if (video.isPlaying()) {
        video.pause();
        playBtn.setText("Play");
      } else {
        video.start();
        playBtn.setText("Pause");
      }
    } catch (Exception e) {
      OpLog.err(this, "togglePlay", e);
      Ui.toast(this, "Playback error");
    }
    updateTime();
  }
  private void updateTime() {
    if (video == null || timeView == null) {
      return;
    }
    int pos = 0;
    try {
      if (prepared) {
        pos = video.getCurrentPosition();
        if (pos < 0) {
          pos = 0;
        }
      }
    } catch (Exception ignored) {}
    timeView.setText(fmt(pos) + " / " + fmt(duration));
    if (seek != null) {
      seek.setMax(duration > 0 ? duration : 1);
      seek.setValue(pos);
    }
  }
  private static String fmt(int ms) {
    if (ms < 0) {
      ms = 0;
    }
    int total = ms / 1000;
    int h = total / 3600;
    int m = (total % 3600) / 60;
    int s = total % 60;
    if (h > 0) {
      return h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }
    return m + ":" + (s < 10 ? "0" : "") + s;
  }
  private void toggleBars() {
    if (nameBar != null) {
      nameBar.setVisibility(nameBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
    if (videoBar != null) {
      videoBar.setVisibility(videoBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
  }
  private void showVideo(FrameLayout stage, File f) {
    video = new VideoView(this);
    video.setKeepScreenOn(true);
    try {
      video.setVideoPath(f.getAbsolutePath());
    } catch (Exception e) {
      OpLog.err(this, "setVideoPath", e);
      Ui.toast(this, "Cannot open video");
      finish();
      return;
    }
    video.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { toggleBars(); }
    });
    video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        prepared = true;
        try {
          mp.setLooping(false);
          duration = mp.getDuration();
          if (duration < 0) {
            duration = 0;
          }
        } catch (Exception e) {
          OpLog.err(ViewerActivity.this, "onPrepared", e);
          duration = 0;
        }
        if (seek != null) {
          seek.setMax(duration > 0 ? duration : 1);
        }
        try {
          if (position > 0) {
            video.seekTo(position);
          }
          video.start();
        } catch (Exception e) {
          OpLog.err(ViewerActivity.this, "video start", e);
          Ui.toast(ViewerActivity.this, "Cannot start video");
          return;
        }
        if (playBtn != null) {
          playBtn.setText("Pause");
        }
        if (videoBar != null) {
          videoBar.setVisibility(View.VISIBLE);
        }
        updateTime();
        progress.removeCallbacks(progressTick);
        progress.postDelayed(progressTick, 250);
      }
    });
    video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        if (playBtn != null) {
          playBtn.setText("Play");
        }
        if (seek != null && duration > 0) {
          seek.setValue(duration);
        }
        updateTime();
      }
    });
    video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
      @Override
      public boolean onError(MediaPlayer mp, int what, int extra) {
        OpLog.err(ViewerActivity.this, "video error what=" + what + " extra=" + extra);
        if (!ViewerActivity.this.isFinishing()) {
          Ui.toast(ViewerActivity.this, "Cannot play video (" + what + "/" + extra + ")");
        }
        return true;
      }
    });
    stage.addView(video, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
  }
  @Override
  protected void onPause() {
    super.onPause();
    progress.removeCallbacks(progressTick);
    if (gif != null) {
      gif.stopMovie();
    }
    if (video != null && prepared) {
      try {
        wasPlaying = video.isPlaying();
        position = video.getCurrentPosition();
        if (wasPlaying) {
          video.pause();
        }
      } catch (Exception e) {
        wasPlaying = false;
      }
    } else {
      wasPlaying = false;
    }
  }
  @Override
  protected void onResume() {
    super.onResume();
    if (gif != null) {
      gif.startMovie();
    }
    if (video != null && prepared) {
      progress.postDelayed(progressTick, 250);
      if (wasPlaying) {
        try {
          video.seekTo(position);
          video.start();
        } catch (Exception e) {
          OpLog.err(this, "resume video", e);
        }
        wasPlaying = false;
      }
    }
  }
  @Override
  protected void onDestroy() {
    Ui.dismissOpen();
    progress.removeCallbacks(progressTick);
    if (gif != null) {
      gif.stopMovie();
      gif = null;
    }
    if (video != null) {
      try {
        video.stopPlayback();
      } catch (Exception ignored) {}
      video = null;
    }
    if (imageBmp != null && !imageBmp.isRecycled()) {
      imageBmp.recycle();
      imageBmp = null;
    }
    super.onDestroy();
  }
}
