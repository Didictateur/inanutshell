package fr.didictateur.inanutshell.technical.performance;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PerformanceManager {
    private static PerformanceManager instance;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ConcurrentHashMap<String, Object> cache;
    private ConcurrentHashMap<String, Long> cacheTimestamps;
    private long cacheExpiryTime = 5 * 60 * 1000; // 5 minutes
    
    public enum TaskType {
        COMPUTATION,
        IO,
        NETWORK,
        DATABASE
    }
    
    public interface PerformanceTask<T> {
        T execute() throws Exception;
        TaskType getType();
    }
    
    public interface PerformanceCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
    
    private PerformanceManager() {
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }
    
    public static PerformanceManager getInstance() {
        if (instance == null) {
            synchronized (PerformanceManager.class) {
                if (instance == null) {
                    instance = new PerformanceManager();
                }
            }
        }
        return instance;
    }
    
    public <T> Future<?> executeAsync(PerformanceTask<T> task, PerformanceCallback<T> callback) {
        return executorService.submit(() -> {
            try {
                long startTime = System.currentTimeMillis();
                T result = task.execute();
                long endTime = System.currentTimeMillis();
                
                // Log performance metrics
                logPerformance(task.getType(), endTime - startTime);
                
                // Return result on main thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public <T> void executeWithCache(String cacheKey, PerformanceTask<T> task, PerformanceCallback<T> callback) {
        // Check cache first
        Object cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onSuccess((T) cachedResult);
                }
            });
            return;
        }
        
        // Execute task and cache result
        executeAsync(new PerformanceTask<T>() {
            @Override
            public T execute() throws Exception {
                T result = task.execute();
                cacheResult(cacheKey, result);
                return result;
            }
            
            @Override
            public TaskType getType() {
                return task.getType();
            }
        }, callback);
    }
    
    public <T> T executeSync(PerformanceTask<T> task) throws Exception {
        long startTime = System.currentTimeMillis();
        T result = task.execute();
        long endTime = System.currentTimeMillis();
        
        logPerformance(task.getType(), endTime - startTime);
        return result;
    }
    
    private void cacheResult(String key, Object result) {
        if (key != null && result != null) {
            cache.put(key, result);
            cacheTimestamps.put(key, System.currentTimeMillis());
        }
    }
    
    private Object getCachedResult(String key) {
        if (key == null) return null;
        
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp == null) return null;
        
        // Check if cache has expired
        if (System.currentTimeMillis() - timestamp > cacheExpiryTime) {
            cache.remove(key);
            cacheTimestamps.remove(key);
            return null;
        }
        
        return cache.get(key);
    }
    
    public void clearCache() {
        cache.clear();
        cacheTimestamps.clear();
    }
    
    public void clearCache(String key) {
        cache.remove(key);
        cacheTimestamps.remove(key);
    }
    
    public void setCacheExpiryTime(long expiryTime) {
        this.cacheExpiryTime = expiryTime;
    }
    
    private void logPerformance(TaskType taskType, long executionTime) {
        // Log performance metrics for monitoring
        // This could be extended to send metrics to analytics services
        if (executionTime > 1000) { // Log slow operations (> 1 second)
            android.util.Log.w("PerformanceManager", 
                String.format("Slow %s operation: %dms", taskType.name(), executionTime));
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Memory management
    public void onLowMemory() {
        // Clear cache on low memory
        clearCache();
        
        // Force garbage collection
        System.gc();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public boolean isCached(String key) {
        return getCachedResult(key) != null;
    }
}
