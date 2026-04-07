package com.example.lacetastic;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CustomizationPage extends AppCompatActivity {

    private static final String KEY_AI_PROMPT     = "ai_prompt";
    private static final String KEY_AI_PANEL_OPEN = "ai_panel_open";

    private DesignCanvasView designCanvas;

    private ImageButton btnBack, btnUndo, btnRedo;
    private Button      btnPreview;

    private FrameLayout  aiPanelContainer;
    private EditText     aiPromptInput;
    private Button       aiGenerateBtn;
    private ProgressBar  aiProgress;
    private LinearLayout aiTemplateResults;
    private boolean      aiPanelOpen = false;
    private ImageButton  btnToggleAiFab;
    private ImageButton  btnToggleAiClose;

    private Button btnAddText;
    private Button btnAddSticker;

    private LinearLayout textPropertiesPanel;
    private Button btnBold, btnItalic;
    private Button btnAlignLeft, btnAlignCenter, btnAlignRight;
    private Button btnColorBlack, btnColorWhite, btnColorRed, btnColorBlue, btnColorYellow;
    private Button btnFontSmall, btnFontMedium, btnFontLarge;

    private LinearLayout tabText, tabElements, tabAI, tabBackground;
    private FrameLayout  editPanelContainer;

    private DesignElement currentSelectedElement = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customization_page);

        bindViews();
        loadLanyardBackground();
        setupListeners();
        setupTabBar();

        aiPanelContainer.setTranslationX(aiPanelContainer.getWidth());
        aiPanelContainer.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            aiPromptInput.setText(savedInstanceState.getString(KEY_AI_PROMPT, ""));
            if (savedInstanceState.getBoolean(KEY_AI_PANEL_OPEN, false)) {
                aiPanelContainer.setVisibility(View.VISIBLE);
                aiPanelContainer.setTranslationX(0f);
                aiPanelOpen = true;
            }
        }
    }

    private void bindViews() {
        designCanvas       = findViewById(R.id.designCanvas);

        btnBack            = findViewById(R.id.btnBack);
        btnUndo            = findViewById(R.id.btnUndo);
        btnRedo            = findViewById(R.id.btnRedo);
        btnPreview         = findViewById(R.id.btnPreview);

        aiPanelContainer   = findViewById(R.id.aiPanelContainer);
        aiPromptInput      = findViewById(R.id.aiPromptInput);
        aiGenerateBtn      = findViewById(R.id.aiGenerateBtn);
        aiProgress         = findViewById(R.id.aiProgress);
        aiTemplateResults  = findViewById(R.id.aiTemplateResults);
        btnToggleAiFab     = findViewById(R.id.btnToggleAiFab);
        btnToggleAiClose   = findViewById(R.id.btnToggleAi);

        btnAddText         = findViewById(R.id.btnAddText);
        btnAddSticker      = findViewById(R.id.btnAddSticker);

        textPropertiesPanel = findViewById(R.id.textPropertiesPanel);
        btnBold            = findViewById(R.id.btnBold);
        btnItalic          = findViewById(R.id.btnItalic);
        btnAlignLeft       = findViewById(R.id.btnAlignLeft);
        btnAlignCenter     = findViewById(R.id.btnAlignCenter);
        btnAlignRight      = findViewById(R.id.btnAlignRight);
        btnColorBlack      = findViewById(R.id.btnColorBlack);
        btnColorWhite      = findViewById(R.id.btnColorWhite);
        btnColorRed        = findViewById(R.id.btnColorRed);
        btnColorBlue       = findViewById(R.id.btnColorBlue);
        btnColorYellow     = findViewById(R.id.btnColorYellow);
        btnFontSmall       = findViewById(R.id.btnFontSmall);
        btnFontMedium      = findViewById(R.id.btnFontMedium);
        btnFontLarge       = findViewById(R.id.btnFontLarge);

        tabText            = findViewById(R.id.tabText);
        tabElements        = findViewById(R.id.tabElements);
        tabAI              = findViewById(R.id.tabAI);
        tabBackground      = findViewById(R.id.tabBackground);
        editPanelContainer = findViewById(R.id.editPanelContainer);

        textPropertiesPanel.setVisibility(View.GONE);
    }

    private void loadLanyardBackground() {
        try {
            InputStream is = getAssets().open("plain_lanyard.png");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp != null) {
                designCanvas.setBackgroundBitmap(bmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTabBar() {
        tabText.setOnClickListener(v -> {
            showEditPanel(textPropertiesPanel);
            DesignElement el = designCanvas.addTextElement("Your Text");
            currentSelectedElement = el;
        });

        tabElements.setOnClickListener(v -> {
            hideEditPanel();
            showStickerPicker();
        });

        tabAI.setOnClickListener(v -> toggleAiPanel());

        tabBackground.setOnClickListener(v -> {
            hideEditPanel();
            Toast.makeText(this, "Background colour — coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showEditPanel(View panelToShow) {
        for (int i = 0; i < editPanelContainer.getChildCount(); i++) {
            editPanelContainer.getChildAt(i).setVisibility(View.GONE);
        }
        panelToShow.setVisibility(View.VISIBLE);
    }

    private void hideEditPanel() {
        for (int i = 0; i < editPanelContainer.getChildCount(); i++) {
            editPanelContainer.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnUndo.setOnClickListener(v -> designCanvas.undo());
        btnRedo.setOnClickListener(v -> designCanvas.redo());
        btnPreview.setOnClickListener(v -> openPreview());

        btnToggleAiFab.setOnClickListener(v -> toggleAiPanel());
        btnToggleAiClose.setOnClickListener(v -> toggleAiPanel());
        aiGenerateBtn.setOnClickListener(v -> runAiGeneration());

        designCanvas.setOnElementSelectedListener(element -> {
            currentSelectedElement = element;
            boolean isText = element != null
                    && element.getType() == DesignElement.ElementType.TEXT;
            if (isText) showEditPanel(textPropertiesPanel);
            else        hideEditPanel();
        });

        designCanvas.setOnElementDoubleTappedListener(element -> {
            if (element != null && element.getType() == DesignElement.ElementType.TEXT) {
                showTextEditDialog(element);
            }
        });

        btnBold.setOnClickListener(v        -> applyTextStyle("bold"));
        btnItalic.setOnClickListener(v      -> applyTextStyle("italic"));
        btnAlignLeft.setOnClickListener(v   -> applyTextAlign("left"));
        btnAlignCenter.setOnClickListener(v -> applyTextAlign("center"));
        btnAlignRight.setOnClickListener(v  -> applyTextAlign("right"));
        btnColorBlack.setOnClickListener(v  -> applyTextColor(Color.BLACK));
        btnColorWhite.setOnClickListener(v  -> applyTextColor(Color.WHITE));
        btnColorRed.setOnClickListener(v    -> applyTextColor(Color.RED));
        btnColorBlue.setOnClickListener(v   -> applyTextColor(Color.BLUE));
        btnColorYellow.setOnClickListener(v -> applyTextColor(Color.YELLOW));
        btnFontSmall.setOnClickListener(v   -> applyFontSize(24f));
        btnFontMedium.setOnClickListener(v  -> applyFontSize(40f));
        btnFontLarge.setOnClickListener(v   -> applyFontSize(60f));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (aiPromptInput != null) {
            outState.putString(KEY_AI_PROMPT,      aiPromptInput.getText().toString());
            outState.putBoolean(KEY_AI_PANEL_OPEN, aiPanelOpen);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (aiPanelContainer == null) return;
        int w = getResources().getDisplayMetrics().widthPixels;
        aiPanelContainer.getLayoutParams().width =
                (int)(w * (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 0.45f : 0.80f));
        aiPanelContainer.requestLayout();
    }

    private void toggleAiPanel() {
        if (aiPanelOpen) {
            aiPanelContainer.post(() -> {
                ObjectAnimator anim = ObjectAnimator.ofFloat(
                        aiPanelContainer, "translationX", 0f, aiPanelContainer.getWidth());
                anim.setDuration(280);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
                anim.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator animation) {
                        aiPanelContainer.setVisibility(View.GONE);
                    }
                });
            });
        } else {
            aiPanelContainer.setVisibility(View.VISIBLE);
            aiPanelContainer.post(() -> {
                aiPanelContainer.setTranslationX(aiPanelContainer.getWidth());
                ObjectAnimator anim = ObjectAnimator.ofFloat(
                        aiPanelContainer, "translationX", aiPanelContainer.getWidth(), 0f);
                anim.setDuration(280);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
            });
        }
        aiPanelOpen = !aiPanelOpen;
        btnToggleAiFab.setAlpha(aiPanelOpen ? 1.0f : 0.85f);
    }

    private void runAiGeneration() {
        String prompt = aiPromptInput.getText().toString().trim();
        if (prompt.isEmpty()) prompt = "floral tropical pattern, vibrant colors";
        aiProgress.setVisibility(View.VISIBLE);
        aiGenerateBtn.setEnabled(false);
        aiGenerateBtn.setText("Generating…");
        final String finalPrompt = prompt;
        LanyardApiService.generateLanyardDesign(this, finalPrompt,
                new LanyardApiService.LanyardCallback() {
                    @Override public void onSuccess(byte[] imageBytes) {
                        Bitmap full = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        runOnUiThread(() -> {
                            aiProgress.setVisibility(View.GONE);
                            aiGenerateBtn.setEnabled(true);
                            aiGenerateBtn.setText("Generate");
                            if (full == null) { Toast.makeText(CustomizationPage.this, "Could not decode image", Toast.LENGTH_SHORT).show(); return; }
                            designCanvas.addImageElement(full);
                            addThumbnailToPanel(full);
                            Toast.makeText(CustomizationPage.this, "Template added!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onError(String message) {
                        runOnUiThread(() -> {
                            aiProgress.setVisibility(View.GONE);
                            aiGenerateBtn.setEnabled(true);
                            aiGenerateBtn.setText("Generate");
                            Toast.makeText(CustomizationPage.this, "AI error: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void addThumbnailToPanel(Bitmap bmp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int dp4 = dp(4), dp80 = dp(80);
        card.setPadding(dp4, dp4, dp4, dp4);
        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(dp80, dp80));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setImageBitmap(bmp);
        thumb.setBackgroundColor(0xFFEEEEEE);
        TextView label = new TextView(this);
        label.setText("Tap to apply");
        label.setTextSize(10f);
        label.setTextColor(0xFF666666);
        label.setPadding(0, dp4, 0, 0);
        card.addView(thumb);
        card.addView(label);
        card.setOnClickListener(v -> { designCanvas.addImageElement(bmp); Toast.makeText(this, "Template applied!", Toast.LENGTH_SHORT).show(); });
        aiTemplateResults.addView(card, 0, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    private void showStickerPicker() {
        int[] res = { android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera,
                android.R.drawable.ic_menu_edit, android.R.drawable.star_big_on,
                android.R.drawable.btn_star_big_on, android.R.drawable.ic_menu_compass };
        String[] names = {"Gallery","Camera","Edit","Star","Star2","Compass"};
        new AlertDialog.Builder(this).setTitle("Pick a Sticker")
                .setItems(names, (d, w) -> { Bitmap bmp = BitmapFactory.decodeResource(getResources(), res[w]); if (bmp != null) designCanvas.addImageElement(bmp); })
                .show();
    }

    private void openPreview() {
        Bitmap finalDesign = designCanvas.captureBitmap();
        try {
            File outFile = new File(getCacheDir(), "lanyard_final.png");
            FileOutputStream fos = new FileOutputStream(outFile);
            finalDesign.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("imagePath", outFile.getAbsolutePath());
            intent.putExtra("productType", "lanyard");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Preview error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onPreviewClicked(View v) { openPreview(); }

    private void applyTextStyle(String style) {
        if (currentSelectedElement == null || currentSelectedElement.getType() != DesignElement.ElementType.TEXT) return;
        android.graphics.Typeface cur = currentSelectedElement.getTextPaint().getTypeface();
        boolean isBold = cur != null && cur.isBold(), isItalic = cur != null && cur.isItalic();
        int s;
        if ("bold".equals(style)) s = isBold ? (isItalic ? android.graphics.Typeface.ITALIC : android.graphics.Typeface.NORMAL) : (isItalic ? android.graphics.Typeface.BOLD_ITALIC : android.graphics.Typeface.BOLD);
        else                       s = isItalic ? (isBold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL) : (isBold ? android.graphics.Typeface.BOLD_ITALIC : android.graphics.Typeface.ITALIC);
        currentSelectedElement.getTextPaint().setTypeface(android.graphics.Typeface.defaultFromStyle(s));
        designCanvas.invalidate();
    }

    private void applyTextAlign(String align) {
        if (currentSelectedElement == null || currentSelectedElement.getType() != DesignElement.ElementType.TEXT) return;
        switch (align) {
            case "left":   currentSelectedElement.setAlignment(android.graphics.Paint.Align.LEFT);   break;
            case "center": currentSelectedElement.setAlignment(android.graphics.Paint.Align.CENTER); break;
            case "right":  currentSelectedElement.setAlignment(android.graphics.Paint.Align.RIGHT);  break;
        }
        designCanvas.invalidate();
    }

    private void applyTextColor(int color) {
        if (currentSelectedElement == null || currentSelectedElement.getType() != DesignElement.ElementType.TEXT) return;
        currentSelectedElement.getTextPaint().setColor(color);
        designCanvas.invalidate();
    }

    private void applyFontSize(float sp) {
        if (currentSelectedElement == null || currentSelectedElement.getType() != DesignElement.ElementType.TEXT) return;
        float px = sp * getResources().getDisplayMetrics().scaledDensity;
        currentSelectedElement.getTextPaint().setTextSize(px);
        designCanvas.invalidate();
    }

    private void showTextEditDialog(DesignElement element) {
        if (element == null || element.getType() != DesignElement.ElementType.TEXT) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Text");
        final EditText input = new EditText(this);
        input.setText(element.getText());
        input.selectAll();
        int pad = dp(16);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String t = input.getText().toString();
            if (!t.isEmpty()) { element.setText(t); designCanvas.invalidate(); }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
        input.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }
}
