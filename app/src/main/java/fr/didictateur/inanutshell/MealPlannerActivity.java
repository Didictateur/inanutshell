package fr.didictateur.inanutshell;

import fr.didictateur.inanutshell.data.meal.MealPlan;
import fr.didictateur.inanutshell.data.meal.MealPlanAdapter;
import fr.didictateur.inanutshell.data.meal.MealPlanManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

/**
 * Activité principale pour la planification des repas
 */
public class MealPlannerActivity extends AppCompatActivity {
    
    private CalendarView calendarView;
    private RecyclerView mealsRecyclerView;
    private TextView selectedDateText;
    private TextView noMealsText;
    private MaterialCardView mealsCard;
    private FloatingActionButton fabAddMeal;
    private MaterialButton btnWeekView, btnMonthView, btnGenerateMenu;
    
    private MealPlanAdapter mealPlanAdapter;
    private MealPlanManager mealPlanManager;
    private Date selectedDate;
    private SimpleDateFormat dateFormatter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);
        
        initializeViews();
        setupToolbar();
        setupCalendar();
        setupRecyclerView();
        setupListeners();
        
        mealPlanManager = MealPlanManager.getInstance(this);
        dateFormatter = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault());
        
        // Initialiser avec la date actuelle
        selectedDate = new Date();
        updateSelectedDate();
        loadMealsForSelectedDate();
    }
    
    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        mealsRecyclerView = findViewById(R.id.recyclerViewMeals);
        selectedDateText = findViewById(R.id.textSelectedDate);
        noMealsText = findViewById(R.id.textNoMeals);
        mealsCard = findViewById(R.id.cardMeals);
        fabAddMeal = findViewById(R.id.fabAddMeal);
        btnWeekView = findViewById(R.id.btnWeekView);
        btnMonthView = findViewById(R.id.btnMonthView);
        btnGenerateMenu = findViewById(R.id.btnGenerateMenu);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Planification des Repas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupCalendar() {
        // Configurer le calendrier
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);
        
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime();
            updateSelectedDate();
            loadMealsForSelectedDate();
        });
    }
    
    private void setupRecyclerView() {
        mealPlanAdapter = new MealPlanAdapter(this);
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mealsRecyclerView.setAdapter(mealPlanAdapter);
        
        mealPlanAdapter.setOnMealPlanClickListener(new MealPlanAdapter.OnMealPlanClickListener() {
            @Override
            public void onMealPlanClick(MealPlan mealPlan) {
                // TODO: Ouvrir le détail de la recette quand l'activité sera créée
                // Intent intent = new Intent(MealPlannerActivity.this, ViewRecetteActivity.class);
                // intent.putExtra("recetteId", mealPlan.getRecipeId());
                // startActivity(intent);
                Toast.makeText(MealPlannerActivity.this, "Ouvrir recette: " + mealPlan.getRecipeName(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onEditClick(MealPlan mealPlan) {
                // Ouvrir l'édition du repas planifié
                openMealPlanEditor(mealPlan);
            }
            
            @Override
            public void onDeleteClick(MealPlan mealPlan) {
                deleteMealPlan(mealPlan);
            }
            
            @Override
            public void onMarkCompletedClick(MealPlan mealPlan) {
                markMealAsCompleted(mealPlan);
            }
        });
    }
    
    private void setupListeners() {
        fabAddMeal.setOnClickListener(v -> openMealPlanEditor(null));
        
        btnWeekView.setOnClickListener(v -> openWeekView());
        btnMonthView.setOnClickListener(v -> openMonthView());
        btnGenerateMenu.setOnClickListener(v -> showGenerateMenuDialog());
    }
    
    private void updateSelectedDate() {
        if (selectedDate != null) {
            selectedDateText.setText(dateFormatter.format(selectedDate));
        }
    }
    
    private void loadMealsForSelectedDate() {
        if (selectedDate == null) return;
        
        mealPlanManager.getMealPlansForDate(selectedDate).observe(this, new Observer<List<MealPlan>>() {
            @Override
            public void onChanged(List<MealPlan> mealPlans) {
                if (mealPlans != null && !mealPlans.isEmpty()) {
                    mealPlanAdapter.setMealPlans(mealPlans);
                    mealsRecyclerView.setVisibility(View.VISIBLE);
                    noMealsText.setVisibility(View.GONE);
                } else {
                    mealsRecyclerView.setVisibility(View.GONE);
                    noMealsText.setVisibility(View.VISIBLE);
                    mealPlanAdapter.setMealPlans(new ArrayList<>());
                }
            }
        });
    }
    
    private void openMealPlanEditor(MealPlan mealPlan) {
        Intent intent = new Intent(this, EditMealPlanActivity.class);
        if (mealPlan != null) {
            intent.putExtra("mealPlanId", mealPlan.getId());
        } else {
            intent.putExtra("selectedDate", selectedDate.getTime());
        }
        startActivityForResult(intent, 1001);
    }
    
    private void deleteMealPlan(MealPlan mealPlan) {
        mealPlanManager.deleteMealPlan(mealPlan, new MealPlanManager.OnMealPlanActionListener() {
            @Override
            public void onSuccess(MealPlan deletedMealPlan) {
                runOnUiThread(() -> {
                    Toast.makeText(MealPlannerActivity.this, "Repas supprimé avec succès", Toast.LENGTH_SHORT).show();
                    loadMealsForSelectedDate();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MealPlannerActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void markMealAsCompleted(MealPlan mealPlan) {
        mealPlanManager.markMealAsCompleted(mealPlan, new MealPlanManager.OnMealPlanActionListener() {
            @Override
            public void onSuccess(MealPlan completedMealPlan) {
                runOnUiThread(() -> {
                    Toast.makeText(MealPlannerActivity.this, "Repas marqué comme terminé", Toast.LENGTH_SHORT).show();
                    loadMealsForSelectedDate();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MealPlannerActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void openWeekView() {
        // TODO: Créer WeekPlannerActivity
        Toast.makeText(this, "Vue hebdomadaire - À venir", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, WeekPlannerActivity.class);
        // if (selectedDate != null) {
        //     intent.putExtra("selectedDate", selectedDate.getTime());
        // }
        // startActivity(intent);
    }
    
    private void openMonthView() {
        // TODO: Créer MonthPlannerActivity
        Toast.makeText(this, "Vue mensuelle - À venir", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, MonthPlannerActivity.class);
        // if (selectedDate != null) {
        //     intent.putExtra("selectedDate", selectedDate.getTime());
        // }
        // startActivity(intent);
    }
    
    private void showGenerateMenuDialog() {
        // TODO: Implémenter dialogue de génération automatique de menus
        Toast.makeText(this, "Génération de menu automatique - Bientôt disponible", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.meal_planner_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_today) {
            // Aller à aujourd'hui
            selectedDate = new Date();
            calendarView.setDate(selectedDate.getTime());
            updateSelectedDate();
            loadMealsForSelectedDate();
            return true;
        } else if (id == R.id.action_copy_week) {
            // TODO: Implémenter copie de semaine
            Toast.makeText(this, "Copie de semaine - Bientôt disponible", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Recharger les repas après modification
            loadMealsForSelectedDate();
        }
    }
}
