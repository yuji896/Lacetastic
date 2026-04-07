package com.example.lacetastic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LanyardApiService {

    private static final String API_KEY = "sk-h4d0y9mzRJEewRviOhMs1lirnN0UzcYPORQCeRNPFuNROevo";
    private static final String API_URL =
            "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image";

    public interface LanyardCallback {
        void onSuccess(byte[] imageBytes);
        void onError(String message);
    }

    public static void generateLanyardDesign(Context context,
                                             String userPrompt,
                                             LanyardCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = context.getAssets().open("plain_lanyard.png");
                Bitmap bitmap  = BitmapFactory.decodeStream(is);

                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] imageBytes = baos.toByteArray();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                String fullPrompt =
                        "Add a decorative textile pattern design on the lanyard surface only. " +
                                "Keep the lanyard shape, buckle, and background exactly the same. " +
                                "Design theme: " + userPrompt + ". " +
                                "High quality fabric print, flat lay product mockup.";

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("init_image", "lanyard.png",
                                RequestBody.create(imageBytes, MediaType.parse("image/png")))
                        .addFormDataPart("init_image_mode",    "IMAGE_STRENGTH")
                        .addFormDataPart("image_strength",     "0.65")
                        .addFormDataPart("text_prompts[0][text]",   fullPrompt)
                        .addFormDataPart("text_prompts[0][weight]", "1")
                        .addFormDataPart("text_prompts[1][text]",
                                "change lanyard shape, remove buckle, blurry, low quality, background change")
                        .addFormDataPart("text_prompts[1][weight]", "-1")
                        .addFormDataPart("cfg_scale", "7")
                        .addFormDataPart("steps",    "30")
                        .addFormDataPart("samples",  "1")
                        .build();

                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    String base64Image = jsonResponse
                            .getJSONArray("artifacts")
                            .getJSONObject(0)
                            .getString("base64");

                    byte[] resultBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    callback.onSuccess(resultBytes);
                } else {
                    String errorBody = response.body() != null
                            ? response.body().string() : "Unknown error";
                    callback.onError("API Error " + response.code() + ": " + errorBody);
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
