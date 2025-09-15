package fr.didictateur.inanutshell.performance;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gestionnaire de performance pour optimiser les opérations coûteuses
 */
public class PerformanceManager {
    private static final String TAG = "PerformanceManager";
    private static PerformanceManager instance;
    
    // Pool de threads optimisé
    private final ExecutorService backgroundExecutor;
    private final ExecutorService ioExecutor;
    private final ExecutorService computeExecutor;
    private final Handler mainHandler;
    
    // Cache de résultats pour éviter les recalculs
    private final ConcurrentHashMap<String, CacheEntry> resultCache;
    private final ConcurrentHashMap<String, Future<?>> runningTasks;
    
    // Métriques de performance
    private final AtomicLong totalOperations;
    private final AtomicLong cacheHits;
    private final AtomicInteger activeThreads;
    private final ConcurrentHashMap<String, OperationMetrics> operationMetrics;
    
    // Configuration
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int IO_POOL_SIZE = 3;
    private static final long CACHE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_CACHE_SIZE = 100;
    
    private PerformanceManager() {
        // Thread pools spécialisés
        this.backgroundExecutor = Executors.newFixedThreadPool(CORE_POOL_SIZE, r -> {
            Thread thread = new Thread(r, "Performance-Background");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        
        this.ioExecutor = Executors.newFixedThreadPool(IO_POOL_SIZE, r -> {
            Thread thread = new Thread(r, "Performance-IO");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
        
        this.computeExecutor = Executors.newFixedThreadPool(MAX_POOL_SIZE, r -> {
            Thread thread = new Thread(r, "Performance-Compute");
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            return thread;
        });
        
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Caches et métriques
        this.resultCache = new ConcurrentHashMap<>();
        this.runningTasks = new ConcurrentHashMap<>();
        this.totalOperations = new AtomicLong(0);
        this.cacheHits = new AtomicLong(0);
        this.activeThreads = new AtomicInteger(0);
        this.operationMetrics = new ConcurrentHashMap<>();
        
        // Nettoyage périodique du cache
        scheduleCacheCleanup();
    }
    
    public static synchronized PerformanceManager getInstance() {
        if (instance == null) {
            instance = new PerformanceManager();
        }
        return instance;
    }
    
    /**
     * Exécute une tâche avec cache et optimisations
     */
    public <T> void executeWithCache(String key, PerformanceTask<T> task, PerformanceCallback<T> callback) {
        totalOperations.incrementAndGet();
        
        // Vérifier le cache d'abord
        CacheEntry cached = resultCache.get(key);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            @SuppressWarnings("unchecked")
            T result = (T) cached.value;
            mainHandler.post(() -> callback.onSuccess(result));
            Log.d(TAG, "Cache hit for key: " + key);
            return;
        }
        
        // Vérifier si une tâche similaire est déjà en cours
        Future<?> existingTask = runningTasks.get(key);
        if (existingTask != null && !existingTask.isDone()) {
            Log.d(TAG, "Task already running for key: " + key);
            return;
        }
        
        // Choisir l'executor approprié selon le type de tâche
        ExecutorService executor = chooseExecutor(task.getType());
        
        // Démarrer la tâche
        Future<?> future = executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            activeThreads.incrementAndGet();
            
            try {
                T result = task.execute();
                long duration = System.currentTimeMillis() - startTime;
                
                // Mettre en cache le résultat
                if (task.isCacheable()) {
                    resultCache.put(key, new CacheEntry(result, System.currentTimeMillis()));
                    cleanCacheIfNeeded();
                }
                
                // Enregistrer les métriques
                recordMetrics(key, duration, true);
                
                // Callback sur le thread principal
                mainHandler.post(() -> callback.onSuccess(result));
                
                Log.d(TAG, "Task completed for key: " + key + " in " + duration + "ms");
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                recordMetrics(key, duration, false);
                
                Log.e(TAG, "Task failed for key: " + key, e);
                mainHandler.post(() -> callback.onError(e));
                
            } finally {
                activeThreads.decrementAndGet();
                runningTasks.remove(key);
            }
        });
        
        runningTasks.put(key, future);
    }
    
    /**
     * Exécute une tâche simple en arrière-plan
     */
    public void executeBackground(Runnable task) {
        backgroundExecutor.submit(() -> {
            activeThreads.incrementAndGet();
            try {
                task.run();
            } finally {
                activeThreads.decrementAndGet();
            }
        });
    }
    
    /**
     * Exécute sur le thread principal
     */
    public void executeOnMainThread(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler.post(task);
        }
    }
    
    /**
     * Exécute avec délai sur le thread principal
     */
    public void executeOnMainThreadDelayed(Runnable task, long delayMs) {
        mainHandler.postDelayed(task, delayMs);
    }
    
    /**
     * Choisit l'executor approprié selon le type de tâche
     */
    private ExecutorService chooseExecutor(TaskType type) {
        switch (type) {
            case IO:
                return ioExecutor;
            case COMPUTE:
                return computeExecutor;
            case BACKGROUND:
            default:
                return backgroundExecutor;
        }
    }
    
