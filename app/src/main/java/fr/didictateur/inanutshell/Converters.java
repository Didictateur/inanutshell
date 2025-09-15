package fr.didictateur.inanutshell;

import androidx.room.TypeConverter;
import fr.didictateur.inanutshell.data.meal.MealPlan;
import fr.didictateur.inanutshell.data.shopping.ShoppingList;
import fr.didictateur.inanutshell.data.shopping.ShoppingItem;
import fr.didictateur.inanutshell.config.ServerConfig;

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
    
    // Converters pour ShoppingList.GenerationSource
    @TypeConverter
    public static ShoppingList.GenerationSource fromGenerationSourceString(String value) {
        return value == null ? null : ShoppingList.GenerationSource.valueOf(value);
    }
    
    @TypeConverter
    public static String generationSourceToString(ShoppingList.GenerationSource source) {
        return source == null ? null : source.name();
    }
    
    // Converters pour ShoppingItem.Category
    @TypeConverter
    public static ShoppingItem.Category fromCategoryString(String value) {
        return value == null ? null : ShoppingItem.Category.valueOf(value);
    }
    
    @TypeConverter
    public static String categoryToString(ShoppingItem.Category category) {
        return category == null ? null : category.name();
    }
    
    // Converters pour Timer.TimerState
    @TypeConverter
    public static Timer.TimerState fromTimerStateString(String value) {
        return value == null ? null : Timer.TimerState.valueOf(value);
    }
    
    @TypeConverter
    public static String timerStateToString(Timer.TimerState state) {
        return state == null ? null : state.name();
    }
    
    // Converters pour User.UserRole
    @TypeConverter
    public static User.UserRole fromUserRoleString(String value) {
        return value == null ? null : User.UserRole.valueOf(value);
    }
    
    @TypeConverter
    public static String userRoleToString(User.UserRole role) {
        return role == null ? null : role.name();
    }
    
    // Converters pour Group.GroupType
    @TypeConverter
    public static Group.GroupType fromGroupTypeString(String value) {
        return value == null ? null : Group.GroupType.valueOf(value);
    }
    
    @TypeConverter
    public static String groupTypeToString(Group.GroupType type) {
        return type == null ? null : type.name();
    }
    
    // Converters pour GroupMembership.MembershipRole
    @TypeConverter
    public static GroupMembership.MembershipRole fromMembershipRoleString(String value) {
        return value == null ? null : GroupMembership.MembershipRole.valueOf(value);
    }
    
    @TypeConverter
    public static String membershipRoleToString(GroupMembership.MembershipRole role) {
        return role == null ? null : role.name();
    }
    
    // Converters pour GroupMembership.MembershipStatus
    @TypeConverter
    public static GroupMembership.MembershipStatus fromMembershipStatusString(String value) {
        return value == null ? null : GroupMembership.MembershipStatus.valueOf(value);
    }
    
    @TypeConverter
    public static String membershipStatusToString(GroupMembership.MembershipStatus status) {
        return status == null ? null : status.name();
    }
    
    // Converters pour SharedRecipe.ShareType
    @TypeConverter
    public static SharedRecipe.ShareType fromShareTypeString(String value) {
        return value == null ? null : SharedRecipe.ShareType.valueOf(value);
    }
    
    @TypeConverter
    public static String shareTypeToString(SharedRecipe.ShareType shareType) {
        return shareType == null ? null : shareType.name();
    }
    
    // Converters pour SharedRecipe.SharePermission
    @TypeConverter
    public static SharedRecipe.SharePermission fromSharePermissionString(String value) {
        return value == null ? null : SharedRecipe.SharePermission.valueOf(value);
    }
    
    @TypeConverter
    public static String sharePermissionToString(SharedRecipe.SharePermission permission) {
        return permission == null ? null : permission.name();
    }
    
    // Converters pour SharedRecipe.ShareStatus
    @TypeConverter
    public static SharedRecipe.ShareStatus fromShareStatusString(String value) {
        return value == null ? null : SharedRecipe.ShareStatus.valueOf(value);
    }
    
    @TypeConverter
    public static String shareStatusToString(SharedRecipe.ShareStatus status) {
        return status == null ? null : status.name();
    }
    
    // Converters pour Theme.ThemeType
    @TypeConverter
    public static Theme.ThemeType fromThemeTypeString(String value) {
        return value == null ? null : Theme.ThemeType.valueOf(value);
    }
    
    @TypeConverter
    public static String themeTypeToString(Theme.ThemeType themeType) {
        return themeType == null ? null : themeType.name();
    }
    
    // Converters pour ServerConfig.ServerStatus
    @TypeConverter
    public static ServerConfig.ServerStatus fromServerStatusString(String value) {
        return value == null ? null : ServerConfig.ServerStatus.valueOf(value);
    }
    
    @TypeConverter
    public static String serverStatusToString(ServerConfig.ServerStatus status) {
        return status == null ? null : status.name();
    }
}
