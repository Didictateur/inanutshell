package fr.didictateur.inanutshell;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MealPlannerActivity extends BaseActivity {
    
    private AppDatabase db;
    private TextView currentWeekText;
    private RecyclerView weekRecyclerView;
    private MealPlanWeekAdapter weekAdapter;
    private Calendar currentWeekStart;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);
        
        // Configuration de la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Planificateur de repas");
        }
        
        // Appliquer les couleurs du thème
        applyThemeColors();
        
        // Initialisation
        db = AppDatabase.getInstance(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        // Configuration des vues
        setupViews();
        
        // Configuration de la semaine actuelle
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);
        
        loadCurrentWeek();
    }
    
    private void setupViews() {
        currentWeekText = findViewById(R.id.currentWeekText);
        weekRecyclerView = findViewById(R.id.weekRecyclerView);
        
        // Configuration du RecyclerView
        weekRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        weekAdapter = new MealPlanWeekAdapter(new ArrayList<>(), this::onMealPlanClick, this::onAddMealClick);
        weekRecyclerView.setAdapter(weekAdapter);
        
        // Boutons de navigation
        Button prevWeekBtn = findViewById(R.id.prevWeekBtn);
        Button nextWeekBtn = findViewById(R.id.nextWeekBtn);
        
        // Appliquer les couleurs du thème aux boutons
        applyThemeColorsToButtons(prevWeekBtn, nextWeekBtn);
        
        prevWeekBtn.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            loadCurrentWeek();
        });
        
        nextWeekBtn.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            loadCurrentWeek();
        });
        
        // FAB pour ajouter rapidement un repas
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showQuickAddMealDialog());
    }
    
    private void loadCurrentWeek() {
        // Calculer la fin de la semaine
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        String startDate = dateFormat.format(currentWeekStart.getTime());
        String endDate = dateFormat.format(weekEnd.getTime());
        
        // Mettre à jour l'affichage de la semaine
        String weekDisplay = displayDateFormat.format(currentWeekStart.getTime()) + 
                           " - " + displayDateFormat.format(weekEnd.getTime());
        currentWeekText.setText(weekDisplay);
        
        // Charger les repas de la semaine
        new Thread(() -> {
            List<MealPlanWithRecette> mealPlans = db.mealPlanDao().getMealPlansWithRecetteForWeek(startDate, endDate);
            
            // Organiser les repas par jour
            List<DayMealPlan> dayMealPlans = organizeMealsByDay(mealPlans);
            
            runOnUiThread(() -> {
                weekAdapter.updateMealPlans(dayMealPlans);
            });
        }).start();
    }
    
    private List<DayMealPlan> organizeMealsByDay(List<MealPlanWithRecette> mealPlans) {
        List<DayMealPlan> dayMealPlans = new ArrayList<>();
        
        // Créer 7 jours de la semaine
        Calendar dayCalendar = (Calendar) currentWeekStart.clone();
        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(dayCalendar.getTime());
            String dayName = new SimpleDateFormat("EEEE", Locale.getDefault()).format(dayCalendar.getTime());
            
            DayMealPlan dayMealPlan = new DayMealPlan(date, dayName);
            
            // Ajouter les repas pour ce jour
            for (MealPlanWithRecette mealPlan : mealPlans) {
                if (mealPlan.getMealPlan().getDate().equals(date)) {
                    dayMealPlan.addMeal(mealPlan);
                }
            }
            
            dayMealPlans.add(dayMealPlan);
            dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        return dayMealPlans;
    }
    
    private void onMealPlanClick(MealPlanWithRecette mealPlan) {
        // Ouvrir la recette associée
        if (mealPlan.getMealPlan().getRecetteId() != null) {
            new Thread(() -> {
                Recette recette = db.recetteDao().getRecetteById(mealPlan.getMealPlan().getRecetteId());
                runOnUiThread(() -> {
                    if (recette != null) {
                        Intent intent = new Intent(this, ViewRecetteActivity.class);
                        intent.putExtra("recette_id", recette.id);
                        startActivity(intent);
                    }
                });
            }).start();
        }
    }
    
    private void onAddMealClick(String date, String mealType) {
        showSelectRecipeDialog(date, mealType);
    }
    
    private void showQuickAddMealDialog() {
        // Dialog pour ajouter rapidement un repas pour aujourd'hui
        Calendar today = Calendar.getInstance();
        String todayDate = dateFormat.format(today.getTime());
        showSelectRecipeDialog(todayDate, "lunch");
    }
    
    private void showSelectRecipeDialog(String date, String mealType) {
        // Charger toutes les recettes
        new Thread(() -> {
            List<Recette> recettes = db.recetteDao().getAllRecettes();
            
            runOnUiThread(() -> {
                if (recettes.isEmpty()) {
                    Toast.makeText(this, "Aucune recette disponible. Créez d'abord des recettes.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Créer un dialog avec spinner pour sélectionner la recette
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Sélectionner une recette");
                
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_recipe, null);
                Spinner recipeSpinner = dialogView.findViewById(R.id.recipeSpinner);
                
                // Adapter pour le spinner
                List<String> recipeNames = new ArrayList<>();
                for (Recette recette : recettes) {
                    recipeNames.add(recette.titre);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recipeNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                recipeSpinner.setAdapter(adapter);
                
                builder.setView(dialogView);
                builder.setPositiveButton("Ajouter", (dialog, which) -> {
                    int selectedIndex = recipeSpinner.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < recettes.size()) {
                        Recette selectedRecette = recettes.get(selectedIndex);
                        addMealPlan(date, mealType, selectedRecette.id);
                    }
                });
                
                builder.setNegativeButton("Annuler", null);
                builder.show();
            });
        }).start();
    }
    
    private void addMealPlan(String date, String mealType, Long recetteId) {
        new Thread(() -> {
            // Vérifier si un repas existe déjà pour cette date et ce type
            List<MealPlan> existingMeals = db.mealPlanDao().getMealPlansForDate(date);
            boolean mealExists = false;
            for (MealPlan meal : existingMeals) {
                if (meal.getMealType().equals(mealType)) {
                    mealExists = true;
                    break;
                }
            }
            
            if (mealExists) {
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Remplacer le repas ?");
                    builder.setMessage("Un repas est déjà planifié pour ce moment. Voulez-vous le remplacer ?");
                    builder.setPositiveButton("Remplacer", (dialog, which) -> {
                        new Thread(() -> {
                            db.mealPlanDao().deleteMealPlan(date, mealType);
                            MealPlan newMealPlan = new MealPlan(date, mealType, recetteId, null);
                            db.mealPlanDao().insert(newMealPlan);
                            runOnUiThread(() -> {
                                loadCurrentWeek();
                                Toast.makeText(this, "Repas mis à jour !", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    });
                    builder.setNegativeButton("Annuler", null);
                    builder.show();
                });
            } else {
                MealPlan newMealPlan = new MealPlan(date, mealType, recetteId, null);
                db.mealPlanDao().insert(newMealPlan);
                runOnUiThread(() -> {
                    loadCurrentWeek();
                    Toast.makeText(this, "Repas ajouté !", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void applyThemeColors() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        
        // Appliquer la couleur à la toolbar
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        if (colorResId != 0) {
            int toolbarColor = ContextCompat.getColor(this, colorResId);
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setBackgroundColor(toolbarColor);
        }
    }
    
    private void applyThemeColorsToButtons(Button... buttons) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        if (colorResId != 0) {
            int themeColor = ContextCompat.getColor(this, colorResId);
            for (Button button : buttons) {
                button.setTextColor(themeColor);
            }
        }
    }
}
