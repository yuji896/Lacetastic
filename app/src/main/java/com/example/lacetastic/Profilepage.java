package com.example.lacetastic;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Profilepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_layout);

        View rootView = findViewById(R.id.profileScrollView);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        SharedPreferences prefs = getSharedPreferences(Setprofile.PREFS_NAME, MODE_PRIVATE);

        TextView tvProfileNickname = findViewById(R.id.tvProfileNickname);
        TextView tvAvatarInitial   = findViewById(R.id.tvAvatarInitial);
        String nickname = prefs.getString(Setprofile.KEY_NICKNAME, "Friend");

        if (tvProfileNickname != null) tvProfileNickname.setText(nickname + "!");
        if (tvAvatarInitial != null && !nickname.isEmpty())
            tvAvatarInitial.setText(String.valueOf(nickname.charAt(0)).toUpperCase());

        ImageView ivBackBtn = findViewById(R.id.ivBackBtn);
        if (ivBackBtn != null) ivBackBtn.setOnClickListener(v -> finish());

        View btnEditNickname = findViewById(R.id.btnEditNickname);
        if (btnEditNickname != null) {
            btnEditNickname.setOnClickListener(v -> {
                Intent intent = new Intent(this, Setprofile.class);
                startActivity(intent);
            });
        }

        TextView tvStatDesigns = findViewById(R.id.tvStatDesignsCount);
        int designCount = prefs.getInt("design_count", 0);
        if (tvStatDesigns != null) tvStatDesigns.setText(String.valueOf(designCount));

        TextView tvStatLastDate = findViewById(R.id.tvStatLastDate);
        long lastDesignMillis = prefs.getLong("last_design_date", 0);
        if (tvStatLastDate != null) {
            if (lastDesignMillis == 0) {
                tvStatLastDate.setText("—");
            } else {
                String formatted = new SimpleDateFormat("MMM d", Locale.getDefault())
                        .format(new Date(lastDesignMillis));
                tvStatLastDate.setText(formatted);
            }
        }

        TextView tvStatMemberSince = findViewById(R.id.tvStatMemberSince);
        long firstLaunchMillis = prefs.getLong("first_launch_date", 0);
        if (firstLaunchMillis == 0) {
            firstLaunchMillis = System.currentTimeMillis();
            prefs.edit().putLong("first_launch_date", firstLaunchMillis).apply();
        }
        if (tvStatMemberSince != null) {
            String memberSince = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(new Date(firstLaunchMillis));
            tvStatMemberSince.setText(memberSince);
        }

        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        if (switchDarkMode != null) {
            boolean isDarkMode = prefs.getBoolean("dark_mode", false);
            switchDarkMode.setChecked(isDarkMode);
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("dark_mode", isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        View rowAboutApp   = findViewById(R.id.rowAboutApp);
        TextView tvAboutDesc = findViewById(R.id.tvAboutDesc);
        ImageView ivAboutArrow = findViewById(R.id.ivAboutArrow);

        if (rowAboutApp != null && tvAboutDesc != null) {
            rowAboutApp.setOnClickListener(v -> {
                if (tvAboutDesc.getVisibility() == View.GONE) {
                    tvAboutDesc.setVisibility(View.VISIBLE);
                    if (ivAboutArrow != null) ivAboutArrow.setRotation(90f);
                } else {
                    tvAboutDesc.setVisibility(View.GONE);
                    if (ivAboutArrow != null) ivAboutArrow.setRotation(0f);
                }
            });
        }

        View rowClearData = findViewById(R.id.rowClearData);
        if (rowClearData != null) {
            rowClearData.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Clear All Data")
                        .setMessage("This will delete your nickname, all saved designs, and reset all preferences. This cannot be undone.")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            prefs.edit().clear().apply();
                            recreate();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(Setprofile.PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(Setprofile.KEY_NICKNAME, "Friend");

        TextView tvProfileNickname = findViewById(R.id.tvProfileNickname);
        TextView tvAvatarInitial   = findViewById(R.id.tvAvatarInitial);

        if (tvProfileNickname != null) tvProfileNickname.setText(nickname + "!");
        if (tvAvatarInitial != null && !nickname.isEmpty())
            tvAvatarInitial.setText(String.valueOf(nickname.charAt(0)).toUpperCase());
    }
}
