package fr.didictateur.inanutshell;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.AppDatabase;

/**
 * Gestionnaire pour les opérations sur les minuteries
 */
public class TimerManager {
    
    private final TimerDao timerDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // Interfaces de callback
    public interface TimerCallback {
        void onSuccess(Timer timer);
        void onError(String error);
    }
    
    public interface TimersCallback {
        void onSuccess(List<Timer> timers);
        void onError(String error);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public TimerManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.timerDao = db.timerDao();
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // === Méthodes CRUD ===
    
    public void createTimer(String name, long durationMs, TimerCallback callback) {
        createTimer(name, durationMs, null, callback);
    }
    
    public void createTimer(String name, long durationMs, String recipeId, TimerCallback callback) {
        executorService.execute(() -> {
            try {
                String timerName = name;
                if (timerName == null || timerName.trim().isEmpty()) {
                    timerName = "Minuterie " + Timer.formatDuration(durationMs);
                }
                
                if (durationMs <= 0) {
                    mainHandler.post(() -> callback.onError("La durée doit être positive"));
                    return;
                }
                
                Timer timer = new Timer(timerName.trim(), durationMs, recipeId);
                long id = timerDao.insertTimer(timer);
                timer.id = (int) id;
                
                mainHandler.post(() -> callback.onSuccess(timer));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la création: " + e.getMessage()));
            }
        });
    }
    
    public void updateTimer(Timer timer, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour: " + e.getMessage()));
            }
        });
    }
    
    public void deleteTimer(Timer timer, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                timerDao.deleteTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la suppression: " + e.getMessage()));
            }
        });
    }
    
    public void getTimer(int id, TimerCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(id);
                if (timer != null) {
                    timer.updateRemainingTime(); // Mise à jour du temps restant
                    mainHandler.post(() -> callback.onSuccess(timer));
                } else {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération: " + e.getMessage()));
            }
        });
    }
    
    // === Méthodes LiveData (pour les observateurs) ===
    
    public LiveData<List<Timer>> getAllTimersLive() {
        return timerDao.getAllTimersLive();
    }
    
    public LiveData<List<Timer>> getActiveTimersLive() {
        return timerDao.getActiveTimersLive();
    }
    
    public LiveData<List<Timer>> getRunningTimersLive() {
        return timerDao.getRunningTimersLive();
    }
    
    public LiveData<Timer> getTimerLive(int id) {
        return timerDao.getTimerByIdLive(id);
    }
    
    public LiveData<List<Timer>> getTimersForRecipeLive(String recipeId) {
        return timerDao.getTimersForRecipeLive(recipeId);
    }
    
    // === Actions sur les minuteries ===
    
    public void startTimer(int timerId, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(timerId);
                if (timer == null) {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                    return;
                }
                
                timer.start();
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du démarrage: " + e.getMessage()));
            }
        });
    }
    
    public void pauseTimer(int timerId, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(timerId);
                if (timer == null) {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                    return;
                }
                
                // Calculer le temps restant avant la pause
                timer.updateRemainingTime();
                timer.pause();
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la pause: " + e.getMessage()));
            }
        });
    }
    
    public void resetTimer(int timerId, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(timerId);
                if (timer == null) {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                    return;
                }
                
                timer.reset();
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la remise à zéro: " + e.getMessage()));
            }
        });
    }
    
    public void cancelTimer(int timerId, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(timerId);
                if (timer == null) {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                    return;
                }
                
                timer.cancel();
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'annulation: " + e.getMessage()));
            }
        });
    }
    
    public void finishTimer(int timerId, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                Timer timer = timerDao.getTimerById(timerId);
                if (timer == null) {
                    mainHandler.post(() -> callback.onError("Minuterie non trouvée"));
                    return;
                }
                
                timer.finish();
                timerDao.updateTimer(timer);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la finalisation: " + e.getMessage()));
            }
        });
    }
    
    // === Utilitaires ===
    
    public void getAllActiveTimers(TimersCallback callback) {
        executorService.execute(() -> {
            try {
                List<Timer> timers = timerDao.getActiveTimers();
                // Mettre à jour le temps restant pour chaque minuterie
                for (Timer timer : timers) {
                    timer.updateRemainingTime();
                }
                mainHandler.post(() -> callback.onSuccess(timers));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération: " + e.getMessage()));
            }
        });
    }
    
    public void getAllRunningTimers(TimersCallback callback) {
        executorService.execute(() -> {
            try {
                List<Timer> timers = timerDao.getRunningTimers();
                // Mettre à jour le temps restant pour chaque minuterie
                for (Timer timer : timers) {
                    timer.updateRemainingTime();
                    if (timer.remainingTimeMs <= 0) {
                        timer.finish();
                        timerDao.updateTimer(timer);
                    }
                }
                mainHandler.post(() -> callback.onSuccess(timers));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération: " + e.getMessage()));
            }
        });
    }
    
    public void cancelAllActiveTimers(SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                timerDao.cancelAllActiveTimers();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'annulation: " + e.getMessage()));
            }
        });
    }
    
    public void cleanupOldTimers(SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                // Supprimer les minuteries terminées/annulées de plus de 24h
                long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                timerDao.deleteOldTimers(oneDayAgo);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du nettoyage: " + e.getMessage()));
            }
        });
    }
    
    public void getActiveTimersCount(SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                int count = timerDao.getActiveTimersCount();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur: " + e.getMessage()));
            }
        });
    }
    
    // Créer des minuteries prédéfinies pour la cuisson
    public void createPresetTimer(PresetTimer preset, TimerCallback callback) {
        createTimer(preset.name, preset.durationMs, callback);
    }
    
    public enum PresetTimer {
        PATES(10 * 60 * 1000, "Pâtes"),
        RIZ(15 * 60 * 1000, "Riz"),
        OEUF_COQUE(3 * 60 * 1000, "Œuf à la coque"),
        OEUF_DUR(10 * 60 * 1000, "Œuf dur"),
        STEAK(5 * 60 * 1000, "Steak saignant"),
        POULET(25 * 60 * 1000, "Poulet"),
        PIZZA(12 * 60 * 1000, "Pizza"),
        PAIN(30 * 60 * 1000, "Pain"),
        GATEAU(45 * 60 * 1000, "Gâteau"),
        INFUSION(5 * 60 * 1000, "Thé/Infusion");
        
        public final long durationMs;
        public final String name;
        
        PresetTimer(long durationMs, String name) {
            this.durationMs = durationMs;
            this.name = name;
        }
    }
}
