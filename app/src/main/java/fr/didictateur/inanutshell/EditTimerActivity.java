package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Activité pour créer ou éditer une minuterie
 */
public class EditTimerActivity extends AppCompatActivity {
    
    private EditText etTimerName;
    private NumberPicker npHours;
    private NumberPicker npMinutes;
    private NumberPicker npSeconds;
    private Button btnSave;
    private Button btn1Min, btn3Min, btn5Min, btn10Min, btn15Min, btn30Min;
    
    private TimerManager timerManager;
    private Timer currentTimer;
    private boolean isEditMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timer);
        
        initializeViews();
        setupToolbar();
        setupNumberPickers();
        setupShortcutButtons();
        loadTimerData();
        setupSaveButton();
    }
    
    private void initializeViews() {
        etTimerName = findViewById(R.id.etTimerName);
        npHours = findViewById(R.id.npHours);
        npMinutes = findViewById(R.id.npMinutes);
        npSeconds = findViewById(R.id.npSeconds);
        btnSave = findViewById(R.id.btnSave);
        btn1Min = findViewById(R.id.btn1Min);
        btn3Min = findViewById(R.id.btn3Min);
        btn5Min = findViewById(R.id.btn5Min);
        btn10Min = findViewById(R.id.btn10Min);
        btn15Min = findViewById(R.id.btn15Min);
        btn30Min = findViewById(R.id.btn30Min);
        
        timerManager = new TimerManager(this);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupNumberPickers() {
        // Heures: 0-23
        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npHours.setValue(0);
        
        // Minutes: 0-59
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npMinutes.setValue(5); // Valeur par défaut
        
        // Secondes: 0-59
        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);
        npSeconds.setValue(0);
        
        // Formatage à 2 chiffres
        npHours.setFormatter(value -> String.format("%02d", value));
        npMinutes.setFormatter(value -> String.format("%02d", value));
        npSeconds.setFormatter(value -> String.format("%02d", value));
    }
    
    private void loadTimerData() {
        int timerId = getIntent().getIntExtra("timer_id", -1);
        
        if (timerId != -1) {
            // Mode édition
            isEditMode = true;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Modifier minuterie");
            }
            
            timerManager.getTimer(timerId, new TimerManager.TimerCallback() {
                @Override
                public void onSuccess(Timer timer) {
                    currentTimer = timer;
                    populateFields(timer);
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(EditTimerActivity.this, 
                        "Erreur: " + error, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            // Mode création
            isEditMode = false;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Nouvelle minuterie");
            }
        }
    }
    
    private void populateFields(Timer timer) {
        etTimerName.setText(timer.name);
        
        // Convertir les millisecondes en heures/minutes/secondes
        long totalSeconds = timer.originalDurationMs / 1000;
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);
        
        npHours.setValue(hours);
        npMinutes.setValue(minutes);
        npSeconds.setValue(seconds);
    }
    
    private void setupShortcutButtons() {
        btn1Min.setOnClickListener(v -> setTime(0, 1, 0));
        btn3Min.setOnClickListener(v -> setTime(0, 3, 0));
        btn5Min.setOnClickListener(v -> setTime(0, 5, 0));
        btn10Min.setOnClickListener(v -> setTime(0, 10, 0));
        btn15Min.setOnClickListener(v -> setTime(0, 15, 0));
        btn30Min.setOnClickListener(v -> setTime(0, 30, 0));
    }
    
    private void setTime(int hours, int minutes, int seconds) {
        npHours.setValue(hours);
        npMinutes.setValue(minutes);
        npSeconds.setValue(seconds);
    }
    
    private void setupSaveButton() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTimer();
            }
        });
    }
    
    private void saveTimer() {
        String name = etTimerName.getText().toString().trim();
        
        // Calculer la durée en millisecondes
        long durationMs = (npHours.getValue() * 3600L + 
                          npMinutes.getValue() * 60L + 
                          npSeconds.getValue()) * 1000L;
        
        // Validation
        if (durationMs <= 0) {
            Toast.makeText(this, "La durée doit être supérieure à 0", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (name.isEmpty()) {
            name = "Minuterie " + Timer.formatDuration(durationMs);
        }
        
        btnSave.setEnabled(false);
        
        if (isEditMode && currentTimer != null) {
            // Mode édition
            currentTimer.name = name;
            if (currentTimer.state == Timer.TimerState.CREATED) {
                // Permettre la modification de la durée seulement si pas encore démarrée
                currentTimer.originalDurationMs = durationMs;
                currentTimer.remainingTimeMs = durationMs;
            }
            
            timerManager.updateTimer(currentTimer, new TimerManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    setResult(RESULT_OK);
                    finish();
                }
                
                @Override
                public void onError(String error) {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditTimerActivity.this, 
                        "Erreur: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Mode création
            timerManager.createTimer(name, durationMs, new TimerManager.TimerCallback() {
                @Override
                public void onSuccess(Timer timer) {
                    setResult(RESULT_OK);
                    finish();
                }
                
                @Override
                public void onError(String error) {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditTimerActivity.this, 
                        "Erreur: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.menu_edit_timer, menu);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            deleteTimer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void deleteTimer() {
        if (currentTimer != null) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer la minuterie")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette minuterie ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    timerManager.deleteTimer(currentTimer, new TimerManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(EditTimerActivity.this, 
                                "Minuterie supprimée", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Toast.makeText(EditTimerActivity.this, 
                                "Erreur: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
        }
    }
}
