package com.example.lacetastic;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

public class ColorPickerDialog extends AlertDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final OnColorSelectedListener listener;
    private final int initialColor;

    public ColorPickerDialog(Context context, int initialColor, OnColorSelectedListener listener) {
        super(context);
        this.initialColor = initialColor;
        this.listener = listener;

        setTitle("Pick a Color");
        View view = LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, null);
        // Simple implementation: grid of colors
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setPadding(20, 20, 20, 20);

        int[] colors = {
                Color.BLACK, Color.DKGRAY, Color.GRAY, Color.LTGRAY, Color.WHITE,
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
                0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3
        };

        for (int c : colors) {
            View colorView = new View(context);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 120;
            params.height = 120;
            params.setMargins(10, 10, 10, 10);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(c);
            colorView.setOnClickListener(v -> {
                if (listener != null) listener.onColorSelected(c);
                dismiss();
            });
            grid.addView(colorView);
        }
        setView(grid);
    }
}
