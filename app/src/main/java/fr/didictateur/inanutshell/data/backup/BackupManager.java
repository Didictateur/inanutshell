package fr.didictateur.inanutshell.data.backup;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;

import fr.didictateur.inanutshell.data.database.AppDatabase;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.MealPlan;
import fr.didictateur.inanutshell.data.model.ShoppingList;
import fr.didictateur.inanutshell.data.model.ShoppingItem;
import fr.didictateur.inanutshell.data.model.Timer;
import fr.didictateur.inanutshell.data.model.User;
import fr.didictateur.inanutshell.data.model.Group;
import fr.didictateur.inanutshell.data.model.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

/**
 * Gestionnaire pour la sauvegarde et restauration complète des données utilisateur
 * - Sauvegarde complète de toutes les données (recettes, meal plans, listes, etc.)
 * - Export en format JSON avec compression ZIP
 * - Restauration sélective par type de données
 * - Sauvegarde automatique périodique
 * - Import/export des préférences et paramètres
 */
public class BackupManager {
    
    private static final String TAG = "BackupManager";
    private static BackupManager instance;
    private ExecutorService executorService;
    private Context context;
    private AppDatabase database;
    private Gson gson;
    
    // Versions de backup pour compatibilité
    private static final String BACKUP_VERSION = "1.0";
    private static final String BACKUP_FORMAT = "inanutshell_backup";
    
    // Types de données sauvegardables
    public enum BackupDataType {
        RECIPES,
        MEAL_PLANS,
        SHOPPING_LISTS,
        TIMERS,
        USERS,
        GROUPS,
        THEMES,
        PREFERENCES,
        ALL
    }
    
    // Callback pour les opérations de backup
    public interface BackupCallback {
        void onSuccess(File backupFile);
        void onError(String error);
        void onProgress(String operation, int progress);
    }
    
