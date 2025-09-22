package fr.didictateur.inanutshell.logging;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Système de logging avancé avec persistence et analyse
 */
public class AppLogger {
    private static final String TAG = "AppLogger";
    private static AppLogger instance;
    
    // Configuration
    private static final String LOG_DIR = "app_logs";
    private static final String LOG_FILE_PREFIX = "inanutshell_";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_LOG_FILES = 10;
    private static final int BUFFER_SIZE = 100;
    
    // État du logger
    private final Context context;
    private final ExecutorService logExecutor;
    private final ConcurrentLinkedQueue<LogEntry> logBuffer;
    private final AtomicBoolean isEnabled;
    private final AtomicLong totalLogs;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat fileNameFormat;
    
    // Niveaux de log personnalisés
    public enum LogLevel {
        VERBOSE(0, "V"),
        DEBUG(1, "D"),
        INFO(2, "I"),
        WARNING(3, "W"),
        ERROR(4, "E"),
        CRITICAL(5, "C");
        
        final int priority;
        final String shortName;
        
        LogLevel(int priority, String shortName) {
            this.priority = priority;
            this.shortName = shortName;
        }
    }
    
    // Configuration du logging
    public static class LogConfig {
        public boolean enableFileLogging = true;
        public boolean enableConsoleLogging = true;
        public LogLevel minLogLevel = LogLevel.DEBUG;
        public boolean includeStackTrace = false;
        public boolean includeSensitiveData = false;
        public long maxFileSize = MAX_LOG_FILE_SIZE;
        public int maxFiles = MAX_LOG_FILES;
        
        public static LogConfig createDefault() {
            return new LogConfig();
        }
        
        public static LogConfig createDebug() {
            LogConfig config = new LogConfig();
            config.minLogLevel = LogLevel.VERBOSE;
            config.includeStackTrace = true;
            config.includeSensitiveData = true;
            return config;
        }
        
        public static LogConfig createProduction() {
            LogConfig config = new LogConfig();
            config.minLogLevel = LogLevel.INFO;
            config.includeStackTrace = false;
            config.includeSensitiveData = false;
            config.enableConsoleLogging = false;
            return config;
        }
    }
    
    private LogConfig currentConfig;
    
    private AppLogger(Context context) {
        this.context = context.getApplicationContext();
        this.logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AppLogger");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        this.logBuffer = new ConcurrentLinkedQueue<>();
        this.isEnabled = new AtomicBoolean(true);
        this.totalLogs = new AtomicLong(0);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        this.fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        this.currentConfig = LogConfig.createDefault();
        
        // Démarrer le processeur de logs
        startLogProcessor();
        
        // Log de démarrage
        logInfo("AppLogger", "Logger initialized");
    }
    
    public static synchronized AppLogger getInstance(Context context) {
        if (instance == null) {
            instance = new AppLogger(context);
        }
        return instance;
    }
    
    public static AppLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppLogger not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Configure le logger
     */
    public void configure(LogConfig config) {
        this.currentConfig = config;
        logInfo("AppLogger", "Logger reconfigured: " + config.minLogLevel);
    }
    
