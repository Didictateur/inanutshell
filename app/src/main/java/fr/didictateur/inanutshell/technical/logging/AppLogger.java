package fr.didictateur.inanutshell.technical.logging;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLogger {
    private static final String TAG = "AppLogger";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static AppLogger instance;
    
    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
    
    private Context context;
    private boolean fileLoggingEnabled;
    private boolean consoleLoggingEnabled;
    private LogLevel minLogLevel;
    private SimpleDateFormat dateFormat;
    private File logFile;
    
    private AppLogger(Context context) {
        this.context = context.getApplicationContext();
        this.fileLoggingEnabled = true;
        this.consoleLoggingEnabled = true;
        this.minLogLevel = LogLevel.DEBUG;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        
        // Create log file
        File logDir = new File(context.getFilesDir(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        this.logFile = new File(logDir, LOG_FILE_NAME);
    }
    
    public static AppLogger getInstance(Context context) {
        if (instance == null) {
            synchronized (AppLogger.class) {
                if (instance == null) {
                    instance = new AppLogger(context);
                }
            }
        }
        return instance;
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
    
    public void logError(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }
    
    public void logError(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }
    
    public void logException(String tag, Throwable throwable) {
        log(LogLevel.ERROR, tag, "Exception occurred", throwable);
    }
    
    private void log(LogLevel level, String tag, String message, Throwable throwable) {
        if (level.ordinal() < minLogLevel.ordinal()) {
            return;
        }
        
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("%s [%s] %s: %s", 
            timestamp, level.name(), tag, message);
        
        if (throwable != null) {
            logMessage += "\n" + Log.getStackTraceString(throwable);
        }
        
        // Console logging
        if (consoleLoggingEnabled) {
            switch (level) {
                case DEBUG:
                    Log.d(tag, message, throwable);
                    break;
                case INFO:
                    Log.i(tag, message, throwable);
                    break;
                case WARNING:
                    Log.w(tag, message, throwable);
                    break;
                case ERROR:
                    Log.e(tag, message, throwable);
                    break;
            }
        }
        
        // File logging
        if (fileLoggingEnabled) {
            writeToFile(logMessage);
        }
    }
    
    private void writeToFile(String message) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log to file", e);
        }
    }
    
    public void clearLog() {
        if (logFile.exists()) {
            logFile.delete();
        }
    }
    
    public File getLogFile() {
        return logFile;
    }
    
    // Configuration methods
    public void setFileLoggingEnabled(boolean enabled) {
        this.fileLoggingEnabled = enabled;
    }
    
    public void setConsoleLoggingEnabled(boolean enabled) {
        this.consoleLoggingEnabled = enabled;
    }
    
    public void setMinLogLevel(LogLevel level) {
        this.minLogLevel = level;
    }
    
    public boolean isFileLoggingEnabled() {
        return fileLoggingEnabled;
    }
    
    public boolean isConsoleLoggingEnabled() {
        return consoleLoggingEnabled;
    }
    
    public LogLevel getMinLogLevel() {
        return minLogLevel;
    }
}
