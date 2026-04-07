package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * LanyardPreviewView — custom View that composites the full lanyard preview.
 *
 * Renamed from "LanyardPreviewActivity" (misleading — it is NOT an Activity).
 * Reads colours and canvas references from LanyardDesignHolder and draws
 * all three straps (left, middle, right) with their design elements.
 *
 * Geometry is identical to LanyardStrapView and LanyardCanvasView so the
 * preview is pixel-perfect with the editor.
 *
 * Usage in XML:
 *   <com.example.lacetastic.LanyardPreviewView
 *       android:id="@+id/lanyardPreviewView"
 *       android:layout_width="0dp"
 *       android:layout_height="0dp" ... />
 */
public class LanyardPreviewView extends View {

    private final Paint strapPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint designPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private int colorLeft   = 0xFF333333;
    private int colorMiddle = 0xFF333333;
    private int colorRight  = 0xFF333333;

    public LanyardPreviewView(Context context) {
        super(context); init();
    }
    public LanyardPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public LanyardPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        strapPaint.setStyle(Paint.Style.FILL);
        strapPaint.setShadowLayer(4f, 0f, 2f, Color.argb(60, 0, 0, 0));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setColorLeft(int c)   { colorLeft   = c; invalidate(); }
    public void setColorMiddle(int c) { colorMiddle = c; invalidate(); }
    public void setColorRight(int c)  { colorRight  = c; invalidate(); }

    /** Called by PreviewActivity to pull current state from the static holder. */
    public void loadFromHolder() {
        colorLeft   = LanyardDesignHolder.colorLeft;
        colorMiddle = LanyardDesignHolder.colorMiddle;
        colorRight  = LanyardDesignHolder.colorRight;
        invalidate();
    }

    /** Renders the full preview to a Bitmap for saving / sharing. */
    public Bitmap captureBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            w = 1080; h = 1920;
            measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            layout(0, 0, w, h);
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        draw(c);
        return bmp;
    }

    // ── Canvas capture helper ─────────────────────────────────────────────────

    private Bitmap captureCanvas(LanyardCanvasView cv) {
        if (cv == null) return null;
        if (cv.getElements() == null || cv.getElements().isEmpty()) return null;
        int w = cv.getWidth();
        int h = cv.getHeight();
        if (w <= 0 || h <= 0) {
            w = 1080; h = 1920;
            cv.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            cv.layout(0, 0, w, h);
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        cv.draw(c);
        return bmp;
    }

    // ── onDraw ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;

        // Shared strap dimensions — mirrors LanyardStrapView left/right geometry exactly
        float strapW = w * 0.15f;
        float strapH = h * 0.26f;
        float lrTop  = cy - strapH * 1.69f;
        float lrBot  = lrTop + strapH * 3.11f;

        // ── LEFT STRAP ────────────────────────────────────────────────────────
        float leftOffsetX   = w * 0.067f;
        float leftRectLeft  = cx - strapW - leftOffsetX;
        float leftRectRight = cx - leftOffsetX;
        float leftPivotX    = (leftRectLeft + leftRectRight) / 2f;
        float leftPivotY    = (lrTop + lrBot) / 2f;

        canvas.save();
        canvas.rotate(-13f, leftPivotX, leftPivotY);
        strapPaint.setColor(colorLeft);
        canvas.drawRect(leftRectLeft, lrTop, leftRectRight, lrBot, strapPaint);
        canvas.restore();

        Bitmap bmpLeft = captureCanvas(LanyardDesignHolder.canvasLeft);
        if (bmpLeft != null) {
            Path leftClip = new Path();
            leftClip.addRect(leftRectLeft, lrTop, leftRectRight, lrBot, Path.Direction.CW);
            Matrix mLeft = new Matrix();
            mLeft.setRotate(-13f, leftPivotX, leftPivotY);
            leftClip.transform(mLeft);
            float strapCx = (leftRectLeft + leftRectRight) / 2f;
            float strapCy = (lrTop + lrBot) / 2f;
            canvas.save();
            canvas.clipPath(leftClip);
            canvas.drawBitmap(bmpLeft,
                    strapCx - bmpLeft.getWidth()  / 2f,
                    strapCy - bmpLeft.getHeight() / 2f,
                    designPaint);
            canvas.restore();
            bmpLeft.recycle();
        }

        // ── RIGHT STRAP ───────────────────────────────────────────────────────
        float rightOffsetX   = w * 0.067f;
        float rightRectLeft  = cx + rightOffsetX;
        float rightRectRight = cx + strapW + rightOffsetX;
        float rightPivotX    = (rightRectLeft + rightRectRight) / 2f;
        float rightPivotY    = (lrTop + lrBot) / 2f;

        canvas.save();
        canvas.rotate(13f, rightPivotX, rightPivotY);
        strapPaint.setColor(colorRight);
        canvas.drawRect(rightRectLeft, lrTop, rightRectRight, lrBot, strapPaint);
        canvas.restore();

        Bitmap bmpRight = captureCanvas(LanyardDesignHolder.canvasRight);
        if (bmpRight != null) {
            Path rightClip = new Path();
            rightClip.addRect(rightRectLeft, lrTop, rightRectRight, lrBot, Path.Direction.CW);
            Matrix mRight = new Matrix();
            mRight.setRotate(13f, rightPivotX, rightPivotY);
            rightClip.transform(mRight);
            float strapCx = (rightRectLeft + rightRectRight) / 2f;
            float strapCy = (lrTop + lrBot) / 2f;
            canvas.save();
            canvas.clipPath(rightClip);
            canvas.drawBitmap(bmpRight,
                    strapCx - bmpRight.getWidth()  / 2f,
                    strapCy - bmpRight.getHeight() / 2f,
                    designPaint);
            canvas.restore();
            bmpRight.recycle();
        }

        // ── MIDDLE STRAP ──────────────────────────────────────────────────────
        float midH   = w * 0.13f;
        float midTop = 0f;
        float midBot = midTop + midH;

        float padH        = w * 0.18f;
        float bottomExtra = w * 0.04f;
        float botLeft     = padH - bottomExtra;
        float botRight    = w - padH + bottomExtra;
        float topInset    = (w - 2 * padH) * 0.18f;
        float tlX         = padH + topInset;
        float trX         = w - padH - topInset;

        Path midPath = new Path();
        midPath.moveTo(botLeft, midBot);
        midPath.lineTo(botRight, midBot);
        midPath.lineTo(trX,     midTop);
        midPath.lineTo(tlX,     midTop);
        midPath.close();

        strapPaint.setColor(colorMiddle);
        canvas.drawPath(midPath, strapPaint);

        Bitmap bmpMiddle = captureCanvas(LanyardDesignHolder.canvasMiddle);
        if (bmpMiddle != null) {
            float trapCx = w / 2f;
            float trapCy = midTop + midH / 2f;
            canvas.save();
            canvas.clipPath(midPath);
            canvas.drawBitmap(bmpMiddle,
                    trapCx - bmpMiddle.getWidth()  / 2f,
                    trapCy - bmpMiddle.getHeight() / 2f,
                    designPaint);
            canvas.restore();
            bmpMiddle.recycle();
        }
    }
}