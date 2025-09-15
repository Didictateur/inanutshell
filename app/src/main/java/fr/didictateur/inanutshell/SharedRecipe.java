package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

/**
 * Entité SharedRecipe pour gérer le partage de recettes entre utilisateurs et groupes
 */
@Entity(tableName = "shared_recipes",
        foreignKeys = {
            @ForeignKey(entity = User.class,
                       parentColumns = "userId",
                       childColumns = "sharedBy",
                       onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = User.class,
                       parentColumns = "userId",
                       childColumns = "sharedWith",
                       onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Group.class,
                       parentColumns = "groupId",
                       childColumns = "groupId",
                       onDelete = ForeignKey.CASCADE)
        },
        indices = {
            @Index(value = {"recipeId", "sharedBy", "sharedWith"}),
            @Index(value = "sharedBy"),
            @Index(value = "sharedWith"),
            @Index(value = "groupId"),
            @Index(value = "recipeId")
        })
@TypeConverters({Converters.class})
public class SharedRecipe {

    @PrimaryKey(autoGenerate = true)
    public int shareId;

    public String recipeId; // ID de la recette dans l'API Mealie
    public String recipeName; // Nom de la recette pour référence rapide
    public int sharedBy; // ID de l'utilisateur qui partage
    public Integer sharedWith; // ID de l'utilisateur destinataire (null si partage de groupe)
    public Integer groupId; // ID du groupe (null si partage individuel)
    public Date dateShared;
    public ShareType shareType;
    public SharePermission permission;
    public ShareStatus status;
    public String message; // Message accompagnant le partage
    public Date expiryDate; // Date d'expiration du partage (optionnel)
    
    // Métadonnées de collaboration
    public boolean allowComments;
    public boolean allowRating;
    public boolean allowModifications;
    public String collaborationNotes; // Notes de collaboration

    /**
     * Types de partage
     */
    public enum ShareType {
        INDIVIDUAL("Individuel", "Partagé avec un utilisateur specific"),
        GROUP("Groupe", "Partagé avec un groupe d'utilisateurs"),
        PUBLIC("Public", "Visible par tous les utilisateurs"),
        LINK("Lien", "Partage via lien temporaire"),
        FAMILY("Famille", "Partagé avec le groupe familial"),
        COLLABORATIVE("Collaboratif", "Édition collaborative autorisée");

        private final String displayName;
        private final String description;

