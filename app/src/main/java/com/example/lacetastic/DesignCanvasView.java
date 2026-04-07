package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class DesignCanvasView extends View {

    private List<DesignElement> elements;
    private DesignElement selectedElement;
    private Bitmap backgroundBitmap = null;
    private float lastTouchX, lastTouchY;
    private boolean isDragging  = false;
    private boolean isRotating  = false;
    private boolean isResizing  = false;
    private int activeHandleIndex = -1;
    private float resizeStartX, resizeStartY;
    private float resizeStartW, resizeStartH;
    private float resizeAnchorX, resizeAnchorY;

    private static final float TOUCH_TOLERANCE        = 50f;
    private static final float ROTATION_HANDLE_RADIUS = 25f;
    private static final float ROTATION_HANDLE_DIST   = 60f;
    private static final float DELETE_BUTTON_RADIUS   = 25f;
    private static final float RESIZE_HANDLE_RADIUS   = 18f;
    private static final float RESIZE_HANDLE_HIT      = 36f;

    private long  lastTapTime = 0;
    private static final long  DOUBLE_TAP_TIMEOUT = 300;
    private float lastTapX, lastTapY;
    private static final float DOUBLE_TAP_SLOP = 50f;

    private Stack<List<DesignElement>> undoStack;
    private Stack<List<DesignElement>> redoStack;
    private static final int MAX_HISTORY = 20;

    private OnElementSelectedListener     onElementSelectedListener;
    private OnElementDoubleTappedListener onElementDoubleTappedListener;
    private OnElementChangedListener      onElementChangedListener;

    public interface OnElementSelectedListener     { void onElementSelected(DesignElement e); }
    public interface OnElementDoubleTappedListener { void onElementDoubleTapped(DesignElement e); }
    public interface OnElementChangedListener      { void onElementChanged(); }

    public void setOnElementSelectedListener(OnElementSelectedListener l)         { this.onElementSelectedListener = l; }
    public void setOnElementDoubleTappedListener(OnElementDoubleTappedListener l) { this.onElementDoubleTappedListener = l; }
    public void setOnElementChangedListener(OnElementChangedListener l)           { this.onElementChangedListener = l; }

    private Paint selectionPaint;
    private Paint handleFillPaint;
    private Paint handleStrokePaint;
    private Paint rotationHandlePaint;
    private Paint rotationLinePaint;
    private Paint deleteButtonPaint;
    private Paint deleteIconPaint;

    private final float[] localHandleX = new float[8];
    private final float[] localHandleY = new float[8];
    private final float[] handleX = new float[8];
    private final float[] handleY = new float[8];

    private float rotationHandleX, rotationHandleY;
    private float deleteButtonX,   deleteButtonY;
    private float localRotHandleX, localRotHandleY;
    private float localDelHandleX, localDelHandleY;

    public DesignCanvasView(Context context)                     { super(context); init(); }
    public DesignCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        elements  = new ArrayList<>();
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        setBackgroundColor(Color.WHITE);

        selectionPaint = new Paint();
        selectionPaint.setColor(0xFFE91E63);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(2.5f);
        selectionPaint.setAntiAlias(true);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{12, 6}, 0));

        handleFillPaint = new Paint();
        handleFillPaint.setColor(Color.WHITE);
        handleFillPaint.setStyle(Paint.Style.FILL);
        handleFillPaint.setAntiAlias(true);

        handleStrokePaint = new Paint();
        handleStrokePaint.setColor(0xFFE91E63);
        handleStrokePaint.setStyle(Paint.Style.STROKE);
        handleStrokePaint.setStrokeWidth(2.5f);
        handleStrokePaint.setAntiAlias(true);

        rotationHandlePaint = new Paint();
        rotationHandlePaint.setColor(0xFF4CAF50);
        rotationHandlePaint.setStyle(Paint.Style.FILL);
        rotationHandlePaint.setAntiAlias(true);

        rotationLinePaint = new Paint();
        rotationLinePaint.setColor(0xFF4CAF50);
        rotationLinePaint.setStyle(Paint.Style.STROKE);
        rotationLinePaint.setStrokeWidth(2f);
        rotationLinePaint.setAntiAlias(true);

        deleteButtonPaint = new Paint();
        deleteButtonPaint.setColor(0xFFFF5252);
        deleteButtonPaint.setStyle(Paint.Style.FILL);
        deleteButtonPaint.setAntiAlias(true);

        deleteIconPaint = new Paint();
        deleteIconPaint.setColor(Color.WHITE);
        deleteIconPaint.setStyle(Paint.Style.STROKE);
        deleteIconPaint.setStrokeWidth(3f);
        deleteIconPaint.setAntiAlias(true);
    }

    public void setBackgroundBitmap(Bitmap bmp) {
        this.backgroundBitmap = bmp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            Rect dst = new Rect(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(backgroundBitmap, null, dst, bmpPaint);
        }
        for (DesignElement element : elements) {
            if (element.getType() == DesignElement.ElementType.TEXT)
                drawTextElement(canvas, element);
            else if (element.getType() == DesignElement.ElementType.IMAGE)
                drawImageElement(canvas, element);

            if (element == selectedElement) drawSelectionBox(canvas, element);
        }
    }

    private void drawTextElement(Canvas canvas, DesignElement el) {
        canvas.save();
        canvas.translate(el.getX(), el.getY());
        canvas.rotate(el.getRotation());
        if (el.isFlipHorizontal()) canvas.scale(-1, 1);
        if (el.isFlipVertical())   canvas.scale(1, -1);
        Paint paint = el.getTextPaint();
        String text = (el.getText() == null || el.getText().isEmpty()) ? "Text" : el.getText();
        float textY = 0;
        if (el.isVertical()) {
            for (int i = 0; i < text.length(); i++) {
                canvas.drawText(String.valueOf(text.charAt(i)), 0, textY, paint);
                textY += paint.getTextSize() * el.getLineSpacing();
            }
        } else {
            for (String line : text.split("\n")) {
                canvas.drawText(line, 0, textY, paint);
                textY += paint.getTextSize() * el.getLineSpacing();
            }
        }
        canvas.restore();
        updateTextBounds(el, text);
    }

    private void updateTextBounds(DesignElement el, String text) {
        Paint paint = el.getTextPaint();
        Rect tb = new Rect();
        if (el.isVertical()) {
            float w = paint.getTextSize();
            float h = text.length() * paint.getTextSize() * el.getLineSpacing();
            el.setBounds(new Rect(
                    (int)(el.getX() - w / 2 - TOUCH_TOLERANCE),
                    (int)(el.getY() - h / 2 - TOUCH_TOLERANCE),
                    (int)(el.getX() + w / 2 + TOUCH_TOLERANCE),
                    (int)(el.getY() + h / 2 + TOUCH_TOLERANCE)));
        } else {
            paint.getTextBounds(text, 0, text.length(), tb);
            float sw = tb.width()  * el.getScale();
            float sh = tb.height() * el.getScale();
            float offsetX = 0;
            if (el.getAlignment() == Paint.Align.CENTER)     offsetX = -sw / 2;
            else if (el.getAlignment() == Paint.Align.RIGHT) offsetX = -sw;
            el.setBounds(new Rect(
                    (int)(el.getX() + offsetX - TOUCH_TOLERANCE),
                    (int)(el.getY() - sh - TOUCH_TOLERANCE),
                    (int)(el.getX() + offsetX + sw + TOUCH_TOLERANCE),
                    (int)(el.getY() + TOUCH_TOLERANCE)));
        }
    }

    private void drawImageElement(Canvas canvas, DesignElement el) {
        Bitmap bmp = el.getImageBitmap();
        if (bmp == null) return;
        canvas.save();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setAlpha(el.getOpacity());
        if (el.getColorFilter() != null) paint.setColorFilter(el.getColorFilter());
        el.updateImageMatrix();
        canvas.drawBitmap(bmp, el.getImageMatrix(), paint);
        canvas.restore();
        updateImageBounds(el);
    }

    private void updateImageBounds(DesignElement el) {
        float hw = el.getDisplayWidth()  / 2f;
        float hh = el.getDisplayHeight() / 2f;
        el.setBounds(new Rect(
                (int)(el.getX() - hw - TOUCH_TOLERANCE),
                (int)(el.getY() - hh - TOUCH_TOLERANCE),
                (int)(el.getX() + hw + TOUCH_TOLERANCE),
                (int)(el.getY() + hh + TOUCH_TOLERANCE)));
    }

    private void drawSelectionBox(Canvas canvas, DesignElement el) {
        float w = el.getDisplayWidth();
        float h = el.getDisplayHeight();
        if (w <= 0 || h <= 0) return;
        float hw = w / 2f;
        float hh = h / 2f;
        canvas.save();
        canvas.translate(el.getX(), el.getY());
        canvas.rotate(el.getRotation());
        if (el.isFlipHorizontal()) canvas.scale(-1, 1);
        if (el.isFlipVertical())   canvas.scale(1, -1);
        canvas.drawRect(new RectF(-hw, -hh, hw, hh), selectionPaint);
        float[][] localPts = {
                {-hw, -hh}, {0, -hh}, {hw, -hh},
                {-hw,   0},           {hw,   0},
                {-hw,  hh}, {0,  hh}, {hw,  hh}
        };
        for (int i = 0; i < 8; i++) {
            localHandleX[i] = localPts[i][0];
            localHandleY[i] = localPts[i][1];
            canvas.drawCircle(localHandleX[i], localHandleY[i], RESIZE_HANDLE_RADIUS, handleFillPaint);
            canvas.drawCircle(localHandleX[i], localHandleY[i], RESIZE_HANDLE_RADIUS, handleStrokePaint);
        }
        localRotHandleX = 0;
        localRotHandleY = -hh - ROTATION_HANDLE_DIST;
        canvas.drawLine(0, -hh, localRotHandleX, localRotHandleY, rotationLinePaint);
        canvas.drawCircle(localRotHandleX, localRotHandleY, ROTATION_HANDLE_RADIUS, rotationHandlePaint);
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.WHITE);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(2.5f);
        float ri = 9f;
        canvas.drawArc(new RectF(localRotHandleX - ri, localRotHandleY - ri,
                localRotHandleX + ri, localRotHandleY + ri), -60, 300, false, iconPaint);
        canvas.drawLine(localRotHandleX + ri, localRotHandleY - 1,
                localRotHandleX + ri + 6, localRotHandleY - 6, iconPaint);
        canvas.drawLine(localRotHandleX + ri, localRotHandleY - 1,
                localRotHandleX + ri - 4, localRotHandleY - 6, iconPaint);
        localDelHandleX = hw + DELETE_BUTTON_RADIUS + 4;
        localDelHandleY = -hh - DELETE_BUTTON_RADIUS - 4;
        canvas.drawCircle(localDelHandleX, localDelHandleY, DELETE_BUTTON_RADIUS, deleteButtonPaint);
        float xs = 9f;
        canvas.drawLine(localDelHandleX - xs, localDelHandleY - xs,
                localDelHandleX + xs, localDelHandleY + xs, deleteIconPaint);
        canvas.drawLine(localDelHandleX + xs, localDelHandleY - xs,
                localDelHandleX - xs, localDelHandleY + xs, deleteIconPaint);
        canvas.restore();
        for (int i = 0; i < 8; i++) {
            float[] s = localToScreen(el, localHandleX[i], localHandleY[i]);
            handleX[i] = s[0];
            handleY[i] = s[1];
        }
        float[] sr = localToScreen(el, localRotHandleX, localRotHandleY);
        rotationHandleX = sr[0];
        rotationHandleY = sr[1];
        float[] sd = localToScreen(el, localDelHandleX, localDelHandleY);
        deleteButtonX = sd[0];
        deleteButtonY = sd[1];
    }

    private Matrix elementMatrix(DesignElement el) {
        Matrix m = new Matrix();
        m.postTranslate(-el.getX(), -el.getY());
        float sx = el.isFlipHorizontal() ? -1f : 1f;
        float sy = el.isFlipVertical()   ? -1f : 1f;
        m.postScale(sx, sy);
        m.postRotate(el.getRotation());
        m.postTranslate(el.getX(), el.getY());
        return m;
    }

    private float[] localToScreen(DesignElement el, float lx, float ly) {
        Matrix m = new Matrix();
        float sx = el.isFlipHorizontal() ? -1f : 1f;
        float sy = el.isFlipVertical()   ? -1f : 1f;
        m.postScale(sx, sy);
        m.postRotate(el.getRotation());
        m.postTranslate(el.getX(), el.getY());
        float[] pts = {lx, ly};
        m.mapPoints(pts);
        return pts;
    }

    private float[] screenToLocal(DesignElement el, float sx, float sy) {
        Matrix m = new Matrix();
        float flipX = el.isFlipHorizontal() ? -1f : 1f;
        float flipY = el.isFlipVertical()   ? -1f : 1f;
        m.postScale(flipX, flipY);
        m.postRotate(el.getRotation());
        m.postTranslate(el.getX(), el.getY());
        Matrix inv = new Matrix();
        m.invert(inv);
        float[] pts = {sx, sy};
        inv.mapPoints(pts);
        return pts;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (selectedElement != null && isTouchingDeleteButton(x, y)) {
                    removeElement(selectedElement);
                    selectedElement = null;
                    invalidate();
                    return true;
                }
                if (selectedElement != null) {
                    int hi = hitResizeHandle(x, y);
                    if (hi >= 0) {
                        isResizing = true;
                        activeHandleIndex = hi;
                        lastTouchX = x; lastTouchY = y;
                        resizeStartX = selectedElement.getX();
                        resizeStartY = selectedElement.getY();
                        resizeStartW = selectedElement.getDisplayWidth();
                        resizeStartH = selectedElement.getDisplayHeight();
                        int opposite = oppositeHandle(hi);
                        resizeAnchorX = handleX[opposite];
                        resizeAnchorY = handleY[opposite];
                        saveStateForUndo();
                        return true;
                    }
                }
                if (selectedElement != null && isTouchingRotationHandle(x, y)) {
                    isRotating = true;
                    lastTouchX = x; lastTouchY = y;
                    saveStateForUndo();
                    return true;
                }
                DesignElement touched = findElementAt(x, y);
                if (touched != null) {
                    long now = System.currentTimeMillis();
                    float dx = Math.abs(x - lastTapX);
                    float dy = Math.abs(y - lastTapY);
                    if (now - lastTapTime < DOUBLE_TAP_TIMEOUT
                            && dx < DOUBLE_TAP_SLOP && dy < DOUBLE_TAP_SLOP
                            && touched == selectedElement) {
                        if (onElementDoubleTappedListener != null)
                            onElementDoubleTappedListener.onElementDoubleTapped(touched);
                        return true;
                    }
                    lastTapTime = now; lastTapX = x; lastTapY = y;
                    selectedElement = touched;
                    isDragging = true;
                    lastTouchX = x; lastTouchY = y;
                    if (onElementSelectedListener != null)
                        onElementSelectedListener.onElementSelected(selectedElement);
                    invalidate();
                    return true;
                }
                selectedElement = null;
                if (onElementSelectedListener != null)
                    onElementSelectedListener.onElementSelected(null);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isResizing && selectedElement != null) {
                    handleResizeDrag(x, y);
                    lastTouchX = x; lastTouchY = y;
                    invalidate(); return true;
                }
                if (isRotating && selectedElement != null) {
                    float cx = selectedElement.getX();
                    float cy = selectedElement.getY();
                    float startAngle   = (float) Math.toDegrees(Math.atan2(lastTouchY - cy, lastTouchX - cx));
                    float currentAngle = (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
                    float delta = currentAngle - startAngle;
                    float newRot = (selectedElement.getRotation() + delta + 360) % 360;
                    selectedElement.setRotation(newRot);
                    lastTouchX = x; lastTouchY = y;
                    invalidate(); return true;
                }
                if (isDragging && selectedElement != null) {
                    float newX = selectedElement.getX() + (x - lastTouchX);
                    float newY = selectedElement.getY() + (y - lastTouchY);
                    float hw = selectedElement.getDisplayWidth()  / 2f;
                    float hh = selectedElement.getDisplayHeight() / 2f;
                    newX = Math.max(hw, Math.min(getWidth()  - hw, newX));
                    newY = Math.max(hh, Math.min(getHeight() - hh, newY));
                    selectedElement.setX(newX);
                    selectedElement.setY(newY);
                    lastTouchX = x; lastTouchY = y;
                    invalidate(); return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (isDragging || isRotating || isResizing) {
                    saveStateForUndo();
                    if (onElementChangedListener != null)
                        onElementChangedListener.onElementChanged();
                }
                isDragging = false; isRotating = false; isResizing = false;
                activeHandleIndex = -1;
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleResizeDrag(float touchX, float touchY) {
        int hi = activeHandleIndex;
        float ax = resizeAnchorX, ay = resizeAnchorY;
        float newW = resizeStartW, newH = resizeStartH;
        switch (hi) {
            case 0: newW = Math.abs(ax - touchX); newH = Math.abs(ay - touchY); break;
            case 1: newH = Math.abs(ay - touchY); break;
            case 2: newW = Math.abs(touchX - ax); newH = Math.abs(ay - touchY); break;
            case 3: newW = Math.abs(ax - touchX); break;
            case 4: newW = Math.abs(touchX - ax); break;
            case 5: newW = Math.abs(ax - touchX); newH = Math.abs(touchY - ay); break;
            case 6: newH = Math.abs(touchY - ay); break;
            case 7: newW = Math.abs(touchX - ax); newH = Math.abs(touchY - ay); break;
        }
        newW = Math.max(20f, newW);
        newH = Math.max(20f, newH);
        float newCX = resizeStartX, newCY = resizeStartY;
        switch (hi) {
            case 0: newCX = ax - newW / 2f; newCY = ay - newH / 2f; break;
            case 1: newCY = ay - newH / 2f; break;
            case 2: newCX = ax + newW / 2f; newCY = ay - newH / 2f; break;
            case 3: newCX = ax - newW / 2f; break;
            case 4: newCX = ax + newW / 2f; break;
            case 5: newCX = ax - newW / 2f; newCY = ay + newH / 2f; break;
            case 6: newCY = ay + newH / 2f; break;
            case 7: newCX = ax + newW / 2f; newCY = ay + newH / 2f; break;
        }
        newCX = Math.max(newW / 2f, Math.min(getWidth()  - newW / 2f, newCX));
        newCY = Math.max(newH / 2f, Math.min(getHeight() - newH / 2f, newCY));
        selectedElement.setX(newCX); selectedElement.setY(newCY);
        selectedElement.setDisplayWidth(newW); selectedElement.setDisplayHeight(newH);
    }

    private int hitResizeHandle(float x, float y) {
        for (int i = 0; i < 8; i++) {
            float dx = x - handleX[i], dy = y - handleY[i];
            if (dx * dx + dy * dy <= RESIZE_HANDLE_HIT * RESIZE_HANDLE_HIT) return i;
        }
        return -1;
    }

    private int oppositeHandle(int hi) {
        switch (hi) {
            case 0: return 7; case 1: return 6; case 2: return 5;
            case 3: return 4; case 4: return 3; case 5: return 2;
            case 6: return 1; case 7: return 0; default: return 7;
        }
    }

    private boolean isTouchingRotationHandle(float x, float y) {
        if (selectedElement == null) return false;
        float dx = x - rotationHandleX, dy = y - rotationHandleY;
        return dx * dx + dy * dy <= (ROTATION_HANDLE_RADIUS + 20) * (ROTATION_HANDLE_RADIUS + 20);
    }

    private boolean isTouchingDeleteButton(float x, float y) {
        if (selectedElement == null) return false;
        float dx = x - deleteButtonX, dy = y - deleteButtonY;
        return dx * dx + dy * dy <= (DELETE_BUTTON_RADIUS + 20) * (DELETE_BUTTON_RADIUS + 20);
    }

    private DesignElement findElementAt(float x, float y) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            if (elements.get(i).contains(x, y)) return elements.get(i);
        }
        return null;
    }

    private void saveStateForUndo() {
        List<DesignElement> state = new ArrayList<>();
        for (DesignElement e : elements) state.add(e.clone());
        undoStack.push(state);
        if (undoStack.size() > MAX_HISTORY) undoStack.remove(0);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        List<DesignElement> cur = new ArrayList<>();
        for (DesignElement e : elements) cur.add(e.clone());
        redoStack.push(cur);
        List<DesignElement> prev = undoStack.pop();
        elements.clear();
        for (DesignElement e : prev) elements.add(e.clone());
        selectedElement = null;
        invalidate();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        List<DesignElement> cur = new ArrayList<>();
        for (DesignElement e : elements) cur.add(e.clone());
        undoStack.push(cur);
        List<DesignElement> next = redoStack.pop();
        elements.clear();
        for (DesignElement e : next) elements.add(e.clone());
        selectedElement = null;
        invalidate();
    }

    public DesignElement addTextElement(String text) {
        saveStateForUndo();
        DesignElement el = new DesignElement(DesignElement.ElementType.TEXT);
        el.setText(text);
        el.setX(getWidth()  / 2f);
        el.setY(getHeight() / 2f + (elements.isEmpty() ? 0 : elements.size() * 30f));
        elements.add(el);
        selectedElement = el;
        invalidate();
        if (onElementSelectedListener != null) onElementSelectedListener.onElementSelected(el);
        return el;
    }

    public DesignElement addImageElement(Bitmap bmp) {
        saveStateForUndo();
        DesignElement el = new DesignElement(DesignElement.ElementType.IMAGE);
        int canvasW = getWidth();
        int canvasH = getHeight();
        if (canvasW <= 0) canvasW = 512;
        if (canvasH <= 0) canvasH = 512;
        float bmpW = bmp.getWidth();
        float bmpH = bmp.getHeight();
        float targetSize = canvasW * 0.28f;
        float longestSide = Math.max(bmpW, bmpH);
        float scaleFactor = (longestSide > 0) ? targetSize / longestSide : 1f;
        el.setImageBitmap(bmp);
        el.setDisplayWidth(bmpW * scaleFactor);
        el.setDisplayHeight(bmpH * scaleFactor);
        el.setX(canvasW / 2f);
        el.setY(canvasH / 2f);
        elements.add(el);
        selectedElement = el;
        invalidate();
        if (onElementSelectedListener != null) onElementSelectedListener.onElementSelected(el);
        return el;
    }

    public DesignElement getSelectedElement()                  { return selectedElement; }
    public void          setSelectedElement(DesignElement e)   { this.selectedElement = e; invalidate(); }
    public List<DesignElement> getElements()                   { return elements; }

    public void removeElement(DesignElement el) {
        saveStateForUndo();
        elements.remove(el);
        if (selectedElement == el) selectedElement = null;
        invalidate();
    }

    public void clearElements() {
        saveStateForUndo();
        elements.clear();
        selectedElement = null;
        invalidate();
    }

    public void bringToFront() {
        if (selectedElement == null) return;
        saveStateForUndo();
        int index = elements.indexOf(selectedElement);
        if (index < elements.size() - 1) { elements.remove(index); elements.add(selectedElement); invalidate(); }
    }

    public void sendToBack() {
        if (selectedElement == null) return;
        saveStateForUndo();
        int index = elements.indexOf(selectedElement);
        if (index > 0) { elements.remove(index); elements.add(0, selectedElement); invalidate(); }
    }

    public void moveForward() {
        if (selectedElement == null) return;
        saveStateForUndo();
        int index = elements.indexOf(selectedElement);
        if (index < elements.size() - 1) { Collections.swap(elements, index, index + 1); invalidate(); }
    }

    public void moveBackward() {
        if (selectedElement == null) return;
        saveStateForUndo();
        int index = elements.indexOf(selectedElement);
        if (index > 0) { Collections.swap(elements, index, index - 1); invalidate(); }
    }

    public void applyEffect(String name) {
        if (selectedElement != null && selectedElement.getType() == DesignElement.ElementType.IMAGE)
            applyEffectToElement(selectedElement, name);
    }

    public void applyEffectToElement(DesignElement el, String name) {
        if (el == null || el.getType() != DesignElement.ElementType.IMAGE) return;
        saveStateForUndo();
        ColorMatrix cm = new ColorMatrix();
        switch (name.toLowerCase().trim()) {
            case "original": el.setColorFilter(null); break;
            case "grayscale": cm.setSaturation(0); el.setColorFilter(new ColorMatrixColorFilter(cm)); break;
            case "sepia":
                cm.setSaturation(0);
                ColorMatrix sep = new ColorMatrix(new float[]{
                        .393f,.769f,.189f,0,0, .349f,.686f,.168f,0,0,
                        .272f,.534f,.131f,0,0, 0,0,0,1,0});
                cm.postConcat(sep);
                el.setColorFilter(new ColorMatrixColorFilter(cm));
                break;
            case "bright":
                cm.set(new float[]{1.5f,0,0,0,0, 0,1.5f,0,0,0, 0,0,1.5f,0,0, 0,0,0,1,0});
                el.setColorFilter(new ColorMatrixColorFilter(cm)); break;
            case "contrast":
                float c = 1.5f, t = (-.5f*c+.5f)*255f;
                cm.set(new float[]{c,0,0,0,t, 0,c,0,0,t, 0,0,c,0,t, 0,0,0,1,0});
                el.setColorFilter(new ColorMatrixColorFilter(cm)); break;
            case "vintage":
                cm.set(new float[]{.6f,.3f,.1f,0,0, .2f,.5f,.1f,0,0, .2f,.3f,.4f,0,0, 0,0,0,1,0});
                el.setColorFilter(new ColorMatrixColorFilter(cm)); break;
            default: el.setColorFilter(null); break;
        }
        invalidate();
    }

    private int bgColor = Color.WHITE;

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        bgColor = color;
    }

    public Bitmap captureBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            int fallback = 512;
            measure(MeasureSpec.makeMeasureSpec(fallback, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(fallback, MeasureSpec.EXACTLY));
            layout(0, 0, fallback, fallback);
            w = fallback; h = fallback;
        }
        DesignElement tmp = selectedElement;
        selectedElement = null;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(bgColor);
        if (backgroundBitmap != null) {
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            c.drawBitmap(backgroundBitmap, null, new Rect(0, 0, w, h), p);
        }
        for (DesignElement element : elements) {
            if (element.getType() == DesignElement.ElementType.TEXT)
                drawTextElement(c, element);
            else if (element.getType() == DesignElement.ElementType.IMAGE)
                drawImageElement(c, element);
        }
        selectedElement = tmp;
        return bmp;
    }
}
