package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * DAO pour les opérations sur les appartenances aux groupes
 */
@Dao
public interface GroupMembershipDao {

    // ===================== Opérations CRUD de base =====================

    @Insert
    long insert(GroupMembership membership);

    @Update
    void update(GroupMembership membership);

    @Delete
    void delete(GroupMembership membership);

    @Query("SELECT * FROM group_memberships WHERE membershipId = :membershipId")
    GroupMembership getMembershipById(int membershipId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND groupId = :groupId")
    GroupMembership getMembership(int userId, int groupId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND groupId = :groupId")
    LiveData<GroupMembership> getMembershipLive(int userId, int groupId);

    // ===================== Gestion des membres par groupe =====================

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND status = 'ACTIVE'")
    List<GroupMembership> getActiveGroupMembers(int groupId);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND status = 'ACTIVE'")
    LiveData<List<GroupMembership>> getActiveGroupMembersLive(int groupId);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId")
    List<GroupMembership> getAllGroupMembers(int groupId);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId")
    LiveData<List<GroupMembership>> getAllGroupMembersLive(int groupId);

    @Query("SELECT COUNT(*) FROM group_memberships WHERE groupId = :groupId AND status = 'ACTIVE'")
    int getActiveGroupMemberCount(int groupId);

    @Query("SELECT COUNT(*) FROM group_memberships WHERE groupId = :groupId AND status = 'ACTIVE'")
    LiveData<Integer> getActiveGroupMemberCountLive(int groupId);

    // ===================== Gestion des groupes par utilisateur =====================

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND status = 'ACTIVE'")
    List<GroupMembership> getUserActiveGroups(int userId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND status = 'ACTIVE'")
    LiveData<List<GroupMembership>> getUserActiveGroupsLive(int userId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId")
    List<GroupMembership> getAllUserMemberships(int userId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId")
    LiveData<List<GroupMembership>> getAllUserMembershipsLive(int userId);

    // ===================== Gestion des rôles =====================

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND role = :role AND status = 'ACTIVE'")
    List<GroupMembership> getMembersByRole(int groupId, GroupMembership.MembershipRole role);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND role = 'OWNER' AND status = 'ACTIVE'")
    GroupMembership getGroupOwner(int groupId);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND role IN ('OWNER', 'ADMIN') AND status = 'ACTIVE'")
    List<GroupMembership> getGroupAdmins(int groupId);

    @Query("UPDATE group_memberships SET role = :newRole WHERE membershipId = :membershipId")
    void updateMemberRole(int membershipId, GroupMembership.MembershipRole newRole);

    @Query("SELECT COUNT(*) > 0 FROM group_memberships WHERE userId = :userId AND groupId = :groupId AND role IN ('OWNER', 'ADMIN') AND status = 'ACTIVE'")
    boolean isUserGroupAdmin(int userId, int groupId);

    // ===================== Gestion des statuts =====================

    @Query("SELECT * FROM group_memberships WHERE status = :status")
    List<GroupMembership> getMembershipsByStatus(GroupMembership.MembershipStatus status);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND status = 'PENDING'")
    List<GroupMembership> getUserPendingInvitations(int userId);

    @Query("SELECT * FROM group_memberships WHERE userId = :userId AND status = 'PENDING'")
    LiveData<List<GroupMembership>> getUserPendingInvitationsLive(int userId);

    @Query("SELECT * FROM group_memberships WHERE groupId = :groupId AND status = 'PENDING'")
    List<GroupMembership> getGroupPendingInvitations(int groupId);

    @Query("UPDATE group_memberships SET status = :newStatus WHERE membershipId = :membershipId")
    void updateMembershipStatus(int membershipId, GroupMembership.MembershipStatus newStatus);

    // ===================== Actions sur les appartenances =====================

    @Query("UPDATE group_memberships SET status = 'ACTIVE', dateJoined = :timestamp WHERE membershipId = :membershipId")
    void acceptInvitation(int membershipId, long timestamp);

    @Query("UPDATE group_memberships SET status = 'LEFT' WHERE membershipId = :membershipId")
    void declineInvitation(int membershipId);

    @Query("UPDATE group_memberships SET status = 'LEFT' WHERE userId = :userId AND groupId = :groupId")
    void leaveGroup(int userId, int groupId);

    @Query("UPDATE group_memberships SET status = 'REMOVED' WHERE membershipId = :membershipId")
    void removeMember(int membershipId);

    @Query("UPDATE group_memberships SET status = 'SUSPENDED' WHERE membershipId = :membershipId")
    void suspendMember(int membershipId);

    @Query("UPDATE group_memberships SET status = 'ACTIVE' WHERE membershipId = :membershipId")
    void reactivateMember(int membershipId);

    // ===================== Validation et vérifications =====================

    @Query("SELECT COUNT(*) > 0 FROM group_memberships WHERE userId = :userId AND groupId = :groupId")
    boolean membershipExists(int userId, int groupId);

    @Query("SELECT COUNT(*) > 0 FROM group_memberships WHERE userId = :userId AND groupId = :groupId AND status = 'ACTIVE'")
    boolean isActiveMember(int userId, int groupId);

    @Query("SELECT COUNT(*) > 0 FROM group_memberships WHERE userId = :userId AND groupId = :groupId AND status = 'PENDING'")
    boolean hasPendingInvitation(int userId, int groupId);

    // ===================== Invitations =====================

    @Query("SELECT * FROM group_memberships WHERE invitedBy = :inviterId AND status = 'PENDING'")
    List<GroupMembership> getInvitationsSentBy(int inviterId);

    @Query("UPDATE group_memberships SET invitedBy = :inviterId, inviteMessage = :message, dateInvited = :timestamp WHERE membershipId = :membershipId")
    void updateInvitationInfo(int membershipId, int inviterId, String message, long timestamp);

    // ===================== Recherche et filtrage =====================

    @Query("SELECT gm.* FROM group_memberships gm " +
           "INNER JOIN users u ON gm.userId = u.userId " +
           "WHERE gm.groupId = :groupId AND gm.status = 'ACTIVE' " +
           "AND (u.username LIKE '%' || :query || '%' OR u.email LIKE '%' || :query || '%')")
    List<GroupMembership> searchGroupMembers(int groupId, String query);

    // ===================== Statistiques =====================

    @Query("SELECT COUNT(*) FROM group_memberships WHERE status = 'ACTIVE'")
    int getTotalActiveMemberships();

    @Query("SELECT COUNT(*) FROM group_memberships WHERE userId = :userId AND status = 'ACTIVE'")
    int getUserActiveGroupCount(int userId);

    @Query("SELECT COUNT(*) FROM group_memberships WHERE role = :role AND status = 'ACTIVE'")
    int getMemberCountByRole(GroupMembership.MembershipRole role);

    @Query("SELECT COUNT(*) FROM group_memberships WHERE status = 'PENDING'")
    int getPendingInvitationCount();

    // ===================== Nettoyage et maintenance =====================

    @Query("DELETE FROM group_memberships WHERE status IN ('LEFT', 'REMOVED') AND dateJoined < :cutoffDate")
    int cleanupOldMemberships(long cutoffDate);

    @Query("UPDATE group_memberships SET status = 'LEFT' WHERE status = 'PENDING' AND dateInvited < :cutoffDate")
    int expirePendingInvitations(long cutoffDate);

    // ===================== Transfert de propriété =====================

    @Query("UPDATE group_memberships SET role = 'ADMIN' WHERE groupId = :groupId AND role = 'OWNER'")
    void demoteCurrentOwner(int groupId);

    @Query("UPDATE group_memberships SET role = 'OWNER' WHERE membershipId = :membershipId")
    void promoteToOwner(int membershipId);

    // ===================== Requêtes avancées avec jointures =====================

    @Query("SELECT gm.* FROM group_memberships gm " +
           "INNER JOIN groups g ON gm.groupId = g.groupId " +
           "WHERE gm.userId = :userId AND gm.status = 'ACTIVE' " +
           "AND g.isActive = 1 AND g.shareRecipes = 1")
    List<GroupMembership> getUserGroupsWithRecipeSharing(int userId);

    @Query("SELECT gm.* FROM group_memberships gm " +
           "INNER JOIN groups g ON gm.groupId = g.groupId " +
           "WHERE gm.userId = :userId AND gm.status = 'ACTIVE' " +
           "AND g.isActive = 1 AND g.shareMealPlanning = 1")
    List<GroupMembership> getUserGroupsWithMealPlanSharing(int userId);

    @Query("SELECT DISTINCT gm.userId FROM group_memberships gm " +
           "WHERE gm.groupId = :groupId AND gm.status = 'ACTIVE' " +
           "AND gm.userId != :excludeUserId")
    List<Integer> getOtherGroupMemberIds(int groupId, int excludeUserId);
}
