package fr.didictateur.inanutshell.ui.sync;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Activité pour résoudre les conflits de synchronisation
 * TODO: Implémenter l'interface complète de résolution de conflits
 */
public class ConflictResolutionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Interface temporaire
        TextView textView = new TextView(this);
        textView.setText("Résolution de conflits - À implémenter");
        textView.setPadding(32, 32, 32, 32);
        textView.setTextSize(16);
        
        setContentView(textView);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Résolution de conflit");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
