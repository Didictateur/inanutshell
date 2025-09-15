package fr.didictateur.inanutshell.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import fr.didictateur.inanutshell.data.cache.CachedRecipe;
import fr.didictateur.inanutshell.data.cache.CachedRecipeDao;
import fr.didictateur.inanutshell.data.meal.MealPlan;
import fr.didictateur.inanutshell.data.meal.MealPlanDao;
import fr.didictateur.inanutshell.data.shopping.ShoppingItem;
import fr.didictateur.inanutshell.data.shopping.ShoppingList;
import fr.didictateur.inanutshell.data.shopping.ShoppingItemDao;
import fr.didictateur.inanutshell.data.shopping.ShoppingListDao;
import fr.didictateur.inanutshell.Converters;

/**
 * Base de données Room principale pour l'application
 */

@Database(
    entities = {CachedRecipe.class, MealPlan.class, ShoppingList.class, ShoppingItem.class}, 
    version = 3,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "inanutshell_app.db";
    private static AppDatabase INSTANCE;
    
    // DAOs existants
    public abstract CachedRecipeDao cachedRecipeDao();
    
    // Nouveau DAO pour les repas planifiés
    public abstract MealPlanDao mealPlanDao();
    
    // DAOs pour les listes de courses
    public abstract ShoppingListDao shoppingListDao();
    public abstract ShoppingItemDao shoppingItemDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // Pour la migration v1->v3
            .allowMainThreadQueries() // À utiliser avec parcimonie, idéalement async
            .build();
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
