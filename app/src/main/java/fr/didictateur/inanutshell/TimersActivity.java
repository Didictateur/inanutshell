package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité principale pour gérer les minuteries
 */
public class TimersActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private TimersAdapter adapter;
    private FloatingActionButton fabAddTimer;
    private TimerManager timerManager;
    
    private static final int REQUEST_CREATE_TIMER = 1001;
    private static final int REQUEST_EDIT_TIMER = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timers);
        
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeTimers();
        
        // Démarrer le service de mise à jour des timers
        TimerService.updateTimers(this);
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewTimers);
        fabAddTimer = findViewById(R.id.fabAddTimer);
        timerManager = new TimerManager(this);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Minuteries");
        }
    }
    
    private void setupRecyclerView() {
        adapter = new TimersAdapter(new ArrayList<>(), new TimersAdapter.TimerClickListener() {
            @Override
            public void onTimerClick(Timer timer) {
                // Ouvrir les détails/édition de la minuterie
                Intent intent = new Intent(TimersActivity.this, EditTimerActivity.class);
                intent.putExtra("timer_id", timer.id);
                startActivityForResult(intent, REQUEST_EDIT_TIMER);
            }
            
            @Override
            public void onStartPauseClick(Timer timer) {
                if (timer.isRunning()) {
                    pauseTimer(timer);
                } else if (timer.isPaused() || timer.state == Timer.TimerState.CREATED) {
                    startTimer(timer);
                }
            }
            
            @Override
            public void onResetClick(Timer timer) {
                resetTimer(timer);
            }
            
            @Override
            public void onDeleteClick(Timer timer) {
                deleteTimer(timer);
            }
            
            @Override
            public void onFullscreenClick(Timer timer) {
                openFullscreenTimer(timer);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupFab() {
        fabAddTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TimersActivity.this, EditTimerActivity.class);
                startActivityForResult(intent, REQUEST_CREATE_TIMER);
            }
        });
    }
    
    private void observeTimers() {
        timerManager.getAllTimersLive().observe(this, new Observer<List<Timer>>() {
            @Override
            public void onChanged(List<Timer> timers) {
                if (timers != null) {
                    adapter.updateTimers(timers);
                    
                    // Afficher un message si aucune minuterie
                    View emptyView = findViewById(R.id.emptyView);
                    if (timers.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }
    
    private void startTimer(Timer timer) {
        timerManager.startTimer(timer.id, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                TimerService.startTimer(TimersActivity.this, timer.id);
                Toast.makeText(TimersActivity.this, "Minuterie démarrée", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(TimersActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void pauseTimer(Timer timer) {
        timerManager.pauseTimer(timer.id, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                TimerService.pauseTimer(TimersActivity.this, timer.id);
                Toast.makeText(TimersActivity.this, "Minuterie en pause", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(TimersActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void resetTimer(Timer timer) {
        timerManager.resetTimer(timer.id, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                TimerService.cancelTimer(TimersActivity.this, timer.id);
                Toast.makeText(TimersActivity.this, "Minuterie remise à zéro", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(TimersActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_timers, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_presets) {
            showPresetsDialog();
            return true;
        } else if (id == R.id.action_clear_finished) {
            clearFinishedTimers();
            return true;
        } else if (id == R.id.action_pause_all) {
            pauseAllTimers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showPresetsDialog() {
        // TODO: Afficher un dialogue avec les minuteries prédéfinies
        String[] presets = {"Pâtes (10 min)", "Riz (15 min)", "Œuf coque (3 min)", 
                           "Œuf dur (10 min)", "Steak (5 min)", "Poulet (25 min)"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Minuteries prédéfinies");
        builder.setItems(presets, (dialog, which) -> {
            TimerManager.PresetTimer[] presetTimers = TimerManager.PresetTimer.values();
            if (which < presetTimers.length) {
                TimerManager.PresetTimer preset = presetTimers[which];
                timerManager.createPresetTimer(preset, new TimerManager.TimerCallback() {
                    @Override
                    public void onSuccess(Timer timer) {
                        Toast.makeText(TimersActivity.this, 
                            "Minuterie créée: " + timer.name, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onError(String error) {
                        Toast.makeText(TimersActivity.this, 
                            "Erreur: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
    
    private void clearFinishedTimers() {
        // TODO: Implémenter la suppression des minuteries terminées
        Toast.makeText(this, "Minuteries terminées supprimées", Toast.LENGTH_SHORT).show();
    }
    
    private void pauseAllTimers() {
        // TODO: Implémenter la pause de toutes les minuteries
        Toast.makeText(this, "Toutes les minuteries mises en pause", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_TIMER:
                    Toast.makeText(this, "Minuterie créée avec succès", Toast.LENGTH_SHORT).show();
                    break;
                case REQUEST_EDIT_TIMER:
                    Toast.makeText(this, "Minuterie mise à jour", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    
    private void deleteTimer(Timer timer) {
        timerManager.deleteTimer(timer, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                TimerService.cancelTimer(TimersActivity.this, timer.id);
                Toast.makeText(TimersActivity.this, "Minuterie supprimée", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(TimersActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void openFullscreenTimer(Timer timer) {
        Intent intent = new Intent(TimersActivity.this, FullscreenTimerActivity.class);
        intent.putExtra("timer_id", timer.id);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mettre à jour les timers quand l'activité revient au premier plan
        TimerService.updateTimers(this);
    }
}
