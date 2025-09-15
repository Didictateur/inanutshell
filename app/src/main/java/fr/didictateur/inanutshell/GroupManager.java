package fr.didictateur.inanutshell;

import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager pour la gestion des groupes, invitations et collaborations
 */
public class GroupManager {
    
    private final GroupDao groupDao;
    private final GroupMembershipDao membershipDao;
    private final UserDao userDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public GroupManager(AppDatabase database) {
        this.groupDao = database.groupDao();
        this.membershipDao = database.groupMembershipDao();
        this.userDao = database.userDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // ===================== Interfaces de callback =====================
    
    public interface GroupCallback {
        void onSuccess(Group group);
        void onError(String message);
    }
    
    public interface GroupListCallback {
        void onSuccess(List<Group> groups);
        void onError(String message);
    }
    
    public interface MembershipCallback {
        void onSuccess(GroupMembership membership);
        void onError(String message);
    }
    
    public interface MembershipListCallback {
        void onSuccess(List<GroupMembership> memberships);
        void onError(String message);
    }
    
    public interface BooleanCallback {
        void onResult(boolean result);
    }
    
    // ===================== Gestion des groupes =====================
    
    /**
     * Crée un nouveau groupe
     */
    public void createGroup(String groupName, String description, Group.GroupType groupType, int ownerId, GroupCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifier les permissions
                User owner = userDao.getUserById(ownerId);
                if (owner == null || !owner.hasPermission("CREATE_GROUP")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes pour créer un groupe"));
                    return;
                }
                
                // Vérifier si le nom existe déjà pour ce propriétaire
                if (groupDao.groupNameExistsForOwner(groupName, ownerId)) {
                    mainHandler.post(() -> callback.onError("Vous avez déjà un groupe avec ce nom"));
                    return;
                }
                
                // Créer le groupe
                Group group = new Group(groupName, ownerId, groupType);
                group.description = description;
                group.applyDefaultSettings();
                
                long groupId = groupDao.insert(group);
                group.groupId = (int) groupId;
                
                // Ajouter le propriétaire comme membre
                GroupMembership ownerMembership = new GroupMembership(ownerId, group.groupId, GroupMembership.MembershipRole.OWNER);
                ownerMembership.status = GroupMembership.MembershipStatus.ACTIVE;
                membershipDao.insert(ownerMembership);
                
                mainHandler.post(() -> callback.onSuccess(group));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la création du groupe: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Met à jour un groupe
     */
    public void updateGroup(Group group, int requestingUserId, GroupCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifier les permissions
                if (!canUserModifyGroup(requestingUserId, group.groupId)) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                groupDao.update(group);
                mainHandler.post(() -> callback.onSuccess(group));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Supprime un groupe
     */
    public void deleteGroup(int groupId, int requestingUserId, GroupCallback callback) {
        executor.execute(() -> {
            try {
                Group group = groupDao.getGroupById(groupId);
                if (group == null) {
                    mainHandler.post(() -> callback.onError("Groupe introuvable"));
                    return;
                }
                
                // Seul le propriétaire peut supprimer le groupe
                if (group.ownerId != requestingUserId) {
                    mainHandler.post(() -> callback.onError("Seul le propriétaire peut supprimer le groupe"));
                    return;
                }
                
                // Désactiver au lieu de supprimer
                group.isActive = false;
                groupDao.update(group);
                
                mainHandler.post(() -> callback.onSuccess(group));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la suppression: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des invitations =====================
    
    /**
     * Invite un utilisateur à rejoindre un groupe
     */
    public void inviteUser(int groupId, int userId, int inviterId, String message, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifications préliminaires
                Group group = groupDao.getGroupById(groupId);
                User user = userDao.getUserById(userId);
                User inviter = userDao.getUserById(inviterId);
                
                if (group == null || user == null || inviter == null) {
                    mainHandler.post(() -> callback.onError("Groupe ou utilisateur introuvable"));
                    return;
                }
                
                // Vérifier les permissions de l'inviteur
                GroupMembership inviterMembership = membershipDao.getMembership(inviterId, groupId);
                if (inviterMembership == null || !inviterMembership.canPerformAction("INVITE_MEMBERS")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes pour inviter des membres"));
                    return;
                }
                
                // Vérifier que l'utilisateur n'est pas déjà membre
                if (membershipDao.membershipExists(userId, groupId)) {
                    mainHandler.post(() -> callback.onError("Cet utilisateur fait déjà partie du groupe"));
                    return;
                }
                
                // Vérifier la limite de membres
                int currentMemberCount = membershipDao.getActiveGroupMemberCount(groupId);
                if (!group.canAcceptNewMembers(currentMemberCount)) {
                    mainHandler.post(() -> callback.onError("Le groupe a atteint sa limite de membres"));
                    return;
                }
                
                // Créer l'invitation
                GroupMembership invitation = new GroupMembership(userId, groupId, inviterId, message);
                long membershipId = membershipDao.insert(invitation);
                invitation.membershipId = (int) membershipId;
                
                mainHandler.post(() -> callback.onSuccess(invitation));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'invitation: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Accepte une invitation à rejoindre un groupe
     */
    public void acceptInvitation(int membershipId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                GroupMembership membership = membershipDao.getMembershipById(membershipId);
                if (membership == null) {
                    mainHandler.post(() -> callback.onError("Invitation introuvable"));
                    return;
                }
                
                if (membership.status != GroupMembership.MembershipStatus.PENDING) {
                    mainHandler.post(() -> callback.onError("Cette invitation n'est plus valide"));
                    return;
                }
                
                // Vérifier encore la limite de membres
                Group group = groupDao.getGroupById(membership.groupId);
                int currentMemberCount = membershipDao.getActiveGroupMemberCount(membership.groupId);
                if (!group.canAcceptNewMembers(currentMemberCount)) {
                    mainHandler.post(() -> callback.onError("Le groupe a atteint sa limite de membres"));
                    return;
                }
                
                // Accepter l'invitation
                membership.acceptInvitation();
                membershipDao.update(membership);
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'acceptation: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Refuse une invitation
     */
    public void declineInvitation(int membershipId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                GroupMembership membership = membershipDao.getMembershipById(membershipId);
                if (membership == null) {
                    mainHandler.post(() -> callback.onError("Invitation introuvable"));
                    return;
                }
                
                membership.declineInvitation();
                membershipDao.update(membership);
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du refus: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Rejoindre un groupe avec un code d'invitation
     */
    public void joinGroupWithCode(String inviteCode, int userId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                Group group = groupDao.getGroupByInviteCode(inviteCode);
                if (group == null || !group.isInviteCodeValid()) {
                    mainHandler.post(() -> callback.onError("Code d'invitation invalide ou expiré"));
                    return;
                }
                
                // Vérifier que l'utilisateur n'est pas déjà membre
                if (membershipDao.membershipExists(userId, group.groupId)) {
                    mainHandler.post(() -> callback.onError("Vous faites déjà partie de ce groupe"));
                    return;
                }
                
                // Vérifier la limite de membres
                int currentMemberCount = membershipDao.getActiveGroupMemberCount(group.groupId);
                if (!group.canAcceptNewMembers(currentMemberCount)) {
                    mainHandler.post(() -> callback.onError("Le groupe a atteint sa limite de membres"));
                    return;
                }
                
                // Créer l'appartenance
                GroupMembership membership = new GroupMembership(userId, group.groupId, GroupMembership.MembershipRole.MEMBER);
                
                // Si le groupe ne nécessite pas d'approbation, activer directement
                if (!group.requireApproval) {
                    membership.status = GroupMembership.MembershipStatus.ACTIVE;
                }
                
                long membershipId = membershipDao.insert(membership);
                membership.membershipId = (int) membershipId;
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'adhésion: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des membres =====================
    
    /**
     * Retire un membre d'un groupe
     */
    public void removeMember(int membershipId, int requestingUserId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                GroupMembership membership = membershipDao.getMembershipById(membershipId);
                if (membership == null) {
                    mainHandler.post(() -> callback.onError("Membre introuvable"));
                    return;
                }
                
                GroupMembership requesterMembership = membershipDao.getMembership(requestingUserId, membership.groupId);
                if (requesterMembership == null || !requesterMembership.canManageMember(membership)) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                membership.status = GroupMembership.MembershipStatus.REMOVED;
                membershipDao.update(membership);
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du retrait: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Quitter un groupe
     */
    public void leaveGroup(int userId, int groupId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                GroupMembership membership = membershipDao.getMembership(userId, groupId);
                if (membership == null) {
                    mainHandler.post(() -> callback.onError("Vous n'êtes pas membre de ce groupe"));
                    return;
                }
                
                // Le propriétaire ne peut pas quitter son propre groupe
                if (membership.role == GroupMembership.MembershipRole.OWNER) {
                    mainHandler.post(() -> callback.onError("Le propriétaire ne peut pas quitter le groupe. Transférez d'abord la propriété."));
                    return;
                }
                
                membership.leaveGroup();
                membershipDao.update(membership);
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur en quittant le groupe: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Met à jour le rôle d'un membre
     */
    public void updateMemberRole(int membershipId, GroupMembership.MembershipRole newRole, int requestingUserId, MembershipCallback callback) {
        executor.execute(() -> {
            try {
                GroupMembership membership = membershipDao.getMembershipById(membershipId);
                if (membership == null) {
                    mainHandler.post(() -> callback.onError("Membre introuvable"));
                    return;
                }
                
                GroupMembership requesterMembership = membershipDao.getMembership(requestingUserId, membership.groupId);
                if (requesterMembership == null || !requesterMembership.canManageMember(membership)) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                // Ne pas permettre de créer plusieurs propriétaires
                if (newRole == GroupMembership.MembershipRole.OWNER && requesterMembership.role != GroupMembership.MembershipRole.OWNER) {
                    mainHandler.post(() -> callback.onError("Seul le propriétaire peut transférer la propriété"));
                    return;
                }
                
                membership.role = newRole;
                membershipDao.update(membership);
                
                mainHandler.post(() -> callback.onSuccess(membership));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour du rôle: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Utilitaires =====================
    
    /**
     * Vérifie si un utilisateur peut modifier un groupe
     */
    private boolean canUserModifyGroup(int userId, int groupId) {
        try {
            GroupMembership membership = membershipDao.getMembership(userId, groupId);
            return membership != null && membership.canPerformAction("MODIFY_SETTINGS");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Génère un nouveau code d'invitation pour un groupe
     */
    public void regenerateInviteCode(int groupId, int requestingUserId, GroupCallback callback) {
        executor.execute(() -> {
            try {
                if (!canUserModifyGroup(requestingUserId, groupId)) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                Group group = groupDao.getGroupById(groupId);
                if (group == null) {
                    mainHandler.post(() -> callback.onError("Groupe introuvable"));
                    return;
                }
                
                group.generateInviteCode();
                groupDao.update(group);
                
                mainHandler.post(() -> callback.onSuccess(group));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la génération du code: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Recherche des groupes publics
     */
    public void searchPublicGroups(String query, GroupListCallback callback) {
        executor.execute(() -> {
            try {
                List<Group> groups = groupDao.searchPublicGroups(query);
                mainHandler.post(() -> callback.onSuccess(groups));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la recherche: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Obtient les groupes d'un utilisateur
     */
    public void getUserGroups(int userId, MembershipListCallback callback) {
        executor.execute(() -> {
            try {
                List<GroupMembership> memberships = membershipDao.getUserActiveGroups(userId);
                mainHandler.post(() -> callback.onSuccess(memberships));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération des groupes: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        executor.shutdown();
    }
}
