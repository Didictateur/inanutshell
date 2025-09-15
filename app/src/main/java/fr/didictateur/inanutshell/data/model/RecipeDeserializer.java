package fr.didictateur.inanutshell.data.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import android.util.Log;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipeDeserializer implements JsonDeserializer<Recipe> {
    private static final String TAG = "RecipeDeserializer";
    
    @Override
    public Recipe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // Log all available keys for debugging
        Set<String> keys = jsonObject.keySet();
        Log.d(TAG, "Available JSON keys: " + keys.toString());
        
        // Look for time-related fields
        for (String key : keys) {
            if (key.toLowerCase().contains("time") || key.toLowerCase().contains("duration")) {
                JsonElement value = jsonObject.get(key);
                String valueStr = value.isJsonNull() ? "null" : value.getAsString();
                Log.d(TAG, "Time-related field found - " + key + ": " + valueStr);
            }
        }
        
        Recipe recipe = new Recipe();
        
        // Basic fields
        if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
            recipe.setId(jsonObject.get("id").getAsString());
        }
        
        if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull()) {
            recipe.setName(jsonObject.get("name").getAsString());
        }
        
        if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
            recipe.setDescription(jsonObject.get("description").getAsString());
        }
        
        if (jsonObject.has("image") && !jsonObject.get("image").isJsonNull()) {
            recipe.setImage(jsonObject.get("image").getAsString());
        }
        
        // Try different possible time field names
        String[] timeFieldNames = {
            "totalTime", "total_time", "total-time",
            "prepTime", "prep_time", "prep-time", "preparationTime", "preparation_time",
            "cookTime", "cook_time", "cook-time", "cookingTime", "cooking_time",
            "performTime", "perform_time", "perform-time",
            "duration", "time"
        };
        
        for (String fieldName : timeFieldNames) {
            if (jsonObject.has(fieldName) && !jsonObject.get(fieldName).isJsonNull()) {
                String value = jsonObject.get(fieldName).getAsString();
                Log.d(TAG, "Found time field: " + fieldName + " = " + value);
                
                // Map to our recipe fields based on field name
                if (fieldName.contains("total")) {
                    recipe.setTotalTime(value);
                } else if (fieldName.contains("prep")) {
                    recipe.setPrepTime(value);
                } else if (fieldName.contains("cook")) {
                    recipe.setCookTime(value);
                } else if (fieldName.contains("perform")) {
                    recipe.setPerformTime(value);
                }
            }
        }
        
        // Recipe yield
        if (jsonObject.has("recipeYield") && !jsonObject.get("recipeYield").isJsonNull()) {
            recipe.setRecipeYield(jsonObject.get("recipeYield").getAsString());
        }
        
        // Handle ingredients - try different approaches for compatibility
        if (jsonObject.has("recipeIngredient")) {
            try {
                JsonElement ingredientsElement = jsonObject.get("recipeIngredient");
                if (ingredientsElement.isJsonArray()) {
                    JsonArray ingredientsArray = ingredientsElement.getAsJsonArray();
                    List<RecipeIngredient> ingredients = new java.util.ArrayList<>();
                    
                    for (JsonElement element : ingredientsArray) {
                        if (element.isJsonObject()) {
                            // Complex ingredient object
                            RecipeIngredient ingredient = context.deserialize(element, RecipeIngredient.class);
                            ingredients.add(ingredient);
                        } else if (element.isJsonPrimitive()) {
                            // Simple string ingredient
                            RecipeIngredient ingredient = new RecipeIngredient();
                            ingredient.setNote(element.getAsString());
                            ingredients.add(ingredient);
                        }
                    }
                    recipe.setRecipeIngredient(ingredients);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing ingredients: " + e.getMessage());
            }
        }
        
        // Handle instructions
        if (jsonObject.has("recipeInstructions")) {
            try {
                JsonElement instructionsElement = jsonObject.get("recipeInstructions");
                if (instructionsElement.isJsonArray()) {
                    JsonArray instructionsArray = instructionsElement.getAsJsonArray();
                    List<RecipeInstruction> instructions = new java.util.ArrayList<>();
                    
                    for (JsonElement element : instructionsArray) {
                        if (element.isJsonObject()) {
                            RecipeInstruction instruction = context.deserialize(element, RecipeInstruction.class);
                            instructions.add(instruction);
                        } else if (element.isJsonPrimitive()) {
                            RecipeInstruction instruction = new RecipeInstruction();
                            instruction.setText(element.getAsString());
                            instructions.add(instruction);
                        }
                    }
                    recipe.setRecipeInstructions(instructions);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing instructions: " + e.getMessage());
            }
        }
        
        // Other fields
        String[] otherFields = {"dateAdded", "dateUpdated", "createdAt", "updatedAt"};
        for (String field : otherFields) {
            if (jsonObject.has(field) && !jsonObject.get(field).isJsonNull()) {
                String value = jsonObject.get(field).getAsString();
                switch (field) {
                    case "dateAdded": recipe.setDateAdded(value); break;
                    case "dateUpdated": recipe.setDateUpdated(value); break;
                    case "createdAt": recipe.setCreatedAt(value); break;
                    case "updatedAt": recipe.setUpdatedAt(value); break;
                }
            }
        }
        
        return recipe;
    }
}
