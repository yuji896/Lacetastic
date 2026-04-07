package com.example.lacetastic;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * PreviewActivity — unified preview screen.
 *
 * For "lanyard" productType: inflates activity_lanyard_preview.xml which
 * hosts a LanyardPreviewActivity (custom View) that reads colours and
 * canvas references directly from LanyardDesignHolder — no bitmap transfer.
 *
 * For other product types: inflates activity_preview.xml and displays the
 * bitmap passed via "imagePath" intent extra in ivPreviewImage.
 */
public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "PreviewActivity";

    private ImageView    backButtonPreview;
    private LinearLayout downloadButton;
    private LinearLayout saveDesignButton;

    // Lanyard preview path
    private com.example.lacetastic.LanyardPreviewView lanyardPreviewView;

    // Generic preview path
    private ImageView ivPreviewImage;

    private String productType = "lanyard";
    private String imagePath;
    private Bitmap previewBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra("productType")) {
            productType = getIntent().getStringExtra("productType");
        }
        imagePath = getIntent().getStringExtra("imagePath");

        if ("lanyard".equals(productType)) {
            // Use the original lanyard preview layout (has LanyardPreviewView + 3 buttons)
            setContentView(R.layout.activity_lanyard_preview);
            lanyardPreviewView = findViewById(R.id.lanyardPreviewView);
            if (lanyardPreviewView != null) lanyardPreviewView.loadFromHolder();

            backButtonPreview = findViewById(R.id.backButtonPreview);
            downloadButton    = findViewById(R.id.downloadButton);
            saveDesignButton  = findViewById(R.id.saveDesignButton);

        } else {
            // Generic preview (mug, t-shirt, etc.)
            setContentView(R.layout.activity_preview);
            ivPreviewImage    = findViewById(R.id.ivPreviewImage);
            backButtonPreview = findViewById(R.id.backButtonPreview);
            downloadButton    = findViewById(R.id.downloadButton);
            saveDesignButton  = findViewById(R.id.saveDesignButton);
            displayPreviewImage();
        }

        setupClickListeners();
    }

    private void displayPreviewImage() {
        if (imagePath == null || imagePath.isEmpty()) return;
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                previewBitmap = BitmapFactory.decodeFile(imagePath);
                if (previewBitmap != null && ivPreviewImage != null) {
                    ivPreviewImage.setImageBitmap(previewBitmap);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "displayPreviewImage: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        if (backButtonPreview != null) backButtonPreview.setOnClickListener(v -> finish());

        if (downloadButton != null) {
            downloadButton.setOnClickListener(v -> {
                Bitmap bmp = getBitmapToExport();
                if (bmp != null) saveBitmapToGallery(bmp);
                else showToast("No design to export");
            });
        }

        if (saveDesignButton != null) {
            saveDesignButton.setOnClickListener(v -> saveToRecentDesigns());
        }
    }

    /**
     * For lanyard: capture the LanyardPreviewView to a bitmap.
     * For others:  use the previewBitmap decoded from imagePath.
     */
    private Bitmap getBitmapToExport() {
        if ("lanyard".equals(productType) && lanyardPreviewView != null) {
            return lanyardPreviewView.captureBitmap();
        }
        return previewBitmap;
    }

    private void saveToRecentDesigns() {
        Bitmap bmp = getBitmapToExport();
        if (bmp == null) { showToast("No design to save"); return; }
        try {
            String filename  = "design_" + System.currentTimeMillis() + ".png";
            File   directory = new File(getFilesDir(), "recent_designs");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, filename);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            SharedPreferences prefs = getSharedPreferences(Setprofile.PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean("has_designs", true).apply();
            showToast("Design saved to Recent Designs!");
            finish();
        } catch (Exception e) {
            showToast("Failed to save design: " + e.getMessage());
            Log.e(TAG, "saveToRecentDesigns error", e);
        }
    }

    private void saveBitmapToGallery(Bitmap bmp) {
        String filename = productType + "_" + System.currentTimeMillis() + ".png";
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        if (uri == null) { showToast("Failed to export to gallery"); return; }
        try (OutputStream fos = getContentResolver().openOutputStream(uri)) {
            if (fos != null) { bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); showToast("Exported to Gallery!"); }
        } catch (Exception e) {
            showToast("Export failed: " + e.getMessage());
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}