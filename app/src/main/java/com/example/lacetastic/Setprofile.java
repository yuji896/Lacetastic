package com.example.lacetastic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Setprofile extends AppCompatActivity {

    public static final String PREFS_NAME = "LacetasticPrefs";
    public static final String KEY_NICKNAME = "nickname";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.contains(KEY_NICKNAME)) {
            startActivity(new Intent(Setprofile.this, HomePage.class));
            finish();
            return;
        }

        setContentView(R.layout.set_profile);

        EditText etNickname = findViewById(R.id.etNickname);
        Button btnGetStarted = findViewById(R.id.btnGetStarted);

        btnGetStarted.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, "Please enter a nickname", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString(KEY_NICKNAME, nickname).apply();
                startActivity(new Intent(Setprofile.this, HomePage.class));
                finish();
            }
        });
    }
}
