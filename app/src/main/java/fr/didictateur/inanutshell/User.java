package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;
import java.util.List;

/**
 * Entité User pour gérer les profils d'utilisateurs et leur système de permissions
 */
@Entity(tableName = "users")
@TypeConverters({Converters.class})
public class User {

    @PrimaryKey(autoGenerate = true)
    public int userId;

    public String username;
    public String email;
    public String passwordHash; // Pour l'authentification locale
    public String avatarPath; // Chemin vers l'image de profil
    public UserRole role;
    public Date dateCreated;
    public Date lastLogin;
    public boolean isActive;
    
    // Préférences utilisateur
    public String dietaryRestrictions; // JSON des restrictions alimentaires
    public String preferences; // JSON des préférences culinaires
    public int defaultGroupId; // Groupe principal de l'utilisateur
    
    // Paramètres de partage
    public boolean allowNotifications;
    public boolean shareRecipesPublically;
    public boolean acceptCollaborationInvites;

    /**
     * Énumération des rôles utilisateur pour le système de permissions
     */
    public enum UserRole {
        ADMIN("Administrateur", "Contrôle complet de l'application"),
        EDITOR("Éditeur", "Peut créer, modifier et partager des recettes"),
        VIEWER("Lecteur", "Peut uniquement consulter les recettes partagées"),
        GUEST("Invité", "Accès limité aux recettes publiques");

        private final String displayName;
        private final String description;

        UserRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Vérifie si ce rôle peut créer des recettes
         */
        public boolean canCreateRecipes() {
            return this == ADMIN || this == EDITOR;
        }

        /**
         * Vérifie si ce rôle peut modifier des recettes existantes
         */
        public boolean canEditRecipes() {
            return this == ADMIN || this == EDITOR;
        }

        /**
         * Vérifie si ce rôle peut supprimer des recettes
         */
        public boolean canDeleteRecipes() {
            return this == ADMIN || this == EDITOR;
        }

        /**
         * Vérifie si ce rôle peut partager des recettes
         */
        public boolean canShareRecipes() {
            return this == ADMIN || this == EDITOR;
        }

        /**
         * Vérifie si ce rôle peut gérer les utilisateurs
         */
        public boolean canManageUsers() {
            return this == ADMIN;
        }

        /**
         * Vérifie si ce rôle peut créer des groupes
         */
        public boolean canCreateGroups() {
            return this == ADMIN || this == EDITOR;
        }
    }

    // Constructeurs
    public User() {
        this.dateCreated = new Date();
        this.role = UserRole.VIEWER;
        this.isActive = true;
        this.allowNotifications = true;
        this.shareRecipesPublically = false;
        this.acceptCollaborationInvites = true;
    }

    @androidx.room.Ignore
    public User(String username, String email, UserRole role) {
        this();
        this.username = username;
        this.email = email;
        this.role = role;
    }

    // Méthodes utilitaires

    /**
     * Vérifie si l'utilisateur a la permission pour une action donnée
     */
    public boolean hasPermission(String permission) {
        if (!isActive) {
            return false;
        }

        switch (permission) {
            case "CREATE_RECIPE":
                return role.canCreateRecipes();
            case "EDIT_RECIPE":
                return role.canEditRecipes();
            case "DELETE_RECIPE":
                return role.canDeleteRecipes();
            case "SHARE_RECIPE":
                return role.canShareRecipes();
            case "MANAGE_USERS":
                return role.canManageUsers();
            case "CREATE_GROUP":
                return role.canCreateGroups();
            default:
                return false;
        }
    }

    /**
     * Met à jour la date de dernière connexion
     */
    public void updateLastLogin() {
        this.lastLogin = new Date();
    }

    /**
     * Retourne le nom d'affichage avec le rôle
     */
    public String getDisplayNameWithRole() {
        return username + " (" + role.getDisplayName() + ")";
    }

    /**
     * Vérifie si l'utilisateur est un administrateur
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Vérifie si l'utilisateur peut collaborer
     */
    public boolean canCollaborate() {
        return isActive && acceptCollaborationInvites && 
               (role == UserRole.ADMIN || role == UserRole.EDITOR);
    }

    /**
     * Génère un avatar par défaut basé sur les initiales
     */
    public String getInitials() {
        if (username == null || username.trim().isEmpty()) {
            return "??";
        }
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else {
            return username.substring(0, Math.min(2, username.length())).toUpperCase();
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                '}';
    }
}