        ShareType(String displayName, String description) {
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

    /**
     * Permissions de partage
     */
    public enum SharePermission {
        VIEW_ONLY("Lecture seule", "Peut uniquement consulter la recette"),
        COMMENT("Commentaire", "Peut consulter et commenter"),
        RATE("Notation", "Peut consulter, commenter et noter"),
        SUGGEST("Suggestion", "Peut proposer des modifications"),
        EDIT("Édition", "Peut modifier la recette"),
        FULL_ACCESS("Accès complet", "Contrôle total sur la recette partagée");

        private final String displayName;
        private final String description;

        SharePermission(String displayName, String description) {
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
         * Vérifie si cette permission permet de voir la recette
         */
        public boolean canView() {
            return true; // Toutes les permissions permettent la consultation
        }

        /**
         * Vérifie si cette permission permet de commenter
         */
        public boolean canComment() {
            return this != VIEW_ONLY;
        }

        /**
         * Vérifie si cette permission permet de noter
         */
        public boolean canRate() {
            return this == RATE || this == SUGGEST || this == EDIT || this == FULL_ACCESS;
        }

        /**
         * Vérifie si cette permission permet de suggérer des modifications
         */
        public boolean canSuggest() {
            return this == SUGGEST || this == EDIT || this == FULL_ACCESS;
        }

        /**
         * Vérifie si cette permission permet de modifier
         */
        public boolean canEdit() {
            return this == EDIT || this == FULL_ACCESS;
        }

        /**
         * Vérifie si cette permission permet un contrôle complet
         */
        public boolean hasFullAccess() {
            return this == FULL_ACCESS;
        }
    }

    /**
     * Statuts du partage
     */
    public enum ShareStatus {
        PENDING("En attente", "Invitation en attente d'acceptation"),
        ACCEPTED("Accepté", "Partage actif et accessible"),
        DECLINED("Refusé", "Invitation refusée"),
        EXPIRED("Expiré", "Partage arrivé à expiration"),
        REVOKED("Révoqué", "Partage annulé par le propriétaire"),
        SUSPENDED("Suspendu", "Accès temporairement révoqué");

        private final String displayName;
        private final String description;

        ShareStatus(String displayName, String description) {
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
         * Vérifie si ce statut permet l'accès à la recette
         */
        public boolean allowsAccess() {
            return this == ACCEPTED;
        }

        /**
         * Vérifie si ce statut peut être réactivé
         */
        public boolean canBeReactivated() {
            return this == DECLINED || this == SUSPENDED;
        }
    }

    // Constructeurs
    public SharedRecipe() {
        this.dateShared = new Date();
        this.status = ShareStatus.PENDING;
        this.permission = SharePermission.VIEW_ONLY;
        this.allowComments = true;
        this.allowRating = true;
        this.allowModifications = false;
    }

    @androidx.room.Ignore
    public SharedRecipe(String recipeId, String recipeName, int sharedBy, 
                       ShareType shareType, SharePermission permission) {
        this();
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.sharedBy = sharedBy;
        this.shareType = shareType;
        this.permission = permission;
    }

    // Méthodes utilitaires

    /**
     * Accepte le partage
     */
    public void acceptShare() {
        this.status = ShareStatus.ACCEPTED;
    }

    /**
     * Refuse le partage
     */
    public void declineShare() {
        this.status = ShareStatus.DECLINED;
    }

    /**
     * Révoque le partage
     */
    public void revokeShare() {
        this.status = ShareStatus.REVOKED;
    }

    /**
     * Suspend temporairement le partage
     */
    public void suspendShare() {
        this.status = ShareStatus.SUSPENDED;
    }

    /**
     * Vérifie si le partage est encore valide
     */
    public boolean isValid() {
        if (!status.allowsAccess()) {
            return false;
        }
        
        if (expiryDate != null && new Date().after(expiryDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Vérifie si le partage a expiré
     */
    public boolean isExpired() {
        return expiryDate != null && new Date().after(expiryDate);
    }

    /**
     * Définit une date d'expiration
     */
    public void setExpiryDays(int days) {
        long daysInMs = days * 24L * 60L * 60L * 1000L;
        this.expiryDate = new Date(System.currentTimeMillis() + daysInMs);
    }

    /**
     * Vérifie si un utilisateur peut effectuer une action
     */
    public boolean canUserPerformAction(String action) {
        if (!isValid()) {
            return false;
        }

        switch (action) {
            case "VIEW":
                return permission.canView();
            case "COMMENT":
                return permission.canComment() && allowComments;
            case "RATE":
                return permission.canRate() && allowRating;
            case "SUGGEST":
                return permission.canSuggest() && allowModifications;
            case "EDIT":
                return permission.canEdit() && allowModifications;
            case "FULL_ACCESS":
                return permission.hasFullAccess();
            default:
                return false;
        }
    }

    /**
     * Met à jour les permissions de collaboration
     */
    public void updateCollaborationSettings(boolean allowComments, boolean allowRating, boolean allowModifications) {
        this.allowComments = allowComments;
        this.allowRating = allowRating;
        this.allowModifications = allowModifications;
    }

    /**
     * Retourne le type de destinataire
     */
    public String getRecipientType() {
        if (shareType == ShareType.PUBLIC) {
            return "Public";
        } else if (groupId != null) {
            return "Groupe";
        } else if (sharedWith != null) {
            return "Utilisateur";
        } else {
            return "Inconnu";
        }
    }

    /**
     * Calcule le nombre de jours avant expiration
     */
    public int getDaysUntilExpiry() {
        if (expiryDate == null) {
            return -1; // Pas d'expiration
        }
        
        long diffMs = expiryDate.getTime() - System.currentTimeMillis();
        return (int) (diffMs / (24L * 60L * 60L * 1000L));
    }

    /**
     * Retourne une description du partage
     */
    public String getShareDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(shareType.getDisplayName());
        desc.append(" - ").append(permission.getDisplayName());
        
        if (expiryDate != null) {
            int daysLeft = getDaysUntilExpiry();
            if (daysLeft > 0) {
                desc.append(" (expire dans ").append(daysLeft).append(" jours)");
            } else if (daysLeft == 0) {
                desc.append(" (expire aujourd'hui)");
            } else {
                desc.append(" (expiré)");
            }
        }
        
        return desc.toString();
    }

    @Override
    public String toString() {
        return "SharedRecipe{" +
                "shareId=" + shareId +
                ", recipeId='" + recipeId + '\'' +
                ", recipeName='" + recipeName + '\'' +
                ", shareType=" + shareType +
                ", permission=" + permission +
                ", status=" + status +
                ", sharedBy=" + sharedBy +
                ", sharedWith=" + sharedWith +
                ", groupId=" + groupId +
                '}';
    }
}
