package fr.didictateur.inanutshell.data.sharing;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.Ingredient;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.export.RecipeExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire pour le partage de recettes vers d'autres applications Android
 * Utilise le système d'Intent Android pour partager vers:
 * - Applications de messagerie (WhatsApp, SMS, Email)
 * - Applications de notes (Keep, Evernote, etc.)
 * - Réseaux sociaux (Facebook, Twitter, etc.)  
 * - Applications de stockage cloud (Drive, Dropbox, etc.)
 */
public class RecipeSharingManager {
    
    private static final String TAG = "RecipeSharingManager";
    private static RecipeSharingManager instance;
    private Context context;
    private RecipeExporter recipeExporter;
    
    // Types de partage
    public enum ShareType {
        TEXT_ONLY,           // Texte simple via Intent
        FILE_JSON,          // Fichier JSON
        FILE_PDF,           // Fichier PDF  
        FILE_TEXT,          // Fichier texte
        FILE_HTML,          // Fichier HTML
        RECIPE_CARD         // Card visuelle (future feature)
    }
    
    // Callback pour les résultats de partage
    public interface SharingCallback {
        void onSharingReady(Intent shareIntent);
        void onError(String error);
    }
    
    private RecipeSharingManager(Context context) {
        this.context = context.getApplicationContext();
        this.recipeExporter = RecipeExporter.getInstance(context);
    }
    
