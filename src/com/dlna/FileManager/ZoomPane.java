package com.dlna.FileManager;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
final class ZoomPane extends FrameLayout {
  private final ScaleGestureDetector detector;
  private final int slop;
  private float scale = 1f;
  private float tx;
  private float ty;
  private float lastX;
  private float lastY;
  private float downX;
  private float downY;
  private boolean pan;
  private boolean moved;
  ZoomPane(Context c) {
    super(c);
    setClipChildren(true);
    setClickable(true);
    slop = ViewConfiguration.get(c).getScaledTouchSlop();
    detector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      @Override
      public boolean onScaleBegin(ScaleGestureDetector d) {
        if (getParent() != null) {
          getParent().requestDisallowInterceptTouchEvent(true);
        }
        pan = false;
        return true;
      }
      @Override
      public boolean onScale(ScaleGestureDetector d) {
        float next = scale * d.getScaleFactor();
        if (next < 1f) {
          next = 1f;
        }
        if (next > 8f) {
          next = 8f;
        }
        scale = next;
        if (scale <= 1.01f) {
          scale = 1f;
          tx = 0f;
          ty = 0f;
        }
        clamp();
        apply();
        return true;
      }
    });
  }
  private void apply() {
    View child = getChildCount() > 0 ? getChildAt(0) : null;
    if (child == null) {
      return;
    }
    child.setScaleX(scale);
    child.setScaleY(scale);
    child.setTranslationX(tx);
    child.setTranslationY(ty);
  }
  private void clamp() {
    View child = getChildCount() > 0 ? getChildAt(0) : null;
    if (child == null) {
      return;
    }
    float extraX = Math.max(0f, (child.getWidth() * scale - getWidth()) / 2f);
    float extraY = Math.max(0f, (child.getHeight() * scale - getHeight()) / 2f);
    if (tx > extraX) {
      tx = extraX;
    }
    if (tx < -extraX) {
      tx = -extraX;
    }
    if (ty > extraY) {
      ty = extraY;
    }
    if (ty < -extraY) {
      ty = -extraY;
    }
  }
  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    detector.onTouchEvent(e);
    int act = e.getActionMasked();
    if (act == MotionEvent.ACTION_POINTER_DOWN || e.getPointerCount() > 1) {
      if (getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      return true;
    }
    if (act == MotionEvent.ACTION_DOWN) {
      downX = e.getX();
      downY = e.getY();
      lastX = downX;
      lastY = downY;
      moved = false;
      pan = scale > 1.01f;
    }
    if (act == MotionEvent.ACTION_MOVE && scale > 1.01f) {
      if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) {
        moved = true;
        return true;
      }
    }
    return scale > 1.01f && act != MotionEvent.ACTION_UP && act != MotionEvent.ACTION_CANCEL;
  }
  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (getParent() != null && (e.getPointerCount() > 1 || scale > 1.01f)) {
      getParent().requestDisallowInterceptTouchEvent(true);
    }
    detector.onTouchEvent(e);
    int act = e.getActionMasked();
    if (act == MotionEvent.ACTION_DOWN) {
      lastX = e.getX();
      lastY = e.getY();
      downX = lastX;
      downY = lastY;
      moved = false;
      pan = scale > 1.01f && e.getPointerCount() == 1;
      return true;
    }
    if (act == MotionEvent.ACTION_MOVE) {
      if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) {
        moved = true;
      }
      if (pan && e.getPointerCount() == 1 && !detector.isInProgress() && scale > 1.01f) {
        tx += e.getX() - lastX;
        ty += e.getY() - lastY;
        lastX = e.getX();
        lastY = e.getY();
        clamp();
        apply();
        return true;
      }
      lastX = e.getX();
      lastY = e.getY();
      return true;
    }
    if (act == MotionEvent.ACTION_UP) {
      pan = false;
      if (!moved && !detector.isInProgress()) {
        performClick();
        View child = getChildCount() > 0 ? getChildAt(0) : null;
        if (child != null) {
          child.performClick();
        }
      }
      return true;
    }
    if (act == MotionEvent.ACTION_CANCEL) {
      pan = false;
    }
    return true;
  }
  @Override
  public boolean performClick() {
    return super.performClick();
  }
}
