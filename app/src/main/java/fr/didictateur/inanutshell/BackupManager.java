package fr.didictateur.inanutshell;

import android.content.Context;
import android.os.Environment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupManager {
    
    private Context context;
    private AppDatabase database;
    private ExecutorService executor;
    private Gson gson;
    
    public interface BackupListener {
        void onBackupSuccess(String filePath);
        void onBackupError(String error);
    }
    
    public BackupManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public void exportAllData(BackupListener listener) {
        executor.execute(() -> {
            try {
                // Récupérer toutes les données
                List<Recette> recettes = database.recetteDao().getAllRecettes();
                List<Folder> folders = database.folderDao().getAllFolders();
                
                // Créer l'objet de sauvegarde
                BackupData backupData = new BackupData();
                backupData.recettes = recettes;
                backupData.folders = folders;
                backupData.exportDate = new Date();
                backupData.version = "1.0";
                
                // Convertir en JSON
                String jsonData = gson.toJson(backupData);
                
                // Créer le fichier de sauvegarde
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "inanutshell_backup_" + timestamp + ".json";
                
                File backupDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                
                File backupFile = new File(backupDir, fileName);
                
                // Écrire les données
                FileWriter writer = new FileWriter(backupFile);
                writer.write(jsonData);
                writer.close();
                
                // Notifier le succès
                if (listener != null) {
                    listener.onBackupSuccess(backupFile.getAbsolutePath());
                }
                
            } catch (IOException e) {
                if (listener != null) {
                    listener.onBackupError("Erreur lors de l'exportation: " + e.getMessage());
                }
            }
        });
    }
    
    public void exportToCSV(BackupListener listener) {
        executor.execute(() -> {
            try {
                List<Recette> recettes = database.recetteDao().getAllRecettes();
                
                StringBuilder csvContent = new StringBuilder();
                csvContent.append("Nom,Taille,Temps Préparation,Ingrédients,Préparation,Notes,Tags\n");
                
                for (Recette recette : recettes) {
                    csvContent.append(escapeCSV(recette.getNom())).append(",");
                    csvContent.append(escapeCSV(recette.getTaille())).append(",");
                    csvContent.append(recette.getTempsPrepMin()).append(",");
                    csvContent.append(escapeCSV(recette.getIngredients())).append(",");
                    csvContent.append(escapeCSV(recette.getPreparation())).append(",");
                    csvContent.append(escapeCSV(recette.getNotes())).append(",");
                    csvContent.append(escapeCSV(recette.getTags())).append("\n");
                }
                
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "inanutshell_recipes_" + timestamp + ".csv";
                
                File backupDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "exports");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                
                File csvFile = new File(backupDir, fileName);
                
                FileWriter writer = new FileWriter(csvFile);
                writer.write(csvContent.toString());
                writer.close();
                
                if (listener != null) {
                    listener.onBackupSuccess(csvFile.getAbsolutePath());
                }
                
            } catch (IOException e) {
                if (listener != null) {
                    listener.onBackupError("Erreur lors de l'exportation CSV: " + e.getMessage());
                }
            }
        });
    }
    
    private String escapeCSV(String value) {
        if (value == null) return "";
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            value = "\"" + value + "\"";
        }
        
        return value;
    }
    
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    // Classe pour structurer les données de sauvegarde
    public static class BackupData {
        public List<Recette> recettes;
        public List<Folder> folders;
        public Date exportDate;
        public String version;
    }
}
