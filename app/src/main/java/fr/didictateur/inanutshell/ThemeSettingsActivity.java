package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

/**
 * Activité pour la personnalisation et la gestion des thèmes
 */
public class ThemeSettingsActivity extends AppCompatActivity implements ThemeManager.OnThemeChangedListener {
    
    private ThemeManager themeManager;
    private UserManager userManager;
    
    // Vues de l'interface
    private MaterialToolbar toolbar;
    private RecyclerView themeRecyclerView;
    private ThemeAdapter themeAdapter;
    private FloatingActionButton fabCreateTheme;
    
    // Paramètres d'accessibilité
    private MaterialCardView accessibilityCard;
    private SeekBar textScaleSeeker;
    private TextView textScaleValue;
    private Switch highContrastSwitch;
    private Switch boldTextSwitch;
    private Switch reducedMotionSwitch;
    
    // Paramètres d'interface
    private MaterialCardView interfaceCard;
    private Switch roundedCornersSwitch;
    private SeekBar cornerRadiusSeeker;
    private TextView cornerRadiusValue;
    private SeekBar elevationSeeker;
    private TextView elevationValue;
    
    // Paramètres automatiques
    private MaterialCardView autoCard;
    private Switch autoThemeSwitch;
    private Switch seasonalSwitch;
    private Switch materialYouSwitch;
    
