package com.example.lacetastic;

import android.content.Intent;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import org.json.JSONArray;
import org.json.JSONObject;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * LanyardCustomizationActivity — full three-strap lanyard editor.
 *
 * Ported from com.example.custompage to com.example.lacetastic.
 * Key differences from the old simplified lacetastic version:
 *   - Three independent strap canvases (LEFT / MIDDLE / RIGHT)
 *   - LanyardZoomWrapper provides pinch-to-zoom per strap
 *   - LanyardDesignHolder carries live canvas refs to PreviewActivity
 *   - Tab panel system (Text / Elements / Shapes / Background)
 *   - AI template generator retained from original lacetastic version
 *   - Re-edit via JSON round-trip retained
 */
public class LanyardCustomizationActivity extends AppCompatActivity {

    // ── Top bar ───────────────────────────────────────────────────────────────
    private ImageView btnBack, btnCart, btnProfile;

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private ImageView btnUndo, btnRedo, btnBringFront, btnSendBack;
    private TextView  btnLeft, btnMiddle, btnRight;
    private LinearLayout btnFullPreview;

    // ── Strap wrappers (pinch-to-zoom) ────────────────────────────────────────
    private LanyardZoomWrapper wrapperLeftStrap, wrapperMiddleStrap, wrapperRightStrap;

    // ── Strap views (colour only) ─────────────────────────────────────────────
    private LanyardStrapView viewLeftStrap, viewMiddleStrap, viewRightStrap;

    // ── Per-strap canvases (transparent overlay, free-position, clipped) ──────
    private LanyardCanvasView canvasLeft, canvasMiddle, canvasRight;
    private LanyardCanvasView activeCanvas;

    // ── Which strap is shown ──────────────────────────────────────────────────
    private LanyardStrapView.StrapType activeStrapType = LanyardStrapView.StrapType.MIDDLE;

    // ── Per-strap colours ─────────────────────────────────────────────────────
    private int colorLeft   = 0xFF333333;
    private int colorMiddle = 0xFF333333;
    private int colorRight  = 0xFF333333;

    // ── Panel container & bottom tab bar ─────────────────────────────────────
    private FrameLayout  panelContainer;
    private LinearLayout bottomTabBar;
    private View textPanelView, elementsPanelView, iconsPanelView;
    private String currentPanel = "";

    // ── Bottom tabs ───────────────────────────────────────────────────────────
    private LinearLayout tabText, tabElements, tabIcons, tabBackground;

    // ── UI state ──────────────────────────────────────────────────────────────
    private static final int COLOR_TAB_ACTIVE   = 0xFFFFFFFF;
    private static final int COLOR_TAB_INACTIVE = 0xFF555555;
    private int     currentTextColor  = Color.BLACK;
    private int     currentShapeColor = Color.parseColor("#E91E63");
    private boolean isUpdatingUI      = false;
    private String  selectedShapeType = "circle";

    // ── Re-edit ───────────────────────────────────────────────────────────────
    private String reEditStateJson = null;