    /**
     * Active/désactive le logging
     */
    public void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
        logInfo("AppLogger", "Logger " + (enabled ? "enabled" : "disabled"));
    }
    
    // Méthodes de logging principales
    public void logVerbose(String tag, String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }
    
    public void logDebug(String tag, String message) {
        log(LogLevel.DEBUG, tag, message, null);
    }
    
    public void logInfo(String tag, String message) {
        log(LogLevel.INFO, tag, message, null);
    }
    
    public void logWarning(String tag, String message) {
        log(LogLevel.WARNING, tag, message, null);
    }
    
    public void logWarning(String tag, String message, Throwable throwable) {
        log(LogLevel.WARNING, tag, message, throwable);
    }
    
    public void logError(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }
    
    public void logError(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }
    
    public void logCritical(String tag, String message) {
        log(LogLevel.CRITICAL, tag, message, null);
    }
    
    public void logCritical(String tag, String message, Throwable throwable) {
        log(LogLevel.CRITICAL, tag, message, throwable);
    }
    
    /**
     * Méthode de logging principale
     */
    private void log(LogLevel level, String tag, String message, Throwable throwable) {
        if (!isEnabled.get() || level.priority < currentConfig.minLogLevel.priority) {
            return;
        }
        
        // Créer l'entrée de log
        LogEntry entry = new LogEntry(
            System.currentTimeMillis(),
            level,
            tag,
            sanitizeMessage(message),
            throwable,
            Thread.currentThread().getName(),
            getCurrentMethod()
        );
        
        totalLogs.incrementAndGet();
        
        // Console logging
        if (currentConfig.enableConsoleLogging) {
            logToConsole(entry);
        }
        
        // File logging (asynchrone)
        if (currentConfig.enableFileLogging) {
            logBuffer.offer(entry);
        }
    }
    
    /**
     * Nettoie le message des données sensibles
     */
    private String sanitizeMessage(String message) {
        if (currentConfig.includeSensitiveData || message == null) {
            return message;
        }
        
        // Masquer les mots de passe, tokens, etc.
        return message
            .replaceAll("(?i)(password|token|key|secret)=[^\\s&]+", "$1=***")
            .replaceAll("(?i)(authorization|bearer)\\s+[^\\s]+", "$1 ***")
            .replaceAll("\\b\\d{13,19}\\b", "***"); // Numéros de carte
    }
    
    /**
     * Obtient la méthode appelante
     */
    private String getCurrentMethod() {
        if (!currentConfig.includeStackTrace) {
            return null;
        }
        
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement element = stack[i];
            if (!element.getClassName().equals(AppLogger.class.getName()) &&
                !element.getMethodName().equals("getStackTrace")) {
                return element.getClassName() + "." + element.getMethodName() + 
                       "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
            }
        }
        return null;
    }
    
    /**
     * Log vers la console Android
     */
    private void logToConsole(LogEntry entry) {
        String message = formatLogMessage(entry, false);
        
        switch (entry.level) {
            case VERBOSE:
                Log.v(entry.tag, message, entry.throwable);
                break;
            case DEBUG:
                Log.d(entry.tag, message, entry.throwable);
                break;
            case INFO:
                Log.i(entry.tag, message, entry.throwable);
                break;
            case WARNING:
                Log.w(entry.tag, message, entry.throwable);
                break;
            case ERROR:
            case CRITICAL:
                Log.e(entry.tag, message, entry.throwable);
                break;
        }
    }
    
    /**
     * Démarre le processeur de logs pour l'écriture en fichier
     */
    private void startLogProcessor() {
        logExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Traiter les logs en lot
                    if (!logBuffer.isEmpty()) {
                        processLogBatch();
                    }
                    
                    Thread.sleep(1000); // Traitement toutes les secondes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in log processor", e);
                }
            }
        });
    }
    
    /**
     * Traite un lot de logs
     */
    private void processLogBatch() {
        File logFile = getCurrentLogFile();
        if (logFile == null) return;
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            int processed = 0;
            while (!logBuffer.isEmpty() && processed < BUFFER_SIZE) {
                LogEntry entry = logBuffer.poll();
                if (entry != null) {
                    String logLine = formatLogMessage(entry, true) + "\n";
                    writer.write(logLine);
                    processed++;
                }
            }
            writer.flush();
            
            // Vérifier la taille du fichier
            if (logFile.length() > currentConfig.maxFileSize) {
                rotateLogFiles();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
    
    /**
     * Obtient le fichier de log courant
     */
    private File getCurrentLogFile() {
        File logDir = new File(context.getFilesDir(), LOG_DIR);
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.e(TAG, "Cannot create log directory");
            return null;
        }
        
        String fileName = LOG_FILE_PREFIX + "current" + LOG_FILE_EXTENSION;
        return new File(logDir, fileName);
    }
    
    /**
     * Effectue la rotation des fichiers de logs
     */
    private void rotateLogFiles() {
        try {
            File logDir = new File(context.getFilesDir(), LOG_DIR);
            File currentFile = getCurrentLogFile();
            
            if (currentFile != null && currentFile.exists()) {
                // Renommer le fichier courant avec timestamp
                String timestamp = fileNameFormat.format(new Date());
                String newName = LOG_FILE_PREFIX + timestamp + LOG_FILE_EXTENSION;
                File archivedFile = new File(logDir, newName);
                
                if (currentFile.renameTo(archivedFile)) {
                    Log.i(TAG, "Log file rotated to: " + newName);
                    
                    // Nettoyer les anciens fichiers
                    cleanOldLogFiles(logDir);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rotating log files", e);
        }
    }
    
    /**
     * Nettoie les anciens fichiers de log
     */
    private void cleanOldLogFiles(File logDir) {
        File[] logFiles = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && 
            name.endsWith(LOG_FILE_EXTENSION) && 
            !name.contains("current"));
        
        if (logFiles != null && logFiles.length > currentConfig.maxFiles) {
            // Trier par date de modification
            java.util.Arrays.sort(logFiles, 
                (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            // Supprimer les plus anciens
            int toDelete = logFiles.length - currentConfig.maxFiles;
            for (int i = 0; i < toDelete; i++) {
                if (logFiles[i].delete()) {
                    Log.i(TAG, "Deleted old log file: " + logFiles[i].getName());
                }
            }
        }
    }
    
    /**
     * Formate un message de log
     */
    private String formatLogMessage(LogEntry entry, boolean includeTimestamp) {
        StringBuilder sb = new StringBuilder();
        
        if (includeTimestamp) {
            sb.append(dateFormat.format(new Date(entry.timestamp))).append(" ");
        }
        
        sb.append(entry.level.shortName)
          .append("/")
          .append(entry.tag)
          .append(": ")
          .append(entry.message);
        
        if (entry.threadName != null) {
            sb.append(" [").append(entry.threadName).append("]");
        }
        
        if (entry.method != null) {
            sb.append(" (").append(entry.method).append(")");
        }
        
        if (entry.throwable != null) {
            sb.append("\n").append(Log.getStackTraceString(entry.throwable));
        }
        
        return sb.toString();
    }
    
    /**
     * Obtient les statistiques de logging
     */
    public LogStats getLogStats() {
        File logDir = new File(context.getFilesDir(), LOG_DIR);
        File[] logFiles = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION));
        
        long totalSize = 0;
        int fileCount = 0;
        
        if (logFiles != null) {
            for (File file : logFiles) {
                totalSize += file.length();
                fileCount++;
            }
        }
        
        return new LogStats(
            totalLogs.get(),
            logBuffer.size(),
            fileCount,
            totalSize,
            isEnabled.get(),
            currentConfig.minLogLevel
        );
    }
    
    /**
     * Exporte les logs vers un fichier
     */
    public File exportLogs() {
        try {
            File exportDir = new File(context.getExternalFilesDir(null), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                return null;
            }
            
            String fileName = "logs_export_" + fileNameFormat.format(new Date()) + ".txt";
            File exportFile = new File(exportDir, fileName);
            
            // Copier tous les logs dans le fichier d'export
            try (FileWriter writer = new FileWriter(exportFile)) {
                // Informations système
                writer.write("=== LOG EXPORT ===\n");
                writer.write("App: In a Nutshell\n");
                writer.write("Export Date: " + dateFormat.format(new Date()) + "\n");
                writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
                writer.write("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n");
                writer.write("=================\n\n");
                
                // Logs actuels en mémoire
                for (LogEntry entry : logBuffer) {
                    writer.write(formatLogMessage(entry, true) + "\n");
                }
                
                // Logs archivés
                File logDir = new File(context.getFilesDir(), LOG_DIR);
                File[] logFiles = logDir.listFiles((dir, name) -> 
                    name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION));
                
                if (logFiles != null) {
                    java.util.Arrays.sort(logFiles, 
                        (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    
                    for (File logFile : logFiles) {
                        writer.write("\n=== " + logFile.getName() + " ===\n");
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.FileReader(logFile))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.write(line + "\n");
                            }
                        }
                    }
                }
            }
            
            Log.i(TAG, "Logs exported to: " + exportFile.getAbsolutePath());
            return exportFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting logs", e);
            return null;
        }
    }
    
    /**
     * Nettoie tous les logs
     */
    public void clearLogs() {
        logBuffer.clear();
        
        logExecutor.submit(() -> {
            File logDir = new File(context.getFilesDir(), LOG_DIR);
            File[] logFiles = logDir.listFiles();
            
            if (logFiles != null) {
                for (File file : logFiles) {
                    if (file.delete()) {
                        Log.i(TAG, "Deleted log file: " + file.getName());
                    }
                }
            }
        });
        
        logInfo("AppLogger", "All logs cleared");
    }
    
    /**
     * Libère les ressources
     */
    public void shutdown() {
        // Traiter les logs restants
        processLogBatch();
        
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Log.i(TAG, "AppLogger shutdown completed");
    }
    
    /**
     * Entrée de log
     */
    private static class LogEntry {
        final long timestamp;
        final LogLevel level;
        final String tag;
        final String message;
        final Throwable throwable;
        final String threadName;
        final String method;
        
        LogEntry(long timestamp, LogLevel level, String tag, String message, 
                Throwable throwable, String threadName, String method) {
            this.timestamp = timestamp;
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
            this.threadName = threadName;
            this.method = method;
        }
    }
    
    /**
     * Statistiques de logging
     */
    public static class LogStats {
        public final long totalLogs;
        public final int bufferSize;
        public final int fileCount;
        public final long totalFileSize;
        public final boolean isEnabled;
        public final LogLevel minLogLevel;
        
        LogStats(long totalLogs, int bufferSize, int fileCount, long totalFileSize, 
                boolean isEnabled, LogLevel minLogLevel) {
            this.totalLogs = totalLogs;
            this.bufferSize = bufferSize;
            this.fileCount = fileCount;
            this.totalFileSize = totalFileSize;
            this.isEnabled = isEnabled;
            this.minLogLevel = minLogLevel;
        }
        
        @Override
        public String toString() {
            return String.format(java.util.Locale.ROOT, "LogStats{total=%d, buffer=%d, files=%d (%.1fKB), enabled=%s, level=%s}",
                totalLogs, bufferSize, fileCount, totalFileSize / 1024.0, isEnabled, minLogLevel);
        }
    }
}
