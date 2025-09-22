package fr.didictateur.inanutshell;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mode plein écran immersif pour minuterie de cuisine
 * Interface optimisée pour la cuisine avec contrôles gestuels
 */
public class FullscreenTimerActivity extends AppCompatActivity {
    
    private TextView timerDisplay;
    private TextView timerName;
    private TextView currentTime;
    private ProgressBar circularProgress;
    private MaterialButton playPauseButton;
    private MaterialButton resetButton;
    private MaterialButton addTimeButton;
    private MaterialButton removeTimeButton;
    private ImageButton backButton;
    private MaterialCardView controlsCard;
    
    private TimerManager timerManager;
    private Timer currentTimer;
    private GestureDetector gestureDetector;
    private Handler uiUpdateHandler;
    private Runnable timeUpdateRunnable;
    
    private boolean isControlsVisible = true;
    private boolean isDarkMode = true;
    private long timerId = -1;
    
    // Constantes pour les gestes
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int TAP_DELAY = 3000; // 3 secondes avant de masquer les contrôles
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configuration plein écran immersif
        setupFullscreenMode();
        
        setContentView(R.layout.activity_fullscreen_timer);
        
        // Récupération de l'ID du timer
        timerId = getIntent().getLongExtra("timer_id", -1);
        if (timerId == -1) {
            Toast.makeText(this, "Erreur: Timer non trouvé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupGestures();
        setupButtons();
        loadTimer();
        startTimeUpdates();
        
        // Add smooth entrance animations
        animateUIEntrance();
        
        // Show feature guide if first time
        showFeatureGuideIfNeeded();
        
        // Masquer les contrôles après un délai
        scheduleControlsHiding();
    }
    
    @SuppressLint("SourceLockedOrientationActivity")
    private void setupFullscreenMode() {
        // Mode plein écran immersif
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Masquer la barre de navigation
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        // Écran toujours allumé pendant la cuisson
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Verrouiller l'orientation en portrait pour la stabilité
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    
    private void initializeViews() {
        timerDisplay = findViewById(R.id.timerDisplay);
        timerName = findViewById(R.id.timerName);
        currentTime = findViewById(R.id.currentTime);
        circularProgress = findViewById(R.id.circularProgress);
        playPauseButton = findViewById(R.id.playPauseButton);
        resetButton = findViewById(R.id.resetButton);
        addTimeButton = findViewById(R.id.addTimeButton);
        removeTimeButton = findViewById(R.id.removeTimeButton);
        backButton = findViewById(R.id.backButton);
        controlsCard = findViewById(R.id.controlsCard);
        
        timerManager = new TimerManager(this);
        uiUpdateHandler = new Handler(Looper.getMainLooper());
        
        // Application du thème sombre
        applyDarkTheme();
    }
    
    private void applyDarkTheme() {
        // Background sombre
        findViewById(R.id.rootLayout).setBackgroundColor(
            ContextCompat.getColor(this, R.color.background_dark)
        );
        
        // Textes en blanc/gris clair
        timerDisplay.setTextColor(ContextCompat.getColor(this, R.color.text_primary_dark));
        timerName.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
        currentTime.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dark));
        
        // Progress bar avec accent color
        circularProgress.setProgressTintList(
            ContextCompat.getColorStateList(this, R.color.accent_orange)
        );
    }
    
    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Tap pour afficher/masquer les contrôles
                toggleControls();
                return true;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Double tap pour play/pause
                togglePlayPause();
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                
                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // Swipe vers le bas - Ajouter 1 minute
                            addTime(60);
                        } else {
                            // Swipe vers le haut - Retirer 1 minute
                            removeTime(60);
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe vers la droite - Ajouter 30 secondes
                            addTime(30);
                        } else {
                            // Swipe vers la gauche - Retirer 30 secondes
                            removeTime(30);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
    
    private void setupButtons() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        
        resetButton.setOnClickListener(v -> resetTimer());
        
        addTimeButton.setOnClickListener(v -> addTime(60)); // +1 minute
        
        removeTimeButton.setOnClickListener(v -> removeTime(60)); // -1 minute
        
        backButton.setOnClickListener(v -> finish());
        
        // Maintien appuyé pour ajouts/retraits rapides
        addTimeButton.setOnLongClickListener(v -> {
            addTime(300); // +5 minutes
            return true;
        });
        
        removeTimeButton.setOnLongClickListener(v -> {
            removeTime(300); // -5 minutes
            return true;
        });
    }
    
    private void loadTimer() {
        timerManager.getTimer((int)timerId, new TimerManager.TimerCallback() {
            @Override
            public void onSuccess(Timer timer) {
                currentTimer = timer;
                updateUI();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(FullscreenTimerActivity.this, 
                    "Erreur de chargement: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void togglePlayPause() {
        if (currentTimer == null) return;
        
        showControlsTemporarily();
        
        if (currentTimer.state == Timer.TimerState.RUNNING) {
            TimerService.pauseTimer(this, (int)timerId);
            showToast("⏸️ Pause");
        } else if (currentTimer.state == Timer.TimerState.PAUSED || 
                  currentTimer.state == Timer.TimerState.CREATED) {
            TimerService.startTimer(this, (int)timerId);
            showToast("▶️ Démarré");
        }
    }
    
    private void resetTimer() {
        if (currentTimer == null) return;
        
        showControlsTemporarily();
        TimerService.cancelTimer(this, (int)timerId);
        showToast("🔄 Remis à zéro");
    }
    
    private void addTime(int seconds) {
        if (currentTimer == null) return;
        
        showControlsTemporarily();
        
        // Créer un nouveau timer avec le temps ajusté
        Timer updatedTimer = new Timer();
        updatedTimer.id = currentTimer.id;
        updatedTimer.name = currentTimer.name;
        updatedTimer.originalDurationMs = currentTimer.originalDurationMs + (seconds * 1000);
        updatedTimer.remainingTimeMs = currentTimer.remainingTimeMs + (seconds * 1000);
        updatedTimer.state = currentTimer.state;
        updatedTimer.createdAt = currentTimer.createdAt;
        updatedTimer.startedAt = currentTimer.startedAt;
        updatedTimer.pausedAt = currentTimer.pausedAt;
        updatedTimer.recipeId = currentTimer.recipeId;
        
        timerManager.updateTimer(updatedTimer, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                currentTimer = updatedTimer;
                updateUI();
                showToast("➕ +" + (seconds / 60) + "min " + (seconds % 60) + "s");
            }
            
            @Override
            public void onError(String error) {
                showToast("Erreur: " + error);
            }
        });
    }
    
    private void removeTime(int seconds) {
        if (currentTimer == null) return;
        
        showControlsTemporarily();
        
        // Empêcher les valeurs négatives
        long newDuration = Math.max(10000, currentTimer.originalDurationMs - (seconds * 1000));
        long newRemaining = Math.max(10000, currentTimer.remainingTimeMs - (seconds * 1000));
        
        Timer updatedTimer = new Timer();
        updatedTimer.id = currentTimer.id;
        updatedTimer.name = currentTimer.name;
        updatedTimer.originalDurationMs = newDuration;
        updatedTimer.remainingTimeMs = newRemaining;
        updatedTimer.state = currentTimer.state;
        updatedTimer.createdAt = currentTimer.createdAt;
        updatedTimer.startedAt = currentTimer.startedAt;
        updatedTimer.pausedAt = currentTimer.pausedAt;
        updatedTimer.recipeId = currentTimer.recipeId;
        
        timerManager.updateTimer(updatedTimer, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                currentTimer = updatedTimer;
                updateUI();
                showToast("➖ -" + (seconds / 60) + "min " + (seconds % 60) + "s");
            }
            
            @Override
            public void onError(String error) {
                showToast("Erreur: " + error);
            }
        });
    }
    
    private void toggleControls() {
        isControlsVisible = !isControlsVisible;
        
        if (isControlsVisible) {
            showControls();
            scheduleControlsHiding();
        } else {
            hideControls();
        }
    }
    
    private void showControls() {
        isControlsVisible = true;
        controlsCard.setVisibility(View.VISIBLE);
        controlsCard.setAlpha(0f);
        controlsCard.animate()
            .alpha(1f)
            .setDuration(200)
            .start();
    }
    
    private void hideControls() {
        isControlsVisible = false;
        controlsCard.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> controlsCard.setVisibility(View.GONE))
            .start();
    }
    
    private void showControlsTemporarily() {
        if (!isControlsVisible) {
            showControls();
        }
        scheduleControlsHiding();
    }
    
    private void scheduleControlsHiding() {
        uiUpdateHandler.removeCallbacks(hideControlsRunnable);
        uiUpdateHandler.postDelayed(hideControlsRunnable, TAP_DELAY);
    }
    
    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isControlsVisible) {
                hideControls();
            }
        }
    };
    
    private void startTimeUpdates() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                loadTimer(); // Recharger pour les mises à jour
                uiUpdateHandler.postDelayed(this, 1000); // Mise à jour chaque seconde
            }
        };
        uiUpdateHandler.post(timeUpdateRunnable);
    }
    
    private void updateUI() {
        if (currentTimer == null) return;
        
        runOnUiThread(() -> {
            // Affichage du temps restant
            String timeText = formatTime(currentTimer.remainingTimeMs / 1000);
            timerDisplay.setText(timeText);
            
            // Nom du timer
            timerName.setText(currentTimer.name);
            
            // Barre de progression circulaire
            if (currentTimer.originalDurationMs > 0) {
                int progress = (int) (100 * (currentTimer.originalDurationMs - currentTimer.remainingTimeMs) 
                    / (float) currentTimer.originalDurationMs);
                circularProgress.setProgress(progress);
            }
            
            // Bouton play/pause
            if (currentTimer.state == Timer.TimerState.RUNNING) {
                playPauseButton.setText("⏸️");
                playPauseButton.setIconResource(R.drawable.ic_pause);
            } else {
                playPauseButton.setText("▶️");
                playPauseButton.setIconResource(R.drawable.ic_play_arrow);
            }
            
            // Couleur du timer selon l'état
            int textColor;
            if (currentTimer.state == Timer.TimerState.FINISHED) {
                textColor = ContextCompat.getColor(this, R.color.error_red);
                // Animation de clignotement pour timer fini
                ObjectAnimator.ofFloat(timerDisplay, "alpha", 1f, 0.3f, 1f)
                    .setDuration(1000)
                    .start();
            } else if (currentTimer.remainingTimeMs <= 60000) {
                textColor = ContextCompat.getColor(this, R.color.warning_orange);
            } else {
                textColor = ContextCompat.getColor(this, R.color.text_primary_dark);
            }
            timerDisplay.setTextColor(textColor);
        });
    }
    
    private void updateCurrentTime() {
        runOnUiThread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            currentTime.setText(sdf.format(new Date()));
        });
    }
    
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiUpdateHandler != null && timeUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(timeUpdateRunnable);
        }
        if (uiUpdateHandler != null) {
            uiUpdateHandler.removeCallbacks(hideControlsRunnable);
        }
    }
    
    private void animateUIEntrance() {
        // Animate timer display with scale effect
        timerDisplay.setScaleX(0.8f);
        timerDisplay.setScaleY(0.8f);
        timerDisplay.setAlpha(0f);
        timerDisplay.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(200)
            .start();
        
        // Animate progress circle
        if (circularProgress != null) {
            fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(circularProgress);
        }
        
        // Animate controls card from bottom
        if (controlsCard != null) {
            controlsCard.setTranslationY(200f);
            controlsCard.setAlpha(0f);
            controlsCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(300)
                .start();
        }
        
        // Animate timer name
        if (timerName != null) {
            fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(timerName);
        }
    }
    
    private void showFeatureGuideIfNeeded() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            fr.didictateur.inanutshell.ui.onboarding.FeatureGuideHelper guideHelper = 
                new fr.didictateur.inanutshell.ui.onboarding.FeatureGuideHelper(this);
            guideHelper.showFullscreenTimerGuide();
        }, 1000); // Attendre que les animations se terminent
    }
    
    @Override
    public void onBackPressed() {
        // Add exit animation
        timerDisplay.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction(() -> super.onBackPressed())
            .start();
    }
}
