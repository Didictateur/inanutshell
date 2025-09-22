package fr.didictateur.inanutshell.ui.sync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.sync.model.SyncItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapteur pour afficher les éléments en attente de synchronisation
 */
public class PendingItemsAdapter extends RecyclerView.Adapter<PendingItemsAdapter.PendingViewHolder> {
    
    private List<SyncItem> items = new ArrayList<>();
    private final OnPendingItemClickListener listener;
    
    public interface OnPendingItemClickListener {
        void onPendingItemClick(SyncItem item);
    }
    
    public PendingItemsAdapter(OnPendingItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateItems(List<SyncItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pending_sync, parent, false);
        return new PendingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        SyncItem item = items.get(position);
        holder.bind(item, listener);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class PendingViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView titleText;
        private final TextView actionText;
        private final TextView retryText;
        private final TextView timestampText;
        
        public PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tv_item_title);
            actionText = itemView.findViewById(R.id.tv_item_action);
            retryText = itemView.findViewById(R.id.tv_retry_count);
            timestampText = itemView.findViewById(R.id.tv_item_timestamp);
        }
        
        public void bind(SyncItem item, OnPendingItemClickListener listener) {
            // Titre basé sur le type et les données
            String title = getItemTitle(item);
            titleText.setText(title);
            
            // Action à effectuer
            String action = getActionText(item.getAction());
            actionText.setText(action);
            actionText.setTextColor(getActionColor(item.getAction()));
            
            // Nombre de tentatives
            if (item.getRetryCount() > 0) {
                retryText.setText("Tentatives: " + item.getRetryCount());
                retryText.setVisibility(View.VISIBLE);
                
                // Changer la couleur selon le nombre de tentatives
                if (item.getRetryCount() >= 3) {
                    retryText.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                } else {
                    retryText.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
                }
            } else {
                retryText.setVisibility(View.GONE);
            }
            
            // Timestamp
            String timestamp = formatTimestamp(item.getTimestamp());
            timestampText.setText("Créé le " + timestamp);
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPendingItemClick(item);
                }
            });
            
            // Apparence selon le nombre de tentatives
            if (item.getRetryCount() >= 3) {
                itemView.setAlpha(0.6f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
        
        private String getItemTitle(SyncItem item) {
            switch (item.getType()) {
                case RECIPE:
                    if (item.getData() instanceof fr.didictateur.inanutshell.data.model.Recipe) {
                        fr.didictateur.inanutshell.data.model.Recipe recipe = 
                            (fr.didictateur.inanutshell.data.model.Recipe) item.getData();
                        return "Recette: " + recipe.getName();
                    }
                    return "Recette: " + item.getId();
                case MEAL_PLAN:
                    return "Plan de repas: " + item.getId();
                case SHOPPING_LIST:
                    return "Liste de courses: " + item.getId();
                case USER_PROFILE:
                    return "Profil utilisateur: " + item.getId();
                default:
                    return "Élément: " + item.getId();
            }
        }
        
        private String getActionText(SyncItem.Action action) {
            switch (action) {
                case CREATE:
                    return "Créer";
                case UPDATE:
                    return "Mettre à jour";
                case DELETE:
                    return "Supprimer";
                default:
                    return "Action inconnue";
            }
        }
        
        private int getActionColor(SyncItem.Action action) {
            switch (action) {
                case CREATE:
                    return itemView.getContext().getColor(android.R.color.holo_green_dark);
                case UPDATE:
                    return itemView.getContext().getColor(android.R.color.holo_blue_dark);
                case DELETE:
                    return itemView.getContext().getColor(android.R.color.holo_red_dark);
                default:
                    return itemView.getContext().getColor(android.R.color.darker_gray);
            }
        }
        
        private String formatTimestamp(long timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", 
                                                                            java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }
    }
}
