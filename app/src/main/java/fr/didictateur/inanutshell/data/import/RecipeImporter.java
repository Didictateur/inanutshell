package fr.didictateur.inanutshell.data.import;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.Ingredient;
import fr.didictateur.inanutshell.data.model.Instruction;
import fr.didictateur.inanutshell.data.network.NetworkManager;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour l'import de recettes depuis différentes sources
 * - URLs de sites web (avec parsing automatique JSON-LD, Microdata, etc.)
 * - Fichiers JSON (format Mealie, RecipeKeeper, etc.)
 * - Autres formats courants
 */
public class RecipeImporter {
    
    private static final String TAG = "RecipeImporter";
    private static RecipeImporter instance;
    private ExecutorService executorService;
    private NetworkManager networkManager;
    private Context context;
    
    // Formats supportés
    public enum ImportFormat {
        AUTO_DETECT,
        JSON_LD,
        MICRODATA,
        MEALIE_JSON,
        RECIPE_KEEPER_JSON,
        GENERIC_JSON,
        PLAIN_TEXT
    }
    
    // Callback pour les résultats d'import
    public interface ImportCallback {
        void onSuccess(Recipe recipe);
        void onError(String error);
    }
    
    public interface MultipleImportCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
        void onProgress(int processed, int total);
    }
    
    private RecipeImporter(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(3);
        this.networkManager = NetworkManager.getInstance(context);
    }
    
    public static synchronized RecipeImporter getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeImporter(context.getApplicationContext());
        }
        return instance;
    }
    
    // ===== IMPORT DEPUIS URL =====
    
    /**
     * Importe une recette depuis une URL de site web
     */
    public void importFromUrl(String url, ImportCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Importation depuis URL: " + url);
                
                // Télécharger la page web
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; InANutshell Recipe Importer)")
                    .timeout(10000)
                    .get();
                
                Recipe recipe = null;
                
                // Essayer différentes méthodes de parsing
                recipe = parseJsonLd(doc);
                if (recipe == null) {
                    recipe = parseMicrodata(doc);
                }
                if (recipe == null) {
                    recipe = parseGenericHtml(doc);
                }
                
                if (recipe != null) {
                    // Ajouter l'URL source
                    recipe.setSource(url);
                    callback.onSuccess(recipe);
                } else {
                    callback.onError("Aucune recette trouvée sur cette page");
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Erreur réseau lors de l'import: " + e.getMessage());
                callback.onError("Erreur de connexion: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'import: " + e.getMessage());
                callback.onError("Erreur lors de l'analyse: " + e.getMessage());
            }
        });
    }
    
    /**
     * Importe plusieurs recettes depuis une liste d'URLs
     */
    public void importFromUrls(List<String> urls, MultipleImportCallback callback) {
        executorService.execute(() -> {
            List<Recipe> recipes = new ArrayList<>();
            int processed = 0;
            
            for (String url : urls) {
                try {
                    importFromUrlSynchronous(url, new ImportCallback() {
                        @Override
                        public void onSuccess(Recipe recipe) {
                            synchronized (recipes) {
                                recipes.add(recipe);
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Erreur import URL " + url + ": " + error);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erreur import URL " + url, e);
                }
                
                processed++;
                callback.onProgress(processed, urls.size());
            }
            
            if (recipes.size() > 0) {
                callback.onSuccess(recipes);
            } else {
                callback.onError("Aucune recette n'a pu être importée");
            }
        });
    }
    
    // ===== PARSING JSON-LD (Schema.org) =====
    
    private Recipe parseJsonLd(Document doc) {
        try {
            Elements scripts = doc.select("script[type=application/ld+json]");
            
            for (Element script : scripts) {
                String jsonContent = script.html();
                JSONObject json = new JSONObject(jsonContent);
                
                // Gérer les arrays JSON-LD
                if (json.has("@graph")) {
                    JSONArray graph = json.getJSONArray("@graph");
                    for (int i = 0; i < graph.length(); i++) {
                        JSONObject item = graph.getJSONObject(i);
                        if (isRecipeObject(item)) {
                            return parseRecipeFromJsonLd(item);
                        }
                    }
                } else if (isRecipeObject(json)) {
                    return parseRecipeFromJsonLd(json);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Erreur parsing JSON-LD: " + e.getMessage());
        }
        return null;
    }
    
    private boolean isRecipeObject(JSONObject json) {
        try {
            String type = json.optString("@type", "");
            return type.equals("Recipe") || type.contains("Recipe");
        } catch (Exception e) {
            return false;
        }
    }
    
    private Recipe parseRecipeFromJsonLd(JSONObject json) throws JSONException {
        Recipe recipe = new Recipe();
        
        // Informations de base
        recipe.setName(json.optString("name", "Recette importée"));
        recipe.setDescription(json.optString("description", ""));
        
        // Temps de cuisson
        String cookTime = json.optString("cookTime", "");
        String prepTime = json.optString("prepTime", "");
        String totalTime = json.optString("totalTime", "");
        
        if (!cookTime.isEmpty()) {
            recipe.setCookTime(parseDuration(cookTime));
        }
        if (!prepTime.isEmpty()) {
            recipe.setPrepTime(parseDuration(prepTime));
        }
        if (!totalTime.isEmpty() && recipe.getTotalTime() == 0) {
            recipe.setTotalTime(parseDuration(totalTime));
        }
        
        // Portions
        if (json.has("recipeYield")) {
            String yield = json.optString("recipeYield", "4");
            try {
                recipe.setServings(Integer.parseInt(yield.replaceAll("[^0-9]", "")));
            } catch (NumberFormatException e) {
                recipe.setServings(4);
            }
        }
        
        // Ingrédients
        if (json.has("recipeIngredient")) {
            JSONArray ingredients = json.getJSONArray("recipeIngredient");
            List<Ingredient> ingredientList = new ArrayList<>();
            
            for (int i = 0; i < ingredients.length(); i++) {
                String ingredientText = ingredients.getString(i);
                Ingredient ingredient = parseIngredientText(ingredientText);
                ingredient.setPosition(i + 1);
                ingredientList.add(ingredient);
            }
            recipe.setIngredients(ingredientList);
        }
        
        // Instructions
        if (json.has("recipeInstructions")) {
            JSONArray instructions = json.getJSONArray("recipeInstructions");
            List<Instruction> instructionList = new ArrayList<>();
            
            for (int i = 0; i < instructions.length(); i++) {
                Object instructionObj = instructions.get(i);
                String instructionText = "";
                
                if (instructionObj instanceof JSONObject) {
                    JSONObject instJson = (JSONObject) instructionObj;
                    instructionText = instJson.optString("text", 
                        instJson.optString("name", instructionObj.toString()));
                } else {
                    instructionText = instructionObj.toString();
                }
                
                Instruction instruction = new Instruction();
                instruction.setText(instructionText);
                instruction.setPosition(i + 1);
                instructionList.add(instruction);
            }
            recipe.setInstructions(instructionList);
        }
        
        Log.d(TAG, "Recette JSON-LD parsée: " + recipe.getName());
        return recipe;
    }
    
    // ===== PARSING MICRODATA =====
    
    private Recipe parseMicrodata(Document doc) {
        Elements recipeElements = doc.select("[itemtype*=Recipe]");
        
        if (recipeElements.size() > 0) {
            Element recipeEl = recipeElements.first();
            Recipe recipe = new Recipe();
            
            // Nom de la recette
            Element nameEl = recipeEl.selectFirst("[itemprop=name]");
            if (nameEl != null) {
                recipe.setName(nameEl.text());
            }
            
            // Description
            Element descEl = recipeEl.selectFirst("[itemprop=description]");
            if (descEl != null) {
                recipe.setDescription(descEl.text());
            }
            
            // Temps de cuisson
            Element cookTimeEl = recipeEl.selectFirst("[itemprop=cookTime]");
            if (cookTimeEl != null) {
                recipe.setCookTime(parseDuration(cookTimeEl.attr("datetime")));
            }
            
            // Ingrédients
            Elements ingredientEls = recipeEl.select("[itemprop=recipeIngredient]");
            List<Ingredient> ingredients = new ArrayList<>();
            for (int i = 0; i < ingredientEls.size(); i++) {
                String text = ingredientEls.get(i).text();
                Ingredient ingredient = parseIngredientText(text);
                ingredient.setPosition(i + 1);
                ingredients.add(ingredient);
            }
            recipe.setIngredients(ingredients);
            
            // Instructions
            Elements instructionEls = recipeEl.select("[itemprop=recipeInstructions]");
            List<Instruction> instructions = new ArrayList<>();
            for (int i = 0; i < instructionEls.size(); i++) {
                String text = instructionEls.get(i).text();
                Instruction instruction = new Instruction();
                instruction.setText(text);
                instruction.setPosition(i + 1);
                instructions.add(instruction);
            }
            recipe.setInstructions(instructions);
            
            Log.d(TAG, "Recette Microdata parsée: " + recipe.getName());
            return recipe;
        }
        
        return null;
    }
    
    // ===== PARSING HTML GÉNÉRIQUE =====
    
    private Recipe parseGenericHtml(Document doc) {
        Recipe recipe = new Recipe();
        
        // Essayer de trouver le titre
        String title = doc.title();
        if (title.isEmpty()) {
            Elements h1 = doc.select("h1");
            if (h1.size() > 0) {
                title = h1.first().text();
            }
        }
        recipe.setName(title.isEmpty() ? "Recette importée" : title);
        
        // Essayer de trouver les ingrédients par mots-clés
        Elements ingredientContainers = doc.select("*:contains(ingrédient), *:contains(ingredient), .ingredients, #ingredients");
        List<Ingredient> ingredients = new ArrayList<>();
        
        for (Element container : ingredientContainers) {
            Elements listItems = container.select("li, p");
            for (int i = 0; i < listItems.size(); i++) {
                String text = listItems.get(i).text().trim();
                if (!text.isEmpty() && text.length() > 5) {
                    Ingredient ingredient = parseIngredientText(text);
                    ingredient.setPosition(i + 1);
                    ingredients.add(ingredient);
                }
            }
            if (ingredients.size() > 0) break; // Prendre le premier conteneur trouvé
        }
        recipe.setIngredients(ingredients);
        
        Log.d(TAG, "Recette HTML générique parsée: " + recipe.getName());
        return recipe;
    }
    
    // ===== IMPORT DEPUIS FICHIER =====
    
    /**
     * Importe une recette depuis un fichier JSON
     */
    public void importFromFile(InputStream inputStream, ImportFormat format, ImportCallback callback) {
        executorService.execute(() -> {
            try {
                String jsonContent = readInputStream(inputStream);
                JSONObject json = new JSONObject(jsonContent);
                
                Recipe recipe = null;
                switch (format) {
                    case MEALIE_JSON:
                        recipe = parseMealieJson(json);
                        break;
                    case GENERIC_JSON:
                    case AUTO_DETECT:
                    default:
                        recipe = parseGenericJson(json);
                        break;
                }
                
                if (recipe != null) {
                    callback.onSuccess(recipe);
                } else {
                    callback.onError("Format de fichier non reconnu");
                }
                
            } catch (JSONException e) {
                callback.onError("Erreur de format JSON: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Erreur lors de l'import: " + e.getMessage());
            }
        });
    }
    
    // ===== PARSING JSON MEALIE =====
    
    private Recipe parseMealieJson(JSONObject json) throws JSONException {
        Recipe recipe = new Recipe();
        
        recipe.setName(json.optString("name", "Recette importée"));
        recipe.setDescription(json.optString("description", ""));
        recipe.setCookTime(json.optInt("cookTime", 0));
        recipe.setPrepTime(json.optInt("prepTime", 0));
        recipe.setTotalTime(json.optInt("totalTime", 0));
        recipe.setServings(json.optInt("servings", 4));
        
        // Ingrédients Mealie
        if (json.has("recipeIngredient")) {
            JSONArray ingredients = json.getJSONArray("recipeIngredient");
            List<Ingredient> ingredientList = new ArrayList<>();
            
            for (int i = 0; i < ingredients.length(); i++) {
                JSONObject ingJson = ingredients.getJSONObject(i);
                Ingredient ingredient = new Ingredient();
                ingredient.setName(ingJson.optString("note", ""));
                ingredient.setQuantity(ingJson.optDouble("quantity", 0.0));
                ingredient.setUnit(ingJson.optString("unit", ""));
                ingredient.setPosition(i + 1);
                ingredientList.add(ingredient);
            }
            recipe.setIngredients(ingredientList);
        }
        
        // Instructions Mealie
        if (json.has("recipeInstructions")) {
            JSONArray instructions = json.getJSONArray("recipeInstructions");
            List<Instruction> instructionList = new ArrayList<>();
            
            for (int i = 0; i < instructions.length(); i++) {
                JSONObject instJson = instructions.getJSONObject(i);
                Instruction instruction = new Instruction();
                instruction.setText(instJson.optString("text", ""));
                instruction.setPosition(i + 1);
                instructionList.add(instruction);
            }
            recipe.setInstructions(instructionList);
        }
        
        return recipe;
    }
    
    // ===== PARSING JSON GÉNÉRIQUE =====
    
    private Recipe parseGenericJson(JSONObject json) throws JSONException {
        Recipe recipe = new Recipe();
        
        // Essayer différents noms de champs
        String name = json.optString("name", json.optString("title", "Recette importée"));
        recipe.setName(name);
        
        String description = json.optString("description", json.optString("summary", ""));
        recipe.setDescription(description);
        
        return recipe;
    }
    
    // ===== MÉTHODES UTILITAIRES =====
    
    /**
     * Parse une durée ISO 8601 (PT30M) en minutes
     */
    private int parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        
        try {
            // Format ISO 8601: PT30M, PT1H30M, etc.
            if (duration.startsWith("PT")) {
                duration = duration.substring(2);
                int total = 0;
                
                if (duration.contains("H")) {
                    String[] parts = duration.split("H");
                    total += Integer.parseInt(parts[0]) * 60;
                    duration = parts.length > 1 ? parts[1] : "";
                }
                
                if (duration.contains("M")) {
                    String minutes = duration.replace("M", "");
                    if (!minutes.isEmpty()) {
                        total += Integer.parseInt(minutes);
                    }
                }
                
                return total;
            }
            
            // Format simple en minutes
            return Integer.parseInt(duration.replaceAll("[^0-9]", ""));
            
        } catch (Exception e) {
            Log.w(TAG, "Erreur parsing durée: " + duration);
            return 0;
        }
    }
    
    /**
     * Parse un texte d'ingrédient libre en objet Ingredient
     */
    private Ingredient parseIngredientText(String text) {
        Ingredient ingredient = new Ingredient();
        
        // Parsing simple - améliorer selon les besoins
        String[] parts = text.split(" ", 3);
        
        if (parts.length >= 1) {
            // Essayer de parser la quantité
            try {
                double quantity = Double.parseDouble(parts[0].replaceAll("[^0-9.,]", "").replace(",", "."));
                ingredient.setQuantity(quantity);
                
                if (parts.length >= 2) {
                    ingredient.setUnit(parts[1]);
                    if (parts.length >= 3) {
                        ingredient.setName(parts[2]);
                    }
                } else if (parts.length >= 1) {
                    ingredient.setName(text);
                }
            } catch (NumberFormatException e) {
                // Pas de quantité trouvée, tout est le nom
                ingredient.setName(text);
                ingredient.setQuantity(0.0);
                ingredient.setUnit("");
            }
        }
        
        return ingredient;
    }
    
    /**
     * Import synchrone depuis URL (pour usage interne)
     */
    private void importFromUrlSynchronous(String url, ImportCallback callback) throws Exception {
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; InANutshell Recipe Importer)")
            .timeout(10000)
            .get();
        
        Recipe recipe = parseJsonLd(doc);
        if (recipe == null) {
            recipe = parseMicrodata(doc);
        }
        if (recipe == null) {
            recipe = parseGenericHtml(doc);
        }
        
        if (recipe != null) {
            recipe.setSource(url);
            callback.onSuccess(recipe);
        } else {
            callback.onError("Aucune recette trouvée");
        }
    }
    
    /**
     * Lit un InputStream en String
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.append(new String(buffer, 0, length, "UTF-8"));
        }
        return result.toString();
    }
}
