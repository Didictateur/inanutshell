package fr.didictateur.inanutshell;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import fr.didictateur.inanutshell.data.meal.MealPlan;
import fr.didictateur.inanutshell.data.meal.MealPlanManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activité pour éditer ou créer un repas planifié
 */
public class EditMealPlanActivity extends AppCompatActivity {
    
    private TextInputEditText editRecipeName;
    private MaterialAutoCompleteTextView spinnerMealType;
    private TextInputEditText editServings;
    private TextInputEditText editNotes;
    private MaterialButton btnSelectDate;
    private MaterialButton btnSelectRecipe;
    
    private MealPlanManager mealPlanManager;
    private MealPlan currentMealPlan;
    private Date selectedDate;
    private SimpleDateFormat dateFormatter;
    private boolean isEditMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_meal_plan);
        
        initializeViews();
        setupToolbar();
        setupMealTypeSpinner();
        setupListeners();
        
        mealPlanManager = MealPlanManager.getInstance(this);
        dateFormatter = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault());
        
        loadMealPlanData();
    }
    
    private void initializeViews() {
        editRecipeName = findViewById(R.id.editRecipeName);
        spinnerMealType = findViewById(R.id.spinnerMealType);
        editServings = findViewById(R.id.editServings);
        editNotes = findViewById(R.id.editNotes);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectRecipe = findViewById(R.id.btnSelectRecipe);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupMealTypeSpinner() {
        String[] mealTypes = {"Petit-déjeuner", "Déjeuner", "Dîner", "Collation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, mealTypes);
        spinnerMealType.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectRecipe.setOnClickListener(v -> selectRecipe());
    }
    
    private void loadMealPlanData() {
        Intent intent = getIntent();
        
        if (intent.hasExtra("mealPlanId")) {
            // Mode édition
            isEditMode = true;
            int mealPlanId = intent.getIntExtra("mealPlanId", -1);
            // TODO: Charger le repas planifié existant
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Modifier le repas");
            }
        } else {
            // Mode création
            isEditMode = false;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Planifier un repas");
            }
            
            // Utiliser la date passée ou la date actuelle
            if (intent.hasExtra("selectedDate")) {
                selectedDate = new Date(intent.getLongExtra("selectedDate", System.currentTimeMillis()));
            } else {
                selectedDate = new Date();
            }
            
            updateDateButton();
            
            // Valeurs par défaut
            editServings.setText("4");
            spinnerMealType.setText("Déjeuner", false);
        }
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (DatePicker view, int year, int month, int dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                selectedDate = selectedCalendar.getTime();
                updateDateButton();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
    
    private void updateDateButton() {
        if (selectedDate != null) {
            btnSelectDate.setText(dateFormatter.format(selectedDate));
        }
    }
    
    private void selectRecipe() {
        // TODO: Ouvrir un dialogue de sélection de recette
        // Pour l'instant, permettre la saisie manuelle
        Toast.makeText(this, "Sélection de recette - À venir", Toast.LENGTH_SHORT).show();
    }
    
    private boolean validateInput() {
        String recipeName = editRecipeName.getText().toString().trim();
        String mealType = spinnerMealType.getText().toString().trim();
        String servingsStr = editServings.getText().toString().trim();
        
        if (TextUtils.isEmpty(recipeName)) {
            editRecipeName.setError("Nom de recette requis");
            return false;
        }
        
        if (TextUtils.isEmpty(mealType)) {
            spinnerMealType.setError("Type de repas requis");
            return false;
        }
        
        if (TextUtils.isEmpty(servingsStr)) {
            editServings.setError("Nombre de portions requis");
            return false;
        }
        
        try {
            int servings = Integer.parseInt(servingsStr);
            if (servings <= 0) {
                editServings.setError("Nombre de portions invalide");
                return false;
            }
        } catch (NumberFormatException e) {
            editServings.setError("Nombre de portions invalide");
            return false;
        }
        
        if (selectedDate == null) {
            Toast.makeText(this, "Veuillez sélectionner une date", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveMealPlan() {
        if (!validateInput()) {
            return;
        }
        
        String recipeName = editRecipeName.getText().toString().trim();
        String mealTypeStr = spinnerMealType.getText().toString().trim();
        String servingsStr = editServings.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();
        
        // Convertir le type de repas
        MealPlan.MealType mealType;
        switch (mealTypeStr) {
            case "Petit-déjeuner":
                mealType = MealPlan.MealType.BREAKFAST;
                break;
            case "Déjeuner":
                mealType = MealPlan.MealType.LUNCH;
                break;
            case "Dîner":
                mealType = MealPlan.MealType.DINNER;
                break;
            case "Collation":
                mealType = MealPlan.MealType.SNACK;
                break;
            default:
                mealType = MealPlan.MealType.LUNCH;
                break;
        }
        
        int servings = Integer.parseInt(servingsStr);
        
        if (isEditMode && currentMealPlan != null) {
            // Mode édition
            currentMealPlan.setRecipeName(recipeName);
            currentMealPlan.setMealType(mealType);
            currentMealPlan.setDate(selectedDate);
            currentMealPlan.setServings(servings);
            currentMealPlan.setNotes(notes.isEmpty() ? null : notes);
            
            mealPlanManager.updateMealPlan(currentMealPlan, new MealPlanManager.OnMealPlanActionListener() {
                @Override
                public void onSuccess(MealPlan mealPlan) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditMealPlanActivity.this, "Repas planifié modifié avec succès", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditMealPlanActivity.this, error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // Mode création
            MealPlan newMealPlan = new MealPlan("manual_" + System.currentTimeMillis(), recipeName, selectedDate, mealType);
            newMealPlan.setServings(servings);
            if (!notes.isEmpty()) {
                newMealPlan.setNotes(notes);
            }
            
            mealPlanManager.createMealPlan(newMealPlan, new MealPlanManager.OnMealPlanActionListener() {
                @Override
                public void onSuccess(MealPlan mealPlan) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditMealPlanActivity.this, "Repas planifié ajouté avec succès", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditMealPlanActivity.this, error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            );
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_meal_plan_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            saveMealPlan();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