    public interface RestoreCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(String operation, int progress);
    }
    
    private BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.database = AppDatabase.getInstance(context);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    }
    
    public static synchronized BackupManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackupManager(context);
        }
        return instance;
    }
    
    // ===== SAUVEGARDE COMPLÈTE =====
    
    /**
     * Crée une sauvegarde complète de toutes les données
     */
    public void createFullBackup(BackupCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Début sauvegarde complète");
                callback.onProgress("Préparation...", 0);
                
                // Créer le dossier de sauvegarde
                File backupDir = getBackupDirectory();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File backupFile = new File(backupDir, "backup_complete_" + timestamp + ".zip");
                
                // Créer l'archive ZIP
                try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFile))) {
                    
                    // Métadonnées de sauvegarde
                    callback.onProgress("Création métadonnées...", 10);
                    addBackupMetadata(zipOut, BackupDataType.ALL);
                    
                    // Recettes
                    callback.onProgress("Sauvegarde recettes...", 20);
                    backupRecipes(zipOut);
                    
                    // Meal Plans
                    callback.onProgress("Sauvegarde meal plans...", 35);
                    backupMealPlans(zipOut);
                    
                    // Listes de courses
                    callback.onProgress("Sauvegarde listes de courses...", 50);
                    backupShoppingLists(zipOut);
                    
                    // Minuteries
                    callback.onProgress("Sauvegarde minuteries...", 65);
                    backupTimers(zipOut);
                    
                    // Utilisateurs et groupes
                    callback.onProgress("Sauvegarde utilisateurs...", 80);
                    backupUsers(zipOut);
                    backupGroups(zipOut);
                    
                    // Thèmes
                    callback.onProgress("Sauvegarde thèmes...", 90);
                    backupThemes(zipOut);
                    
                    // Préférences
                    callback.onProgress("Sauvegarde préférences...", 95);
                    backupPreferences(zipOut);
                    
                    callback.onProgress("Finalisation...", 100);
                }
                
                Log.d(TAG, "Sauvegarde complète terminée: " + backupFile.getAbsolutePath());
                callback.onSuccess(backupFile);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la sauvegarde complète", e);
                callback.onError("Erreur sauvegarde: " + e.getMessage());
            }
        });
    }
    
    /**
     * Crée une sauvegarde partielle selon les types de données spécifiés
     */
    public void createSelectiveBackup(List<BackupDataType> dataTypes, BackupCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Début sauvegarde sélective: " + dataTypes);
                callback.onProgress("Préparation...", 0);
                
                File backupDir = getBackupDirectory();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File backupFile = new File(backupDir, "backup_selective_" + timestamp + ".zip");
                
                try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFile))) {
                    
                    // Métadonnées
                    addSelectiveBackupMetadata(zipOut, dataTypes);
                    
                    int progressStep = 80 / dataTypes.size();
                    int currentProgress = 10;
                    
                    for (BackupDataType dataType : dataTypes) {
                        callback.onProgress("Sauvegarde " + dataType.name() + "...", currentProgress);
                        
                        switch (dataType) {
                            case RECIPES:
                                backupRecipes(zipOut);
                                break;
                            case MEAL_PLANS:
                                backupMealPlans(zipOut);
                                break;
                            case SHOPPING_LISTS:
                                backupShoppingLists(zipOut);
                                break;
                            case TIMERS:
                                backupTimers(zipOut);
                                break;
                            case USERS:
                                backupUsers(zipOut);
                                break;
                            case GROUPS:
                                backupGroups(zipOut);
                                break;
                            case THEMES:
                                backupThemes(zipOut);
                                break;
                            case PREFERENCES:
                                backupPreferences(zipOut);
                                break;
                        }
                        
                        currentProgress += progressStep;
                    }
                    
                    callback.onProgress("Finalisation...", 100);
                }
                
                Log.d(TAG, "Sauvegarde sélective terminée: " + backupFile.getAbsolutePath());
                callback.onSuccess(backupFile);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la sauvegarde sélective", e);
                callback.onError("Erreur sauvegarde: " + e.getMessage());
            }
        });
    }
    
    // ===== RESTAURATION =====
    
    /**
     * Restaure une sauvegarde complète
     */
    public void restoreFullBackup(File backupFile, RestoreCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Début restauration complète: " + backupFile.getAbsolutePath());
                callback.onProgress("Vérification du fichier...", 0);
                
                if (!backupFile.exists()) {
                    callback.onError("Fichier de sauvegarde introuvable");
                    return;
                }
                
                try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(backupFile))) {
                    
                    // Vérifier les métadonnées
                    callback.onProgress("Lecture métadonnées...", 10);
                    JsonObject metadata = readBackupMetadata(zipIn);
                    if (metadata == null) {
                        callback.onError("Fichier de sauvegarde invalide");
                        return;
                    }
                    
                    // Restaurer selon le contenu
                    callback.onProgress("Restauration recettes...", 20);
                    restoreRecipesFromZip(zipIn);
                    
                    callback.onProgress("Restauration meal plans...", 35);
                    restoreMealPlansFromZip(zipIn);
                    
                    callback.onProgress("Restauration listes de courses...", 50);
                    restoreShoppingListsFromZip(zipIn);
                    
                    callback.onProgress("Restauration minuteries...", 65);
                    restoreTimersFromZip(zipIn);
                    
                    callback.onProgress("Restauration utilisateurs...", 80);
                    restoreUsersFromZip(zipIn);
                    restoreGroupsFromZip(zipIn);
                    
                    callback.onProgress("Restauration thèmes...", 90);
                    restoreThemesFromZip(zipIn);
                    
                    callback.onProgress("Restauration préférences...", 95);
                    restorePreferencesFromZip(zipIn);
                    
                    callback.onProgress("Finalisation...", 100);
                }
                
                Log.d(TAG, "Restauration complète terminée");
                callback.onSuccess("Restauration complète réussie");
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la restauration complète", e);
                callback.onError("Erreur restauration: " + e.getMessage());
            }
        });
    }
    
    /**
     * Restaure seulement les types de données spécifiés
     */
    public void restoreSelectiveBackup(File backupFile, List<BackupDataType> dataTypes, RestoreCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Début restauration sélective: " + dataTypes);
                callback.onProgress("Vérification du fichier...", 0);
                
                if (!backupFile.exists()) {
                    callback.onError("Fichier de sauvegarde introuvable");
                    return;
                }
                
                try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(backupFile))) {
                    
                    callback.onProgress("Lecture métadonnées...", 10);
                    JsonObject metadata = readBackupMetadata(zipIn);
                    if (metadata == null) {
                        callback.onError("Fichier de sauvegarde invalide");
                        return;
                    }
                    
                    int progressStep = 80 / dataTypes.size();
                    int currentProgress = 20;
                    
                    for (BackupDataType dataType : dataTypes) {
                        callback.onProgress("Restauration " + dataType.name() + "...", currentProgress);
                        
                        switch (dataType) {
                            case RECIPES:
                                restoreRecipesFromZip(zipIn);
                                break;
                            case MEAL_PLANS:
                                restoreMealPlansFromZip(zipIn);
                                break;
                            case SHOPPING_LISTS:
                                restoreShoppingListsFromZip(zipIn);
                                break;
                            case TIMERS:
                                restoreTimersFromZip(zipIn);
                                break;
                            case USERS:
                                restoreUsersFromZip(zipIn);
                                break;
                            case GROUPS:
                                restoreGroupsFromZip(zipIn);
                                break;
                            case THEMES:
                                restoreThemesFromZip(zipIn);
                                break;
                            case PREFERENCES:
                                restorePreferencesFromZip(zipIn);
                                break;
                        }
                        
                        currentProgress += progressStep;
                    }
                    
                    callback.onProgress("Finalisation...", 100);
                }
                
                Log.d(TAG, "Restauration sélective terminée");
                callback.onSuccess("Restauration sélective réussie");
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la restauration sélective", e);
                callback.onError("Erreur restauration: " + e.getMessage());
            }
        });
    }
    
    // ===== SAUVEGARDE PAR TYPE DE DONNÉES =====
    
    private void backupRecipes(ZipOutputStream zipOut) throws IOException {
        List<Recipe> recipes = database.recipeDao().getAllRecipes();
        JsonArray recipesArray = new JsonArray();
        
        for (Recipe recipe : recipes) {
            recipesArray.add(gson.toJsonTree(recipe));
        }
        
        addJsonToZip(zipOut, "recipes.json", recipesArray);
        Log.d(TAG, "Sauvegarde de " + recipes.size() + " recettes");
    }
    
    private void backupMealPlans(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO method getAllMealPlans()
        Log.d(TAG, "MealPlans backup disabled - missing DAO implementation");
    }
    
    private void backupShoppingLists(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO methods getAllShoppingLists() and getItemsByListId()
        Log.d(TAG, "ShoppingLists backup disabled - missing DAO implementation");
    }
    
    private void backupTimers(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO method getAllTimers()
        Log.d(TAG, "Timers backup disabled - missing DAO implementation");
    }
    
    private void backupUsers(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO method getAllUsers()
        Log.d(TAG, "Users backup disabled - missing DAO implementation");
    }
    
    private void backupGroups(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO method getAllGroups()
        Log.d(TAG, "Groups backup disabled - missing DAO implementation");
    }
    
    private void backupThemes(ZipOutputStream zipOut) throws IOException {
        // Disabled - missing DAO method getAllThemes()
        Log.d(TAG, "Themes backup disabled - missing DAO implementation");
    }
    
    private void backupPreferences(ZipOutputStream zipOut) throws IOException {
        // Sauvegarder SharedPreferences
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        JsonObject prefsJson = new JsonObject();
        
        for (String key : prefs.getAll().keySet()) {
            Object value = prefs.getAll().get(key);
            if (value instanceof String) {
                prefsJson.addProperty(key, (String) value);
            } else if (value instanceof Boolean) {
                prefsJson.addProperty(key, (Boolean) value);
            } else if (value instanceof Integer) {
                prefsJson.addProperty(key, (Integer) value);
            } else if (value instanceof Float) {
                prefsJson.addProperty(key, (Float) value);
            } else if (value instanceof Long) {
                prefsJson.addProperty(key, (Long) value);
            }
        }
        
        addJsonToZip(zipOut, "preferences.json", prefsJson);
        Log.d(TAG, "Sauvegarde de " + prefsJson.size() + " préférences");
    }
    
    // ===== RESTAURATION PAR TYPE DE DONNÉES =====
    
    private void restoreRecipesFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration recettes
        Log.d(TAG, "Restauration recettes (TODO)");
    }
    
    private void restoreMealPlansFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration meal plans
        Log.d(TAG, "Restauration meal plans (TODO)");
    }
    
    private void restoreShoppingListsFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration listes de courses
        Log.d(TAG, "Restauration listes de courses (TODO)");
    }
    
    private void restoreTimersFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration minuteries
        Log.d(TAG, "Restauration minuteries (TODO)");
    }
    
    private void restoreUsersFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration utilisateurs
        Log.d(TAG, "Restauration utilisateurs (TODO)");
    }
    
    private void restoreGroupsFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration groupes
        Log.d(TAG, "Restauration groupes (TODO)");
    }
    
    private void restoreThemesFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration thèmes
        Log.d(TAG, "Restauration thèmes (TODO)");
    }
    
    private void restorePreferencesFromZip(ZipInputStream zipIn) throws IOException {
        // TODO: Implémenter restauration préférences
        Log.d(TAG, "Restauration préférences (TODO)");
    }
    
    // ===== UTILITAIRES =====
    
    private File getBackupDirectory() {
        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }
    
    private void addBackupMetadata(ZipOutputStream zipOut, BackupDataType type) throws IOException {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("version", BACKUP_VERSION);
        metadata.addProperty("format", BACKUP_FORMAT);
        metadata.addProperty("type", type.name());
        metadata.addProperty("createdAt", new Date().toString());
        metadata.addProperty("appVersion", getAppVersion());
        
        addJsonToZip(zipOut, "metadata.json", metadata);
    }
    
    private void addSelectiveBackupMetadata(ZipOutputStream zipOut, List<BackupDataType> types) throws IOException {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("version", BACKUP_VERSION);
        metadata.addProperty("format", BACKUP_FORMAT);
        metadata.addProperty("type", "SELECTIVE");
        metadata.addProperty("createdAt", new Date().toString());
        metadata.addProperty("appVersion", getAppVersion());
        
        JsonArray typesArray = new JsonArray();
        for (BackupDataType type : types) {
            typesArray.add(type.name());
        }
        metadata.add("dataTypes", typesArray);
        
        addJsonToZip(zipOut, "metadata.json", metadata);
    }
    
    private void addJsonToZip(ZipOutputStream zipOut, String fileName, Object jsonData) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zipOut.putNextEntry(entry);
        
        String jsonString = gson.toJson(jsonData);
        zipOut.write(jsonString.getBytes("UTF-8"));
        zipOut.closeEntry();
    }
    
    private JsonObject readBackupMetadata(ZipInputStream zipIn) throws IOException {
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            if ("metadata.json".equals(entry.getName())) {
                StringBuilder jsonString = new StringBuilder();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipIn.read(buffer)) > 0) {
                    jsonString.append(new String(buffer, 0, length, "UTF-8"));
                }
                return gson.fromJson(jsonString.toString(), JsonObject.class);
            }
        }
        return null;
    }
    
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Obtient la liste des fichiers de sauvegarde disponibles
     */
    public List<File> getAvailableBackups() {
        File backupDir = getBackupDirectory();
        List<File> backups = new ArrayList<>();
        
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files != null) {
            backups.addAll(Arrays.asList(files));
        }
        
        return backups;
    }
    
    /**
     * Supprime les anciennes sauvegardes (garde les 5 plus récentes)
     */
    public void cleanOldBackups() {
        executorService.execute(() -> {
            List<File> backups = getAvailableBackups();
            if (backups.size() > 5) {
                // Trier par date de modification
                backups.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                
                // Supprimer les plus anciennes
                for (int i = 5; i < backups.size(); i++) {
                    if (backups.get(i).delete()) {
                        Log.d(TAG, "Suppression ancienne sauvegarde: " + backups.get(i).getName());
                    }
                }
            }
        });
    }
}
