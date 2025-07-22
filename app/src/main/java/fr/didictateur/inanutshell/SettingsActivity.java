package fr.didictateur.inanutshell;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnOrange = findViewById(R.id.btnOrange);
        ImageButton btnBlue = findViewById(R.id.btnBlue);
        ImageButton btnGreen = findViewById(R.id.btnGreen);
        ImageButton btnBrown = findViewById(R.id.btnBrown);
        ImageButton btnRed = findViewById(R.id.btnRed);
        ImageButton btnPurple = findViewById(R.id.btnPurple);

        btnOrange.setOnClickListener(v -> saveColor("toolbar_bg_orange"));
        btnBlue.setOnClickListener(v -> saveColor("toolbar_bg_blue"));
        btnGreen.setOnClickListener(v -> saveColor("toolbar_bg_green"));
        btnBrown.setOnClickListener(v -> saveColor("toolbar_bg_brown"));
        btnRed.setOnClickListener(v -> saveColor("toolbar_bg_red"));
        btnPurple.setOnClickListener(v -> saveColor("toolbar_bg_purple"));
    }

    private void saveColor(String colorName) {
        android.util.Log.d("SettingsActivity", "saveColor called with: " + colorName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean success = prefs.edit().putString("toolbar_color", colorName).commit();
        android.util.Log.d("SettingsActivity", "Color saved: " + success + ", value: " + colorName);
        setResult(RESULT_OK);
        android.util.Log.d("SettingsActivity", "Result set to RESULT_OK, finishing activity");
        finish();
    }
}

