package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * LanyardStrapView — draws a single strap (LEFT, MIDDLE, or RIGHT).
 *
 * Geometry notes (all ratios mirror LanyardPreviewActivity 1-to-1):
 *
 *  LEFT / RIGHT
 *    strapW = w * 0.15,  strapH = h * 0.26
 *    lrTop  = cy - strapH * 1.58,  lrBot = lrTop + strapH * 3.11
 *    rotated ±13° around the rect centre
 *
 *  MIDDLE
 *    midTop = h/2 - midH/2,  midH = w * 0.13
 *    padH = w*0.18, bottomExtra = w*0.04, topInset = (w-2*padH)*0.18
 *
 * captureBitmap() is intentionally kept but the PRIMARY composite path
 * is now syncStrapToHolder() in LanyardCustomizationActivity which
 * composites BOTH this view and the LanyardCanvasView at the correct size.
 */
public class LanyardStrapView extends View {

    public enum StrapType { LEFT, MIDDLE, RIGHT }

    private StrapType strapType  = StrapType.LEFT;
    private final Paint strapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int strapColor = 0xFF333333;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LanyardStrapView(Context context) {
        super(context);
        init();
    }

    public LanyardStrapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LanyardStrapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strapPaint.setStyle(Paint.Style.FILL);
        strapPaint.setColor(strapColor);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        strapPaint.setShadowLayer(4f, 0f, 2f, Color.argb(60, 0, 0, 0));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setStrapType(StrapType type) {
        this.strapType = type;
        invalidate();
    }

    public void setStrapColor(int color) {
        this.strapColor = color;
        strapPaint.setColor(color);
        strapPaint.setShadowLayer(4f, 0f, 2f, Color.argb(60, 0, 0, 0));
        invalidate();
    }

    public int getStrapColor() {
        return strapColor;
    }

    /**
     * Renders this strap view into a Bitmap (colour only, no elements).
     * Use syncStrapToHolder() in LanyardCustomizationActivity for the
     * full composite (colour + elements).
     */
    public Bitmap captureBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            int fallback = 512;
            measure(MeasureSpec.makeMeasureSpec(fallback, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(fallback, MeasureSpec.EXACTLY));
            layout(0, 0, fallback, fallback);
            w = fallback;
            h = fallback;
        }

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(android.graphics.Color.TRANSPARENT,
                android.graphics.PorterDuff.Mode.CLEAR);
        draw(c);
        return bmp;
    }

    // ── Measurement ───────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (strapType == StrapType.MIDDLE) {
            int w = getMeasuredWidth();
            int h = (int) (w * 0.13f + 0.5f) + 8; // trapezoid height + small safety margin
            if (h < 1) h = 1;
            setMeasuredDimension(w, h);
        }
    }

    // ── Draw dispatch ─────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        strapPaint.setColor(strapColor);

        switch (strapType) {
            case LEFT:   drawLeftStrap(canvas, w, h);   break;
            case MIDDLE: drawMiddleStrap(canvas, w, h); break;
            case RIGHT:  drawRightStrap(canvas, w, h);  break;
        }
    }

    // ── LEFT ──────────────────────────────────────────────────────────────────

    private void drawLeftStrap(Canvas canvas, int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;

        float strapW = w * 0.15f;
        float strapH = h * 0.26f;

        float lrTop = cy - strapH * 1.58f;
        float lrBot = lrTop + strapH * 3.11f;

        float rectLeft  = cx - strapW / 2f;
        float rectRight = cx + strapW / 2f;

        float pivotX = cx;
        float pivotY = (lrTop + lrBot) / 2f;

        canvas.save();
        canvas.rotate(-13f, pivotX, pivotY);
        canvas.drawRect(rectLeft, lrTop, rectRight, lrBot, strapPaint);
        canvas.restore();
    }

    // ── MIDDLE ────────────────────────────────────────────────────────────────

    private void drawMiddleStrap(Canvas canvas, int w, int h) {
        float midH   = w * 0.13f;
        float midTop = h / 2f - midH / 2f;
        float midBot = midTop + midH;

        float padH        = w * 0.18f;
        float bottomExtra = w * 0.04f;

        float botLeft  = padH - bottomExtra;
        float botRight = w - padH + bottomExtra;

        float topInset = (w - 2 * padH) * 0.18f;
        float tlX      = padH + topInset;
        float trX      = w - padH - topInset;

        Path path = new Path();
        path.moveTo(botLeft,  midBot);
        path.lineTo(botRight, midBot);
        path.lineTo(trX,      midTop);
        path.lineTo(tlX,      midTop);
        path.close();

        canvas.drawPath(path, strapPaint);
    }

    // ── RIGHT ─────────────────────────────────────────────────────────────────

    private void drawRightStrap(Canvas canvas, int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;

        float strapW = w * 0.15f;
        float strapH = h * 0.26f;

        float lrTop = cy - strapH * 1.58f;
        float lrBot = lrTop + strapH * 3.11f;

        float rectLeft  = cx - strapW / 2f;
        float rectRight = cx + strapW / 2f;

        float pivotX = cx;
        float pivotY = (lrTop + lrBot) / 2f;

        canvas.save();
        canvas.rotate(13f, pivotX, pivotY);
        canvas.drawRect(rectLeft, lrTop, rectRight, lrBot, strapPaint);
        canvas.restore();
    }
}