    // Couleurs personnalisées
    private MaterialCardView colorCard;
    private LinearLayout colorButtonsLayout;
    private Button primaryColorButton;
    private Button secondaryColorButton;
    private Button backgroundColorButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);
        
        initializeManagers();
        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadCurrentSettings();
        
        // Observer les changements de thème
        // themeManager.addThemeChangedListener(this); // Method not implemented yet
        observeCurrentTheme();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeManager != null) {
            // themeManager.removeThemeChangedListener(this); // Method not implemented yet
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // ===================== Initialisation =====================
    
    private void initializeManagers() {
        themeManager = ThemeManager.getInstance(this);
        // userManager = UserManager.getInstance(this); // Method not available
    }
    
    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Personnalisation");
        }
        
        // RecyclerView pour les thèmes
        themeRecyclerView = findViewById(R.id.themeRecyclerView);
        fabCreateTheme = findViewById(R.id.fabCreateTheme);
        
        // Cartes de paramètres
        accessibilityCard = findViewById(R.id.accessibilityCard);
        interfaceCard = findViewById(R.id.interfaceCard);
        autoCard = findViewById(R.id.autoCard);
        colorCard = findViewById(R.id.colorCard);
        
        // Paramètres d'accessibilité
        textScaleSeeker = findViewById(R.id.textScaleSeeker);
        textScaleValue = findViewById(R.id.textScaleValue);
        highContrastSwitch = findViewById(R.id.highContrastSwitch);
        boldTextSwitch = findViewById(R.id.boldTextSwitch);
        reducedMotionSwitch = findViewById(R.id.reducedMotionSwitch);
        
        // Paramètres d'interface
        roundedCornersSwitch = findViewById(R.id.roundedCornersSwitch);
        cornerRadiusSeeker = findViewById(R.id.cornerRadiusSeeker);
        cornerRadiusValue = findViewById(R.id.cornerRadiusValue);
        elevationSeeker = findViewById(R.id.elevationSeeker);
        elevationValue = findViewById(R.id.elevationValue);
        
        // Paramètres automatiques
        autoThemeSwitch = findViewById(R.id.autoThemeSwitch);
        seasonalSwitch = findViewById(R.id.seasonalSwitch);
        materialYouSwitch = findViewById(R.id.materialYouSwitch);
        
        // Boutons de couleurs
        colorButtonsLayout = findViewById(R.id.colorButtonsLayout);
        primaryColorButton = findViewById(R.id.primaryColorButton);
        secondaryColorButton = findViewById(R.id.secondaryColorButton);
        backgroundColorButton = findViewById(R.id.backgroundColorButton);
    }
    
    private void setupRecyclerView() {
        if (themeRecyclerView != null) {
            themeAdapter = new ThemeAdapter(this, new ThemeAdapter.OnThemeClickListener() {
                @Override
                public void onThemeClick(Theme theme) {
                    themeManager.applyTheme(theme.getThemeId());
                }
                
                @Override
                public void onThemeEdit(Theme theme) {
                    openThemeEditor(theme);
                }
                
                @Override
                public void onThemeDelete(Theme theme) {
                    if (!theme.isDefault()) {
                        themeManager.deleteTheme(theme.getThemeId());
                        loadThemes();
                    }
                }
            });
            
            themeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            themeRecyclerView.setAdapter(themeAdapter);
            
            loadThemes();
        }
    }
    
    private void setupListeners() {
        // FAB pour créer un nouveau thème
        if (fabCreateTheme != null) {
            fabCreateTheme.setOnClickListener(v -> {
                createCustomTheme();
            });
        }
        
        // Listeners pour l'accessibilité
        setupAccessibilityListeners();
        
        // Listeners pour l'interface
        setupInterfaceListeners();
        
        // Listeners pour les paramètres automatiques
        setupAutoListeners();
        
        // Listeners pour les couleurs
        setupColorListeners();
    }
    
    private void setupAccessibilityListeners() {
        if (textScaleSeeker != null) {
            textScaleSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float scale = 0.5f + (progress / 100.0f) * 1.5f; // 0.5x à 2.0x
                        if (textScaleValue != null) {
                            textScaleValue.setText(String.format("%.1fx", scale));
                        }
                        themeManager.updateTextScale(scale);
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        if (highContrastSwitch != null) {
            highContrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                themeManager.setHighContrast(isChecked));
        }
        
        if (boldTextSwitch != null) {
            boldTextSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                themeManager.setBoldText(isChecked));
        }
        
        if (reducedMotionSwitch != null) {
            reducedMotionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                themeManager.setReducedMotion(isChecked));
        }
    }
    
    private void setupInterfaceListeners() {
        if (roundedCornersSwitch != null) {
            roundedCornersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Theme currentTheme = themeManager.getCurrentTheme();
                if (currentTheme != null) {
                    currentTheme.setRoundedCorners(isChecked);
                    themeManager.updateTheme(currentTheme);
                }
                if (cornerRadiusSeeker != null) {
                    cornerRadiusSeeker.setEnabled(isChecked);
                }
            });
        }
        
        if (cornerRadiusSeeker != null) {
            cornerRadiusSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float radius = progress; // 0 à 50 dp
                    if (cornerRadiusValue != null) {
                        cornerRadiusValue.setText(String.format("%.0f dp", radius));
                    }
                    Theme currentTheme = themeManager.getCurrentTheme();
                    if (currentTheme != null) {
                        currentTheme.setCornerRadius(radius);
                        themeManager.updateTheme(currentTheme);
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        }
        
        if (elevationSeeker != null) {
            elevationSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float elevation = progress; // 0 à 24 dp
                    if (elevationValue != null) {
                        elevationValue.setText(String.format("%.0f dp", elevation));
                    }
                    Theme currentTheme = themeManager.getCurrentTheme();
                    if (currentTheme != null) {
                        currentTheme.setElevation(elevation);
                        themeManager.updateTheme(currentTheme);
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        }
    }
    
    private void setupAutoListeners() {
        if (autoThemeSwitch != null) {
            autoThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // themeManager.setAutoThemeMode(isChecked);
            });
        }
        
        if (seasonalSwitch != null) {
            seasonalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // themeManager.setSeasonalEnabled(isChecked);
            });
        }
        
        if (materialYouSwitch != null) {
            materialYouSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Implementation for Material You if supported
            });
        }
    }
    
    private void setupColorListeners() {
        if (primaryColorButton != null) {
            primaryColorButton.setOnClickListener(v -> openColorPicker("primary"));
        }
        if (secondaryColorButton != null) {
            secondaryColorButton.setOnClickListener(v -> openColorPicker("secondary"));
        }
        if (backgroundColorButton != null) {
            backgroundColorButton.setOnClickListener(v -> openColorPicker("background"));
        }
    }
    
    // ===================== Actions simplifiées =====================
    
    private void openColorPicker(String colorType) {
        android.widget.Toast.makeText(this, "Sélecteur de couleur " + colorType + " - Bientôt disponible", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String colorType = data.getStringExtra("color_type");
            String selectedColor = data.getStringExtra("selected_color");
            
            if (colorType != null && selectedColor != null) {
                switch (colorType) {
                    case "primary":
                        themeManager.updateCurrentThemePrimaryColor(selectedColor);
                        break;
                    case "secondary":
                        themeManager.updateCurrentThemeSecondaryColor(selectedColor);
                        break;
                    case "background":
                        themeManager.updateCurrentThemeBackgroundColor(selectedColor);
                        break;
                }
            }
        }
    }
    
    // ===================== Implémentation ThemeManager.OnThemeChangedListener =====================
    
    @Override
    public void onThemeChanged(Theme newTheme) {
        runOnUiThread(() -> {
            updateSettingsFromTheme(newTheme);
            applyThemeToActivity(newTheme);
        });
    }
    
    @Override
    public void onThemeApplying() {
        // Optionnel: afficher un indicateur de chargement
    }
    
    @Override
    public void onThemeApplied() {
        // Optionnel: masquer l'indicateur de chargement
    }
    
    // ===================== Méthodes utilitaires simples =====================
    
    private void loadThemes() {
        // Méthode simplifiée - charge les thèmes disponibles
        if (themeAdapter != null) {
            // Pour l'instant, on utilise juste le thème actuel
            Theme currentTheme = themeManager.getCurrentTheme();
            if (currentTheme != null) {
                java.util.List<Theme> themes = new java.util.ArrayList<>();
                themes.add(currentTheme);
                themeAdapter.updateThemes(themes);
                themeAdapter.setCurrentTheme(currentTheme);
            }
        }
    }
    
    private void loadCurrentSettings() {
        Theme currentTheme = themeManager.getCurrentTheme();
        if (currentTheme != null) {
            updateSettingsFromTheme(currentTheme);
        }
    }
    
    private void observeCurrentTheme() {
        themeManager.getCurrentThemeLive().observe(this, theme -> {
            if (theme != null) {
                updateSettingsFromTheme(theme);
                if (themeAdapter != null) {
                    themeAdapter.setCurrentTheme(theme);
                }
            }
        });
    }
    
    private void updateSettingsFromTheme(Theme theme) {
        if (theme != null) {
            // Mise à jour des contrôles d'accessibilité
            if (textScaleSeeker != null && textScaleValue != null) {
                int progress = (int) ((theme.textScale - 0.8f) * 100f);
                textScaleSeeker.setProgress(Math.max(0, Math.min(100, progress)));
                textScaleValue.setText((int) (theme.textScale * 100) + "%");
            }
            
            if (highContrastSwitch != null) {
                highContrastSwitch.setChecked(theme.highContrast);
            }
            
            if (boldTextSwitch != null) {
                boldTextSwitch.setChecked(theme.boldText);
            }
            
            if (reducedMotionSwitch != null) {
                reducedMotionSwitch.setChecked(theme.reducedMotion);
            }
            
            // Mise à jour des contrôles d'interface
            if (roundedCornersSwitch != null) {
                roundedCornersSwitch.setChecked(theme.roundedCorners);
            }
            
            if (cornerRadiusSeeker != null && cornerRadiusValue != null) {
                cornerRadiusSeeker.setProgress((int) theme.cornerRadius);
                cornerRadiusValue.setText((int) theme.cornerRadius + "dp");
            }
            
            if (elevationSeeker != null && elevationValue != null) {
                elevationSeeker.setProgress((int) theme.elevation);
                elevationValue.setText((int) theme.elevation + "dp");
            }
            
            // Mise à jour des contrôles automatiques
            if (autoThemeSwitch != null) {
                autoThemeSwitch.setChecked(theme.themeType == Theme.ThemeType.AUTO);
            }
            
            if (seasonalSwitch != null) {
                seasonalSwitch.setChecked(theme.themeType == Theme.ThemeType.SEASONAL);
            }
            
            if (materialYouSwitch != null) {
                materialYouSwitch.setChecked(theme.themeType == Theme.ThemeType.MATERIAL_YOU);
            }
        }
    }
    
    private void applyThemeToActivity(Theme theme) {
        // Appliquer le thème à l'activité courante
        recreate();
    }
    
    private void openThemeEditor(Theme theme) {
        // TODO: Implémenter l'éditeur de thème
        android.widget.Toast.makeText(this, "Éditeur de thème - Bientôt disponible", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void createCustomTheme() {
        String themeName = "Mon thème " + System.currentTimeMillis();
        // Pour l'instant, on crée juste un thème simple
        android.widget.Toast.makeText(this, "Création de thème personnalisé - Bientôt disponible", android.widget.Toast.LENGTH_SHORT).show();
        loadThemes();
    }
}
