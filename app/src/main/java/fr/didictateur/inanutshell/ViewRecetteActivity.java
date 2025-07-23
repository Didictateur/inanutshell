package fr.didictateur.inanutshell;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import io.noties.markwon.Markwon;

public class ViewRecetteActivity extends BaseActivity implements TimerManager.TimerListener {
    private boolean isReadingMode = false;
    private TimerManager timerManager;
    private String currentTimerId = null;
    
    // Données de la recette
    private String titre, taille, tempsPrep, ingredients, preparation, notes;
    
    // Vues mode normal
    private TextView textTitre, textTaille, textTempsPrep, textIngredients, textPreparation, textNotes;
    
    // Vues mode lecture
    private TextView recetteTitreReading, tempsPrepReading, tailleReading;
    private TextView ingredientsReading, preparationReading;
    private LinearLayout timerLayout;
    private TextView timerDisplay;
    private Button pauseTimerButton, stopTimerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Récupération des données de la recette
        titre = getIntent().getStringExtra("titre");
        taille = getIntent().getStringExtra("taille");
        tempsPrep = getIntent().getStringExtra("tempsPrep");
        ingredients = getIntent().getStringExtra("ingredients");
        preparation = getIntent().getStringExtra("preparation");
        notes = getIntent().getStringExtra("notes");
        
        // Initialisation du timer manager
        timerManager = TimerManager.getInstance(this);
        
