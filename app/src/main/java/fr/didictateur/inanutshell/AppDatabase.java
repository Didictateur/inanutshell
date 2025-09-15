package fr.didictateur.inanutshell;

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
import fr.didictateur.inanutshell.config.ServerConfig;
import fr.didictateur.inanutshell.database.dao.ServerConfigDao;

/**
 * Base de données Room principale pour l'application
 */
@Database(
    entities = {
        // Entités existantes
        CachedRecipe.class, 
        MealPlan.class, 
        ShoppingList.class, 
        ShoppingItem.class, 
        Timer.class,
        // Nouvelles entités multi-utilisateurs
        User.class,
        Group.class,
        GroupMembership.class,
        SharedRecipe.class,
        // Entité de personnalisation
        Theme.class,
        // Configuration serveurs
        ServerConfig.class
    }, 
    version = 7, // Incrémenté pour ServerConfig
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "inanutshell_app.db";
    private static AppDatabase INSTANCE;
    
    // DAOs existants
    public abstract CachedRecipeDao cachedRecipeDao();
    public abstract MealPlanDao mealPlanDao();
    public abstract ShoppingListDao shoppingListDao();
    public abstract ShoppingItemDao shoppingItemDao();
    public abstract TimerDao timerDao();
    
    // Nouveaux DAOs pour multi-utilisateurs
    public abstract UserDao userDao();
    public abstract GroupDao groupDao();
    public abstract GroupMembershipDao groupMembershipDao();
    public abstract SharedRecipeDao sharedRecipeDao();
    
    // DAO pour la personnalisation
    public abstract ThemeDao themeDao();
    
    // DAO pour la configuration serveurs
    public abstract ServerConfigDao serverConfigDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // Pour la migration automatique
            .allowMainThreadQueries() // À utiliser avec parcimonie
            .build();
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
}