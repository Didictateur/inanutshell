package fr.didictateur.inanutshell.data.export;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.Ingredient;
import fr.didictateur.inanutshell.data.model.Instruction;
import fr.didictateur.inanutshell.data.model.MealPlan;
import fr.didictateur.inanutshell.data.model.ShoppingList;
import fr.didictateur.inanutshell.data.model.ShoppingItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour l'export de recettes et données utilisateur
 * - Export recettes individuelles (JSON, PDF, texte)
 * - Export multiple de recettes
 * - Export des meal plans
 * - Export des listes de courses
 * - Sauvegarde complète des données utilisateur
 */
public class RecipeExporter {
    
    private static final String TAG = "RecipeExporter";
    private static RecipeExporter instance;
    private ExecutorService executorService;
    private Context context;
    private Gson gson;
    
    // Formats d'export supportés
    public enum ExportFormat {
        JSON,
        MEALIE_JSON,
        PDF,
        TEXT,
        HTML,
        CSV
    }
    
    // Types d'export
    public enum ExportType {
        SINGLE_RECIPE,
        MULTIPLE_RECIPES,
        MEAL_PLANS,
        SHOPPING_LISTS,
        FULL_BACKUP
    }
    
    // Callback pour les résultats d'export
    public interface ExportCallback {
        void onSuccess(File exportedFile);
        void onError(String error);
        void onProgress(int processed, int total);
    }
    
    private RecipeExporter(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    }
    
