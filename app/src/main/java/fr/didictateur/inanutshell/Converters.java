package fr.didictateur.inanutshell;

import androidx.room.TypeConverter;
import fr.didictateur.inanutshell.data.meal.MealPlan;

import java.util.Date;

/**
 * Convertisseurs de types pour Room Database
 */
public class Converters {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    @TypeConverter
    public static MealPlan.MealType fromMealTypeString(String value) {
        return value == null ? null : MealPlan.MealType.valueOf(value);
    }
    
    @TypeConverter
    public static String mealTypeToString(MealPlan.MealType mealType) {
        return mealType == null ? null : mealType.name();
    }
}
