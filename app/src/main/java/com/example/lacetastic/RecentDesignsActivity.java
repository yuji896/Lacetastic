package com.example.lacetastic;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecentDesignsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent_designs_layout);

        View rootView = findViewById(R.id.rootLayout);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }

        TextView tvProfileInitial = findViewById(R.id.tvProfileInitial);
        SharedPreferences prefs = getSharedPreferences(Setprofile.PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(Setprofile.KEY_NICKNAME, "");
        if (tvProfileInitial != null && !nickname.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(nickname.charAt(0)).toUpperCase());
        }

        ImageView ivHomeTab = findViewById(R.id.ivHomeTab);
        if (ivHomeTab != null) {
            ivHomeTab.setOnClickListener(v -> {
                animateClick(v, () -> {
                    Intent intent = new Intent(RecentDesignsActivity.this, HomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                });
            });
        }

        ImageView ivPenIcon = findViewById(R.id.ivPenIcon);
        if (ivPenIcon != null) {
            ivPenIcon.setOnClickListener(v -> {
                animateClick(v, () -> {
                    startActivity(new Intent(RecentDesignsActivity.this, CustomizationPage.class));
                });
            });
        }

        View historyTab = findViewById(R.id.ivHistoryTab);
        if (historyTab != null) {
            historyTab.setOnClickListener(v -> animateClick(v, null));
        }
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
