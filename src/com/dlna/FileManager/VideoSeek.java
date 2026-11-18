package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
final class VideoSeek extends View {
  interface OnSeek {
    void onSeek(int value, boolean fromUser);
    void onStart();
    void onStop();
  }
  private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF r = new RectF();
  private int max = 1;
  private int value;
  private boolean drag;
  private OnSeek listener;
  VideoSeek(Context c) {
    super(c);
    setClickable(true);
    setFocusable(true);
  }
  void setListener(OnSeek l) {
    listener = l;
  }
  void setMax(int m) {
    max = m < 1 ? 1 : m;
    if (value > max) {
      value = max;
    }
    invalidate();
  }
  void setValue(int v) {
    if (drag) {
      return;
    }
    if (v < 0) {
      v = 0;
    }
    if (v > max) {
      v = max;
    }
    if (value != v) {
      value = v;
      invalidate();
    }
  }
  @Override
  protected void onMeasure(int wSpec, int hSpec) {
    int h = Theme.dp(getContext(), 28);
    setMeasuredDimension(MeasureSpec.getSize(wSpec), h);
  }
  @Override
  protected void onDraw(Canvas c) {
    int w = getWidth();
    int h = getHeight();
    float cy = h / 2f;
    float thick = Theme.dp(getContext(), 4);
    float pad = Theme.dp(getContext(), 10);
    r.set(pad, cy - thick / 2f, w - pad, cy + thick / 2f);
    track.setColor(Theme.SURFACE_HI);
    c.drawRoundRect(r, thick, thick, track);
    float t = max <= 0 ? 0f : (value / (float) max);
    float x = pad + t * (w - pad * 2);
    fill.setColor(Theme.ACCENT);
    r.right = x;
    c.drawRoundRect(r, thick, thick, fill);
    thumb.setColor(Theme.TEXT);
    c.drawCircle(x, cy, Theme.dp(getContext(), 7), thumb);
  }
  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int act = e.getActionMasked();
    if (act == MotionEvent.ACTION_DOWN) {
      drag = true;
      if (getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      apply(e.getX());
      if (listener != null) {
        listener.onStart();
      }
      return true;
    }
    if (act == MotionEvent.ACTION_MOVE && drag) {
      apply(e.getX());
      return true;
    }
    if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
      if (drag) {
        apply(e.getX());
        drag = false;
        if (listener != null) {
          listener.onStop();
        }
      }
      return true;
    }
    return super.onTouchEvent(e);
  }
  private void apply(float x) {
    float pad = Theme.dp(getContext(), 10);
    float w = getWidth() - pad * 2;
    if (w <= 0f) {
      return;
    }
    float t = (x - pad) / w;
    if (t < 0f) {
      t = 0f;
    }
    if (t > 1f) {
      t = 1f;
    }
    int next = (int) (t * max + 0.5f);
    if (next != value) {
      value = next;
      invalidate();
      if (listener != null) {
        listener.onSeek(value, true);
      }
    }
  }
}
