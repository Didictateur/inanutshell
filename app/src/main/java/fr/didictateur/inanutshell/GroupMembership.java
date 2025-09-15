package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

/**
 * Entité GroupMembership pour gérer l'appartenance des utilisateurs aux groupes
 */
@Entity(tableName = "group_memberships",
        foreignKeys = {
            @ForeignKey(entity = User.class,
                       parentColumns = "userId",
                       childColumns = "userId",
                       onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Group.class,
                       parentColumns = "groupId",
                       childColumns = "groupId",
                       onDelete = ForeignKey.CASCADE)
        },
        indices = {
            @Index(value = {"userId", "groupId"}, unique = true),
            @Index(value = "userId"),
            @Index(value = "groupId")
        })
@TypeConverters({Converters.class})
public class GroupMembership {

    @PrimaryKey(autoGenerate = true)
    public int membershipId;

    public int userId;
    public int groupId;
    public Date dateJoined;
    public MembershipRole role;
    public MembershipStatus status;
    public Date dateInvited;
    public int invitedBy; // ID de l'utilisateur qui a envoyé l'invitation
    public String inviteMessage;

    /**
     * Rôles au sein d'un groupe
     */
    public enum MembershipRole {
        OWNER("Propriétaire", "Contrôle complet du groupe"),
        ADMIN("Administrateur", "Peut gérer les membres et paramètres"),
        MODERATOR("Modérateur", "Peut modérer le contenu partagé"),
        MEMBER("Membre", "Participation normale au groupe"),
        VIEWER("Observateur", "Accès en lecture seule");

        private final String displayName;
        private final String description;

        MembershipRole(String displayName, String description) {
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
         * Vérifie si ce rôle peut inviter d'autres membres
         */
        public boolean canInviteMembers() {
            return this == OWNER || this == ADMIN;
        }

        /**
         * Vérifie si ce rôle peut modifier les paramètres du groupe
         */
        public boolean canModifyGroupSettings() {
            return this == OWNER || this == ADMIN;
        }

        /**
         * Vérifie si ce rôle peut supprimer des membres
         */
        public boolean canRemoveMembers() {
            return this == OWNER || this == ADMIN;
        }

        /**
         * Vérifie si ce rôle peut modérer le contenu
         */
        public boolean canModerateContent() {
            return this == OWNER || this == ADMIN || this == MODERATOR;
        }

        /**
         * Vérifie si ce rôle peut partager du contenu
         */
        public boolean canShareContent() {
            return this != VIEWER;
        }
    }

    /**
     * Statuts d'appartenance au groupe
     */
    public enum MembershipStatus {
        PENDING("En attente", "Invitation en attente d'acceptation"),
        ACTIVE("Actif", "Membre actif du groupe"),
        SUSPENDED("Suspendu", "Accès temporairement révoqué"),
        LEFT("Parti", "A quitté le groupe"),
        REMOVED("Exclu", "A été retiré du groupe");

        private final String displayName;
        private final String description;

        MembershipStatus(String displayName, String description) {
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
         * Vérifie si ce statut permet l'accès au groupe
         */
        public boolean allowsAccess() {
            return this == ACTIVE;
        }

        /**
         * Vérifie si ce statut peut être reactivé
         */
        public boolean canBeReactivated() {
            return this == SUSPENDED || this == LEFT;
        }
    }

    // Constructeurs
    public GroupMembership() {
        this.dateJoined = new Date();
        this.role = MembershipRole.MEMBER;
        this.status = MembershipStatus.PENDING;
        this.dateInvited = new Date();
    }

    @androidx.room.Ignore
    public GroupMembership(int userId, int groupId, MembershipRole role) {
        this();
        this.userId = userId;
        this.groupId = groupId;
        this.role = role;
        
        // Le propriétaire est automatiquement actif
        if (role == MembershipRole.OWNER) {
            this.status = MembershipStatus.ACTIVE;
        }
    }

    @androidx.room.Ignore
    public GroupMembership(int userId, int groupId, int invitedBy, String inviteMessage) {
        this();
        this.userId = userId;
        this.groupId = groupId;
        this.invitedBy = invitedBy;
        this.inviteMessage = inviteMessage;
    }

    // Méthodes utilitaires

    /**
     * Accepte l'invitation et active l'appartenance
     */
    public void acceptInvitation() {
        this.status = MembershipStatus.ACTIVE;
        this.dateJoined = new Date();
    }

    /**
     * Refuse l'invitation
     */
    public void declineInvitation() {
        this.status = MembershipStatus.LEFT;
    }

    /**
     * Quitte le groupe
     */
    public void leaveGroup() {
        this.status = MembershipStatus.LEFT;
    }

    /**
     * Suspend l'appartenance
     */
    public void suspend() {
        this.status = MembershipStatus.SUSPENDED;
    }

    /**
     * Réactive l'appartenance
     */
    public void reactivate() {
        if (canBeReactivated()) {
            this.status = MembershipStatus.ACTIVE;
            this.dateJoined = new Date();
        }
    }

    /**
     * Vérifie si l'appartenance peut être réactivée
     */
    public boolean canBeReactivated() {
        return status.canBeReactivated();
    }

    /**
     * Vérifie si le membre a accès au groupe
     */
    public boolean hasAccess() {
        return status.allowsAccess();
    }

    /**
     * Vérifie si le membre peut effectuer une action donnée
     */
    public boolean canPerformAction(String action) {
        if (!hasAccess()) {
            return false;
        }

        switch (action) {
            case "INVITE_MEMBERS":
                return role.canInviteMembers();
            case "MODIFY_SETTINGS":
                return role.canModifyGroupSettings();
            case "REMOVE_MEMBERS":
                return role.canRemoveMembers();
            case "MODERATE_CONTENT":
                return role.canModerateContent();
            case "SHARE_CONTENT":
                return role.canShareContent();
            default:
                return false;
        }
    }

    /**
     * Vérifie si ce membre peut gérer un autre membre
     */
    public boolean canManageMember(GroupMembership otherMember) {
        if (!hasAccess() || !canPerformAction("REMOVE_MEMBERS")) {
            return false;
        }

        // Le propriétaire peut gérer tout le monde
        if (this.role == MembershipRole.OWNER) {
            return true;
        }

        // Les admins peuvent gérer les membres et observateurs, mais pas d'autres admins
        if (this.role == MembershipRole.ADMIN) {
            return otherMember.role == MembershipRole.MEMBER || 
                   otherMember.role == MembershipRole.VIEWER ||
                   otherMember.role == MembershipRole.MODERATOR;
        }

        return false;
    }

    /**
     * Retourne le nom d'affichage complet
     */
    public String getDisplayInfo() {
        return role.getDisplayName() + " - " + status.getDisplayName();
    }

    /**
     * Calcule la durée d'appartenance en jours
     */
    public long getMembershipDurationDays() {
        if (dateJoined == null) {
            return 0;
        }
        long diffMs = new Date().getTime() - dateJoined.getTime();
        return diffMs / (1000L * 60L * 60L * 24L);
    }

    @Override
    public String toString() {
        return "GroupMembership{" +
                "membershipId=" + membershipId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", role=" + role +
                ", status=" + status +
                ", dateJoined=" + dateJoined +
                '}';
    }
}
