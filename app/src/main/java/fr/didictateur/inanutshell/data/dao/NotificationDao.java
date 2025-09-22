package fr.didictateur.inanutshell.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

import fr.didictateur.inanutshell.data.model.Notification;

/**
 * DAO pour gérer les opérations sur les notifications
 */
@Dao
public interface NotificationDao {
    
    // Insertion
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNotification(Notification notification);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertNotifications(List<Notification> notifications);
    
    // Mise à jour
    @Update
    void updateNotification(Notification notification);
    
    @Query("UPDATE notifications SET isRead = :isRead WHERE notificationId = :id")
    void markAsRead(int id, boolean isRead);
    
    @Query("UPDATE notifications SET isRead = 1 WHERE type = :type")
    void markAllAsReadByType(Notification.NotificationType type);
    
    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();
    
    // Suppression
    @Delete
    void deleteNotification(Notification notification);
    
    @Query("DELETE FROM notifications WHERE notificationId = :id")
    void deleteNotificationById(int id);
    
    @Query("DELETE FROM notifications WHERE type = :type")
    void deleteNotificationsByType(Notification.NotificationType type);
    
    @Query("DELETE FROM notifications WHERE isRead = 1 AND createdAt < :cutoffDate")
    void deleteOldReadNotifications(Date cutoffDate);
    
    @Query("DELETE FROM notifications")
    void deleteAllNotifications();
    
    // Sélection
    @Query("SELECT * FROM notifications WHERE notificationId = :id")
    Notification getNotificationById(int id);
    
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    List<Notification> getAllNotifications();
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    List<Notification> getUnreadNotifications();
    
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY createdAt DESC")
    List<Notification> getNotificationsByType(Notification.NotificationType type);
    
    @Query("SELECT * FROM notifications WHERE type = :type AND isRead = 0 ORDER BY createdAt DESC")
    List<Notification> getUnreadNotificationsByType(Notification.NotificationType type);
    
    @Query("SELECT * FROM notifications WHERE priority = :priority ORDER BY createdAt DESC")
    List<Notification> getNotificationsByPriority(Notification.NotificationPriority priority);
    
    @Query("SELECT * FROM notifications WHERE isScheduled = 1 AND scheduledTime > :currentTime ORDER BY scheduledTime ASC")
    List<Notification> getScheduledNotifications(Date currentTime);
    
    @Query("SELECT * FROM notifications WHERE isScheduled = 1 AND scheduledTime <= :currentTime AND isRead = 0 ORDER BY scheduledTime ASC")
    List<Notification> getOverdueNotifications(Date currentTime);
    
    @Query("SELECT * FROM notifications WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    List<Notification> getNotificationsBetweenDates(Date startDate, Date endDate);
    
    @Query("SELECT * FROM notifications WHERE createdAt >= :date ORDER BY createdAt DESC")
    List<Notification> getNotificationsSince(Date date);
    
    @Query("SELECT * FROM notifications WHERE isRepeating = 1")
    List<Notification> getRepeatingNotifications();
    
    // Statistiques
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();
    
    @Query("SELECT COUNT(*) FROM notifications WHERE type = :type AND isRead = 0")
    int getUnreadCountByType(Notification.NotificationType type);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE priority = :priority AND isRead = 0")
    int getUnreadCountByPriority(Notification.NotificationPriority priority);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE createdAt >= :date")
    int getCountSince(Date date);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isScheduled = 1 AND scheduledTime > :currentTime")
    int getScheduledCount(Date currentTime);
    
    // Recherche
    @Query("SELECT * FROM notifications WHERE title LIKE :searchTerm OR message LIKE :searchTerm ORDER BY createdAt DESC")
    List<Notification> searchNotifications(String searchTerm);
    
    @Query("SELECT * FROM notifications WHERE (title LIKE :searchTerm OR message LIKE :searchTerm) AND type = :type ORDER BY createdAt DESC")
    List<Notification> searchNotificationsByType(String searchTerm, Notification.NotificationType type);
    
    // Nettoyage et maintenance
    @Query("SELECT * FROM notifications WHERE isRead = 1 AND createdAt < :cutoffDate")
    List<Notification> getOldReadNotifications(Date cutoffDate);
    
    @Query("SELECT DISTINCT type FROM notifications")
    List<Notification.NotificationType> getAllUsedTypes();
    
    @Query("SELECT * FROM notifications WHERE actionData = :actionData")
    List<Notification> getNotificationsByActionData(String actionData);
    
    // Méthodes pour la gestion des notifications programmées
    @Query("SELECT * FROM notifications WHERE isScheduled = 1 AND scheduledTime BETWEEN :startTime AND :endTime")
    List<Notification> getNotificationsInTimeRange(Date startTime, Date endTime);
    
    @Query("SELECT * FROM notifications WHERE isScheduled = 1 AND isRepeating = 1")
    List<Notification> getScheduledRepeatingNotifications();
    
    @Query("UPDATE notifications SET scheduledTime = :newTime WHERE notificationId = :id")
    void rescheduleNotification(int id, Date newTime);
}
