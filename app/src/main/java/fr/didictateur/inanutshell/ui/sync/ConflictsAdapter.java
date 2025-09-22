package fr.didictateur.inanutshell.ui.sync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.sync.model.ConflictResolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapteur pour afficher les conflits de synchronisation
 */
public class ConflictsAdapter extends RecyclerView.Adapter<ConflictsAdapter.ConflictViewHolder> {
    
    private List<ConflictResolution> conflicts = new ArrayList<>();
    private final OnConflictClickListener listener;
    
    public interface OnConflictClickListener {
        void onConflictClick(ConflictResolution conflict);
    }
    
    public ConflictsAdapter(OnConflictClickListener listener) {
        this.listener = listener;
    }
    
    public void updateConflicts(List<ConflictResolution> newConflicts) {
        this.conflicts.clear();
        if (newConflicts != null) {
            this.conflicts.addAll(newConflicts);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ConflictViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_conflict, parent, false);
        return new ConflictViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ConflictViewHolder holder, int position) {
        ConflictResolution conflict = conflicts.get(position);
        holder.bind(conflict, listener);
    }
    
    @Override
    public int getItemCount() {
        return conflicts.size();
    }
    
    static class ConflictViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView statusText;
        private final TextView timestampText;
        
        public ConflictViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tv_conflict_title);
            descriptionText = itemView.findViewById(R.id.tv_conflict_description);
            statusText = itemView.findViewById(R.id.tv_conflict_status);
            timestampText = itemView.findViewById(R.id.tv_conflict_timestamp);
        }
        
        public void bind(ConflictResolution conflict, OnConflictClickListener listener) {
            // Titre basé sur le type de conflit
            String title = getConflictTitle(conflict);
            titleText.setText(title);
            
            // Description du conflit
            descriptionText.setText(conflict.getConflictDescription());
            
            // Statut de résolution
            if (conflict.isResolved()) {
                statusText.setText("Résolu: " + getStrategyText(conflict.getStrategy()));
                statusText.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                statusText.setText("En attente de résolution");
                statusText.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
            }
            
            // Timestamp
            String timestamp = formatTimestamp(conflict.getTimestamp());
            timestampText.setText("Détecté le " + timestamp);
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null && !conflict.isResolved()) {
                    listener.onConflictClick(conflict);
                }
            });
            
            // Apparence différente si résolu
            float alpha = conflict.isResolved() ? 0.6f : 1.0f;
            itemView.setAlpha(alpha);
        }
        
        private String getConflictTitle(ConflictResolution conflict) {
            switch (conflict.getType()) {
                case RECIPE_CONFLICT:
                    if (conflict.getLocalRecipe() != null) {
                        return "Recette: " + conflict.getLocalRecipe().getName();
                    }
                    return "Conflit de recette";
                case MEAL_PLAN_CONFLICT:
                    return "Conflit de plan de repas";
                case SHOPPING_LIST_CONFLICT:
                    return "Conflit de liste de courses";
                default:
                    return "Conflit inconnu";
            }
        }
        
        private String getStrategyText(ConflictResolution.Strategy strategy) {
            switch (strategy) {
                case USE_LOCAL:
                    return "Version locale utilisée";
                case USE_SERVER:
                    return "Version serveur utilisée";
                case MERGE:
                    return "Versions fusionnées";
                case ASK_USER:
                    return "Résolution manuelle";
                default:
                    return "Stratégie inconnue";
            }
        }
        
        private String formatTimestamp(long timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", 
                                                                            java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }
    }
}
