package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.widget.TextView;

public class ViewRecetteActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView textView = new TextView(this);
        textView.setText("Visualisation de Recette");
        setContentView(textView);
    }
}
