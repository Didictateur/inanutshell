package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import fr.didictateur.inanutshell.ui.main.MainActivity;
import fr.didictateur.inanutshell.ui.setup.SetupActivity;
import fr.didictateur.inanutshell.utils.MealiePreferences;

public class LauncherActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MealiePreferences preferences = new MealiePreferences(this);
        
        Intent intent;
        if (preferences.hasValidCredentials()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, SetupActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
}
