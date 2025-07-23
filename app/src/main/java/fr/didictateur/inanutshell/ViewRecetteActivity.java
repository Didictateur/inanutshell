package fr.didictateur.inanutshell;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
    private TextView recetteTitreReading, tempsPrepReading;
    private TextView ingredientsReading, preparationReading;
    private EditText currentPortionsText;
    private ImageButton decreasePortionsButton, increasePortionsButton;
    private LinearLayout timerLayout;
    private TextView timerDisplay;
    private Button pauseTimerButton, stopTimerButton;
    
    // Gestion des portions
    private double originalPortions = 1.0;
    private double currentPortions = 1.0;
    private String originalIngredientsText = "";

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
        ingredientsReading = findViewById(R.id.ingredientsReading);
        preparationReading = findViewById(R.id.preparationReading);
        currentPortionsText = findViewById(R.id.currentPortionsText);
        decreasePortionsButton = findViewById(R.id.decreasePortionsButton);
        increasePortionsButton = findViewById(R.id.increasePortionsButton);
        timerLayout = findViewById(R.id.timerLayout);
        timerDisplay = findViewById(R.id.timerDisplay);
        pauseTimerButton = findViewById(R.id.pauseTimerButton);
        stopTimerButton = findViewById(R.id.stopTimerButton);
        
        // Extraire le nombre de portions original
        extractOriginalPortions();
        
        // Remplissage des données
        recetteTitreReading.setText(titre);
        tempsPrepReading.setText(tempsPrep);
        originalIngredientsText = ingredients != null ? ingredients : "";
        preparationReading.setText(preparation != null ? preparation : "");
        
        // Mise à jour de l'affichage des portions
        updatePortionsDisplay();
        
        // Configuration de l'EditText pour les portions
        currentPortionsText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (!s.toString().isEmpty()) {
                        double newPortions = Double.parseDouble(s.toString().replace(",", "."));
                        if (newPortions > 0 && newPortions <= 50) { // Limite raisonnable
                            if (Math.abs(currentPortions - newPortions) > 0.001) { // Éviter les boucles infinies
                                currentPortions = newPortions;
                                updateIngredientsOnly();
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les entrées invalides
                }
            }
        });
        
        // Configuration des boutons de portions
        decreasePortionsButton.setOnClickListener(v -> {
            double newValue = Math.max(0.1, currentPortions - 0.5);
            currentPortions = newValue;
            updatePortionsDisplay();
        });
        
        increasePortionsButton.setOnClickListener(v -> {
            double newValue = Math.min(50, currentPortions + 0.5);
            currentPortions = newValue;
            updatePortionsDisplay();
        });
        
        // Configuration des autres boutons
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
    
    private void extractOriginalPortions() {
        // Extraire le nombre de portions depuis le texte "taille"
        if (taille != null && !taille.isEmpty()) {
            // Chercher des patterns comme "4 pers", "6 personnes", "2.5", etc.
            String[] patterns = {"(\\d+(?:[.,]\\d+)?)\\s*pers", "(\\d+(?:[.,]\\d+)?)\\s*personnes?", "^(\\d+(?:[.,]\\d+)?)$"};
            
            for (String pattern : patterns) {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(taille.trim());
                if (m.find()) {
                    try {
                        String numberStr = m.group(1).replace(",", ".");
                        originalPortions = Double.parseDouble(numberStr);
                        currentPortions = originalPortions;
                        // Debug: Log des portions extraites
                        Log.d("ViewRecette", "Portions extraites de '" + taille + "': " + originalPortions);
                        return;
                    } catch (NumberFormatException e) {
                        // Continuer avec le pattern suivant
                    }
                }
            }
        }
        
        // Par défaut, considérer 1 portion
        originalPortions = 1.0;
        currentPortions = 1.0;
        Log.d("ViewRecette", "Aucune portion trouvée dans '" + taille + "', défaut: 1 portion");
    }
    
    private void updatePortionsDisplay() {
        // Mettre à jour le texte du nombre de portions
        String formattedPortions = formatPortions(currentPortions);
        currentPortionsText.setText(formattedPortions);
        
        // Calculer et afficher les ingrédients ajustés
        updateIngredientsOnly();
        
        // Mettre à jour l'état des boutons
        decreasePortionsButton.setEnabled(currentPortions > 0.1);
        increasePortionsButton.setEnabled(currentPortions < 50);
    }
    
    private void updateIngredientsOnly() {
        String adjustedIngredients = calculateAdjustedIngredients();
        ingredientsReading.setText(adjustedIngredients);
    }
    
    private String formatPortions(double portions) {
        if (portions == Math.floor(portions)) {
            return String.valueOf((int) portions);
        } else {
            return String.format("%.1f", portions).replace(".0", "");
        }
    }
    
    private String calculateAdjustedIngredients() {
        if (originalIngredientsText == null || originalIngredientsText.isEmpty()) {
            return "";
        }
        
        if (Math.abs(currentPortions - originalPortions) < 0.001) {
            return originalIngredientsText;
        }
        
        // Le ratio correct : multiplier par le nombre de portions actuel
        // Exemple: recette pour 1 portion, on veut 2 portions -> multiplier par 2
        double multiplier = currentPortions / originalPortions;
        
        // Debug: Log du calcul
        Log.d("ViewRecette", "Original: " + originalPortions + " portions, Actuel: " + currentPortions + " portions, Multiplier: " + multiplier);
        
        // IMPORTANT: Toujours partir du texte original pour éviter les erreurs cumulatives
        String result = originalIngredientsText;
        
        // Pattern simple et efficace pour capturer quantité + unité
        String pattern = "\\b(\\d+(?:[.,]\\d+)?|\\d+/\\d+)\\s*(g|kg|mg|l|ml|cl|dl|cuillères?|cuill|c\\.|cs|cc|tasses?|t\\.|sachets?|pincées?|œufs?|blancs?|jaunes?)\\b";
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(result);
        
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        
        while (m.find()) {
            sb.append(result.substring(lastEnd, m.start()));
            
            String quantityStr = m.group(1);
            String unit = m.group(2);
            
            double originalValue = parseQuantity(quantityStr);
            double newValue = originalValue * multiplier;
            String formattedValue = formatQuantity(newValue);
            
            Log.d("ViewRecette", "Transform: " + quantityStr + " " + unit + " -> " + formattedValue + " " + unit + " (x" + multiplier + ")");
            
            sb.append(formattedValue).append(" ").append(unit);
            lastEnd = m.end();
        }
        sb.append(result.substring(lastEnd));
        
        Log.d("ViewRecette", "Final: '" + originalIngredientsText + "' -> '" + sb.toString() + "'");
        
        return sb.toString();
    }
    
    private double parseQuantity(String quantity) {
        try {
            // Gérer les fractions
            if (quantity.contains("/")) {
                String[] parts = quantity.split("/");
                if (parts.length == 2) {
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    return numerator / denominator;
                }
            }
            
            // Gérer les nombres décimaux (avec virgule ou point)
            String normalizedQuantity = quantity.replace(",", ".");
            return Double.parseDouble(normalizedQuantity);
            
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private String formatQuantity(double value) {
        // Arrondir les très petites valeurs à 0
        if (value < 0.01) {
            return "0";
        }
        
        // Si c'est un nombre entier, l'afficher sans décimales
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.valueOf(Math.round(value));
        }
        
        // Pour les fractions simples courantes, les afficher comme telles
        if (Math.abs(value - 0.5) < 0.01) return "1/2";
        if (Math.abs(value - 0.25) < 0.01) return "1/4";
        if (Math.abs(value - 0.75) < 0.01) return "3/4";
        if (Math.abs(value - 1.5) < 0.01) return "1,5";
        if (Math.abs(value - 2.5) < 0.01) return "2,5";
        
        // Sinon, formater avec 1-2 décimales maximum
        if (value < 1) {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "").replace(".", ",");
        } else {
            return String.format("%.1f", value).replaceAll("0+$", "").replaceAll("\\.$", "").replace(".", ",");
        }
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
