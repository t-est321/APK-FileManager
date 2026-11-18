package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.View;
final class ProgressBar extends View {
  private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF r = new RectF();
  private final Handler h = new Handler();
  private final Runnable tick = new Runnable() {
    @Override
    public void run() {
      phase = (phase + 4) % 360;
      invalidate();
      if (running) {
        h.postDelayed(this, 16);
      }
    }
  };
  private boolean running;
  private int phase;
  private float frac = -1f;
  ProgressBar(Context c) {
    super(c);
  }
  void startAnim() {
    running = true;
    frac = -1f;
    h.removeCallbacks(tick);
    h.post(tick);
  }
  void stopAnim() {
    running = false;
    h.removeCallbacks(tick);
  }
  void setFraction(float v) {
    if (v < 0f) {
      v = 0f;
    }
    if (v > 1f) {
      v = 1f;
    }
    frac = v;
    running = false;
    h.removeCallbacks(tick);
    invalidate();
  }
  @Override
  protected void onDetachedFromWindow() {
    stopAnim();
    super.onDetachedFromWindow();
  }
  @Override
  protected void onDraw(Canvas c) {
    int w = getWidth();
    int hgt = getHeight();
    float rad = hgt / 2f;
    r.set(0, 0, w, hgt);
    track.setColor(Theme.SURFACE_HI);
    c.drawRoundRect(r, rad, rad, track);
    fill.setColor(Theme.ACCENT);
    if (frac >= 0f) {
      float fw = w * frac;
      if (fw > 0f) {
        r.set(0, 0, fw, hgt);
        c.drawRoundRect(r, rad, rad, fill);
      }
      return;
    }
    float span = w * 0.38f;
    float x = (w + span) * (phase / 360f) - span;
    r.set(Math.max(0, x), 0, Math.min(w, x + span), hgt);
    c.drawRoundRect(r, rad, rad, fill);
  }
}
