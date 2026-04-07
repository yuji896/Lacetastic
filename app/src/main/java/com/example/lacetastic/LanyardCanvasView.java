package com.example.lacetastic;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

/**
 * LanyardCanvasView — transparent DesignCanvasView for lanyard editing only.
 *
 * Two differences from the base DesignCanvasView:
 *
 * 1. isFreePositioning() → true: drag/resize clamping disabled so elements
 *    can be placed freely on (and beyond) the strap canvas. Text can overlap
 *    the strap edges. This does NOT affect mug/t-shirt editors.
 *
 * 2. getClipPath() → returns the exact strap shape path for this strap type.
 *    IMAGE elements (images + shapes) are clipped to this path — the overlapping
 *    part is hidden. TEXT elements are never clipped (they can overflow freely).
 *
 * Geometry mirrors LanyardStrapView exactly so the clip is pixel-perfect.
 *
 * NOTE: This view must be MATCH_PARENT width AND height so that its coordinate
 * space matches the full editor canvas. syncStrapToHolder() in
 * LanyardCustomizationActivity handles the scale-down when compositing.
 */
public class LanyardCanvasView extends DesignCanvasView {

    public enum StrapType { LEFT, MIDDLE, RIGHT }

    private StrapType strapType = StrapType.MIDDLE;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LanyardCanvasView(Context context) {
        super(context);
        init();
    }

    public LanyardCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Must be transparent so the LanyardStrapView below shows through.
        setBackgroundColor(Color.TRANSPARENT);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setStrapType(StrapType type) {
        this.strapType = type;
        invalidate();
    }

    // ── Free positioning ──────────────────────────────────────────────────────

    @Override
    protected boolean isFreePositioning() {
        return true;
    }

    // ── Clip path ─────────────────────────────────────────────────────────────

    /**
     * Returns the clip path for IMAGE elements (text is never clipped).
     * The path is computed from the current view dimensions so it stays
     * correct on every layout change.
     */
    @Override
    protected Path getClipPath() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return null;

        switch (strapType) {
            case LEFT:   return leftStrapPath(w, h);
            case RIGHT:  return rightStrapPath(w, h);
            case MIDDLE: return middleStrapPath(w, h);
            default:     return null;
        }
    }

    // ── Left strap path: rotated rectangle -13° ───────────────────────────────

    private Path leftStrapPath(int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float strapW = w * 0.15f;
        float strapH = h * 0.26f;
        float lrTop  = cy - strapH * 1.58f;
        float lrBot  = lrTop + strapH * 3.11f;
        float left   = cx - strapW / 2f;
        float right  = cx + strapW / 2f;
        float pivotX = cx;
        float pivotY = (lrTop + lrBot) / 2f;

        Path path = new Path();
        path.addRect(left, lrTop, right, lrBot, Path.Direction.CW);

        Matrix m = new Matrix();
        m.postRotate(-13f, pivotX, pivotY);
        path.transform(m);
        return path;
    }

    // ── Right strap path: rotated rectangle +13° ─────────────────────────────

    private Path rightStrapPath(int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float strapW = w * 0.15f;
        float strapH = h * 0.26f;
        float lrTop  = cy - strapH * 1.58f;
        float lrBot  = lrTop + strapH * 3.11f;
        float left   = cx - strapW / 2f;
        float right  = cx + strapW / 2f;
        float pivotX = cx;
        float pivotY = (lrTop + lrBot) / 2f;

        Path path = new Path();
        path.addRect(left, lrTop, right, lrBot, Path.Direction.CW);

        Matrix m = new Matrix();
        m.postRotate(13f, pivotX, pivotY);
        path.transform(m);
        return path;
    }

    // ── Middle strap path: trapezoid ─────────────────────────────────────────

    private Path middleStrapPath(int w, int h) {
        float midH    = w * 0.13f;
        float midTop  = h / 2f - midH / 2f;
        float midBot  = midTop + midH;

        float padH        = w * 0.18f;
        float bottomExtra = w * 0.04f;
        float botLeft     = padH - bottomExtra;
        float botRight    = w - padH + bottomExtra;

        float topInset = (w - 2 * padH) * 0.18f;
        float tlX = padH + topInset;
        float trX = w - padH - topInset;

        Path path = new Path();
        path.moveTo(botLeft, midBot);
        path.lineTo(botRight, midBot);
        path.lineTo(trX,     midTop);
        path.lineTo(tlX,     midTop);
        path.close();
        return path;
    }
}