    /**
     * Enregistre les métriques de performance
     */
    private void recordMetrics(String key, long duration, boolean success) {
        OperationMetrics metrics = operationMetrics.computeIfAbsent(key, 
            k -> new OperationMetrics());
        
        metrics.recordOperation(duration, success);
    }
    
    /**
     * Nettoie le cache si nécessaire
     */
    private void cleanCacheIfNeeded() {
        if (resultCache.size() > MAX_CACHE_SIZE) {
            // Supprimer les entrées les plus anciennes
            long now = System.currentTimeMillis();
            resultCache.entrySet().removeIf(entry -> 
                entry.getValue().timestamp < now - CACHE_EXPIRY_MS);
        }
    }
    
    /**
     * Programme le nettoyage périodique du cache
     */
    private void scheduleCacheCleanup() {
        backgroundExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                    
                    long now = System.currentTimeMillis();
                    int removed = 0;
                    
                    // Nettoyer les entrées expirées
                    for (String key : resultCache.keySet()) {
                        CacheEntry entry = resultCache.get(key);
                        if (entry != null && entry.isExpired()) {
                            resultCache.remove(key);
                            removed++;
                        }
                    }
                    
                    if (removed > 0) {
                        Log.d(TAG, "Cleaned " + removed + " expired cache entries");
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Invalide le cache pour une clé
     */
    public void invalidateCache(String key) {
        resultCache.remove(key);
        Log.d(TAG, "Cache invalidated for key: " + key);
    }
    
    /**
     * Invalide tout le cache
     */
    public void invalidateAllCache() {
        int size = resultCache.size();
        resultCache.clear();
        Log.d(TAG, "All cache invalidated (" + size + " entries)");
    }
    
    /**
     * Annule une tâche en cours
     */
    public void cancelTask(String key) {
        Future<?> task = runningTasks.remove(key);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            Log.d(TAG, "Task cancelled for key: " + key);
        }
    }
    
    /**
     * Obtient les statistiques de performance
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            totalOperations.get(),
            cacheHits.get(),
            activeThreads.get(),
            resultCache.size(),
            runningTasks.size(),
            getAverageResponseTime(),
            getCacheHitRate()
        );
    }
    
    /**
     * Calcule le temps de réponse moyen
     */
    private double getAverageResponseTime() {
        if (operationMetrics.isEmpty()) return 0.0;
        
        double totalTime = 0.0;
        long totalOps = 0;
        
        for (OperationMetrics metrics : operationMetrics.values()) {
            totalTime += metrics.getTotalTime();
            totalOps += metrics.getOperationCount();
        }
        
        return totalOps > 0 ? totalTime / totalOps : 0.0;
    }
    
    /**
     * Calcule le taux de succès du cache
     */
    private double getCacheHitRate() {
        long total = totalOperations.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }
    
    /**
     * Libère les ressources
     */
    public void shutdown() {
        backgroundExecutor.shutdown();
        ioExecutor.shutdown();
        computeExecutor.shutdown();
        
        try {
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
            if (!computeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                computeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Log.i(TAG, "PerformanceManager shutdown completed");
    }
    
    /**
     * Interface pour les tâches de performance
     */
    public interface PerformanceTask<T> {
        T execute() throws Exception;
        TaskType getType();
        boolean isCacheable();
    }
    
    /**
     * Interface pour les callbacks
     */
    public interface PerformanceCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
    
    /**
     * Types de tâches
     */
    public enum TaskType {
        BACKGROUND,    // Tâche générale en arrière-plan
        IO,           // Opération I/O (réseau, fichiers)
        COMPUTE       // Calcul intensif
    }
    
    /**
     * Entrée de cache
     */
    private static class CacheEntry {
        final Object value;
        final long timestamp;
        
        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
    
    /**
     * Métriques pour une opération
     */
    private static class OperationMetrics {
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong operationCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        
        void recordOperation(long duration, boolean success) {
            totalTime.addAndGet(duration);
            operationCount.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            }
        }
        
        long getTotalTime() {
            return totalTime.get();
        }
        
        long getOperationCount() {
            return operationCount.get();
        }
        
        double getSuccessRate() {
            long total = operationCount.get();
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }
    }
    
    /**
     * Statistiques de performance
     */
    public static class PerformanceStats {
        public final long totalOperations;
        public final long cacheHits;
        public final int activeThreads;
        public final int cacheSize;
        public final int runningTasks;
        public final double averageResponseTime;
        public final double cacheHitRate;
        
        public PerformanceStats(long totalOperations, long cacheHits, int activeThreads,
                              int cacheSize, int runningTasks, double averageResponseTime, 
                              double cacheHitRate) {
            this.totalOperations = totalOperations;
            this.cacheHits = cacheHits;
            this.activeThreads = activeThreads;
            this.cacheSize = cacheSize;
            this.runningTasks = runningTasks;
            this.averageResponseTime = averageResponseTime;
            this.cacheHitRate = cacheHitRate;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceStats{operations=%d, cache=%d/%d (%.1f%%), threads=%d, tasks=%d, avgTime=%.1fms}",
                totalOperations, cacheHits, totalOperations, cacheHitRate * 100,
                activeThreads, runningTasks, averageResponseTime
            );
        }
    }
}
