package fr.didictateur.inanutshell;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        currentWeekText = view.findViewById(R.id.currentWeekText);
        weekRecyclerView = view.findViewById(R.id.weekRecyclerView);
        
        // Configuration du RecyclerView
        weekRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        weekAdapter = new MealPlanWeekAdapter(new ArrayList<>(), 
            mealPlan -> {
                // Gérer le clic sur un repas planifié
            },
            (date, mealType) -> {
                // Gérer l'ajout d'un nouveau repas
                showRecipeSelectionDialog(date, mealType);
            });
        weekRecyclerView.setAdapter(weekAdapter);
        
        // Boutons de navigation
        Button prevWeekBtn = view.findViewById(R.id.prevWeekBtn);
        Button nextWeekBtn = view.findViewById(R.id.nextWeekBtn);
        
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
        
        // Mettre à jour le texte de la semaine
        String weekText = displayDateFormat.format(currentWeekStart.getTime()) + 
                         " - " + displayDateFormat.format(weekEnd.getTime());
        currentWeekText.setText(weekText);
        
        // Charger les données de la semaine
        new Thread(() -> {
            // Implémenter le chargement des meal plans
            requireActivity().runOnUiThread(() -> {
                // Mettre à jour l'adapter
                weekAdapter.updateMealPlans(new ArrayList<>());
            });
        }).start();
    }
    
    private void showRecipeSelectionDialog(String date, String mealType) {
        // Implémenter la sélection de recette
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
