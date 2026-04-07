package com.example.lacetastic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;

public class LanyardImageGenerator extends AppCompatActivity {

    private EditText    promptInput;
    private Button      generateBtn;
    private Button      customizeBtn;
    private ImageView   resultImage;
    private ProgressBar progressBar;

    private byte[] lastGeneratedBytes = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_generator);

        promptInput  = findViewById(R.id.promptInput);
        generateBtn  = findViewById(R.id.generateBtn);
        customizeBtn = findViewById(R.id.customizeBtn);
        resultImage  = findViewById(R.id.resultImage);
        progressBar  = findViewById(R.id.progressBar);

        customizeBtn.setVisibility(View.GONE);

        generateBtn.setOnClickListener(v -> {
            String prompt = promptInput.getText().toString().trim();
            if (prompt.isEmpty()) {
                prompt = "floral tropical pattern, vibrant colors";
            }

            progressBar.setVisibility(View.VISIBLE);
            generateBtn.setEnabled(false);
            customizeBtn.setVisibility(View.GONE);

            final String finalPrompt = prompt;

            LanyardApiService.generateLanyardDesign(
                    this,
                    finalPrompt,
                    new LanyardApiService.LanyardCallback() {

                        @Override
                        public void onSuccess(byte[] imageBytes) {
                            lastGeneratedBytes = imageBytes;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(
                                    imageBytes, 0, imageBytes.length);

                            runOnUiThread(() -> {
                                resultImage.setImageBitmap(bitmap);
                                progressBar.setVisibility(View.GONE);
                                generateBtn.setEnabled(true);
                                customizeBtn.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                Toast.makeText(LanyardImageGenerator.this,
                                        "Error: " + message, Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                generateBtn.setEnabled(true);
                            });
                        }
                    });
        });

        customizeBtn.setOnClickListener(v -> {
            if (lastGeneratedBytes == null) return;
            try {
                File outFile = new File(getCacheDir(), "generated_lanyard.png");
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(lastGeneratedBytes);
                fos.close();

                Intent intent = new Intent(
                        LanyardImageGenerator.this,
                        LanyardCustomizationActivity.class);
                intent.putExtra("generatedImagePath", outFile.getAbsolutePath());
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(this,
                        "Could not open editor: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