    public static synchronized RecipeSharingManager getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeSharingManager(context);
        }
        return instance;
    }
    
    // ===== PARTAGE RECETTE INDIVIDUELLE =====
    
    /**
     * Partage une recette selon le type spécifié
     */
    public void shareRecipe(Recipe recipe, ShareType shareType, SharingCallback callback) {
        switch (shareType) {
            case TEXT_ONLY:
                shareRecipeAsText(recipe, callback);
                break;
            case FILE_JSON:
                shareRecipeAsFile(recipe, RecipeExporter.ExportFormat.JSON, callback);
                break;
            case FILE_PDF:
                shareRecipeAsFile(recipe, RecipeExporter.ExportFormat.PDF, callback);
                break;
            case FILE_TEXT:
                shareRecipeAsFile(recipe, RecipeExporter.ExportFormat.TEXT, callback);
                break;
            case FILE_HTML:
                shareRecipeAsFile(recipe, RecipeExporter.ExportFormat.HTML, callback);
                break;
            default:
                callback.onError("Type de partage non supporté");
        }
    }
    
    /**
     * Partage rapide d'une recette en texte (défaut)
     */
    public Intent shareRecipeQuick(Recipe recipe) {
        String recipeText = formatRecipeAsText(recipe);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
        
        return Intent.createChooser(shareIntent, "Partager la recette");
    }
    
    // ===== PARTAGE MULTIPLE =====
    
    /**
     * Partage plusieurs recettes
     */
    public void shareRecipes(List<Recipe> recipes, ShareType shareType, SharingCallback callback) {
        if (recipes.isEmpty()) {
            callback.onError("Aucune recette à partager");
            return;
        }
        
        if (recipes.size() == 1) {
            // Partage simple si une seule recette
            shareRecipe(recipes.get(0), shareType, callback);
            return;
        }
        
        switch (shareType) {
            case TEXT_ONLY:
                shareRecipesAsText(recipes, callback);
                break;
            case FILE_JSON:
            case FILE_PDF:
            case FILE_TEXT:
            case FILE_HTML:
                shareRecipesAsFile(recipes, convertToExportFormat(shareType), callback);
                break;
            default:
                callback.onError("Type de partage non supporté pour multiple recettes");
        }
    }
    
    // ===== PARTAGE TEXTE =====
    
    private void shareRecipeAsText(Recipe recipe, SharingCallback callback) {
        try {
            String recipeText = formatRecipeAsText(recipe);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
            
            Intent chooserIntent = Intent.createChooser(shareIntent, "Partager la recette");
            callback.onSharingReady(chooserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur partage texte", e);
            callback.onError("Erreur lors du partage: " + e.getMessage());
        }
    }
    
    private void shareRecipesAsText(List<Recipe> recipes, SharingCallback callback) {
        try {
            StringBuilder allRecipes = new StringBuilder();
            allRecipes.append("📚 COLLECTION DE RECETTES\n");
            allRecipes.append("========================\n\n");
            
            for (int i = 0; i < recipes.size(); i++) {
                Recipe recipe = recipes.get(i);
                allRecipes.append("🍽️ RECETTE ").append(i + 1).append("\n");
                allRecipes.append(formatRecipeAsText(recipe));
                allRecipes.append("\n" + "─".repeat(50) + "\n\n");
            }
            
            allRecipes.append("Partagé depuis InANutshell 🥜\n");
            allRecipes.append("Nombre total de recettes: ").append(recipes.size());
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, allRecipes.toString());
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Collection de " + recipes.size() + " recettes");
            
            Intent chooserIntent = Intent.createChooser(shareIntent, "Partager les recettes");
            callback.onSharingReady(chooserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur partage texte multiple", e);
            callback.onError("Erreur lors du partage: " + e.getMessage());
        }
    }
    
    // ===== PARTAGE FICHIER =====
    
    private void shareRecipeAsFile(Recipe recipe, RecipeExporter.ExportFormat format, SharingCallback callback) {
        recipeExporter.exportRecipe(recipe, format, new RecipeExporter.ExportCallback() {
            @Override
            public void onSuccess(File exportedFile) {
                createFileShareIntent(exportedFile, "Partager la recette", callback);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Erreur export pour partage: " + error);
                callback.onError("Erreur lors de l'export: " + error);
            }
            
            @Override
            public void onProgress(int processed, int total) {
                // Pas de progress pour partage simple
            }
        });
    }
    
    private void shareRecipesAsFile(List<Recipe> recipes, RecipeExporter.ExportFormat format, SharingCallback callback) {
        recipeExporter.exportRecipes(recipes, format, new RecipeExporter.ExportCallback() {
            @Override
            public void onSuccess(File exportedFile) {
                createFileShareIntent(exportedFile, "Partager les recettes", callback);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Erreur export multiple pour partage: " + error);
                callback.onError("Erreur lors de l'export: " + error);
            }
            
            @Override
            public void onProgress(int processed, int total) {
                // Progress géré par l'UI appelante
            }
        });
    }
    
    private void createFileShareIntent(File file, String title, SharingCallback callback) {
        try {
            // Utiliser FileProvider pour partager le fichier de manière sécurisée
            Uri fileUri = FileProvider.getUriForFile(
                context, 
                context.getPackageName() + ".fileprovider", 
                file
            );
            
            String mimeType = getMimeTypeForFile(file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Ajouter des métadonnées utiles
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Partagé depuis InANutshell 🥜");
            
            Intent chooserIntent = Intent.createChooser(shareIntent, title);
            callback.onSharingReady(chooserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur création Intent partage fichier", e);
            callback.onError("Erreur lors du partage du fichier: " + e.getMessage());
        }
    }
    
    // ===== PARTAGE SPÉCIALISÉ =====
    
    /**
     * Partage optimisé pour WhatsApp (texte court avec emojis)
     */
    public Intent shareToWhatsApp(Recipe recipe) {
        String text = formatRecipeForWhatsApp(recipe);
        
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, text);
        
        return whatsappIntent;
    }
    
    /**
     * Partage optimisé pour email (HTML riche)
     */
    public void shareViaEmail(Recipe recipe, SharingCallback callback) {
        shareRecipeAsFile(recipe, RecipeExporter.ExportFormat.HTML, new SharingCallback() {
            @Override
            public void onSharingReady(Intent shareIntent) {
                // Modifier pour email spécifiquement
                shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{});
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
                shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "Voici une délicieuse recette à essayer !\n\nBon appétit ! 😋\n\nEnvoyé depuis InANutshell");
                
                callback.onSharingReady(shareIntent);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Partage vers le presse-papier
     */
    public boolean copyRecipeToClipboard(Recipe recipe) {
        try {
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            
            String recipeText = formatRecipeAsText(recipe);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Recette", recipeText);
            clipboard.setPrimaryClip(clip);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur copie presse-papier", e);
            return false;
        }
    }
    
    // ===== FORMATAGE TEXTE =====
    
    private String formatRecipeAsText(Recipe recipe) {
        StringBuilder text = new StringBuilder();
        
        text.append("🍽️ ").append(recipe.getName().toUpperCase()).append("\n");
        
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            text.append("\n📝 ").append(recipe.getDescription()).append("\n");
        }
        
        // Informations rapides
        if ((recipe.getPrepTime() != null && !recipe.getPrepTime().isEmpty() && Integer.parseInt(recipe.getPrepTime()) > 0) || (recipe.getCookTime() != null && !recipe.getCookTime().isEmpty() && Integer.parseInt(recipe.getCookTime()) > 0) || (recipe.getRecipeYield() != null && !recipe.getRecipeYield().isEmpty() && Integer.parseInt(recipe.getRecipeYield()) > 0)) {
            text.append("\n⏱️ INFOS:\n");
            if ((recipe.getPrepTime() != null && !recipe.getPrepTime().isEmpty() && Integer.parseInt(recipe.getPrepTime()) > 0)) {
                text.append("• Préparation: ").append(recipe.getPrepTime()).append(" min\n");
            }
            if ((recipe.getCookTime() != null && !recipe.getCookTime().isEmpty() && Integer.parseInt(recipe.getCookTime()) > 0)) {
                text.append("• Cuisson: ").append(recipe.getCookTime()).append(" min\n");
            }
            if ((recipe.getRecipeYield() != null && !recipe.getRecipeYield().isEmpty() && Integer.parseInt(recipe.getRecipeYield()) > 0)) {
                text.append("• Portions: ").append(recipe.getRecipeYield()).append("\n");
            }
        }
        
        // Ingrédients
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            text.append("\n🛒 INGRÉDIENTS:\n");
            for (int i = 0; i < recipe.getRecipeIngredient().size(); i++) {
                RecipeIngredient ingredient = recipe.getRecipeIngredient().get(i);
                text.append("• ");
                
                if (ingredient.getQuantity() > 0) {
                    text.append(ingredient.getQuantity()).append(" ");
                }
                if (ingredient.getUnit() != null && !ingredient.getUnit().isEmpty()) {
                    text.append(ingredient.getUnit()).append(" ");
                }
                text.append(ingredient.getFood()).append("\n");
            }
        }
        
        // Instructions (limitées pour le partage)
        if (recipe.getRecipeInstructions() != null && !recipe.getRecipeInstructions().isEmpty()) {
            text.append("\n👩‍🍳 ÉTAPES:\n");
            int maxSteps = Math.min(recipe.getRecipeInstructions().size(), 5); // Limiter pour partage
            
            for (int i = 0; i < maxSteps; i++) {
                text.append(i + 1).append(". ")
                    .append(recipe.getRecipeInstructions().get(i).getText())
                    .append("\n");
            }
            
            if (recipe.getRecipeInstructions().size() > 5) {
                text.append("... (").append(recipe.getRecipeInstructions().size() - 5)
                    .append(" étapes supplémentaires dans l'app)\n");
            }
        }
        
        text.append("\n🥜 Partagé depuis InANutshell");
        return text.toString();
    }
    
    private String formatRecipeForWhatsApp(Recipe recipe) {
        // Version condensée pour WhatsApp
        StringBuilder text = new StringBuilder();
        
        text.append("🍽️ *").append(recipe.getName()).append("*\n\n");
        
        if ((recipe.getPrepTime() != null && !recipe.getPrepTime().isEmpty() && Integer.parseInt(recipe.getPrepTime()) > 0)) {
            text.append("⏱️ ").append(recipe.getPrepTime()).append(" min");
        }
        if ((recipe.getRecipeYield() != null && !recipe.getRecipeYield().isEmpty() && Integer.parseInt(recipe.getRecipeYield()) > 0)) {
            text.append(" | 👥 ").append(recipe.getRecipeYield()).append(" portions");
        }
        text.append("\n\n");
        
        // Ingrédients principaux seulement
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            text.append("🛒 *Ingrédients:*\n");
            int maxIngredients = Math.min(recipe.getRecipeIngredient().size(), 6);
            
            for (int i = 0; i < maxIngredients; i++) {
                RecipeIngredient ingredient = recipe.getRecipeIngredient().get(i);
                text.append("• ").append(ingredient.getFood()).append("\n");
            }
            
            if (recipe.getRecipeIngredient().size() > 6) {
                text.append("... et ").append(recipe.getRecipeIngredient().size() - 6).append(" autres\n");
            }
        }
        
        text.append("\n🥜 _Recette complète dans InANutshell_");
        return text.toString();
    }
    
    // ===== UTILITAIRES =====
    
    private RecipeExporter.ExportFormat convertToExportFormat(ShareType shareType) {
        switch (shareType) {
            case FILE_JSON: return RecipeExporter.ExportFormat.JSON;
            case FILE_PDF: return RecipeExporter.ExportFormat.PDF;
            case FILE_TEXT: return RecipeExporter.ExportFormat.TEXT;
            case FILE_HTML: return RecipeExporter.ExportFormat.HTML;
            default: return RecipeExporter.ExportFormat.TEXT;
        }
    }
    
    private String getMimeTypeForFile(File file) {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else {
            return "application/octet-stream"; // Générique
        }
    }
    
    /**
     * Vérifie si une application spécifique est installée
     */
    public boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtient la liste des applications qui peuvent recevoir du partage
     */
    public List<String> getAvailableSharingApps() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        List<String> availableApps = new ArrayList<>();
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(shareIntent, 0);
        
        for (ResolveInfo resolveInfo : resolveInfos) {
            availableApps.add(resolveInfo.activityInfo.packageName);
        }
        
        return availableApps;
    }
}
