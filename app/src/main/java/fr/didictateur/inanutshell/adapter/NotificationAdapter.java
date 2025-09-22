package fr.didictateur.inanutshell.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Notification;

/**
 * Adaptateur pour afficher les notifications dans un RecyclerView
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    
    private Context context;
    private List<Notification> notifications;
    private OnNotificationClickListener onNotificationClickListener;
    private OnNotificationDeleteListener onNotificationDeleteListener;
    
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }
    
    public interface OnNotificationDeleteListener {
        void onNotificationDelete(Notification notification);
    }
    
    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }
    
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }
    
    @Override
    public int getItemCount() {
        return notifications.size();
    }
    
    /**
     * Met à jour la liste des notifications
     */
    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }
    
    /**
     * Supprime une notification à la position donnée
     */
    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    /**
     * Ajoute une notification
     */
    public void addNotification(Notification notification) {
        notifications.add(0, notification);
        notifyItemInserted(0);
    }
    
    /**
     * Obtient la liste des notifications
     */
    public List<Notification> getNotifications() {
        return notifications;
    }
    
    /**
     * Définit le listener pour les clics
     */
    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.onNotificationClickListener = listener;
    }
    
    /**
     * Définit le listener pour les suppressions
     */
    public void setOnNotificationDeleteListener(OnNotificationDeleteListener listener) {
        this.onNotificationDeleteListener = listener;
    }
    
    /**
     * ViewHolder pour les notifications
     */
    class NotificationViewHolder extends RecyclerView.ViewHolder {
        
        private MaterialCardView cardView;
        private ImageView imageViewIcon;
        private TextView textViewTitle;
        private TextView textViewMessage;
        private TextView textViewTime;
        private Chip chipType;
        private Chip chipPriority;
        private ImageView imageViewDelete;
        private View indicatorUnread;
        
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            chipType = itemView.findViewById(R.id.chipType);
            chipPriority = itemView.findViewById(R.id.chipPriority);
            imageViewDelete = itemView.findViewById(R.id.imageViewDelete);
            indicatorUnread = itemView.findViewById(R.id.indicatorUnread);
        }
        
        public void bind(Notification notification) {
            // Titre
            textViewTitle.setText(notification.getTitle());
            
            // Message
            textViewMessage.setText(notification.getMessage());
            
            // Temps relatif
            String timeText = DateUtils.getRelativeTimeSpanString(
                notification.getCreatedAt().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString();
            textViewTime.setText(timeText);
            
            // Icône selon le type
            setNotificationIcon(notification.getType());
            
            // Chip du type
            setTypeChip(notification.getType());
            
            // Chip de priorité
            setPriorityChip(notification.getPriority());
            
            // État lu/non lu
            setReadState(notification.isRead());
            
            // État programmé
            if (notification.isScheduled() && notification.isPending()) {
                String scheduledText = "Programmée pour " + 
                    DateUtils.getRelativeTimeSpanString(
                        notification.getScheduledTime().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    );
                textViewTime.setText(scheduledText);
                cardView.setAlpha(0.7f);
            } else {
                cardView.setAlpha(1.0f);
            }
            
            // Listeners
            itemView.setOnClickListener(v -> {
                if (onNotificationClickListener != null) {
                    onNotificationClickListener.onNotificationClick(notification);
                }
            });
            
            imageViewDelete.setOnClickListener(v -> {
                if (onNotificationDeleteListener != null) {
                    onNotificationDeleteListener.onNotificationDelete(notification);
                }
            });
        }
        
        /**
         * Définit l'icône selon le type de notification
         */
        private void setNotificationIcon(Notification.NotificationType type) {
            int iconRes;
            int colorRes;
            
            switch (type) {
                case MEAL_REMINDER:
                case MEAL_PLAN_REMINDER:
                    iconRes = R.drawable.ic_restaurant;
                    colorRes = R.color.notification_meal;
                    break;
                    
                case RECIPE_SUGGESTION:
                case NEW_RECIPE:
                    iconRes = R.drawable.ic_recipe;
                    colorRes = R.color.notification_recipe;
                    break;
                    
                case TIMER_ALERT:
                    iconRes = R.drawable.ic_timer;
                    colorRes = R.color.notification_timer;
                    break;
                    
                case COOKING_TIP:
                    iconRes = R.drawable.ic_lightbulb;
                    colorRes = R.color.notification_tip;
                    break;
                    
                case SHOPPING_REMINDER:
                    iconRes = R.drawable.ic_shopping_cart;
                    colorRes = R.color.notification_shopping;
                    break;
                    
                case SYSTEM_UPDATE:
                    iconRes = R.drawable.ic_system_update;
                    colorRes = R.color.notification_system;
                    break;
                    
                default:
                    iconRes = R.drawable.ic_notification;
                    colorRes = R.color.primary;
                    break;
            }
            
            imageViewIcon.setImageResource(iconRes);
            imageViewIcon.setColorFilter(ContextCompat.getColor(context, colorRes));
        }
        
        /**
         * Configure le chip du type
         */
        private void setTypeChip(Notification.NotificationType type) {
            String typeText;
            int chipColorRes;
            
            switch (type) {
                case MEAL_REMINDER:
                    typeText = "Rappel repas";
                    chipColorRes = R.color.chip_meal;
                    break;
                    
                case RECIPE_SUGGESTION:
                    typeText = "Suggestion";
                    chipColorRes = R.color.chip_suggestion;
                    break;
                    
                case NEW_RECIPE:
                    typeText = "Nouveau";
                    chipColorRes = R.color.chip_new;
                    break;
                    
                case TIMER_ALERT:
                    typeText = "Minuteur";
                    chipColorRes = R.color.chip_timer;
                    break;
                    
                case COOKING_TIP:
                    typeText = "Conseil";
                    chipColorRes = R.color.chip_tip;
                    break;
                    
                case SHOPPING_REMINDER:
                    typeText = "Courses";
                    chipColorRes = R.color.chip_shopping;
                    break;
                    
                case MEAL_PLAN_REMINDER:
                    typeText = "Planning";
                    chipColorRes = R.color.chip_planning;
                    break;
                    
                case SYSTEM_UPDATE:
                    typeText = "Système";
                    chipColorRes = R.color.chip_system;
                    break;
                    
                default:
                    typeText = "Général";
                    chipColorRes = R.color.chip_general;
                    break;
            }
            
            chipType.setText(typeText);
            chipType.setChipBackgroundColorResource(chipColorRes);
        }
        
        /**
         * Configure le chip de priorité
         */
        private void setPriorityChip(Notification.NotificationPriority priority) {
            switch (priority) {
                case LOW:
                    chipPriority.setText("Faible");
                    chipPriority.setChipBackgroundColorResource(R.color.priority_low);
                    chipPriority.setVisibility(View.GONE); // Masquer pour priorité faible
                    break;
                    
                case NORMAL:
                    chipPriority.setVisibility(View.GONE); // Masquer pour priorité normale
                    break;
                    
                case HIGH:
                    chipPriority.setText("Important");
                    chipPriority.setChipBackgroundColorResource(R.color.priority_high);
                    chipPriority.setVisibility(View.VISIBLE);
                    break;
                    
                case URGENT:
                    chipPriority.setText("Urgent");
                    chipPriority.setChipBackgroundColorResource(R.color.priority_urgent);
                    chipPriority.setVisibility(View.VISIBLE);
                    break;
            }
        }
        
        /**
         * Configure l'état lu/non lu
         */
        private void setReadState(boolean isRead) {
            if (isRead) {
                // Notification lue
                textViewTitle.setTypeface(null, Typeface.NORMAL);
                textViewMessage.setAlpha(0.7f);
                indicatorUnread.setVisibility(View.GONE);
                cardView.setCardElevation(2f);
            } else {
                // Notification non lue
                textViewTitle.setTypeface(null, Typeface.BOLD);
                textViewMessage.setAlpha(1.0f);
                indicatorUnread.setVisibility(View.VISIBLE);
                cardView.setCardElevation(4f);
            }
        }
    }
}
