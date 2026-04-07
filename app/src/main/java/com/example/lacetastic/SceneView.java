package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class SceneView extends AppCompatImageView {

    public SceneView(Context context) {
        super(context);
    }

    public SceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SceneView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setupLanyard(Context context) {
        setScaleType(ScaleType.FIT_CENTER);
    }

    public Object getLanyardRenderer() {
        return null;
    }

    public void setDesignImage(Bitmap bitmap) {
        setImageBitmap(bitmap);
    }
}
