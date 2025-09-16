package fr.didictateur.inanutshell.data.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.didictateur.inanutshell.data.model.Ingredient;
import fr.didictateur.inanutshell.data.model.Instruction;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.RecipeInstruction;
import fr.didictateur.inanutshell.data.model.Nutrition;
import fr.didictateur.inanutshell.data.model.Tag;
import fr.didictateur.inanutshell.data.model.Category;
import fr.didictateur.inanutshell.data.model.Tool;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class DatabaseConverters {
    private static Gson gson = new Gson();
    
    // Date converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    // List<String> converters
    @TypeConverter
    public static List<String> fromStringListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String stringListToJson(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<Ingredient> converters
    @TypeConverter
    public static List<Ingredient> fromIngredientListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Ingredient>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String ingredientListToJson(List<Ingredient> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<Instruction> converters
    @TypeConverter
    public static List<Instruction> fromInstructionListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Instruction>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String instructionListToJson(List<Instruction> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<RecipeIngredient> converters
    @TypeConverter
    public static List<RecipeIngredient> fromRecipeIngredientListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<RecipeIngredient>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String recipeIngredientListToJson(List<RecipeIngredient> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<RecipeInstruction> converters
    @TypeConverter
    public static List<RecipeInstruction> fromRecipeInstructionListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<RecipeInstruction>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String recipeInstructionListToJson(List<RecipeInstruction> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // Nutrition converter
    @TypeConverter
    public static Nutrition fromNutritionJson(String value) {
        if (value == null) return null;
        return gson.fromJson(value, Nutrition.class);
    }
    
    @TypeConverter
    public static String nutritionToJson(Nutrition nutrition) {
        if (nutrition == null) return null;
        return gson.toJson(nutrition);
    }
    
    // List<Tag> converters
    @TypeConverter
    public static List<Tag> fromTagListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Tag>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String tagListToJson(List<Tag> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<Category> converters
    @TypeConverter
    public static List<Category> fromCategoryListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Category>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String categoryListToJson(List<Category> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
    
    // List<Tool> converters
    @TypeConverter
    public static List<Tool> fromToolListJson(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<Tool>>(){}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String toolListToJson(List<Tool> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
}
