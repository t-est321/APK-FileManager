package com.dlna.FileManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
final class ScrollRail extends View {
  private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF r = new RectF();
  private final Handler h = new Handler();
  private final Runnable fade = new Runnable() {
    @Override
    public void run() {
      animate().alpha(0f).setDuration(250).start();
    }
  };
  private ListView list;
  private float thumbY;
  private float thumbH;
  private boolean drag;
  private int scrollState;
  ScrollRail(Context c) {
    super(c);
    setAlpha(0f);
  }
  @Override
  protected void onDetachedFromWindow() {
    h.removeCallbacks(fade);
    animate().cancel();
    if (list != null) {
      list.setOnScrollListener(null);
      list = null;
    }
    super.onDetachedFromWindow();
  }
  void attach(ListView lv) {
    list = lv;
    lv.setVerticalScrollBarEnabled(false);
    lv.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView v, int s) {
        scrollState = s;
        if (s == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
          show();
        } else {
          h.removeCallbacks(fade);
          animate().cancel();
          setAlpha(1f);
          layoutThumb();
        }
      }
      @Override
      public void onScroll(AbsListView v, int first, int count, int total) {
        layoutThumb();
        if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
          h.removeCallbacks(fade);
          animate().cancel();
          setAlpha(1f);
          return;
        }
        show();
      }
    });
  }
  private void show() {
    animate().alpha(1f).setDuration(80).start();
    h.removeCallbacks(fade);
    h.postDelayed(fade, 1800);
  }
  private void layoutThumb() {
    if (list == null) {
      return;
    }
    int total = list.getCount();
    int vis = list.getChildCount();
    int hgt = getHeight();
    if (total <= vis || hgt <= 0) {
      thumbH = 0;
      invalidate();
      return;
    }
    thumbH = Math.max(Theme.dp(getContext(), 28), hgt * vis / (float) total);
    int first = list.getFirstVisiblePosition();
    View row = list.getChildAt(0);
    int rowH = row == null ? 1 : Math.max(1, row.getHeight());
    float pix = first * rowH - (row == null ? 0 : row.getTop());
    float maxPix = Math.max(1f, (total - vis) * rowH);
    float range = hgt - thumbH;
    thumbY = range * Math.min(1f, Math.max(0f, pix / maxPix));
    invalidate();
  }
  @Override
  protected void onDraw(Canvas c) {
    if (thumbH <= 0) {
      return;
    }
    int w = getWidth();
    int pad = Theme.dp(getContext(), 3);
    track.setColor(Theme.SURFACE_HI);
    r.set(pad, 0, w - pad, getHeight());
    c.drawRoundRect(r, w / 2f, w / 2f, track);
    thumb.setColor(Theme.ACCENT);
    r.set(pad, thumbY, w - pad, thumbY + thumbH);
    c.drawRoundRect(r, w / 2f, w / 2f, thumb);
  }
  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (list == null || thumbH <= 0) {
      return false;
    }
    int act = e.getAction();
    if (act == MotionEvent.ACTION_DOWN) {
      drag = true;
      h.removeCallbacks(fade);
      setAlpha(1f);
      jump(e.getY());
      return true;
    }
    if (act == MotionEvent.ACTION_MOVE && drag) {
      jump(e.getY());
      return true;
    }
    if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
      drag = false;
      show();
      return true;
    }
    return super.onTouchEvent(e);
  }
  private void jump(float y) {
    int total = list.getCount();
    int vis = Math.max(1, list.getChildCount());
    float range = Math.max(1f, getHeight() - thumbH);
    float t = Math.min(1f, Math.max(0f, (y - thumbH / 2f) / range));
    int pos = (int) (t * Math.max(0, total - vis) + 0.5f);
    list.smoothScrollToPositionFromTop(pos, 0, 70);
    layoutThumb();
  }
}
