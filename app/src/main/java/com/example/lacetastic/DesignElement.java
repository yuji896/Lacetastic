package com.example.lacetastic;

import android.graphics.Bitmap;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class DesignElement {

    public enum ElementType {
        TEXT, IMAGE
    }

    private ElementType type;
    private float x, y;
    private float scale = 1.0f;
    private float rotation = 0f;
    private int opacity = 255;
    private boolean flipHorizontal = false;
    private boolean flipVertical = false;
    private float displayWidth  = -1f;
    private float displayHeight = -1f;
    private String text = "";
    private Paint textPaint;
    private float textSize = 40f;
    private int textColor = 0xFF000000;
    private Typeface typeface = Typeface.DEFAULT;
    private int fontIndex = 0;
    private boolean bold = false;
    private boolean italic = false;
    private boolean underline = false;
    private Paint.Align alignment = Paint.Align.CENTER;
    private float letterSpacing = 0f;
    private float lineSpacing = 1.0f;
    private boolean isVertical = false;
    private Bitmap imageBitmap;
    private Matrix imageMatrix;
    private ColorMatrixColorFilter colorFilter;
    private Rect bounds;

    public DesignElement(ElementType type) {
        this.type = type;
        this.bounds = new Rect();
        if (type == ElementType.TEXT) {
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            updateTextPaint();
        } else {
            imageMatrix = new Matrix();
        }
    }

    public void updateTextPaint() {
        if (textPaint == null) return;
        textPaint.setColor(textColor);
        textPaint.setAlpha(opacity);
        textPaint.setTextAlign(alignment);
        textPaint.setTextSize(textSize * scale);
        textPaint.setUnderlineText(underline);
        int style = Typeface.NORMAL;
        if (bold && italic) style = Typeface.BOLD_ITALIC;
        else if (bold)      style = Typeface.BOLD;
        else if (italic)    style = Typeface.ITALIC;
        Typeface baseTypeface = (typeface != null) ? typeface : Typeface.DEFAULT;
        textPaint.setTypeface(Typeface.create(baseTypeface, style));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            textPaint.setLetterSpacing(letterSpacing / 100f);
        }
    }

    public float getDisplayWidth() {
        if (type == ElementType.TEXT) {
            String t = (text == null || text.isEmpty()) ? "Text" : text;
            if (isVertical) return textPaint.getTextSize();
            float maxW = 0;
            String[] lines = t.split("\n");
            for (String line : lines) maxW = Math.max(maxW, textPaint.measureText(line));
            return maxW;
        } else {
            if (displayWidth > 0) return displayWidth;
            if (imageBitmap != null) return imageBitmap.getWidth() * scale;
        }
        return 100f * scale;
    }

    public float getDisplayHeight() {
        if (type == ElementType.TEXT) {
            String t = (text == null || text.isEmpty()) ? "Text" : text;
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float h = fm.descent - fm.ascent;
            int lines = isVertical ? t.length() : t.split("\n").length;
            return lines * h * lineSpacing;
        } else {
            if (displayHeight > 0) return displayHeight;
            if (imageBitmap != null) return imageBitmap.getHeight() * scale;
        }
        return 100f * scale;
    }

    public void setResizeDimensions(float newW, float newH, int handleIndex) {
        if (type == ElementType.TEXT) {
            float oldW = getDisplayWidth(), oldH = getDisplayHeight();
            if (oldW <= 0 || oldH <= 0) return;
            float factorW = newW / oldW, factorH = newH / oldH;
            this.scale *= (Math.abs(factorW - 1) > Math.abs(factorH - 1)) ? factorW : factorH;
            updateTextPaint();
        } else {
            this.displayWidth = Math.max(20f, newW);
            this.displayHeight = Math.max(20f, newH);
        }
    }

    public void setDisplayWidth(float w) {
        if (type == ElementType.IMAGE) this.displayWidth = Math.max(20f, w);
        else setResizeDimensions(w, getDisplayHeight(), 4);
    }
    public void setDisplayHeight(float h) {
        if (type == ElementType.IMAGE) this.displayHeight = Math.max(20f, h);
        else setResizeDimensions(getDisplayWidth(), h, 6);
    }

    public DesignElement clone() {
        DesignElement cloned = new DesignElement(this.type);
        cloned.x = this.x; cloned.y = this.y;
        cloned.scale = this.scale; cloned.rotation = this.rotation;
        cloned.opacity = this.opacity; cloned.flipHorizontal = this.flipHorizontal;
        cloned.flipVertical = this.flipVertical; cloned.displayWidth  = this.displayWidth;
        cloned.displayHeight = this.displayHeight;
        if (this.type == ElementType.TEXT) {
            cloned.text = this.text; cloned.textSize = this.textSize;
            cloned.textColor = this.textColor; cloned.typeface = this.typeface;
            cloned.fontIndex = this.fontIndex; cloned.bold = this.bold;
            cloned.italic = this.italic; cloned.underline = this.underline;
            cloned.alignment = this.alignment; cloned.letterSpacing = this.letterSpacing;
            cloned.lineSpacing = this.lineSpacing; cloned.isVertical = this.isVertical;
            cloned.updateTextPaint();
        } else {
            cloned.imageBitmap = this.imageBitmap; cloned.colorFilter = this.colorFilter;
            cloned.imageMatrix = new Matrix(this.imageMatrix);
        }
        return cloned;
    }

    public ElementType getType() { return type; }
    public float getX() { return x; }
    public void  setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void  setY(float y) { this.y = y; }
    public float getScale() { return scale; }
    public void  setScale(float scale) {
        this.scale = scale; this.displayWidth = -1; this.displayHeight = -1;
        if (type == ElementType.TEXT) updateTextPaint();
    }
    public float getRotation() { return rotation; }
    public void  setRotation(float rotation) { this.rotation = rotation; }
    public int   getOpacity() { return opacity; }
    public void  setOpacity(int opacity) {
        this.opacity = opacity;
        if (type == ElementType.TEXT) updateTextPaint();
    }
    public boolean isFlipHorizontal() { return flipHorizontal; }
    public void    setFlipHorizontal(boolean v) { this.flipHorizontal = v; }
    public boolean isFlipVertical() { return flipVertical; }
    public void    setFlipVertical(boolean v) { this.flipVertical = v; }
    public String getText() { return text; }
    public void   setText(String text) { this.text = text; }
    public Paint  getTextPaint() { return textPaint; }
    public float  getTextSize() { return textSize; }
    public void   setTextSize(float textSize) { this.textSize = textSize; updateTextPaint(); }
    public int    getTextColor() { return textColor; }
    public void   setTextColor(int textColor) { this.textColor = textColor; updateTextPaint(); }
    public boolean isBold() { return bold; }
    public void    setBold(boolean bold) { this.bold = bold; updateTextPaint(); }
    public boolean isItalic() { return italic; }
    public void    setItalic(boolean italic) { this.italic = italic; updateTextPaint(); }
    public boolean isUnderline() { return underline; }
    public void    setUnderline(boolean underline) { this.underline = underline; updateTextPaint(); }
    public Typeface getTypeface() { return typeface; }
    public void    setTypeface(Typeface tf) { this.typeface = tf; updateTextPaint(); }
    public int     getFontIndex() { return fontIndex; }
    public void    setFontIndex(int i) { this.fontIndex = i; }
    public Paint.Align getAlignment() { return alignment; }
    public void    setAlignment(Paint.Align a) { this.alignment = a; updateTextPaint(); }
    public float   getLetterSpacing() { return letterSpacing; }
    public void    setLetterSpacing(float ls) { this.letterSpacing = ls; updateTextPaint(); }
    public float   getLineSpacing() { return lineSpacing; }
    public void    setLineSpacing(float ls) { this.lineSpacing = ls; }
    public boolean isVertical() { return isVertical; }
    public void    setVertical(boolean v) { this.isVertical = v; }
    public Bitmap getImageBitmap() { return imageBitmap; }
    public void   setImageBitmap(Bitmap bmp) { this.imageBitmap = bmp; this.displayWidth = -1; this.displayHeight = -1; }
    public Matrix getImageMatrix() { return imageMatrix; }
    public ColorMatrixColorFilter getColorFilter() { return colorFilter; }
    public void   setColorFilter(ColorMatrixColorFilter f) { this.colorFilter = f; }
    public void updateImageMatrix() {
        if (imageMatrix == null || imageBitmap == null) return;
        float bw = imageBitmap.getWidth(), bh = imageBitmap.getHeight();
        float sx = (displayWidth > 0) ? displayWidth / bw : scale, sy = (displayHeight > 0) ? displayHeight / bh : scale;
        imageMatrix.reset(); imageMatrix.postTranslate(-bw / 2f, -bh / 2f);
        imageMatrix.postScale(flipHorizontal ? -sx : sx, flipVertical ? -sy : sy);
        imageMatrix.postRotate(rotation); imageMatrix.postTranslate(x, y);
    }
    public Rect getBounds() { return bounds; }
    public void setBounds(Rect b) { this.bounds = b; }
    public boolean contains(float tx, float ty) {
        if (bounds == null) return false;
        Rect r = new Rect(bounds); r.inset(-30, -30);
        return r.contains((int) tx, (int) ty);
    }
}
