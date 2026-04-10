package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
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
    private final Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int strapColor = 0xFF333333;

    /**
     * When enabled, draws fixed template trim (gold–orange chevrons + diagonal word band)
     * like cooperative lanyard reference art. Not editable on-canvas — structure only.
     */
    private boolean templateStyleEnabled = false;

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

    public void setTemplateStyleEnabled(boolean enabled) {
        this.templateStyleEnabled = enabled;
        invalidate();
    }

    public boolean isTemplateStyleEnabled() {
        return templateStyleEnabled;
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
        if (templateStyleEnabled) {
            drawCooperativeTemplateOnVerticalStrap(canvas, rectLeft, lrTop, rectRight, lrBot);
        }
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
        if (templateStyleEnabled) {
            drawCooperativeTemplateOnMiddleStrap(canvas, w, h, path, midTop, midBot,
                    botLeft, botRight, tlX, trX);
        }
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
        if (templateStyleEnabled) {
            drawCooperativeTemplateOnVerticalStrap(canvas, rectLeft, lrTop, rectRight, lrBot);
        }
        canvas.restore();
    }

    // ── TBWSC-style template (fixed graphics on strap, not canvas elements) ─────

    private void drawCooperativeTemplateOnVerticalStrap(Canvas canvas,
            float rectLeft, float lrTop, float rectRight, float lrBot) {
        float strapH = lrBot - lrTop;
        float strapW = rectRight - rectLeft;
        if (strapH < 8 || strapW < 4) return;

        float bandH = Math.min(strapH * 0.13f, strapW * 2.2f);
        drawChevronBand(canvas, rectLeft, lrTop, rectRight, lrTop + bandH, true);
        drawChevronBand(canvas, rectLeft, lrBot - bandH, rectRight, lrBot, false);

        float midT = lrTop + strapH * 0.32f;
        float midB = lrTop + strapH * 0.68f;
        drawDiagonalWordBand(canvas, rectLeft + 1, midT, rectRight - 1, midB);
    }

    private void drawCooperativeTemplateOnMiddleStrap(Canvas canvas, int w, int h, Path strapPath,
            float midTop, float midBot, float botLeft, float botRight, float tlX, float trX) {
        canvas.save();
        canvas.clipPath(strapPath);
        int gold0 = 0xFFFFE082;
        int gold1 = 0xFFFF9800;
        float band = Math.max(4f, (midBot - midTop) * 0.35f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, midTop, w, midTop + band, gold0, gold1, Shader.TileMode.CLAMP));
        canvas.drawRect(0, midTop, w, midTop + band, p);
        p.setShader(new LinearGradient(0, midBot - band, w, midBot, gold1, gold0, Shader.TileMode.CLAMP));
        canvas.drawRect(0, midBot - band, w, midBot, p);
        float innerT = midTop + band * 0.8f;
        float innerB = midBot - band * 0.8f;
        float innerL = Math.min(tlX, botLeft) + 4;
        float innerR = Math.max(trX, botRight) - 4;
        if (innerB > innerT + 6) drawDiagonalWordBand(canvas, innerL, innerT, innerR, innerB);
        p.setShader(null);
        canvas.restore();
    }

    private void drawChevronBand(Canvas c, float l, float t, float r, float b, boolean pointsDown) {
        float h = b - t;
        float w = r - l;
        if (h < 2 || w < 2) return;
        decorPaint.setShader(new LinearGradient(l, t, r, t, 0xFFFFF59D, 0xFFFF9800, Shader.TileMode.CLAMP));
        decorPaint.setStyle(Paint.Style.FILL);
        float cy = (t + b) / 2f;
        float chevW = Math.min(w / 3.2f, h * 0.85f);
        float halfH = h * 0.38f;
        float gap = w * 0.06f;
        float startX = l + gap;
        for (int i = 0; i < 3; i++) {
            float cx = startX + i * (chevW + gap * 0.35f);
            Path ch = chevronPath(cx, cy, chevW * 0.45f, halfH, pointsDown);
            c.drawPath(ch, decorPaint);
        }
        decorPaint.setShader(null);
    }

    /** Single chevron: V opening toward +X (stacked >>>) */
    private Path chevronPath(float cx, float cy, float halfW, float halfH, boolean pointDown) {
        Path path = new Path();
        if (!pointDown) {
            path.moveTo(cx - halfW, cy - halfH);
            path.lineTo(cx + halfW, cy);
            path.lineTo(cx - halfW, cy + halfH);
        } else {
            path.moveTo(cx - halfW, cy + halfH);
            path.lineTo(cx + halfW, cy);
            path.lineTo(cx - halfW, cy - halfH);
        }
        path.close();
        return path;
    }

    private void drawDiagonalWordBand(Canvas c, float l, float t, float r, float b) {
        float bw = r - l;
        float bh = b - t;
        if (bw < 4 || bh < 4) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x5AFFE082);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(Math.max(7f, Math.min(bw, bh) * 0.11f));
        String word = "LACETASTIC";
        float tw = p.measureText(word + "   ");
        float th = p.getTextSize() * 1.75f;
        c.save();
        float cx = (l + r) / 2f;
        float cy = (t + b) / 2f;
        c.rotate(-52f, cx, cy);
        for (float y = t - bh; y < b + bh; y += th) {
            for (float x = l - bw; x < r + bw; x += tw) {
                c.drawText(word, x, y, p);
            }
        }
        c.restore();
    }
}