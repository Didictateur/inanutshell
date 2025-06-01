package fr.didictateur.inanutshell;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBarColor();
    }

    protected void applyStatusBarColor() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_orange");
        String statusBarColorName = colorName.replace("toolbar_", "statusbar_");
        int statusBarColorResId = getResources().getIdentifier(statusBarColorName, "color", getPackageName());
        int statusBarColor = ContextCompat.getColor(this, statusBarColorResId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(statusBarColor);
        }
    }

		@Override
		protected void onResume() {
			super.onResume();
			applyStatusBarColor();
		}
}

