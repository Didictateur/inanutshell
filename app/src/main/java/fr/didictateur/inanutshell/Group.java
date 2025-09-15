package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

/**
 * Entité Group pour gérer les groupes familiaux et la collaboration
 */
@Entity(tableName = "groups")
@TypeConverters({Converters.class})
public class Group {

    @PrimaryKey(autoGenerate = true)
    public int groupId;

    public String groupName;
    public String description;
    public int ownerId; // ID de l'utilisateur propriétaire
    public Date dateCreated;
    public boolean isActive;
    public String inviteCode; // Code pour rejoindre le groupe
    public Date inviteCodeExpiry;
    
    // Paramètres du groupe
    public boolean allowPublicJoin;
    public boolean requireApproval;
    public int maxMembers;
    public GroupType groupType;
    
    // Préférences de collaboration
    public boolean shareRecipes;
    public boolean shareMealPlanning;
    public boolean shareShoppingLists;
    public boolean shareTimers;

    /**
     * Types de groupes pour différentes utilisations
     */
    public enum GroupType {
        FAMILY("Famille", "Groupe familial pour partager recettes et plannings"),
        ROOMMATES("Colocataires", "Groupe de colocataires pour organiser les repas"),
        FRIENDS("Amis", "Groupe d'amis pour partager des recettes"),
        COOKING_CLUB("Club culinaire", "Groupe de passionnés de cuisine"),
        PROFESSIONAL("Professionnel", "Équipe professionnelle ou restaurant"),
        CUSTOM("Personnalisé", "Groupe avec paramètres personnalisés");

        private final String displayName;
        private final String description;

        GroupType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Constructeurs
    public Group() {
        this.dateCreated = new Date();
        this.isActive = true;
        this.allowPublicJoin = false;
        this.requireApproval = true;
        this.maxMembers = 10;
        this.groupType = GroupType.FAMILY;
        this.shareRecipes = true;
        this.shareMealPlanning = true;
        this.shareShoppingLists = true;
        this.shareTimers = false;
        this.generateInviteCode();
    }

    @androidx.room.Ignore
    public Group(String groupName, int ownerId, GroupType groupType) {
        this();
        this.groupName = groupName;
        this.ownerId = ownerId;
        this.groupType = groupType;
    }

    // Méthodes utilitaires

    /**
     * Génère un nouveau code d'invitation
     */
    public void generateInviteCode() {
        // Génère un code aléatoire de 8 caractères
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        this.inviteCode = sb.toString();
        
        // Code valide pendant 30 jours
        long thirtyDaysInMs = 30L * 24L * 60L * 60L * 1000L;
        this.inviteCodeExpiry = new Date(System.currentTimeMillis() + thirtyDaysInMs);
    }

    /**
     * Vérifie si le code d'invitation est encore valide
     */
    public boolean isInviteCodeValid() {
        return inviteCode != null && 
               inviteCodeExpiry != null && 
               new Date().before(inviteCodeExpiry);
    }

    /**
     * Vérifie si le groupe peut accepter de nouveaux membres
     */
    public boolean canAcceptNewMembers(int currentMemberCount) {
        return isActive && 
               currentMemberCount < maxMembers;
    }

    /**
     * Retourne la configuration par défaut selon le type de groupe
     */
    public void applyDefaultSettings() {
        switch (groupType) {
            case FAMILY:
                this.maxMembers = 8;
                this.allowPublicJoin = false;
                this.requireApproval = false;
                this.shareRecipes = true;
                this.shareMealPlanning = true;
                this.shareShoppingLists = true;
                this.shareTimers = true;
                break;
                
            case ROOMMATES:
                this.maxMembers = 6;
                this.allowPublicJoin = false;
                this.requireApproval = true;
                this.shareRecipes = true;
                this.shareMealPlanning = true;
                this.shareShoppingLists = true;
                this.shareTimers = false;
                break;
                
            case FRIENDS:
                this.maxMembers = 15;
                this.allowPublicJoin = true;
                this.requireApproval = false;
                this.shareRecipes = true;
                this.shareMealPlanning = false;
                this.shareShoppingLists = false;
                this.shareTimers = false;
                break;
                
            case COOKING_CLUB:
                this.maxMembers = 25;
                this.allowPublicJoin = true;
                this.requireApproval = true;
                this.shareRecipes = true;
                this.shareMealPlanning = false;
                this.shareShoppingLists = false;
                this.shareTimers = false;
                break;
                
            case PROFESSIONAL:
                this.maxMembers = 50;
                this.allowPublicJoin = false;
                this.requireApproval = true;
                this.shareRecipes = true;
                this.shareMealPlanning = true;
                this.shareShoppingLists = true;
                this.shareTimers = true;
                break;
                
            case CUSTOM:
                // Garde les paramètres actuels
                break;
        }
    }

    /**
     * Formate le nom d'affichage du groupe avec le type
     */
    public String getDisplayName() {
        return groupName + " (" + groupType.getDisplayName() + ")";
    }

    /**
     * Retourne les fonctionnalités activées sous forme de texte
     */
    public String getEnabledFeatures() {
        StringBuilder features = new StringBuilder();
        if (shareRecipes) features.append("Recettes, ");
        if (shareMealPlanning) features.append("Planning, ");
        if (shareShoppingLists) features.append("Courses, ");
        if (shareTimers) features.append("Minuteries, ");
        
        String result = features.toString();
        return result.isEmpty() ? "Aucune" : result.substring(0, result.length() - 2);
    }

    /**
     * Vérifie si une fonctionnalité est partagée
     */
    public boolean isFeatureShared(String feature) {
        switch (feature.toLowerCase()) {
            case "recipes":
                return shareRecipes;
            case "meal_planning":
                return shareMealPlanning;
            case "shopping_lists":
                return shareShoppingLists;
            case "timers":
                return shareTimers;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", ownerId=" + ownerId +
                ", groupType=" + groupType +
                ", isActive=" + isActive +
                ", memberLimit=" + maxMembers +
                '}';
    }
}