    // ── Image picker ──────────────────────────────────────────────────────────
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final String[] FONT_NAMES = {
            "Default", "Serif", "Monospace", "Sans-Serif", "Cursive", "Fantasy"
    };
    private static final Typeface[] TYPEFACES = {
            Typeface.DEFAULT, Typeface.SERIF, Typeface.MONOSPACE,
            Typeface.SANS_SERIF,
            Typeface.create("cursive",  Typeface.NORMAL),
            Typeface.create("fantasy",  Typeface.NORMAL)
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Uses the ORIGINAL (Customization-AppDev) activity_lanyard_customization.xml
        // which has three LanyardZoomWrapper + LanyardStrapView sections,
        // the tab panel system, and the bottom tab bar.
        setContentView(R.layout.activity_lanyard_customization);

        // ── Top bar ───────────────────────────────────────────────────────────
        btnBack    = findViewById(R.id.btnBack);
        btnCart    = findViewById(R.id.btnCart);
        btnProfile = findViewById(R.id.btnProfile);

        // ── Toolbar ───────────────────────────────────────────────────────────
        btnUndo       = findViewById(R.id.btnUndo);
        btnRedo       = findViewById(R.id.btnRedo);
        btnBringFront = findViewById(R.id.btnBringFront);
        btnSendBack   = findViewById(R.id.btnSendBack);
        btnLeft        = findViewById(R.id.btnLeft);
        btnMiddle      = findViewById(R.id.btnMiddle);
        btnRight       = findViewById(R.id.btnRight);
        btnFullPreview = findViewById(R.id.btnFullPreview);

        // ── Strap wrappers ────────────────────────────────────────────────────
        wrapperLeftStrap   = findViewById(R.id.wrapperLeftStrap);
        wrapperMiddleStrap = findViewById(R.id.wrapperMiddleStrap);
        wrapperRightStrap  = findViewById(R.id.wrapperRightStrap);

        // ── Strap views ───────────────────────────────────────────────────────
        viewLeftStrap   = findViewById(R.id.viewLeftStrap);
        viewMiddleStrap = findViewById(R.id.viewMiddleStrap);
        viewRightStrap  = findViewById(R.id.viewRightStrap);
        viewLeftStrap.setStrapType(LanyardStrapView.StrapType.LEFT);
        viewMiddleStrap.setStrapType(LanyardStrapView.StrapType.MIDDLE);
        viewRightStrap.setStrapType(LanyardStrapView.StrapType.RIGHT);

        // ── Per-strap canvases — injected at runtime into the wrappers ────────
        canvasLeft   = makeCanvas(LanyardCanvasView.StrapType.LEFT,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        canvasMiddle = makeCanvas(LanyardCanvasView.StrapType.MIDDLE,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        canvasRight  = makeCanvas(LanyardCanvasView.StrapType.RIGHT,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        wrapperLeftStrap.addView(canvasLeft);
        wrapperMiddleStrap.addView(canvasMiddle);
        wrapperRightStrap.addView(canvasRight);

        // ── Store canvas refs in static holder so PreviewActivity can read them ─
        LanyardDesignHolder.canvasLeft   = canvasLeft;
        LanyardDesignHolder.canvasMiddle = canvasMiddle;
        LanyardDesignHolder.canvasRight  = canvasRight;

        // Sync holder whenever any strap changes
        canvasLeft.setOnElementChangedListener(()   -> syncAllStraps());
        canvasMiddle.setOnElementChangedListener(() -> syncAllStraps());
        canvasRight.setOnElementChangedListener(()  -> syncAllStraps());

        // Fix middle strap view height = width * 0.13 after layout
        wrapperMiddleStrap.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        wrapperMiddleStrap.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int w = wrapperMiddleStrap.getWidth();
                        if (w > 0) {
                            int strapH = (int)(w * 0.13f + 0.5f) + dpToPx(2);
                            ViewGroup.LayoutParams lp = viewMiddleStrap.getLayoutParams();
                            lp.height = strapH;
                            viewMiddleStrap.setLayoutParams(lp);
                            canvasMiddle.invalidate();
                            syncAllStraps();
                        }
                    }
                });

        // ── Panel + bottom tab bar ────────────────────────────────────────────
        panelContainer = findViewById(R.id.panelContainer);
        bottomTabBar   = findViewById(R.id.bottomTabBar);
        tabText        = findViewById(R.id.tabText);
        tabElements    = findViewById(R.id.tabElements);
        tabIcons       = findViewById(R.id.tabIcons);
        tabBackground  = findViewById(R.id.tabBackground);

        // ── Image picker ──────────────────────────────────────────────────────
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            activeCanvas.addImageElement(bmp);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // ── Listeners ─────────────────────────────────────────────────────────
        if (btnBack    != null) btnBack.setOnClickListener(v -> finish());
        if (btnCart    != null) btnCart.setOnClickListener(v -> {});
        if (btnProfile != null) btnProfile.setOnClickListener(v -> {});

        if (btnUndo       != null) btnUndo.setOnClickListener(v       -> { if (activeCanvas != null) activeCanvas.undo(); });
        if (btnRedo       != null) btnRedo.setOnClickListener(v       -> { if (activeCanvas != null) activeCanvas.redo(); });
        if (btnBringFront != null) btnBringFront.setOnClickListener(v -> { if (activeCanvas != null) activeCanvas.bringToFront(); });
        if (btnSendBack   != null) btnSendBack.setOnClickListener(v   -> { if (activeCanvas != null) activeCanvas.sendToBack(); });

        btnLeft.setOnClickListener(v   -> showStrap(LanyardStrapView.StrapType.LEFT));
        btnMiddle.setOnClickListener(v -> showStrap(LanyardStrapView.StrapType.MIDDLE));
        btnRight.setOnClickListener(v  -> showStrap(LanyardStrapView.StrapType.RIGHT));

        if (btnFullPreview != null) btnFullPreview.setOnClickListener(v -> openPreview());

        tabText.setOnClickListener(v       -> togglePanel("text"));
        tabElements.setOnClickListener(v   -> togglePanel("elements"));
        tabIcons.setOnClickListener(v      -> togglePanel("icons"));
        tabBackground.setOnClickListener(v -> togglePanel("background"));

        // ── Apply AI-generated design if launched from LanyardImageGenerator ──
        applyIncomingGeneratedDesign();

        // Show middle strap by default
        showStrap(LanyardStrapView.StrapType.MIDDLE);

        // ── Re-edit: restore saved JSON state ─────────────────────────────────
        reEditStateJson = getIntent().getStringExtra("reEditStateJson");
        if (reEditStateJson != null && !reEditStateJson.isEmpty()) {
            canvasMiddle.post(() -> restoreFromJson(reEditStateJson));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI-generated design — if launched from LanyardImageGenerator the generated
    // image is added as a layer on the currently active strap canvas.
    // ─────────────────────────────────────────────────────────────────────────

    private void applyIncomingGeneratedDesign() {
        String generatedPath = getIntent().getStringExtra("generatedImagePath");
        if (generatedPath == null || generatedPath.isEmpty()) return;
        java.io.File imgFile = new java.io.File(generatedPath);
        if (!imgFile.exists()) return;
        Bitmap bmp = android.graphics.BitmapFactory.decodeFile(generatedPath);
        if (bmp != null && activeCanvas != null) {
            activeCanvas.addImageElement(bmp);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PREVIEW — push design state into LanyardDesignHolder, then navigate.
    // PreviewActivity (activity_lanyard_preview.xml) reads directly from the holder.
    // ─────────────────────────────────────────────────────────────────────────

    private void openPreview() {
        syncAllStraps();
        LanyardDesignHolder.designStateJson = buildDesignStateJson();
        Intent i = new Intent(this, PreviewActivity.class);
        i.putExtra("productType", "lanyard");
        startActivity(i);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESIGN STATE JSON — serialize / restore
    // ─────────────────────────────────────────────────────────────────────────

    private String buildDesignStateJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("colorLeft",      colorLeft);
            root.put("colorMiddle",    colorMiddle);
            root.put("colorRight",     colorRight);
            root.put("elementsLeft",   serializeElements(canvasLeft));
            root.put("elementsMiddle", serializeElements(canvasMiddle));
            root.put("elementsRight",  serializeElements(canvasRight));
            return root.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray serializeElements(LanyardCanvasView canvas) {
        JSONArray arr = new JSONArray();
        if (canvas == null || canvas.getElements() == null) return arr;
        for (DesignElement el : canvas.getElements()) {
            if (el.getType() != DesignElement.ElementType.TEXT) continue;
            try {
                JSONObject obj = new JSONObject();
                obj.put("text",          el.getText());
                obj.put("textColor",     el.getTextColor());
                obj.put("textSize",      el.getTextSize());
                obj.put("x",             el.getX());
                obj.put("y",             el.getY());
                obj.put("rotation",      el.getRotation());
                obj.put("scale",         el.getScale());
                obj.put("opacity",       el.getOpacity());
                obj.put("bold",          el.isBold());
                obj.put("italic",        el.isItalic());
                obj.put("underline",     el.isUnderline());
                obj.put("fontIndex",     el.getFontIndex());
                obj.put("vertical",      el.isVertical());
                obj.put("flipH",         el.isFlipHorizontal());
                obj.put("flipV",         el.isFlipVertical());
                obj.put("letterSpacing", el.getLetterSpacing());
                obj.put("lineSpacing",   el.getLineSpacing());
                String align = "CENTER";
                if (el.getAlignment() == Paint.Align.LEFT)  align = "LEFT";
                if (el.getAlignment() == Paint.Align.RIGHT) align = "RIGHT";
                obj.put("align", align);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr;
    }

    private void restoreFromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);

            if (root.has("colorLeft"))   { colorLeft   = root.getInt("colorLeft");   viewLeftStrap.setStrapColor(colorLeft); }
            if (root.has("colorMiddle")) { colorMiddle = root.getInt("colorMiddle"); viewMiddleStrap.setStrapColor(colorMiddle); }
            if (root.has("colorRight"))  { colorRight  = root.getInt("colorRight");  viewRightStrap.setStrapColor(colorRight); }

            if (canvasLeft.getElements()   != null) canvasLeft.getElements().clear();
            if (canvasMiddle.getElements() != null) canvasMiddle.getElements().clear();
            if (canvasRight.getElements()  != null) canvasRight.getElements().clear();

            restoreElements(canvasLeft,   root.optJSONArray("elementsLeft"));
            restoreElements(canvasMiddle, root.optJSONArray("elementsMiddle"));
            restoreElements(canvasRight,  root.optJSONArray("elementsRight"));

            canvasLeft.invalidate();
            canvasMiddle.invalidate();
            canvasRight.invalidate();
            syncAllStraps();
            showStrap(LanyardStrapView.StrapType.MIDDLE);
            Toast.makeText(this, "Design restored!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Could not restore design: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreElements(LanyardCanvasView canvas, JSONArray arr) {
        if (canvas == null || arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                DesignElement el = canvas.addTextElement(obj.optString("text", "Text"));
                el.setTextColor((int) obj.optLong("textColor", Color.BLACK));
                el.setTextSize((float) obj.optDouble("textSize", 40));
                el.setX((float) obj.optDouble("x", 0));
                el.setY((float) obj.optDouble("y", 0));
                el.setRotation((float) obj.optDouble("rotation", 0));
                el.setScale((float) obj.optDouble("scale", 1));
                el.setOpacity(obj.optInt("opacity", 255));
                el.setBold(obj.optBoolean("bold", false));
                el.setItalic(obj.optBoolean("italic", false));
                el.setUnderline(obj.optBoolean("underline", false));
                el.setVertical(obj.optBoolean("vertical", false));
                el.setFlipHorizontal(obj.optBoolean("flipH", false));
                el.setFlipVertical(obj.optBoolean("flipV", false));
                el.setLetterSpacing(obj.optInt("letterSpacing", 0));
                el.setLineSpacing((float) obj.optDouble("lineSpacing", 1f));
                int fontIndex = obj.optInt("fontIndex", 0);
                if (fontIndex >= 0 && fontIndex < TYPEFACES.length) {
                    el.setFontIndex(fontIndex);
                    el.setTypeface(TYPEFACES[fontIndex]);
                }
                String align = obj.optString("align", "CENTER");
                switch (align) {
                    case "LEFT":  el.setAlignment(Paint.Align.LEFT);  break;
                    case "RIGHT": el.setAlignment(Paint.Align.RIGHT); break;
                    default:      el.setAlignment(Paint.Align.CENTER); break;
                }
                canvas.invalidate();
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SYNC — push colours into holder; canvas refs already stored in onCreate()
    // ─────────────────────────────────────────────────────────────────────────

    private void syncAllStraps() {
        LanyardDesignHolder.colorLeft   = colorLeft;
        LanyardDesignHolder.colorMiddle = colorMiddle;
        LanyardDesignHolder.colorRight  = colorRight;
    }

    // ── Canvas factory ────────────────────────────────────────────────────────

    private LanyardCanvasView makeCanvas(LanyardCanvasView.StrapType type, int w, int h) {
        LanyardCanvasView cv = new LanyardCanvasView(this);
        cv.setStrapType(type);
        cv.setLayoutParams(new FrameLayout.LayoutParams(w, h));
        return cv;
    }

    // ── Strap switching ───────────────────────────────────────────────────────

    private void showStrap(LanyardStrapView.StrapType type) {
        activeStrapType = type;

        wrapperLeftStrap.resetZoom();
        wrapperMiddleStrap.resetZoom();
        wrapperRightStrap.resetZoom();

        wrapperLeftStrap.setVisibility(View.GONE);
        wrapperMiddleStrap.setVisibility(View.GONE);
        wrapperRightStrap.setVisibility(View.GONE);

        setTabInactive(btnLeft);
        setTabInactive(btnMiddle);
        setTabInactive(btnRight);

        switch (type) {
            case LEFT:
                wrapperLeftStrap.setVisibility(View.VISIBLE);
                activeCanvas = canvasLeft;
                setTabActive(btnLeft);
                break;
            case MIDDLE:
                wrapperMiddleStrap.setVisibility(View.VISIBLE);
                activeCanvas = canvasMiddle;
                setTabActive(btnMiddle);
                break;
            case RIGHT:
                wrapperRightStrap.setVisibility(View.VISIBLE);
                activeCanvas = canvasRight;
                setTabActive(btnRight);
                break;
        }

        if ("background".equals(currentPanel)) {
            panelContainer.removeAllViews();
            openBackgroundPanel();
        }
    }

    private void applyStrapColor(int color) {
        switch (activeStrapType) {
            case LEFT:   colorLeft   = color; viewLeftStrap.setStrapColor(color);   break;
            case MIDDLE: colorMiddle = color; viewMiddleStrap.setStrapColor(color); break;
            case RIGHT:  colorRight  = color; viewRightStrap.setStrapColor(color);  break;
        }
        syncAllStraps();
    }

    private int getActiveStrapColor() {
        switch (activeStrapType) {
            case LEFT:  return colorLeft;
            case RIGHT: return colorRight;
            default:    return colorMiddle;
        }
    }

    private void setTabActive(TextView tab) {
        tab.setBackgroundResource(R.drawable.tab_active_bg);
        tab.setTextColor(COLOR_TAB_ACTIVE);
    }

    private void setTabInactive(TextView tab) {
        tab.setBackgroundColor(Color.TRANSPARENT);
        tab.setTextColor(COLOR_TAB_INACTIVE);
    }

    // ── Panel toggle ──────────────────────────────────────────────────────────

    private void togglePanel(String name) {
        if (panelContainer == null) return;
        if (currentPanel.equals(name)) { closePanel(); return; }
        currentPanel = name;
        panelContainer.removeAllViews();
        panelContainer.setVisibility(View.VISIBLE);
        if (bottomTabBar != null) bottomTabBar.setVisibility(View.GONE);
        switch (name) {
            case "text":       openTextPanel();       break;
            case "elements":   openElementsPanel();   break;
            case "icons":      openIconsPanel();       break;
            case "background": openBackgroundPanel();  break;
        }
    }

    private void closePanel() {
        if (panelContainer != null) {
            panelContainer.removeAllViews();
            panelContainer.setVisibility(View.GONE);
        }
        if (bottomTabBar != null) bottomTabBar.setVisibility(View.VISIBLE);
        currentPanel = "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEXT PANEL
    // ─────────────────────────────────────────────────────────────────────────

    private void openTextPanel() {
        textPanelView = LayoutInflater.from(this)
                .inflate(R.layout.text_panel_layout, panelContainer, false);
        panelContainer.addView(textPanelView);

        textPanelView.findViewById(R.id.closeTextPanelButton).setOnClickListener(v -> closePanel());

        Button addTextBtn    = textPanelView.findViewById(R.id.addTextButton);
        Button removeTextBtn = textPanelView.findViewById(R.id.removeTextButton);
        EditText textInput   = textPanelView.findViewById(R.id.textInput);

        addTextBtn.setOnClickListener(v -> {
            String t = textInput.getText().toString().trim();
            activeCanvas.addTextElement(t.isEmpty() ? "Text" : t);
        });
        removeTextBtn.setOnClickListener(v -> {
            DesignElement sel = activeCanvas.getSelectedElement();
            if (sel != null) activeCanvas.removeElement(sel);
            else Toast.makeText(this, "Select a text element first", Toast.LENGTH_SHORT).show();
        });

        textInput.addTextChangedListener(simpleTextWatcher(s -> {
            if (isUpdatingUI) return;
            DesignElement sel = activeCanvas.getSelectedElement();
            if (sel != null && sel.getType() == DesignElement.ElementType.TEXT) {
                sel.setText(s); activeCanvas.invalidate();
            }
        }));

        activeCanvas.setOnElementSelectedListener(el -> {
            if (el != null && el.getType() == DesignElement.ElementType.TEXT) {
                isUpdatingUI = true;
                textInput.setText(el.getText());
                isUpdatingUI = false;
                syncTextPanel(el);
            }
        });

        Spinner fontSpinner = textPanelView.findViewById(R.id.fontFamilySpinner);
        fontSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, FONT_NAMES));
        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isUpdatingUI) return;
                DesignElement sel = activeCanvas.getSelectedElement();
                if (sel != null && sel.getType() == DesignElement.ElementType.TEXT) {
                    sel.setTypeface(TYPEFACES[pos]); sel.setFontIndex(pos); activeCanvas.invalidate();
                }
            }
        });

        Button boldBtn      = textPanelView.findViewById(R.id.boldButton);
        Button italicBtn    = textPanelView.findViewById(R.id.italicButton);
        Button underlineBtn = textPanelView.findViewById(R.id.underlineButton);
        boldBtn.setOnClickListener(v      -> { DesignElement s = activeCanvas.getSelectedElement(); if (s == null || s.getType() != DesignElement.ElementType.TEXT) return; s.setBold(!s.isBold()); boldBtn.setAlpha(s.isBold() ? 1f : 0.4f); activeCanvas.invalidate(); });
        italicBtn.setOnClickListener(v    -> { DesignElement s = activeCanvas.getSelectedElement(); if (s == null || s.getType() != DesignElement.ElementType.TEXT) return; s.setItalic(!s.isItalic()); italicBtn.setAlpha(s.isItalic() ? 1f : 0.4f); activeCanvas.invalidate(); });
        underlineBtn.setOnClickListener(v -> { DesignElement s = activeCanvas.getSelectedElement(); if (s == null || s.getType() != DesignElement.ElementType.TEXT) return; s.setUnderline(!s.isUnderline()); underlineBtn.setAlpha(s.isUnderline() ? 1f : 0.4f); activeCanvas.invalidate(); });

        View colorPreview = textPanelView.findViewById(R.id.colorPreview);
        colorPreview.setBackgroundColor(currentTextColor);
        colorPreview.setOnClickListener(v -> new ColorPickerDialog(this, currentTextColor, color -> {
            currentTextColor = color; colorPreview.setBackgroundColor(color);
            DesignElement sel = activeCanvas.getSelectedElement();
            if (sel != null && sel.getType() == DesignElement.ElementType.TEXT) {
                sel.setTextColor(color); activeCanvas.invalidate();
            }
        }).show());

        EditText sizeInput = textPanelView.findViewById(R.id.textSizeInput);
        sizeInput.addTextChangedListener(simpleTextWatcher(s -> {
            if (isUpdatingUI || s.isEmpty()) return;
            try {
                float sz = Float.parseFloat(s);
                DesignElement sel = activeCanvas.getSelectedElement();
                if (sel != null && sel.getType() == DesignElement.ElementType.TEXT) {
                    sel.setTextSize(sz); activeCanvas.invalidate();
                }
            } catch (NumberFormatException ignored) {}
        }));

        textPanelView.findViewById(R.id.alignLeftButton).setOnClickListener(v   -> applyAlignment(Paint.Align.LEFT));
        textPanelView.findViewById(R.id.alignCenterButton).setOnClickListener(v -> applyAlignment(Paint.Align.CENTER));
        textPanelView.findViewById(R.id.alignRightButton).setOnClickListener(v  -> applyAlignment(Paint.Align.RIGHT));

        SeekBar scaleBar = textPanelView.findViewById(R.id.scaleSeekBar);
        SeekBar rotBar   = textPanelView.findViewById(R.id.rotationSeekBar);
        SeekBar opBar    = textPanelView.findViewById(R.id.opacitySeekBar);
        SeekBar letBar   = textPanelView.findViewById(R.id.letterSpacingSeekBar);
        SeekBar lineBar  = textPanelView.findViewById(R.id.lineSpacingSeekBar);
        fixSeekBar(scaleBar); fixSeekBar(rotBar); fixSeekBar(opBar); fixSeekBar(letBar); fixSeekBar(lineBar);

        TextView scaleVal = textPanelView.findViewById(R.id.scaleValue);
        TextView rotVal   = textPanelView.findViewById(R.id.rotationValue);
        TextView opVal    = textPanelView.findViewById(R.id.opacityValue);
        TextView letVal   = textPanelView.findViewById(R.id.letterSpacingValue);
        TextView lineVal  = textPanelView.findViewById(R.id.lineSpacingValue);

        scaleBar.setOnSeekBarChangeListener(simpleSeekBar(p -> { scaleVal.setText(p + "%"); DesignElement s = activeCanvas.getSelectedElement(); if (s != null) { s.setScale(p / 100f); activeCanvas.invalidate(); } }));
        rotBar.setOnSeekBarChangeListener(simpleSeekBar(p   -> { rotVal.setText(p + "°");   DesignElement s = activeCanvas.getSelectedElement(); if (s != null) { s.setRotation(p); activeCanvas.invalidate(); } }));
        opBar.setOnSeekBarChangeListener(simpleSeekBar(p    -> { opVal.setText(p + "%");     DesignElement s = activeCanvas.getSelectedElement(); if (s != null) { s.setOpacity((int)(p / 100f * 255)); activeCanvas.invalidate(); } }));
        letBar.setOnSeekBarChangeListener(simpleSeekBar(p   -> { letVal.setText(String.valueOf(p)); DesignElement s = activeCanvas.getSelectedElement(); if (s != null && s.getType() == DesignElement.ElementType.TEXT) { s.setLetterSpacing(p); activeCanvas.invalidate(); } }));
        lineBar.setOnSeekBarChangeListener(simpleSeekBar(p  -> { float ls = 1f + p / 100f; lineVal.setText(String.format("%.1f", ls)); DesignElement s = activeCanvas.getSelectedElement(); if (s != null && s.getType() == DesignElement.ElementType.TEXT) { s.setLineSpacing(ls); activeCanvas.invalidate(); } }));

        textPanelView.findViewById(R.id.flipHorizontalButton).setOnClickListener(v        -> { DesignElement s = activeCanvas.getSelectedElement(); if (s != null) { s.setFlipHorizontal(!s.isFlipHorizontal()); activeCanvas.invalidate(); } });
        textPanelView.findViewById(R.id.flipVerticalButton).setOnClickListener(v          -> { DesignElement s = activeCanvas.getSelectedElement(); if (s != null) { s.setFlipVertical(!s.isFlipVertical()); activeCanvas.invalidate(); } });
        textPanelView.findViewById(R.id.orientationHorizontalButton).setOnClickListener(v -> { DesignElement s = activeCanvas.getSelectedElement(); if (s != null && s.getType() == DesignElement.ElementType.TEXT) { s.setVertical(false); activeCanvas.invalidate(); } });
        textPanelView.findViewById(R.id.orientationVerticalButton).setOnClickListener(v   -> { DesignElement s = activeCanvas.getSelectedElement(); if (s != null && s.getType() == DesignElement.ElementType.TEXT) { s.setVertical(true); activeCanvas.invalidate(); } });
    }

    private void applyAlignment(Paint.Align a) {
        DesignElement s = activeCanvas.getSelectedElement();
        if (s != null && s.getType() == DesignElement.ElementType.TEXT) { s.setAlignment(a); activeCanvas.invalidate(); }
    }

    private void syncTextPanel(DesignElement el) {
        if (textPanelView == null || el == null) return;
        isUpdatingUI = true;
        Spinner sp = textPanelView.findViewById(R.id.fontFamilySpinner); if (sp != null) sp.setSelection(el.getFontIndex());
        Button b   = textPanelView.findViewById(R.id.boldButton);        if (b  != null) b.setAlpha(el.isBold() ? 1f : 0.4f);
        Button it  = textPanelView.findViewById(R.id.italicButton);      if (it != null) it.setAlpha(el.isItalic() ? 1f : 0.4f);
        Button un  = textPanelView.findViewById(R.id.underlineButton);   if (un != null) un.setAlpha(el.isUnderline() ? 1f : 0.4f);
        View cp    = textPanelView.findViewById(R.id.colorPreview);      if (cp != null) cp.setBackgroundColor(el.getTextColor()); currentTextColor = el.getTextColor();
        EditText si = textPanelView.findViewById(R.id.textSizeInput);   if (si != null) si.setText(String.valueOf((int) el.getTextSize()));
        SeekBar sc  = textPanelView.findViewById(R.id.scaleSeekBar);    if (sc != null) sc.setProgress((int)(el.getScale() * 100));
        TextView sv = textPanelView.findViewById(R.id.scaleValue);       if (sv != null) sv.setText((int)(el.getScale() * 100) + "%");
        SeekBar rb  = textPanelView.findViewById(R.id.rotationSeekBar); if (rb != null) rb.setProgress((int) el.getRotation());
        TextView rv = textPanelView.findViewById(R.id.rotationValue);    if (rv != null) rv.setText((int) el.getRotation() + "°");
        SeekBar ob  = textPanelView.findViewById(R.id.opacitySeekBar);  int op = (int)(el.getOpacity() / 255f * 100); if (ob != null) ob.setProgress(op);
        TextView ov = textPanelView.findViewById(R.id.opacityValue);     if (ov != null) ov.setText(op + "%");
        isUpdatingUI = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ELEMENTS PANEL
    // ─────────────────────────────────────────────────────────────────────────

    private void openElementsPanel() {
        elementsPanelView = LayoutInflater.from(this)
                .inflate(R.layout.elements_panel_layout, panelContainer, false);
        panelContainer.addView(elementsPanelView);
        elementsPanelView.findViewById(R.id.closeElementsPanelButton).setOnClickListener(v -> closePanel());
        elementsPanelView.findViewById(R.id.uploadImageBox).setOnClickListener(v ->
                imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));

        SeekBar imgOpBar  = elementsPanelView.findViewById(R.id.imageOpacitySeekBar);
        TextView imgOpVal = elementsPanelView.findViewById(R.id.imageOpacityValue);
        fixSeekBar(imgOpBar);
        imgOpBar.setOnSeekBarChangeListener(simpleSeekBar(p -> {
            imgOpVal.setText(p + "%");
            DesignElement sel = activeCanvas.getSelectedElement();
            if (sel != null && sel.getType() == DesignElement.ElementType.IMAGE) {
                sel.setOpacity((int)(p / 100f * 255)); activeCanvas.invalidate();
            }
        }));
        activeCanvas.setOnElementSelectedListener(el -> {
            if (el != null && el.getType() == DesignElement.ElementType.IMAGE) {
                int pct = (int)(el.getOpacity() / 255f * 100);
                imgOpBar.setProgress(pct); imgOpVal.setText(pct + "%");
            }
        });
        elementsPanelView.findViewById(R.id.openEffectsButton).setOnClickListener(v -> openEffectsPanel());
    }

    private void openEffectsPanel() {
        panelContainer.removeAllViews();
        View ev = LayoutInflater.from(this).inflate(R.layout.effects_panel_layout, panelContainer, false);
        panelContainer.addView(ev);
        ev.findViewById(R.id.closeEffectsPanelButton).setOnClickListener(v -> {
            panelContainer.removeAllViews(); openElementsPanel();
        });
        int[]    ids   = { R.id.effectOriginal, R.id.effectGrayscale, R.id.effectSepia, R.id.effectBright, R.id.effectContrast, R.id.effectVintage, R.id.effectBlur, R.id.effectSharpen, R.id.effectEmboss };
        String[] names = { "original","grayscale","sepia","bright","contrast","vintage","blur","sharpen","emboss" };
        for (int i = 0; i < ids.length; i++) {
            final String name = names[i];
            View btn = ev.findViewById(ids[i]); if (btn == null) continue;
            btn.setOnClickListener(v -> {
                DesignElement sel = activeCanvas.getSelectedElement();
                if (sel != null && sel.getType() == DesignElement.ElementType.IMAGE) {
                    activeCanvas.applyEffect(name);
                    Toast.makeText(this, name + " applied", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ICONS / SHAPES PANEL
    // ─────────────────────────────────────────────────────────────────────────

    private void openIconsPanel() {
        iconsPanelView = LayoutInflater.from(this)
                .inflate(R.layout.icons_panel_layout, panelContainer, false);
        panelContainer.addView(iconsPanelView);
        iconsPanelView.findViewById(R.id.closeIconsPanelButton).setOnClickListener(v -> closePanel());

        View shapeColorPreview = iconsPanelView.findViewById(R.id.shapeColorPreview);
        shapeColorPreview.setBackgroundColor(currentShapeColor);
        shapeColorPreview.setOnClickListener(v -> new ColorPickerDialog(this, currentShapeColor, color -> {
            currentShapeColor = color; shapeColorPreview.setBackgroundColor(color);
        }).show());

        SeekBar opBar  = iconsPanelView.findViewById(R.id.shapeOpacitySeekBar);
        TextView opVal = iconsPanelView.findViewById(R.id.shapeOpacityValue);
        fixSeekBar(opBar);
        opBar.setOnSeekBarChangeListener(simpleSeekBar(p -> opVal.setText(p + "%")));

        int[]    tileIds = { R.id.shapeCircle, R.id.shapeSquare, R.id.shapeTriangle, R.id.shapeStar, R.id.shapeHeart, R.id.shapePentagon, R.id.shapeHexagon, R.id.shapeOctagon, R.id.shapeDiamond };
        String[] labels  = { "circle","square","triangle","star","heart","pentagon","hexagon","octagon","diamond" };
        for (int i = 0; i < tileIds.length; i++) {
            final String label = labels[i];
            View tile = iconsPanelView.findViewById(tileIds[i]); if (tile == null) continue;
            tile.setOnClickListener(v -> {
                selectedShapeType = label;
                for (int id : tileIds) { View t = iconsPanelView.findViewById(id); if (t != null) t.setAlpha(0.5f); }
                v.setAlpha(1f);
            });
        }

        Button addBtn = iconsPanelView.findViewById(R.id.addShapeToCanvasButton);
        addBtn.setOnClickListener(v -> {
            String sStr = ((EditText) iconsPanelView.findViewById(R.id.shapeSizeInput))
                    .getText().toString().trim();
            int sizeDp = 60; try { sizeDp = Integer.parseInt(sStr); } catch (NumberFormatException ignored) {}
            float sizePx = sizeDp * getResources().getDisplayMetrics().density;
            int opacity  = (int)(opBar.getProgress() / 100f * 255);
            Bitmap bmp   = drawShape(selectedShapeType, (int) sizePx, currentShapeColor, opacity);
            DesignElement el = activeCanvas.addImageElement(bmp);
            el.setOpacity(opacity); activeCanvas.invalidate();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BACKGROUND PANEL
    // ─────────────────────────────────────────────────────────────────────────

    private void openBackgroundPanel() {
        View bgPanel = LayoutInflater.from(this)
                .inflate(R.layout.background_panel_layout, panelContainer, false);
        panelContainer.addView(bgPanel);
        bgPanel.findViewById(R.id.closeBackgroundPanelButton).setOnClickListener(v -> closePanel());

        int      thisColor   = getActiveStrapColor();
        View     colorPreview = bgPanel.findViewById(R.id.backgroundColorPreview);
        EditText hexInput     = bgPanel.findViewById(R.id.backgroundHexInput);
        colorPreview.setBackgroundColor(thisColor);
        isUpdatingUI = true; hexInput.setText(String.format("#%06X", 0xFFFFFF & thisColor)); isUpdatingUI = false;

        hexInput.addTextChangedListener(simpleTextWatcher(s -> {
            if (isUpdatingUI) return;
            String hex = s.startsWith("#") ? s : "#" + s;
            if (hex.length() == 7) {
                try { int c = Color.parseColor(hex); colorPreview.setBackgroundColor(c); applyStrapColor(c); }
                catch (IllegalArgumentException ignored) {}
            }
        }));

        bgPanel.findViewById(R.id.chooseCustomColorButton).setOnClickListener(v ->
                new ColorPickerDialog(this, getActiveStrapColor(), color -> {
                    applyStrapColor(color); colorPreview.setBackgroundColor(color);
                    isUpdatingUI = true; hexInput.setText(String.format("#%06X", 0xFFFFFF & color)); isUpdatingUI = false;
                }).show());

        int[][] presets = {
                {R.id.presetWhite,0xFFFFFFFF},{R.id.presetLightGray,0xFFD3D3D3},{R.id.presetGray,0xFF888888},
                {R.id.presetDarkGray,0xFF444444},{R.id.presetBlack,0xFF000000},{R.id.presetRed,0xFFFF0000},
                {R.id.presetBlue,0xFF0000FF},{R.id.presetGreen,0xFF00FF00},{R.id.presetYellow,0xFFFFFF00},
                {R.id.presetPink,0xFFE91E63}
        };
        for (int[] preset : presets) {
            View sw = bgPanel.findViewById(preset[0]); if (sw == null) continue;
            final int c = preset[1];
            sw.setOnClickListener(v -> {
                applyStrapColor(c); colorPreview.setBackgroundColor(c);
                isUpdatingUI = true; hexInput.setText(String.format("#%06X", 0xFFFFFF & c)); isUpdatingUI = false;
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHAPE DRAWING
    // ─────────────────────────────────────────────────────────────────────────

    private Bitmap drawShape(String type, int size, int color, int opacity) {
        if (size <= 0) size = 1;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p  = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color); p.setAlpha(opacity); p.setStyle(Paint.Style.FILL);
        float cx = size / 2f, cy = size / 2f, r = size / 2f - 2;
        switch (type) {
            case "square":   c.drawRect(2, 2, size - 2, size - 2, p); break;
            case "triangle": Path tri = new Path(); tri.moveTo(cx,2); tri.lineTo(size-2,size-2); tri.lineTo(2,size-2); tri.close(); c.drawPath(tri,p); break;
            case "star":     c.drawPath(starPath(cx,cy,r,r*0.4f,5),p); break;
            case "heart":    c.drawPath(heartPath(cx,cy,r),p); break;
            case "pentagon": c.drawPath(polyPath(cx,cy,r,5,-90),p); break;
            case "hexagon":  c.drawPath(polyPath(cx,cy,r,6,0),p); break;
            case "octagon":  c.drawPath(polyPath(cx,cy,r,8,22.5f),p); break;
            case "diamond":  Path dia = new Path(); dia.moveTo(cx,2); dia.lineTo(size-2,cy); dia.lineTo(cx,size-2); dia.lineTo(2,cy); dia.close(); c.drawPath(dia,p); break;
            default:         c.drawCircle(cx,cy,r,p); break;
        }
        return bmp;
    }

    private Path polyPath(float cx, float cy, float r, int s, float off) { Path p = new Path(); for (int i=0;i<s;i++) { double a=Math.toRadians(off+i*360.0/s); float x=cx+r*(float)Math.cos(a); float y=cy+r*(float)Math.sin(a); if(i==0)p.moveTo(x,y); else p.lineTo(x,y); } p.close(); return p; }
    private Path starPath(float cx, float cy, float oR, float iR, int pts) { Path p = new Path(); double step=Math.PI/pts; for(int i=0;i<2*pts;i++) { double a=-Math.PI/2+i*step; float rad=(i%2==0)?oR:iR; float x=cx+rad*(float)Math.cos(a); float y=cy+rad*(float)Math.sin(a); if(i==0)p.moveTo(x,y); else p.lineTo(x,y); } p.close(); return p; }
    private Path heartPath(float cx, float cy, float r) { Path p=new Path(); float w=r*2,h=r*2,x=cx-r,y=cy-r*0.6f; p.moveTo(x+w/2,y+h/4); p.cubicTo(x+w/2,y,x,y,x,y+h/4); p.cubicTo(x,y+h/2,x+w/2,y+h*3/4,x+w/2,y+h); p.cubicTo(x+w/2,y+h*3/4,x+w,y+h/2,x+w,y+h/4); p.cubicTo(x+w,y,x+w/2,y,x+w/2,y+h/4); p.close(); return p; }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private void fixSeekBar(SeekBar sb) {
        if (sb == null) return;
        sb.setOnTouchListener((v, event) -> {
            int a = event.getAction();
            if (a == MotionEvent.ACTION_DOWN) v.getParent().requestDisallowInterceptTouchEvent(true);
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
    }

    interface ProgressCallback { void onProgress(int p); }
    private SeekBar.OnSeekBarChangeListener simpleSeekBar(ProgressCallback cb) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { cb.onProgress(p); }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
    }

    interface TextCallback { void onText(String s); }
    private TextWatcher simpleTextWatcher(TextCallback cb) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { cb.onText(s.toString()); }
        };
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}