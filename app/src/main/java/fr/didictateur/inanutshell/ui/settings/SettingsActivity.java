package fr.didictateur.inanutshell.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivitySettingsBinding;
import fr.didictateur.inanutshell.ui.setup.SetupActivity;
import fr.didictateur.inanutshell.utils.MealiePreferences;

public class SettingsActivity extends AppCompatActivity {
    
    private ActivitySettingsBinding binding;
    private MealiePreferences preferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        
        preferences = new MealiePreferences(this);
        
        setupToolbar();
        setupViews();
        setupListeners();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings));
        }
    }
    
    private void setupViews() {
        // Show current server info if available
        if (preferences.hasValidCredentials()) {
            binding.tvServerUrl.setText(preferences.getMealieServerUrl());
            binding.tvEmail.setText(preferences.getMealieEmail());
        } else {
            binding.tvServerUrl.setText(getString(R.string.not_configured));
            binding.tvEmail.setText(getString(R.string.not_configured));
        }
    }
    
    private void setupListeners() {
        binding.btnReconfigure.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupActivity.class));
        });
        
        binding.btnClearData.setOnClickListener(v -> {
            preferences.clearMealieCredentials();
            Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show();
            setupViews(); // Refresh the UI
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
