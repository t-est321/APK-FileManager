package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.view.View;
import java.io.FileInputStream;
import java.io.InputStream;
final class GifView extends View {
  private final Handler h = new Handler();
  private final Runnable tick = new Runnable() {
    @Override
    public void run() {
      invalidate();
      if (movie != null) {
        h.postDelayed(this, 33);
      }
    }
  };
  private Movie movie;
  private long startAt;
  GifView(Context c, String path) {
    super(c);
    try {
      InputStream in = new FileInputStream(path);
      movie = Movie.decodeStream(in);
      in.close();
    } catch (Exception ignored) {
      movie = null;
    }
    if (movie != null && movie.width() > 0) {
      startAt = System.currentTimeMillis();
    }
  }
  boolean ready() {
    return movie != null && movie.width() > 0;
  }
  void startMovie() {
    h.removeCallbacks(tick);
    if (movie != null) {
      h.post(tick);
    }
  }
  void stopMovie() {
    h.removeCallbacks(tick);
  }
  @Override
  protected void onDetachedFromWindow() {
    stopMovie();
    super.onDetachedFromWindow();
  }
  @Override
  protected void onDraw(Canvas canvas) {
    if (movie == null) {
      return;
    }
    int dur = movie.duration();
    if (dur <= 0) {
      dur = 1000;
    }
    movie.setTime((int) ((System.currentTimeMillis() - startAt) % dur));
    float s = Math.min(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
    float dw = movie.width() * s;
    float dh = movie.height() * s;
    canvas.save();
    canvas.translate((getWidth() - dw) / 2f, (getHeight() - dh) / 2f);
    canvas.scale(s, s);
    movie.draw(canvas, 0, 0);
    canvas.restore();
  }
}
