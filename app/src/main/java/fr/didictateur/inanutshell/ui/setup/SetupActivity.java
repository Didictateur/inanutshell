package fr.didictateur.inanutshell.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivitySetupBinding;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.ui.main.MainActivity;
import fr.didictateur.inanutshell.utils.MealiePreferences;

public class SetupActivity extends AppCompatActivity {
    
    private ActivitySetupBinding binding;
    private MealiePreferences preferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup);
        
        preferences = new MealiePreferences(this);
        
        setupViews();
        setupListeners();
    }
    
    private void setupViews() {
        // Pre-fill with saved values if they exist
        String savedUrl = preferences.getMealieServerUrl();
        String savedEmail = preferences.getMealieEmail();
        
        if (!savedUrl.isEmpty()) {
            binding.etServerUrl.setText(savedUrl);
        }
        if (!savedEmail.isEmpty()) {
            binding.etEmail.setText(savedEmail);
        }
    }
    
    private void setupListeners() {
        binding.btnConnect.setOnClickListener(v -> attemptConnection());
        binding.btnSkip.setOnClickListener(v -> skipSetup());
    }
    
    private void attemptConnection() {
        String serverUrl = binding.etServerUrl.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        
        if (!validateInput(serverUrl, email, password)) {
            return;
        }
        
        // Normalize server URL
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "https://" + serverUrl;
        }
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        
        setLoadingState(true);
        
        // Create final copies for lambda
        final String finalServerUrl = serverUrl;
        final String finalEmail = email;
        
        // Test connection and authenticate
        NetworkManager.getInstance().login(serverUrl, email, password, new NetworkManager.LoginCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    // Save credentials - the token is already saved by NetworkManager
                    String savedToken = fr.didictateur.inanutshell.MealieApplication.getInstance().getMealieAuthToken();
                    preferences.setMealieCredentials(finalServerUrl, finalEmail, savedToken);
                    
                    setLoadingState(false);
                    Toast.makeText(SetupActivity.this, getString(R.string.connection_successful), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to main activity
                    startActivity(new Intent(SetupActivity.this, MainActivity.class));
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(SetupActivity.this, 
                        getString(R.string.connection_failed, error), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private boolean validateInput(String serverUrl, String email, String password) {
        if (serverUrl.isEmpty()) {
            binding.etServerUrl.setError(getString(R.string.error_server_url_required));
            return false;
        }
        
        if (!isValidUrl(serverUrl)) {
            binding.etServerUrl.setError(getString(R.string.error_invalid_url));
            return false;
        }
        
        if (email.isEmpty()) {
            binding.etEmail.setError(getString(R.string.error_email_required));
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        
        if (password.isEmpty()) {
            binding.etPassword.setError(getString(R.string.error_password_required));
            return false;
        }
        
        return true;
    }
    
    private boolean isValidUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return Patterns.WEB_URL.matcher(url).matches();
    }
    
    private void setLoadingState(boolean loading) {
        binding.btnConnect.setEnabled(!loading);
        binding.btnSkip.setEnabled(!loading);
        binding.etServerUrl.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
        
        if (loading) {
            binding.btnConnect.setText(getString(R.string.connecting));
        } else {
            binding.btnConnect.setText(getString(R.string.connect));
        }
    }
    
    private void skipSetup() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
