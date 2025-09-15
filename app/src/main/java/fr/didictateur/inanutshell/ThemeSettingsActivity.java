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
        themeManager.addThemeChangedListener(this);
        observeCurrentTheme();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeManager != null) {
            themeManager.removeThemeChangedListener(this);
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
        userManager = UserManager.getInstance(this);
    }
    
    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Personnalisation");
        
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
        themeAdapter = new ThemeAdapter(this, new ThemeAdapter.OnThemeClickListener() {
            @Override
            public void onThemeClick(Theme theme) {
                themeManager.applyTheme(theme);
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
    
    private void setupListeners() {
        // FAB pour créer un nouveau thème
        fabCreateTheme.setOnClickListener(v -> {
            User currentUser = userManager.getCurrentUser();
            if (currentUser != null) {
                String themeName = "Mon thème " + System.currentTimeMillis();
                themeManager.createAndApplyCustomTheme(themeName, 
                    "Thème personnalisé", currentUser.getUserId());
                loadThemes();
            }
        });
        
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
        textScaleSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float scale = 0.5f + (progress / 100.0f) * 1.5f; // 0.5x à 2.0x
                    textScaleValue.setText(String.format("%.1fx", scale));
                    themeManager.updateTextScale(scale);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        highContrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            themeManager.setHighContrast(isChecked));
        
        boldTextSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            themeManager.setBoldText(isChecked));
        
        reducedMotionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            themeManager.setReducedMotion(isChecked));
    }
    
    private void setupInterfaceListeners() {
        roundedCornersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Theme currentTheme = themeManager.getCurrentTheme();
            if (currentTheme != null) {
                currentTheme.setRoundedCorners(isChecked);
                themeManager.updateTheme(currentTheme);
            }
            cornerRadiusSeeker.setEnabled(isChecked);
        });
        
        cornerRadiusSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float radius = progress; // 0 à 50 dp
                    cornerRadiusValue.setText(String.format("%.0f dp", radius));
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
        
        elevationSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float elevation = progress; // 0 à 24 dp
                    elevationValue.setText(String.format("%.0f dp", elevation));
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
    
    private void setupAutoListeners() {
        autoThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            themeManager.setAutoThemeMode(isChecked));
        
        seasonalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            themeManager.setSeasonalEnabled(isChecked));
        
        materialYouSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Implementation for Material You if supported
        });
    }
    
    private void setupColorListeners() {
        primaryColorButton.setOnClickListener(v -> openColorPicker("primary"));
        secondaryColorButton.setOnClickListener(v -> openColorPicker("secondary"));
        backgroundColorButton.setOnClickListener(v -> openColorPicker("background"));
    }
    
    // ===================== Gestion des données =====================
    
    private void loadThemes() {
        User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            themeManager.getAvailableThemes(currentUser.getUserId()).observe(this, themes -> {
                if (themes != null) {
                    themeAdapter.updateThemes(themes);
                }
            });
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
                applyThemeToActivity(theme);
            }
        });
    }
    
    private void updateSettingsFromTheme(Theme theme) {
        // Accessibilité
        int textScaleProgress = (int) ((theme.getTextScale() - 0.5f) / 1.5f * 100);
        textScaleSeeker.setProgress(textScaleProgress);
        textScaleValue.setText(String.format("%.1fx", theme.getTextScale()));
        highContrastSwitch.setChecked(theme.isHighContrast());
        boldTextSwitch.setChecked(theme.isBoldText());
        reducedMotionSwitch.setChecked(theme.isReducedMotion());
        
        // Interface
        roundedCornersSwitch.setChecked(theme.isRoundedCorners());
        cornerRadiusSeeker.setProgress((int) theme.getCornerRadius());
        cornerRadiusSeeker.setEnabled(theme.isRoundedCorners());
        cornerRadiusValue.setText(String.format("%.0f dp", theme.getCornerRadius()));
        elevationSeeker.setProgress((int) theme.getElevation());
        elevationValue.setText(String.format("%.0f dp", theme.getElevation()));
        
        // Couleurs des boutons
        updateColorButtons(theme);
    }
    
    private void updateColorButtons(Theme theme) {
        // Mettre à jour l'apparence des boutons de couleur
        if (theme.getPrimaryColor() != null) {
            primaryColorButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(theme.getPrimaryColor())
                )
            );
        }
        
        if (theme.getSecondaryColor() != null) {
            secondaryColorButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(theme.getSecondaryColor())
                )
            );
        }
        
        if (theme.getBackgroundColor() != null) {
            backgroundColorButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(theme.getBackgroundColor())
                )
            );
        }
    }
    
    private void applyThemeToActivity(Theme theme) {
        // Appliquer le thème à l'activité actuelle
        ThemeApplicator.applyThemeToActivity(this, theme);
        ThemeApplicator.applyThemeToToolbar(toolbar, theme);
        ThemeApplicator.applyThemeToFAB(fabCreateTheme, theme);
        
        // Appliquer aux cartes
        ThemeApplicator.applyThemeToCard(accessibilityCard, theme);
        ThemeApplicator.applyThemeToCard(interfaceCard, theme);
        ThemeApplicator.applyThemeToCard(autoCard, theme);
        ThemeApplicator.applyThemeToCard(colorCard, theme);
    }
    
    // ===================== Actions =====================
    
    private void openThemeEditor(Theme theme) {
        Intent intent = new Intent(this, ThemeEditorActivity.class);
        intent.putExtra("theme_id", theme.getThemeId());
        startActivity(intent);
    }
    
    private void openColorPicker(String colorType) {
        Intent intent = new Intent(this, ColorPickerActivity.class);
        intent.putExtra("color_type", colorType);
        
        Theme currentTheme = themeManager.getCurrentTheme();
        if (currentTheme != null) {
            switch (colorType) {
                case "primary":
                    intent.putExtra("current_color", currentTheme.getPrimaryColor());
                    break;
                case "secondary":
                    intent.putExtra("current_color", currentTheme.getSecondaryColor());
                    break;
                case "background":
                    intent.putExtra("current_color", currentTheme.getBackgroundColor());
                    break;
            }
        }
        
        startActivityForResult(intent, 100);
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
}