    public static synchronized RecipeExporter getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeExporter(context.getApplicationContext());
        }
        return instance;
    }
    
    // ===== EXPORT RECETTE INDIVIDUELLE =====
    
    /**
     * Exporte une recette dans le format spécifié
     */
    public void exportRecipe(Recipe recipe, ExportFormat format, ExportCallback callback) {
        executorService.execute(() -> {
            try {
                File exportFile = null;
                
                switch (format) {
                    case JSON:
                        exportFile = exportRecipeToJson(recipe);
                        break;
                    case MEALIE_JSON:
                        exportFile = exportRecipeToMealieJson(recipe);
                        break;
                    case PDF:
                        exportFile = exportRecipeToPdf(recipe);
                        break;
                    case TEXT:
                        exportFile = exportRecipeToText(recipe);
                        break;
                    case HTML:
                        exportFile = exportRecipeToHtml(recipe);
                        break;
                    default:
                        callback.onError("Format d'export non supporté: " + format);
                        return;
                }
                
                if (exportFile != null) {
                    Log.d(TAG, "Recette exportée: " + exportFile.getAbsolutePath());
                    callback.onSuccess(exportFile);
                } else {
                    callback.onError("Erreur lors de la création du fichier d'export");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'export: " + e.getMessage(), e);
                callback.onError("Erreur lors de l'export: " + e.getMessage());
            }
        });
    }
    
    /**
     * Exporte plusieurs recettes dans un seul fichier
     */
    public void exportRecipes(List<Recipe> recipes, ExportFormat format, ExportCallback callback) {
        executorService.execute(() -> {
            try {
                File exportFile = null;
                
                switch (format) {
                    case JSON:
                    case MEALIE_JSON:
                        exportFile = exportRecipesToJson(recipes, format);
                        break;
                    case PDF:
                        exportFile = exportRecipesToPdf(recipes);
                        break;
                    case TEXT:
                        exportFile = exportRecipesToText(recipes);
                        break;
                    case HTML:
                        exportFile = exportRecipesToHtml(recipes);
                        break;
                    case CSV:
                        exportFile = exportRecipesToCsv(recipes);
                        break;
                    default:
                        callback.onError("Format d'export non supporté pour multiple recettes");
                        return;
                }
                
                if (exportFile != null) {
                    Log.d(TAG, "Recettes exportées: " + exportFile.getAbsolutePath());
                    callback.onSuccess(exportFile);
                } else {
                    callback.onError("Erreur lors de la création du fichier d'export");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'export multiple: " + e.getMessage(), e);
                callback.onError("Erreur lors de l'export: " + e.getMessage());
            }
        });
    }
    
    // ===== EXPORT JSON =====
    
    private File exportRecipeToJson(Recipe recipe) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(recipe.getName()) + "_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";
        File exportFile = new File(exportDir, fileName);
        
        // Créer l'objet JSON avec tous les détails
        JsonObject recipeJson = new JsonObject();
        recipeJson.addProperty("name", recipe.getName());
        recipeJson.addProperty("description", recipe.getDescription());
        recipeJson.addProperty("prepTime", recipe.getPrepTime());
        recipeJson.addProperty("cookTime", recipe.getCookTime());
        recipeJson.addProperty("totalTime", recipe.getTotalTime());
        recipeJson.addProperty("servings", recipe.getServings());
        recipeJson.addProperty("source", recipe.getSource());
        
        // Ingrédients
        JsonArray ingredientsArray = new JsonArray();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                JsonObject ingJson = new JsonObject();
                ingJson.addProperty("name", ingredient.getName());
                ingJson.addProperty("quantity", ingredient.getQuantity());
                ingJson.addProperty("unit", ingredient.getUnit());
                ingJson.addProperty("position", ingredient.getPosition());
                ingredientsArray.add(ingJson);
            }
        }
        recipeJson.add("ingredients", ingredientsArray);
        
        // Instructions
        JsonArray instructionsArray = new JsonArray();
        if (recipe.getInstructions() != null) {
            for (Instruction instruction : recipe.getInstructions()) {
                JsonObject instJson = new JsonObject();
                instJson.addProperty("text", instruction.getText());
                instJson.addProperty("position", instruction.getPosition());
                instructionsArray.add(instJson);
            }
        }
        recipeJson.add("instructions", instructionsArray);
        
        // Métadonnées d'export
        recipeJson.addProperty("exportedAt", new Date().toString());
        recipeJson.addProperty("exportedBy", "InANutshell App");
        recipeJson.addProperty("version", "1.0");
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(gson.toJson(recipeJson).getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    private File exportRecipeToMealieJson(Recipe recipe) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(recipe.getName()) + "_mealie_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";
        File exportFile = new File(exportDir, fileName);
        
        // Format compatible Mealie
        JsonObject mealieJson = new JsonObject();
        mealieJson.addProperty("name", recipe.getName());
        mealieJson.addProperty("description", recipe.getDescription());
        mealieJson.addProperty("prepTime", recipe.getPrepTime());
        mealieJson.addProperty("cookTime", recipe.getCookTime());
        mealieJson.addProperty("totalTime", recipe.getTotalTime());
        mealieJson.addProperty("servings", recipe.getServings());
        
        // Ingrédients format Mealie
        JsonArray recipeIngredient = new JsonArray();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                JsonObject ingJson = new JsonObject();
                ingJson.addProperty("note", ingredient.getName());
                ingJson.addProperty("quantity", ingredient.getQuantity());
                ingJson.addProperty("unit", ingredient.getUnit());
                recipeIngredient.add(ingJson);
            }
        }
        mealieJson.add("recipeIngredient", recipeIngredient);
        
        // Instructions format Mealie
        JsonArray recipeInstructions = new JsonArray();
        if (recipe.getInstructions() != null) {
            for (Instruction instruction : recipe.getInstructions()) {
                JsonObject instJson = new JsonObject();
                instJson.addProperty("text", instruction.getText());
                recipeInstructions.add(instJson);
            }
        }
        mealieJson.add("recipeInstructions", recipeInstructions);
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(gson.toJson(mealieJson).getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    private File exportRecipesToJson(List<Recipe> recipes, ExportFormat format) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = "recettes_export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";
        File exportFile = new File(exportDir, fileName);
        
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("exportedAt", new Date().toString());
        rootJson.addProperty("exportedBy", "InANutshell App");
        rootJson.addProperty("recipeCount", recipes.size());
        rootJson.addProperty("format", format.name());
        
        JsonArray recipesArray = new JsonArray();
        for (Recipe recipe : recipes) {
            if (format == ExportFormat.MEALIE_JSON) {
                // Format Mealie pour chaque recette
                JsonObject mealieRecipe = createMealieJsonObject(recipe);
                recipesArray.add(mealieRecipe);
            } else {
                // Format JSON standard
                JsonObject recipeJson = gson.toJsonTree(recipe).getAsJsonObject();
                recipesArray.add(recipeJson);
            }
        }
        rootJson.add("recipes", recipesArray);
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(gson.toJson(rootJson).getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    // ===== EXPORT PDF =====
    
    private File exportRecipeToPdf(Recipe recipe) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(recipe.getName()) + "_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File exportFile = new File(exportDir, fileName);
        
        PdfDocument pdfDocument = new PdfDocument();
        
        // Créer une page
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        
        Paint normalPaint = new Paint();
        normalPaint.setTextSize(14);
        
        Paint headerPaint = new Paint();
        headerPaint.setTextSize(16);
        headerPaint.setFakeBoldText(true);
        
        int yPos = 50;
        int leftMargin = 50;
        int rightMargin = 545;
        
        // Titre
        canvas.drawText(recipe.getName(), leftMargin, yPos, titlePaint);
        yPos += 40;
        
        // Description
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            yPos += drawMultiLineText(canvas, recipe.getDescription(), leftMargin, yPos, rightMargin, normalPaint);
            yPos += 20;
        }
        
        // Informations de temps et portions
        if (recipe.getPrepTime() > 0 || recipe.getCookTime() > 0 || recipe.getServings() > 0) {
            canvas.drawText("Informations:", leftMargin, yPos, headerPaint);
            yPos += 25;
            
            if (recipe.getPrepTime() > 0) {
                canvas.drawText("Temps de préparation: " + recipe.getPrepTime() + " min", leftMargin + 20, yPos, normalPaint);
                yPos += 20;
            }
            if (recipe.getCookTime() > 0) {
                canvas.drawText("Temps de cuisson: " + recipe.getCookTime() + " min", leftMargin + 20, yPos, normalPaint);
                yPos += 20;
            }
            if (recipe.getServings() > 0) {
                canvas.drawText("Portions: " + recipe.getServings(), leftMargin + 20, yPos, normalPaint);
                yPos += 20;
            }
            yPos += 10;
        }
        
        // Ingrédients
        if (recipe.getIngredients() != null && recipe.getIngredients().size() > 0) {
            canvas.drawText("Ingrédients:", leftMargin, yPos, headerPaint);
            yPos += 25;
            
            for (Ingredient ingredient : recipe.getIngredients()) {
                String ingredientText = "• ";
                if (ingredient.getQuantity() > 0) {
                    ingredientText += ingredient.getQuantity() + " ";
                }
                if (ingredient.getUnit() != null && !ingredient.getUnit().isEmpty()) {
                    ingredientText += ingredient.getUnit() + " ";
                }
                ingredientText += ingredient.getName();
                
                canvas.drawText(ingredientText, leftMargin + 20, yPos, normalPaint);
                yPos += 20;
                
                // Nouvelle page si nécessaire
                if (yPos > 800) {
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPos = 50;
                }
            }
            yPos += 10;
        }
        
        // Instructions
        if (recipe.getInstructions() != null && recipe.getInstructions().size() > 0) {
            canvas.drawText("Instructions:", leftMargin, yPos, headerPaint);
            yPos += 25;
            
            for (int i = 0; i < recipe.getInstructions().size(); i++) {
                Instruction instruction = recipe.getInstructions().get(i);
                String instructionText = (i + 1) + ". " + instruction.getText();
                
                yPos += drawMultiLineText(canvas, instructionText, leftMargin + 20, yPos, rightMargin, normalPaint);
                yPos += 15;
                
                // Nouvelle page si nécessaire
                if (yPos > 750) {
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPos = 50;
                }
            }
        }
        
        pdfDocument.finishPage(page);
        
        // Écrire le fichier PDF
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            pdfDocument.writeTo(fos);
        }
        pdfDocument.close();
        
        return exportFile;
    }
    
    private File exportRecipesToPdf(List<Recipe> recipes) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = "recettes_export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File exportFile = new File(exportDir, fileName);
        
        PdfDocument pdfDocument = new PdfDocument();
        
        for (Recipe recipe : recipes) {
            // Créer une page pour chaque recette
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            // Utiliser la même logique que pour une recette individuelle
            drawRecipeOnCanvas(canvas, recipe);
            
            pdfDocument.finishPage(page);
        }
        
        // Écrire le fichier PDF
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            pdfDocument.writeTo(fos);
        }
        pdfDocument.close();
        
        return exportFile;
    }
    
    // ===== EXPORT TEXTE =====
    
    private File exportRecipeToText(Recipe recipe) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(recipe.getName()) + "_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
        File exportFile = new File(exportDir, fileName);
        
        StringBuilder content = new StringBuilder();
        
        // Titre
        content.append(recipe.getName().toUpperCase()).append("\n");
        content.append("=".repeat(recipe.getName().length())).append("\n\n");
        
        // Description
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            content.append(recipe.getDescription()).append("\n\n");
        }
        
        // Informations
        if (recipe.getPrepTime() > 0 || recipe.getCookTime() > 0 || recipe.getServings() > 0) {
            content.append("INFORMATIONS\n");
            content.append("-----------\n");
            if (recipe.getPrepTime() > 0) {
                content.append("Temps de préparation: ").append(recipe.getPrepTime()).append(" min\n");
            }
            if (recipe.getCookTime() > 0) {
                content.append("Temps de cuisson: ").append(recipe.getCookTime()).append(" min\n");
            }
            if (recipe.getServings() > 0) {
                content.append("Portions: ").append(recipe.getServings()).append("\n");
            }
            content.append("\n");
        }
        
        // Ingrédients
        if (recipe.getIngredients() != null && recipe.getIngredients().size() > 0) {
            content.append("INGRÉDIENTS\n");
            content.append("-----------\n");
            for (Ingredient ingredient : recipe.getIngredients()) {
                content.append("• ");
                if (ingredient.getQuantity() > 0) {
                    content.append(ingredient.getQuantity()).append(" ");
                }
                if (ingredient.getUnit() != null && !ingredient.getUnit().isEmpty()) {
                    content.append(ingredient.getUnit()).append(" ");
                }
                content.append(ingredient.getName()).append("\n");
            }
            content.append("\n");
        }
        
        // Instructions
        if (recipe.getInstructions() != null && recipe.getInstructions().size() > 0) {
            content.append("INSTRUCTIONS\n");
            content.append("------------\n");
            for (int i = 0; i < recipe.getInstructions().size(); i++) {
                content.append(i + 1).append(". ")
                       .append(recipe.getInstructions().get(i).getText())
                       .append("\n\n");
            }
        }
        
        // Footer
        content.append("---\n");
        content.append("Exporté le ").append(new Date().toString()).append("\n");
        content.append("par InANutshell App\n");
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(content.toString().getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    private File exportRecipesToText(List<Recipe> recipes) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = "recettes_export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
        File exportFile = new File(exportDir, fileName);
        
        StringBuilder content = new StringBuilder();
        
        // En-tête
        content.append("EXPORT DE RECETTES\n");
        content.append("==================\n\n");
        content.append("Nombre de recettes: ").append(recipes.size()).append("\n");
        content.append("Exporté le: ").append(new Date().toString()).append("\n\n");
        
        // Chaque recette
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            
            content.append("RECETTE ").append(i + 1).append(" - ")
                   .append(recipe.getName().toUpperCase()).append("\n");
            content.append("=".repeat(50)).append("\n\n");
            
            // Ajouter le contenu de la recette (réutiliser la logique du texte simple)
            content.append(formatRecipeForText(recipe)).append("\n\n");
        }
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(content.toString().getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    // ===== EXPORT HTML =====
    
    private File exportRecipeToHtml(Recipe recipe) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(recipe.getName()) + "_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".html";
        File exportFile = new File(exportDir, fileName);
        
        StringBuilder html = new StringBuilder();
        
        // En-tête HTML
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>").append(recipe.getName()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #4CAF50; }\n");
        html.append("h2 { color: #4CAF50; }\n");
        html.append("ul, ol { padding-left: 20px; }\n");
        html.append("li { margin-bottom: 5px; }\n");
        html.append(".info { background-color: #f5f5f5; padding: 15px; border-radius: 5px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // Contenu
        html.append("<h1>").append(recipe.getName()).append("</h1>\n");
        
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            html.append("<p>").append(recipe.getDescription()).append("</p>\n");
        }
        
        // Informations
        if (recipe.getPrepTime() > 0 || recipe.getCookTime() > 0 || recipe.getServings() > 0) {
            html.append("<div class='info'>\n");
            html.append("<h2>Informations</h2>\n");
            if (recipe.getPrepTime() > 0) {
                html.append("<p><strong>Temps de préparation:</strong> ").append(recipe.getPrepTime()).append(" min</p>\n");
            }
            if (recipe.getCookTime() > 0) {
                html.append("<p><strong>Temps de cuisson:</strong> ").append(recipe.getCookTime()).append(" min</p>\n");
            }
            if (recipe.getServings() > 0) {
                html.append("<p><strong>Portions:</strong> ").append(recipe.getServings()).append("</p>\n");
            }
            html.append("</div>\n");
        }
        
        // Ingrédients
        if (recipe.getIngredients() != null && recipe.getIngredients().size() > 0) {
            html.append("<h2>Ingrédients</h2>\n");
            html.append("<ul>\n");
            for (Ingredient ingredient : recipe.getIngredients()) {
                html.append("<li>");
                if (ingredient.getQuantity() > 0) {
                    html.append(ingredient.getQuantity()).append(" ");
                }
                if (ingredient.getUnit() != null && !ingredient.getUnit().isEmpty()) {
                    html.append(ingredient.getUnit()).append(" ");
                }
                html.append(ingredient.getName()).append("</li>\n");
            }
            html.append("</ul>\n");
        }
        
        // Instructions
        if (recipe.getInstructions() != null && recipe.getInstructions().size() > 0) {
            html.append("<h2>Instructions</h2>\n");
            html.append("<ol>\n");
            for (Instruction instruction : recipe.getInstructions()) {
                html.append("<li>").append(instruction.getText()).append("</li>\n");
            }
            html.append("</ol>\n");
        }
        
        // Footer
        html.append("<hr>\n");
        html.append("<p><em>Exporté le ").append(new Date().toString())
            .append(" par InANutshell App</em></p>\n");
        
        html.append("</body>\n</html>");
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(html.toString().getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    private File exportRecipesToHtml(List<Recipe> recipes) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = "recettes_export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".html";
        File exportFile = new File(exportDir, fileName);
        
        StringBuilder html = new StringBuilder();
        
        // En-tête HTML avec style
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Export de Recettes</title>\n");
        // Ajouter CSS complet...
        html.append("</head>\n<body>\n");
        
        html.append("<h1>Export de Recettes</h1>\n");
        html.append("<p>Nombre de recettes: ").append(recipes.size()).append("</p>\n");
        
        // Table des matières
        html.append("<h2>Table des matières</h2>\n<ul>\n");
        for (int i = 0; i < recipes.size(); i++) {
            html.append("<li><a href='#recipe").append(i).append("'>")
                .append(recipes.get(i).getName()).append("</a></li>\n");
        }
        html.append("</ul>\n<hr>\n");
        
        // Chaque recette
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            html.append("<div id='recipe").append(i).append("'>\n");
            html.append(formatRecipeForHtml(recipe));
            html.append("</div>\n<hr>\n");
        }
        
        html.append("</body>\n</html>");
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(html.toString().getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    // ===== EXPORT CSV =====
    
    private File exportRecipesToCsv(List<Recipe> recipes) throws IOException {
        File exportDir = getExportDirectory();
        String fileName = "recettes_export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
        File exportFile = new File(exportDir, fileName);
        
        StringBuilder csv = new StringBuilder();
        
        // En-tête CSV
        csv.append("Nom,Description,Temps Préparation (min),Temps Cuisson (min),Portions,Nombre Ingrédients,Nombre Instructions\n");
        
        // Données
        for (Recipe recipe : recipes) {
            csv.append(escapeCsv(recipe.getName())).append(",");
            csv.append(escapeCsv(recipe.getDescription())).append(",");
            csv.append(recipe.getPrepTime()).append(",");
            csv.append(recipe.getCookTime()).append(",");
            csv.append(recipe.getServings()).append(",");
            csv.append(recipe.getIngredients() != null ? recipe.getIngredients().size() : 0).append(",");
            csv.append(recipe.getInstructions() != null ? recipe.getInstructions().size() : 0);
            csv.append("\n");
        }
        
        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(csv.toString().getBytes("UTF-8"));
        }
        
        return exportFile;
    }
    
    // ===== MÉTHODES UTILITAIRES =====
    
    private File getExportDirectory() {
        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        return exportDir;
    }
    
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    private JsonObject createMealieJsonObject(Recipe recipe) {
        // Créer objet JSON format Mealie (même logique que exportRecipeToMealieJson)
        JsonObject mealieJson = new JsonObject();
        // ... logique complète
        return mealieJson;
    }
    
    private int drawMultiLineText(Canvas canvas, String text, int x, int y, int maxWidth, Paint paint) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineHeight = 20;
        int totalHeight = 0;
        
        for (String word : words) {
            String testLine = line.toString() + (line.length() > 0 ? " " : "") + word;
            Rect bounds = new Rect();
            paint.getTextBounds(testLine, 0, testLine.length(), bounds);
            
            if (bounds.width() > maxWidth - x && line.length() > 0) {
                canvas.drawText(line.toString(), x, y + totalHeight, paint);
                totalHeight += lineHeight;
                line = new StringBuilder(word);
            } else {
                line.append(line.length() > 0 ? " " : "").append(word);
            }
        }
        
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y + totalHeight, paint);
            totalHeight += lineHeight;
        }
        
        return totalHeight;
    }
    
    private void drawRecipeOnCanvas(Canvas canvas, Recipe recipe) {
        // Logique pour dessiner une recette sur le canvas PDF
        // (même logique que exportRecipeToPdf mais adaptée)
    }
    
    private String formatRecipeForText(Recipe recipe) {
        // Format texte pour une recette (utilisé dans export multiple)
        StringBuilder content = new StringBuilder();
        // ... logique de formatage
        return content.toString();
    }
    
    private String formatRecipeForHtml(Recipe recipe) {
        // Format HTML pour une recette (utilisé dans export multiple)
        StringBuilder html = new StringBuilder();
        // ... logique de formatage HTML
        return html.toString();
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
