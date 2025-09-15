package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * DAO pour les opérations sur les utilisateurs avec gestion des permissions
 */
@Dao
public interface UserDao {

    // ===================== Opérations CRUD de base =====================

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<User> getUserByIdLive(int userId);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllUsersLive();

    // ===================== Authentification =====================

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash AND isActive = 1")
    User authenticate(String email, String passwordHash);

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash AND isActive = 1")
    User authenticateByUsername(String username, String passwordHash);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    boolean emailExists(String email);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE username = :username")
    boolean usernameExists(String username);

    // ===================== Gestion des rôles et permissions =====================

    @Query("SELECT * FROM users WHERE role = :role AND isActive = 1")
    List<User> getUsersByRole(User.UserRole role);

    @Query("SELECT * FROM users WHERE role = :role AND isActive = 1")
    LiveData<List<User>> getUsersByRoleLive(User.UserRole role);

    @Query("SELECT * FROM users WHERE role IN ('ADMIN', 'EDITOR') AND isActive = 1")
    List<User> getActiveCollaborators();

    @Query("SELECT * FROM users WHERE role IN ('ADMIN', 'EDITOR') AND isActive = 1")
    LiveData<List<User>> getActiveCollaboratorsLive();

    @Query("UPDATE users SET role = :role WHERE userId = :userId")
    void updateUserRole(int userId, User.UserRole role);

    // ===================== Gestion de l'état des utilisateurs =====================

    @Query("SELECT * FROM users WHERE isActive = :isActive")
    List<User> getUsersByActiveStatus(boolean isActive);

    @Query("SELECT * FROM users WHERE isActive = :isActive")
    LiveData<List<User>> getUsersByActiveStatusLive(boolean isActive);

    @Query("UPDATE users SET isActive = :isActive WHERE userId = :userId")
    void setUserActiveStatus(int userId, boolean isActive);

    @Query("UPDATE users SET lastLogin = :timestamp WHERE userId = :userId")
    void updateLastLogin(int userId, long timestamp);

    // ===================== Gestion des groupes =====================

    @Query("SELECT * FROM users WHERE defaultGroupId = :groupId AND isActive = 1")
    List<User> getUsersByGroup(int groupId);

    @Query("SELECT * FROM users WHERE defaultGroupId = :groupId AND isActive = 1")
    LiveData<List<User>> getUsersByGroupLive(int groupId);

    @Query("UPDATE users SET defaultGroupId = :groupId WHERE userId = :userId")
    void updateUserGroup(int userId, int groupId);

    // ===================== Préférences et paramètres =====================

    @Query("UPDATE users SET allowNotifications = :allow WHERE userId = :userId")
    void updateNotificationPreference(int userId, boolean allow);

    @Query("UPDATE users SET shareRecipesPublically = :share WHERE userId = :userId")
    void updatePublicSharingPreference(int userId, boolean share);

    @Query("UPDATE users SET acceptCollaborationInvites = :accept WHERE userId = :userId")
    void updateCollaborationPreference(int userId, boolean accept);

    @Query("UPDATE users SET dietaryRestrictions = :restrictions WHERE userId = :userId")
    void updateDietaryRestrictions(int userId, String restrictions);

    @Query("UPDATE users SET preferences = :preferences WHERE userId = :userId")
    void updateUserPreferences(int userId, String preferences);

    @Query("UPDATE users SET avatarPath = :avatarPath WHERE userId = :userId")
    void updateUserAvatar(int userId, String avatarPath);

    // ===================== Recherche et filtrage =====================

    @Query("SELECT * FROM users WHERE " +
           "(username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%') " +
           "AND isActive = 1")
    List<User> searchUsers(String query);

    @Query("SELECT * FROM users WHERE " +
           "(username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%') " +
           "AND isActive = 1")
    LiveData<List<User>> searchUsersLive(String query);

    @Query("SELECT * FROM users WHERE " +
           "acceptCollaborationInvites = 1 AND isActive = 1 " +
           "AND role IN ('ADMIN', 'EDITOR') " +
           "AND userId != :currentUserId")
    List<User> getAvailableCollaborators(int currentUserId);

    @Query("SELECT * FROM users WHERE " +
           "shareRecipesPublically = 1 AND isActive = 1 " +
           "AND role IN ('ADMIN', 'EDITOR')")
    List<User> getPublicSharers();

    // ===================== Statistiques =====================

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    int getActiveUserCount();

    @Query("SELECT COUNT(*) FROM users WHERE role = :role AND isActive = 1")
    int getUserCountByRole(User.UserRole role);

    @Query("SELECT COUNT(*) FROM users WHERE defaultGroupId = :groupId AND isActive = 1")
    int getUserCountInGroup(int groupId);

    @Query("SELECT COUNT(*) FROM users WHERE lastLogin > :timestamp AND isActive = 1")
    int getActiveUsersSince(long timestamp);

    // ===================== Nettoyage et maintenance =====================

    @Query("DELETE FROM users WHERE isActive = 0 AND userId NOT IN " +
           "(SELECT DISTINCT sharedBy FROM shared_recipes WHERE sharedBy IS NOT NULL)")
    int cleanupInactiveUsers();

    @Query("SELECT * FROM users WHERE lastLogin < :cutoffDate AND isActive = 1")
    List<User> getInactiveUsersSince(long cutoffDate);

    // ===================== Utilisateur par défaut =====================

    @Query("SELECT * FROM users WHERE role = 'ADMIN' AND isActive = 1 LIMIT 1")
    User getDefaultAdminUser();

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND isActive = 1")
    int getAdminCount();
}
