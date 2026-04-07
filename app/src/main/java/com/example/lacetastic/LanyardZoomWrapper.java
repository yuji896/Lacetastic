package com.example.lacetastic;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

/**
 * LanyardZoomWrapper — a FrameLayout that applies pinch-to-zoom to its children
 * (the LanyardStrapView + LanyardCanvasView pair).
 *
 * Rules:
 *  - Zoom in: up to 4× the original size.
 *  - Zoom out: cannot go below 1× (the constant/original view). This is the
 *    "zoom-out limit is the constant view" requirement.
 *  - No zoom button needed — pinch gesture only.
 *  - The zoom origin is always the pinch midpoint so the strap stays under fingers.
 *  - Pan is allowed while zoomed in so the user can reach any part of the strap.
 *  - When at 1× scale, pan resets to (0,0) so the strap snaps back to its natural position.
 *
 * Usage: wrap each strap wrapper FrameLayout in this in the XML, or (simpler)
 * apply the zoom transform to the existing wrapper from the activity.
 *
 * This class is self-contained — just drop it in and wrap the strap wrappers.
 */
public class LanyardZoomWrapper extends FrameLayout {

    private static final float MIN_SCALE = 1.0f;   // zoom-out limit = original view
    private static final float MAX_SCALE = 4.0f;   // zoom-in max

    private float scale    = 1.0f;
    private float transX   = 0f;
    private float transY   = 0f;

    // For pan gesture tracking
    private float lastPanX = 0f;
    private float lastPanY = 0f;
    private boolean isPanning = false;
    private int activePanPointerId = -1;

    private ScaleGestureDetector scaleDetector;
    private boolean isScaling = false;

    public LanyardZoomWrapper(Context context) {
        super(context);
        init(context);
    }

    public LanyardZoomWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LanyardZoomWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);  // we override dispatchDraw to apply transform
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    // ── Touch handling ────────────────────────────────────────────────────────

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Intercept only when we need to handle scaling or panning ourselves.
        // Pass single-finger taps straight through to children (for element selection).
        return ev.getPointerCount() > 1;  // intercept multi-finger for zoom
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!isScaling) {
                    activePanPointerId = event.getPointerId(0);
                    lastPanX = event.getX();
                    lastPanY = event.getY();
                    isPanning = false;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger down — stop pan, start scale
                isPanning = false;
                isScaling = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isScaling && event.getPointerCount() == 1) {
                    int idx = event.findPointerIndex(activePanPointerId);
                    if (idx >= 0 && scale > MIN_SCALE) {
                        float dx = event.getX(idx) - lastPanX;
                        float dy = event.getY(idx) - lastPanY;
                        isPanning = true;
                        applyPan(dx, dy);
                        lastPanX = event.getX(idx);
                        lastPanY = event.getY(idx);
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // One finger lifted during scale — re-anchor pan to remaining finger
                int upIdx = event.getActionIndex();
                int upId  = event.getPointerId(upIdx);
                if (upId == activePanPointerId) {
                    // Switch tracking to the other pointer
                    int newIdx = (upIdx == 0) ? 1 : 0;
                    activePanPointerId = event.getPointerId(newIdx);
                    lastPanX = event.getX(newIdx);
                    lastPanY = event.getY(newIdx);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isScaling = false;
                isPanning = false;
                activePanPointerId = -1;
                // Snap translation back to (0,0) if scale is at minimum
                if (scale <= MIN_SCALE) {
                    transX = 0f;
                    transY = 0f;
                    invalidate();
                }
                break;
        }

        return true;
    }

    private void applyPan(float dx, float dy) {
        transX += dx;
        transY += dy;
        clampTranslation();
        invalidate();
    }

    private void clampTranslation() {
        // Don't let the content pan so far that empty space shows inside the view
        float w = getWidth();
        float h = getHeight();
        float scaledW = w * scale;
        float scaledH = h * scale;
        float maxTx = (scaledW - w) / 2f;
        float maxTy = (scaledH - h) / 2f;
        transX = Math.max(-maxTx, Math.min(maxTx, transX));
        transY = Math.max(-maxTy, Math.min(maxTy, transY));
    }

    // ── Draw with transform ───────────────────────────────────────────────────

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        // Apply scale around the center of the view, then translation
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        canvas.translate(cx + transX, cy + transY);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    // ── Touch coordinate mapping for children ─────────────────────────────────

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Transform touch coordinates into child space before dispatching
        // so element selection/drag in LanyardCanvasView works correctly.
        MotionEvent transformed = transformEvent(ev);
        boolean handled = super.dispatchTouchEvent(transformed);
        transformed.recycle();
        return handled;
    }

    private MotionEvent transformEvent(MotionEvent ev) {
        MotionEvent copy = MotionEvent.obtain(ev);
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        // Reverse the transform: screen → child
        // child_x = (screen_x - cx - transX) / scale + cx
        int count = copy.getPointerCount();
        float[] x = new float[count];
        float[] y = new float[count];
        for (int i = 0; i < count; i++) {
            x[i] = (copy.getX(i) - cx - transX) / scale + cx;
            y[i] = (copy.getY(i) - cy - transY) / scale + cy;
        }
        // Build a new MotionEvent with transformed coordinates
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[count];
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[count];
        for (int i = 0; i < count; i++) {
            coords[i] = new MotionEvent.PointerCoords();
            copy.getPointerCoords(i, coords[i]);
            coords[i].x = x[i];
            coords[i].y = y[i];
            props[i] = new MotionEvent.PointerProperties();
            copy.getPointerProperties(i, props[i]);
        }
        copy.recycle();
        return MotionEvent.obtain(
                ev.getDownTime(), ev.getEventTime(), ev.getAction(),
                count, props, coords,
                ev.getMetaState(), ev.getButtonState(),
                ev.getXPrecision(), ev.getYPrecision(),
                ev.getDeviceId(), ev.getEdgeFlags(),
                ev.getSource(), ev.getFlags()
        );
    }

    // ── Scale gesture ─────────────────────────────────────────────────────────

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scale * detector.getScaleFactor();
            newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

            // Scale around the pinch focus point
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float cx = getWidth()  / 2f;
            float cy = getHeight() / 2f;

            // Adjust translation so the focus point stays fixed on screen
            float scaleDelta = newScale / scale;
            transX = focusX - cx + (transX + cx - focusX) * scaleDelta;
            transY = focusY - cy + (transY + cy - focusY) * scaleDelta;

            scale = newScale;

            // Reset translation when back to 1×
            if (scale <= MIN_SCALE) {
                scale = MIN_SCALE;
                transX = 0f;
                transY = 0f;
            } else {
                clampTranslation();
            }

            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
        }
    }

    /** Reset zoom and pan to the original 1× state. */
    public void resetZoom() {
        scale  = MIN_SCALE;
        transX = 0f;
        transY = 0f;
        invalidate();
    }
}