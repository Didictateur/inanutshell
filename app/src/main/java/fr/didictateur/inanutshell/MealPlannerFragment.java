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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
        
        // Boutons de liste de courses
        Button shoppingListDayBtn = view.findViewById(R.id.shopping_list_day_btn);
        Button shoppingListWeekBtn = view.findViewById(R.id.shopping_list_week_btn);
        
        // Appliquer les couleurs du thème aux boutons de shopping list
        applyThemeColorsToButtons(shoppingListDayBtn, shoppingListWeekBtn);
        
        shoppingListDayBtn.setOnClickListener(v -> showDaySelectionForShoppingList());
        shoppingListWeekBtn.setOnClickListener(v -> generateWeekShoppingList());
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
                Button decreaseButton = dialogView.findViewById(R.id.decreasePortionsButton);
                Button increaseButton = dialogView.findViewById(R.id.increasePortionsButton);
                
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
    
    private void showDaySelectionForShoppingList() {
        // Créer un dialog pour sélectionner le jour
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sélectionner un jour");
        
        // Créer la liste des jours de la semaine
        List<String> dayOptions = new ArrayList<>();
        List<String> dayDates = new ArrayList<>();
        
        Calendar dayCalendar = (Calendar) currentWeekStart.clone();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE dd MMM", Locale.getDefault());
        
        for (int i = 0; i < 7; i++) {
            String dayDisplay = dayFormat.format(dayCalendar.getTime());
            String dayDate = dateFormat.format(dayCalendar.getTime());
            
            dayOptions.add(dayDisplay);
            dayDates.add(dayDate);
            
            dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        String[] dayArray = dayOptions.toArray(new String[0]);
        
        builder.setItems(dayArray, (dialog, which) -> {
            String selectedDate = dayDates.get(which);
            String selectedDay = dayOptions.get(which);
            generateDayShoppingList(selectedDate, selectedDay);
        });
        
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
    
    private void generateDayShoppingList(String date, String dayName) {
        new Thread(() -> {
            // Récupérer tous les repas pour ce jour
            List<MealPlanWithRecette> dayMealPlans = db.mealPlanDao().getMealPlansWithRecetteForDate(date);
            
            if (dayMealPlans.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Aucun repas planifié pour " + dayName, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            // Agréger et fusionner les ingrédients identiques
            java.util.Map<String, Double> ingredientMap = new java.util.HashMap<>();
            
            for (MealPlanWithRecette mealPlan : dayMealPlans) {
                if (mealPlan.getRecetteIngredients() != null && !mealPlan.getRecetteIngredients().trim().isEmpty()) {
                    // Ajuster les ingrédients selon les portions
                    String adjustedIngredients = calculateAdjustedIngredients(
                        mealPlan.getRecetteIngredients(), 
                        mealPlan.getMealPlan().getPortions()
                    );
                    
                    // Diviser les ingrédients par ligne et les fusionner
                    String[] ingredients = adjustedIngredients.split("\\n");
                    for (String ingredient : ingredients) {
                        if (!ingredient.trim().isEmpty()) {
                            mergeIngredient(ingredientMap, ingredient.trim());
                        }
                    }
                }
            }
            
            // Convertir la map en liste avec quantités formatées
            List<String> allIngredients = new ArrayList<>();
            for (java.util.Map.Entry<String, Double> entry : ingredientMap.entrySet()) {
                String baseIngredient = entry.getKey();
                Double quantity = entry.getValue();
                
                // Formater la quantité
                String formattedQuantity;
                if (quantity == Math.floor(quantity)) {
                    formattedQuantity = String.valueOf((int) Math.round(quantity));
                } else {
                    formattedQuantity = String.format("%.1f", quantity);
                }
                
                // Reconstuire l'ingrédient avec la quantité formatée
                allIngredients.add("• " + formattedQuantity + " " + baseIngredient);
            }
            
            requireActivity().runOnUiThread(() -> {
                if (allIngredients.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucun ingrédient trouvé pour " + dayName, Toast.LENGTH_SHORT).show();
                } else {
                    showShoppingListDialog("Liste de courses - " + dayName, allIngredients);
                }
            });
        }).start();
    }
    
    private void generateWeekShoppingList() {
        new Thread(() -> {
            // Calculer la fin de la semaine
            Calendar weekEnd = (Calendar) currentWeekStart.clone();
            weekEnd.add(Calendar.DAY_OF_YEAR, 6);
            
            String startDate = dateFormat.format(currentWeekStart.getTime());
            String endDate = dateFormat.format(weekEnd.getTime());
            
            // Récupérer tous les repas de la semaine
            List<MealPlanWithRecette> weekMealPlans = db.mealPlanDao().getMealPlansWithRecetteForWeek(startDate, endDate);
            
            if (weekMealPlans.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Aucun repas planifié pour cette semaine", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            // Agréger et fusionner les ingrédients identiques de la semaine
            java.util.Map<String, Double> ingredientMap = new java.util.HashMap<>();
            
            for (MealPlanWithRecette mealPlan : weekMealPlans) {
                if (mealPlan.getRecetteIngredients() != null && !mealPlan.getRecetteIngredients().trim().isEmpty()) {
                    // Ajuster les ingrédients selon les portions
                    String adjustedIngredients = calculateAdjustedIngredients(
                        mealPlan.getRecetteIngredients(), 
                        mealPlan.getMealPlan().getPortions()
                    );
                    
                    // Diviser les ingrédients par ligne et les fusionner
                    String[] ingredients = adjustedIngredients.split("\\n");
                    for (String ingredient : ingredients) {
                        if (!ingredient.trim().isEmpty()) {
                            mergeIngredient(ingredientMap, ingredient.trim());
                        }
                    }
                }
            }
            
            // Convertir la map en liste avec quantités formatées
            List<String> allIngredients = new ArrayList<>();
            for (java.util.Map.Entry<String, Double> entry : ingredientMap.entrySet()) {
                String baseIngredient = entry.getKey();
                Double quantity = entry.getValue();
                
                // Formater la quantité
                String formattedQuantity;
                if (quantity == Math.floor(quantity)) {
                    formattedQuantity = String.valueOf((int) Math.round(quantity));
                } else {
                    formattedQuantity = String.format("%.1f", quantity);
                }
                
                // Reconstuire l'ingrédient avec la quantité formatée
                allIngredients.add("• " + formattedQuantity + " " + baseIngredient);
            }
            
            requireActivity().runOnUiThread(() -> {
                if (allIngredients.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucun ingrédient trouvé pour cette semaine", Toast.LENGTH_SHORT).show();
                } else {
                    showShoppingListDialog("Liste de courses - Semaine complète", allIngredients);
                }
            });
        }).start();
    }
    
    private String calculateAdjustedIngredients(String originalIngredients, String portions) {
        if (portions == null || portions.trim().isEmpty()) {
            return originalIngredients;
        }
        
        try {
            double portionMultiplier = Double.parseDouble(portions.trim());
            if (portionMultiplier == 1.0) {
                return originalIngredients;
            }
            
            // Pattern pour capturer tous les nombres suivis d'unités
            String pattern = "\\b(\\d+(?:[.,]\\d+)?)\\s*(g|gr|grammes?|kg|kilogrammes?|mg|milligrammes?|l|litre|litres|ml|millilitres?|cl|centilitres?|dl|décilitres?|cuillères?\\s+à\\s+soupe|cuillères?\\s+à\\s+café|c\\.à\\.s|c\\.à\\.c|tasses?|sachets?|pincées?|œufs?|oeufs?|portions?|morceaux?|tranches?|gousses?|feuilles?|brins?|noix|noisettes?|amandes?)\\b";
            
            Pattern regexPattern = Pattern.compile(pattern);
            Matcher matcher = regexPattern.matcher(originalIngredients);
            
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String numberStr = matcher.group(1).replace(",", ".");
                String unit = matcher.group(2);
                
                try {
                    double originalAmount = Double.parseDouble(numberStr);
                    double newAmount = originalAmount * portionMultiplier;
                    
                    // Formater le nouveau nombre
                    String formattedAmount;
                    if (newAmount == Math.floor(newAmount)) {
                        formattedAmount = String.valueOf((int) newAmount);
                    } else {
                        formattedAmount = String.format("%.1f", newAmount);
                    }
                    
                    matcher.appendReplacement(result, formattedAmount + " " + unit);
                } catch (NumberFormatException e) {
                    // Garder l'original si erreur
                    matcher.appendReplacement(result, matcher.group());
                }
            }
            matcher.appendTail(result);
            
            return result.toString();
            
        } catch (NumberFormatException e) {
            return originalIngredients;
        }
    }
    
    private void showShoppingListDialog(String title, List<String> ingredients) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        
        // Utiliser le même layout que dans ViewRecetteActivity
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shopping_list, null);
        android.widget.ListView listView = dialogView.findViewById(R.id.shopping_list_view);
        
        // Adapter avec checkboxes
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
            requireContext(), 
            R.layout.shopping_list_item, 
            R.id.itemText, 
            ingredients
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.CheckBox checkBox = view.findViewById(R.id.itemCheckBox);
                android.widget.TextView textView = view.findViewById(R.id.itemText);
                
                // S'assurer que le checkbox est correctement configuré
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(false);
                
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        textView.setPaintFlags(textView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        textView.setAlpha(0.5f);
                    } else {
                        textView.setPaintFlags(textView.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        textView.setAlpha(1.0f);
                    }
                });
                
                return view;
            }
        };
        
        listView.setAdapter(adapter);
        builder.setView(dialogView);
        
        builder.setPositiveButton("Fermer", null);
        
        builder.setNeutralButton("Partager", (dialog, which) -> {
            // Créer le texte à partager
            StringBuilder shareText = new StringBuilder();
            shareText.append(title).append("\n\n");
            
            for (String ingredient : ingredients) {
                shareText.append(ingredient).append("\n");
            }
            
            // Partager via Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
            startActivity(Intent.createChooser(shareIntent, "Partager la liste de courses"));
        });
        
        builder.show();
    }
    
    private void mergeIngredient(java.util.Map<String, Double> ingredientMap, String ingredient) {
        // Pattern pour extraire quantité et reste de l'ingrédient
        Pattern quantityPattern = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s+(.+)");
        Matcher matcher = quantityPattern.matcher(ingredient);
        
        if (matcher.matches()) {
            try {
                double quantity = Double.parseDouble(matcher.group(1).replace(",", "."));
                String baseIngredient = matcher.group(2); // tout ce qui suit le nombre
                
                // Fusionner avec l'existant s'il y en a un
                Double existingQuantity = ingredientMap.get(baseIngredient);
                if (existingQuantity != null) {
                    quantity += existingQuantity;
                }
                
                ingredientMap.put(baseIngredient, quantity);
                
            } catch (NumberFormatException e) {
                // Si pas de quantité détectable, ajouter tel quel
                Double count = ingredientMap.get(ingredient);
                ingredientMap.put(ingredient, count == null ? 1.0 : count + 1.0);
            }
        } else {
            // Ingrédient sans quantité détectable
            Double count = ingredientMap.get(ingredient);
            ingredientMap.put(ingredient, count == null ? 1.0 : count + 1.0);
        }
    }

    private void applyThemeColorsToButtons(Button... buttons) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        
        int colorResId = getResources().getIdentifier(colorName, "color", requireContext().getPackageName());
        if (colorResId != 0) {
            int themeColor = ContextCompat.getColor(requireContext(), colorResId);
            for (Button button : buttons) {
                // Appliquer la couleur de fond pour les boutons de shopping list
                if (button.getId() == R.id.shopping_list_day_btn || button.getId() == R.id.shopping_list_week_btn) {
                    button.setBackgroundColor(themeColor);
                    button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                } else {
                    // Garder le comportement original pour les autres boutons
                    button.setTextColor(themeColor);
                }
            }
        }
    }
}