        // Mode lecture par défaut
        setupReadingMode();
    }
    
    private void setupReadingMode() {
        setContentView(R.layout.activity_view_recette_reading);
        isReadingMode = true;
        
        // Initialisation des vues mode lecture
        recetteTitreReading = findViewById(R.id.recetteTitreReading);
        tempsPrepReading = findViewById(R.id.tempsPrepReading);
        tailleReading = findViewById(R.id.tailleReading);
        ingredientsReading = findViewById(R.id.ingredientsReading);
        preparationReading = findViewById(R.id.preparationReading);
        timerLayout = findViewById(R.id.timerLayout);
        timerDisplay = findViewById(R.id.timerDisplay);
        pauseTimerButton = findViewById(R.id.pauseTimerButton);
        stopTimerButton = findViewById(R.id.stopTimerButton);
        
        // Remplissage des données
        recetteTitreReading.setText(titre);
        tempsPrepReading.setText(tempsPrep);
        tailleReading.setText(taille);
        ingredientsReading.setText(ingredients != null ? ingredients : "");
        preparationReading.setText(preparation != null ? preparation : "");
        
        // Configuration des boutons
        ImageButton notesButton = findViewById(R.id.exitReadingModeButton);
        notesButton.setImageResource(android.R.drawable.ic_menu_info_details);
        notesButton.setOnClickListener(v -> showNotesDialog());
        
        ImageButton timerButton = findViewById(R.id.timerButton);
        timerButton.setOnClickListener(v -> showTimerDialog());
        
        pauseTimerButton.setOnClickListener(v -> toggleTimer());
        stopTimerButton.setOnClickListener(v -> stopCurrentTimer());
        
        // Masquer le timer au début
        timerLayout.setVisibility(View.GONE);
    }
    
    private void showNotesDialog() {
        if (notes == null || notes.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("📝 Notes")
                .setMessage("Aucune note pour cette recette.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        
        // Créer une vue avec du texte formaté markdown
        ScrollView scrollView = new ScrollView(this);
        TextView notesTextView = new TextView(this);
        notesTextView.setPadding(48, 48, 48, 48);
        notesTextView.setTextSize(16);
        notesTextView.setLineSpacing(8, 1.2f);
        
        // Appliquer le markdown aux notes
        Markwon markdown = Markwon.create(this);
        markdown.setMarkdown(notesTextView, notes);
        
        scrollView.addView(notesTextView);
        
        new AlertDialog.Builder(this)
            .setTitle("📝 Notes de la recette")
            .setView(scrollView)
            .setPositiveButton("Fermer", null)
            .show();
    }
    
    private void showTimerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timer_setup, null);
        
        // Configuration des boutons rapides
        Button timer1Min = dialogView.findViewById(R.id.timer1MinButton);
        Button timer5Min = dialogView.findViewById(R.id.timer5MinButton);
        Button timer10Min = dialogView.findViewById(R.id.timer10MinButton);
        Button timer15Min = dialogView.findViewById(R.id.timer15MinButton);
        
        timer1Min.setOnClickListener(v -> startQuickTimer(1));
        timer5Min.setOnClickListener(v -> startQuickTimer(5));
        timer10Min.setOnClickListener(v -> startQuickTimer(10));
        timer15Min.setOnClickListener(v -> startQuickTimer(15));
        
        // Configuration des NumberPickers
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);
        NumberPicker secondsPicker = dialogView.findViewById(R.id.secondsPicker);
        TextInputEditText descriptionEdit = dialogView.findViewById(R.id.timerDescriptionEdit);
        
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59);
        minutesPicker.setValue(5);
        
        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(59);
        secondsPicker.setValue(0);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
        
        // Boutons d'action
        Button cancelButton = dialogView.findViewById(R.id.cancelTimerButton);
        Button startButton = dialogView.findViewById(R.id.startTimerButton);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        startButton.setOnClickListener(v -> {
            int minutes = minutesPicker.getValue();
            int seconds = secondsPicker.getValue();
            String description = descriptionEdit.getText().toString();
            if (description.isEmpty()) {
                description = "Timer de cuisine";
            }
            
            long durationMs = (minutes * 60 + seconds) * 1000;
            if (durationMs > 0) {
                startCustomTimer(durationMs, description);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void startQuickTimer(int minutes) {
        long durationMs = minutes * 60 * 1000;
        String description = minutes + " minute" + (minutes > 1 ? "s" : "");
        startCustomTimer(durationMs, description);
    }
    
    private void startCustomTimer(long durationMs, String description) {
        if (currentTimerId != null) {
            timerManager.stopTimer(currentTimerId);
        }
        
        currentTimerId = timerManager.createTimer(description, durationMs, this);
        timerManager.startTimer(currentTimerId);
        
        // Afficher le timer
        timerLayout.setVisibility(View.VISIBLE);
        pauseTimerButton.setText("⏸️ Pause");
    }
    
    private void toggleTimer() {
        if (currentTimerId != null) {
            TimerManager.CookingTimer timer = findCurrentTimer();
            if (timer != null) {
                if (timer.isRunning()) {
                    timerManager.pauseTimer(currentTimerId);
                    pauseTimerButton.setText("▶️ Reprendre");
                } else if (timer.isPaused()) {
                    timerManager.resumeTimer(currentTimerId);
                    pauseTimerButton.setText("⏸️ Pause");
                }
            }
        }
    }
    
    private void stopCurrentTimer() {
        if (currentTimerId != null) {
            timerManager.stopTimer(currentTimerId);
            currentTimerId = null;
            timerLayout.setVisibility(View.GONE);
        }
    }
    
    private TimerManager.CookingTimer findCurrentTimer() {
        for (TimerManager.CookingTimer timer : timerManager.getActiveTimers()) {
            if (timer.getId().equals(currentTimerId)) {
                return timer;
            }
        }
        return null;
    }
    
    // Implémentation TimerListener
    @Override
    public void onTimerTick(String timerId, long millisUntilFinished) {
        if (timerId.equals(currentTimerId) && timerDisplay != null) {
            runOnUiThread(() -> {
                String timeText = TimerManager.formatTime(millisUntilFinished);
                timerDisplay.setText(timeText);
            });
        }
    }
    
    @Override
    public void onTimerFinished(String timerId, String description) {
        if (timerId.equals(currentTimerId)) {
            runOnUiThread(() -> {
                currentTimerId = null;
                timerLayout.setVisibility(View.GONE);
                
                // Afficher une alerte
                new AlertDialog.Builder(this)
                    .setTitle("⏰ Timer terminé !")
                    .setMessage(description + "\n\nC'est prêt !")
                    .setPositiveButton("OK", null)
                    .show();
            });
        }
    }
    
    @Override
    public void onTimerCancelled(String timerId) {
        if (timerId.equals(currentTimerId)) {
            runOnUiThread(() -> {
                currentTimerId = null;
                timerLayout.setVisibility(View.GONE);
            });
        }
    }

	private int getStatusBarColor() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
		String statusBarColorName = colorName.replace("toolbar_", "statusbar_");
		int statusBarColorResId = getResources().getIdentifier(
				statusBarColorName,
				"color",
				getPackageName()
		);

		return ContextCompat.getColor(this, statusBarColorResId);
	}
}
