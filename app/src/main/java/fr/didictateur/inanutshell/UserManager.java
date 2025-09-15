package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager pour la gestion des utilisateurs, authentification et permissions
 */
public class UserManager {
    
    private final UserDao userDao;
    private final GroupDao groupDao;
    private final GroupMembershipDao membershipDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final SharedPreferences preferences;
    
    // Préférences pour la session utilisateur
    private static final String PREF_CURRENT_USER_ID = "current_user_id";
    private static final String PREF_AUTO_LOGIN = "auto_login";
    private static final String PREF_LAST_LOGIN = "last_login";
    
    public UserManager(Context context, AppDatabase database) {
        this.userDao = database.userDao();
        this.groupDao = database.groupDao();
        this.membershipDao = database.groupMembershipDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.preferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        
        // Créer l'utilisateur admin par défaut si nécessaire
        createDefaultAdminIfNeeded();
    }
    
    // ===================== Interfaces de callback =====================
    
    public interface AuthenticationCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    
    public interface UserListCallback {
        void onSuccess(List<User> users);
        void onError(String message);
    }
    
    public interface PermissionCallback {
        void onResult(boolean hasPermission);
    }
    
    // ===================== Authentification =====================
    
    /**
     * Authentifie un utilisateur avec email et mot de passe
     */
    public void authenticateUser(String email, String password, AuthenticationCallback callback) {
        executor.execute(() -> {
            try {
                String passwordHash = hashPassword(password);
                User user = userDao.authenticate(email, passwordHash);
                
                if (user != null) {
                    // Mise à jour de la dernière connexion
                    user.updateLastLogin();
                    userDao.update(user);
                    
                    // Sauvegarde de la session
                    setCurrentUser(user);
                    
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    mainHandler.post(() -> callback.onError("Email ou mot de passe incorrect"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur d'authentification: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Crée un nouveau compte utilisateur
     */
    public void createUser(String username, String email, String password, User.UserRole role, UserCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifications
                if (userDao.emailExists(email)) {
                    mainHandler.post(() -> callback.onError("Cette adresse email existe déjà"));
                    return;
                }
                
                if (userDao.usernameExists(username)) {
                    mainHandler.post(() -> callback.onError("Ce nom d'utilisateur existe déjà"));
                    return;
                }
                
                // Création de l'utilisateur
                User user = new User(username, email, role);
                user.passwordHash = hashPassword(password);
                
                long userId = userDao.insert(user);
                user.userId = (int) userId;
                
                // Création du groupe personnel par défaut
                createPersonalGroup(user);
                
                mainHandler.post(() -> callback.onSuccess(user));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la création: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Déconnexion de l'utilisateur actuel
     */
    public void logout() {
        preferences.edit()
                .remove(PREF_CURRENT_USER_ID)
                .putLong(PREF_LAST_LOGIN, System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Retourne l'utilisateur actuellement connecté
     */
    public int getCurrentUserId() {
        return preferences.getInt(PREF_CURRENT_USER_ID, -1);
    }
    
    /**
     * Vérifie si un utilisateur est connecté
     */
    public boolean isUserLoggedIn() {
        return getCurrentUserId() != -1;
    }
    
    // ===================== Gestion des utilisateurs =====================
    
    /**
     * Met à jour le profil d'un utilisateur
     */
    public void updateUser(User user, UserCallback callback) {
        executor.execute(() -> {
            try {
                userDao.update(user);
                mainHandler.post(() -> callback.onSuccess(user));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Supprime un utilisateur (nécessite permissions admin)
     */
    public void deleteUser(int userId, int requestingUserId, UserCallback callback) {
        executor.execute(() -> {
            try {
                User requestingUser = userDao.getUserById(requestingUserId);
                if (requestingUser == null || !requestingUser.hasPermission("MANAGE_USERS")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                User userToDelete = userDao.getUserById(userId);
                if (userToDelete == null) {
                    mainHandler.post(() -> callback.onError("Utilisateur introuvable"));
                    return;
                }
                
                // Désactiver au lieu de supprimer pour préserver l'intégrité
                userToDelete.isActive = false;
                userDao.update(userToDelete);
                
                mainHandler.post(() -> callback.onSuccess(userToDelete));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la suppression: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Recherche d'utilisateurs
     */
    public void searchUsers(String query, UserListCallback callback) {
        executor.execute(() -> {
            try {
                List<User> users = userDao.searchUsers(query);
                mainHandler.post(() -> callback.onSuccess(users));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la recherche: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des permissions =====================
    
    /**
     * Vérifie si un utilisateur a une permission spécifique
     */
    public void checkPermission(int userId, String permission, PermissionCallback callback) {
        executor.execute(() -> {
            try {
                User user = userDao.getUserById(userId);
                boolean hasPermission = user != null && user.hasPermission(permission);
                mainHandler.post(() -> callback.onResult(hasPermission));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }
    
    /**
     * Met à jour le rôle d'un utilisateur
     */
    public void updateUserRole(int userId, User.UserRole newRole, int requestingUserId, UserCallback callback) {
        executor.execute(() -> {
            try {
                User requestingUser = userDao.getUserById(requestingUserId);
                if (requestingUser == null || !requestingUser.hasPermission("MANAGE_USERS")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                userDao.updateUserRole(userId, newRole);
                User updatedUser = userDao.getUserById(userId);
                
                mainHandler.post(() -> callback.onSuccess(updatedUser));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour du rôle: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des groupes personnels =====================
    
    /**
     * Crée un groupe personnel pour un nouvel utilisateur
     */
    private void createPersonalGroup(User user) {
        try {
            Group personalGroup = new Group(user.username + " - Personnel", user.userId, Group.GroupType.CUSTOM);
            personalGroup.description = "Groupe personnel de " + user.username;
            personalGroup.maxMembers = 1;
            personalGroup.allowPublicJoin = false;
            personalGroup.requireApproval = false;
            
            long groupId = groupDao.insert(personalGroup);
            
            // Ajouter l'utilisateur comme propriétaire
            GroupMembership membership = new GroupMembership(user.userId, (int) groupId, GroupMembership.MembershipRole.OWNER);
            membership.status = GroupMembership.MembershipStatus.ACTIVE;
            membershipDao.insert(membership);
            
            // Mettre à jour l'utilisateur avec son groupe par défaut
            user.defaultGroupId = (int) groupId;
            userDao.update(user);
            
        } catch (Exception e) {
            // Log l'erreur mais ne bloque pas la création d'utilisateur
            e.printStackTrace();
        }
    }
    
    // ===================== Utilitaires privés =====================
    
    /**
     * Hache un mot de passe avec salt
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Ajouter un salt simple (dans une vraie app, utiliser un salt unique par utilisateur)
            String saltedPassword = "inanutshell_" + password + "_salt";
            byte[] hash = md.digest(saltedPassword.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur de hachage du mot de passe", e);
        }
    }
    
    /**
     * Définit l'utilisateur actuel dans les préférences
     */
    private void setCurrentUser(User user) {
        preferences.edit()
                .putInt(PREF_CURRENT_USER_ID, user.userId)
                .putLong(PREF_LAST_LOGIN, System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Crée l'utilisateur administrateur par défaut si nécessaire
     */
    private void createDefaultAdminIfNeeded() {
        executor.execute(() -> {
            try {
                int adminCount = userDao.getAdminCount();
                if (adminCount == 0) {
                    // Créer l'admin par défaut
                    User admin = new User("Admin", "admin@inanutshell.com", User.UserRole.ADMIN);
                    admin.passwordHash = hashPassword("admin123"); // Mot de passe par défaut
                    admin.isActive = true;
                    
                    long userId = userDao.insert(admin);
                    admin.userId = (int) userId;
                    
                    createPersonalGroup(admin);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Génère un mot de passe temporaire
     */
    public String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        executor.shutdown();
    }
}
