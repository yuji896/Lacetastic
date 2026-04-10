package com.example.lacetastic;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_layout);

        View rootView = findViewById(R.id.rootLayout);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        TextView tvNickname       = findViewById(R.id.tvNickname);
        TextView tvProfileInitial = findViewById(R.id.tvProfileInitial);
        CardView ivProfileIcon    = findViewById(R.id.ivProfileIcon);
        ImageView ivHistoryTab    = findViewById(R.id.ivHistoryTab);
        LinearLayout llEmptyState    = findViewById(R.id.llEmptyState);
        LinearLayout llRecentDesigns = findViewById(R.id.llRecentDesigns);
        TextView tvViewAll        = findViewById(R.id.tvViewAll);

        SharedPreferences prefs = getSharedPreferences(Setprofile.PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(Setprofile.KEY_NICKNAME, "Friend");
        tvNickname.setText(String.format("%s!", nickname));
        if (!nickname.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(nickname.charAt(0)).toUpperCase());
        }

        boolean hasDesigns = prefs.getBoolean("has_designs", false);
        if (hasDesigns) {
            llEmptyState.setVisibility(View.GONE);
            llRecentDesigns.setVisibility(View.VISIBLE);
            tvViewAll.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(View.VISIBLE);
            llRecentDesigns.setVisibility(View.GONE);
            tvViewAll.setVisibility(View.GONE);
        }

        if (ivProfileIcon != null) {
            ivProfileIcon.setOnClickListener(v -> {
                animateClick(v, () -> {
                    startActivity(new Intent(HomePage.this, Profilepage.class));
                });
            });
        }

        View btnCreateDesign = findViewById(R.id.btnCreateDesign);
        if (btnCreateDesign != null) {
            btnCreateDesign.setOnClickListener(v -> {
                animateClick(v, () -> {
                    // Changed from CustomizationPage to LanyardCustomizationActivity
                    startActivity(new Intent(HomePage.this, LanyardCustomizationActivity.class));
                });
            });
        }

        View penIcon = findViewById(R.id.ivPenIcon);
        if (penIcon != null) {
            penIcon.setOnClickListener(v -> {
                animateClick(v, () -> {
                    // Changed from CustomizationPage to LanyardCustomizationActivity
                    startActivity(new Intent(HomePage.this, LanyardCustomizationActivity.class));
                });
            });
        }

        View homeTab = findViewById(R.id.ivHomeTab);
        if (homeTab != null) {
            homeTab.setOnClickListener(v -> animateClick(v, null));
        }

        if (ivHistoryTab != null) {
            ivHistoryTab.setOnClickListener(v -> {
                animateClick(v, () -> {
                    startActivity(new Intent(HomePage.this, RecentDesignsActivity.class));
                    overridePendingTransition(0, 0);
                });
            });
        }

        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                animateClick(v, () -> {
                    startActivity(new Intent(HomePage.this, RecentDesignsActivity.class));
                });
            });
        }

        View step1 = findViewById(R.id.llStep1);
        View step2 = findViewById(R.id.llStep2);
        View step3 = findViewById(R.id.llStep3);

        if (step1 != null) {
            step1.setOnClickListener(v -> animateClick(v, () -> 
                showStepBottomSheet("Step 1: Start", "Click the pen icon in the bottom navigation bar or the pink banner to begin your new lanyard design.")));
        }
        if (step2 != null) {
            step2.setOnClickListener(v -> animateClick(v, () -> 
                showStepBottomSheet("Step 2: Customize", "Use the editor to add your name, stickers, or even generate a unique AI pattern for your lanyard.")));
        }
        if (step3 != null) {
            step3.setOnClickListener(v -> animateClick(v, () -> 
                showStepBottomSheet("Step 3: Save", "Once you're happy with your design, tap Preview to see it, then Save it to your history or export it to your gallery.")));
        }
    }

    private void showStepBottomSheet(String title, String description) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_step, null);
        
        TextView tvTitle = view.findViewById(R.id.tvStepTitle);
        TextView tvDesc  = view.findViewById(R.id.tvStepDescription);
        Button btnGotIt  = view.findViewById(R.id.btnGotIt);
        
        tvTitle.setText(title);
        tvDesc.setText(description);
        btnGotIt.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void animateClick(View view, Runnable onEnd) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.85f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.85f, 1f)
        );
        animator.setDuration(200);
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onEnd != null) onEnd.run();
            }
        });
        animator.start();
    }
}
