package fr.didictateur.inanutshell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MealPlannerFragment extends Fragment {
    
    private AppDatabase db;
    private TextView currentWeekText;
    private RecyclerView weekRecyclerView;
    private MealPlanWeekAdapter weekAdapter;
    private Calendar currentWeekStart;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meal_planner, container, false);
        
        // Initialisation
        db = AppDatabase.getInstance(requireContext());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        // Configuration des vues
        setupViews(view);
        
        // Configuration de la semaine actuelle
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);
        
        loadCurrentWeek();
        
        return view;
    }
    
    private void setupViews(View view) {
        currentWeekText = view.findViewById(R.id.current_week_text);
        weekRecyclerView = view.findViewById(R.id.week_recycler_view);
        
        // Configuration du RecyclerView
        weekRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        weekAdapter = new MealPlanWeekAdapter(new ArrayList<>(), 
            this::onMealPlanClick,
            this::onAddMealClick,
            this::onDeleteMealClick);
        weekRecyclerView.setAdapter(weekAdapter);
        
        // Boutons de navigation
        Button prevWeekBtn = view.findViewById(R.id.previous_week_btn);
        Button nextWeekBtn = view.findViewById(R.id.next_week_btn);
        
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
            
            requireActivity().runOnUiThread(() -> {
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
                requireActivity().runOnUiThread(() -> {
                    if (recette != null) {
                        Intent intent = new Intent(requireContext(), ViewRecetteActivity.class);
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
    
    private void showSelectRecipeDialog(String date, String mealType) {
        // Charger toutes les recettes
        new Thread(() -> {
            List<Recette> recettes = db.recetteDao().getAllRecettes();
            
            requireActivity().runOnUiThread(() -> {
                if (recettes.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucune recette disponible. Créez d'abord des recettes.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Créer un dialog avec spinner pour sélectionner la recette
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Sélectionner une recette");
                
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_recipe, null);
                Spinner recipeSpinner = dialogView.findViewById(R.id.recipeSpinner);
                EditText portionsEditText = dialogView.findViewById(R.id.portionsEditText);
                ImageButton decreaseButton = dialogView.findViewById(R.id.decreasePortionsButton);
                ImageButton increaseButton = dialogView.findViewById(R.id.increasePortionsButton);
                
                // Configuration des boutons de portions
                decreaseButton.setOnClickListener(v -> {
                    try {
                        double currentValue = Double.parseDouble(portionsEditText.getText().toString());
                        double newValue = Math.max(0.5, currentValue - 0.5);
                        portionsEditText.setText(formatPortions(newValue));
                    } catch (NumberFormatException e) {
                        portionsEditText.setText("1");
                    }
                });
                
                increaseButton.setOnClickListener(v -> {
                    try {
                        double currentValue = Double.parseDouble(portionsEditText.getText().toString());
                        double newValue = Math.min(20, currentValue + 0.5);
                        portionsEditText.setText(formatPortions(newValue));
                    } catch (NumberFormatException e) {
                        portionsEditText.setText("1");
                    }
                });
                
                // Adapter pour le spinner
                List<String> recipeNames = new ArrayList<>();
                for (Recette recette : recettes) {
                    recipeNames.add(recette.titre);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, recipeNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                recipeSpinner.setAdapter(adapter);
                
                builder.setView(dialogView);
                builder.setPositiveButton("Ajouter", (dialog, which) -> {
                    int selectedIndex = recipeSpinner.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < recettes.size()) {
                        Recette selectedRecette = recettes.get(selectedIndex);
                        String portions = portionsEditText.getText().toString();
                        addMealPlan(date, mealType, selectedRecette.id, portions);
                    }
                });
                
                builder.setNegativeButton("Annuler", null);
                builder.show();
            });
        }).start();
    }
    
    private void addMealPlan(String date, String mealType, Long recetteId, String portions) {
        new Thread(() -> {
            // Ajouter le nouveau repas avec les portions spécifiées
            MealPlan newMealPlan = new MealPlan(date, mealType, recetteId, null, portions);
            db.mealPlanDao().insert(newMealPlan);
            requireActivity().runOnUiThread(() -> {
                loadCurrentWeek();
                Toast.makeText(requireContext(), "Repas ajouté !", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private String formatPortions(double portions) {
        if (portions == Math.floor(portions)) {
            return String.valueOf((int) portions);
        } else {
            return String.format("%.1f", portions).replace(".0", "");
        }
    }
    
    private void onDeleteMealClick(MealPlanWithRecette mealPlan) {
        // Afficher une confirmation avant de supprimer
        new AlertDialog.Builder(requireContext())
            .setTitle("Supprimer le repas")
            .setMessage("Voulez-vous vraiment supprimer \"" + mealPlan.getRecetteTitre() + "\" ?")
            .setPositiveButton("Supprimer", (dialog, which) -> {
                // Supprimer de la base de données
                new Thread(() -> {
                    db.mealPlanDao().delete(mealPlan.getMealPlan());
                    requireActivity().runOnUiThread(() -> {
                        loadCurrentWeek();
                        Toast.makeText(requireContext(), "Repas supprimé", Toast.LENGTH_SHORT).show();
                    });
                }).start();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void applyThemeColorsToButtons(Button... buttons) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        
        int colorResId = getResources().getIdentifier(colorName, "color", requireContext().getPackageName());
        if (colorResId != 0) {
            int themeColor = ContextCompat.getColor(requireContext(), colorResId);
            for (Button button : buttons) {
                button.setTextColor(themeColor);
            }
        }
    }
}
