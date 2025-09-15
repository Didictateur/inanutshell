package fr.didictateur.inanutshell.ui.shopping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.shopping.ShoppingList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour l'affichage des listes de courses
 */
public class ShoppingListsAdapter extends RecyclerView.Adapter<ShoppingListsAdapter.ViewHolder> {
    
    private final Context context;
    private List<ShoppingList> shoppingLists;
    private final ShoppingListListener listener;
    private final SimpleDateFormat dateFormat;
    
    public interface ShoppingListListener {
        void onListClicked(ShoppingList shoppingList);
        void onDeleteClicked(ShoppingList shoppingList);
        void onToggleCompletedClicked(ShoppingList shoppingList);
        void onShareClicked(ShoppingList shoppingList);
    }
    
    public ShoppingListsAdapter(Context context, ShoppingListListener listener) {
        this.context = context;
        this.listener = listener;
        this.shoppingLists = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    public void updateLists(List<ShoppingList> newLists) {
        this.shoppingLists = newLists != null ? newLists : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingList list = shoppingLists.get(position);
        holder.bind(list);
    }
    
    @Override
    public int getItemCount() {
        return shoppingLists.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView textListName;
        private final TextView textItemCount;
        private final TextView textProgress;
        private final TextView textDate;
        private final TextView textSource;
        private final ProgressBar progressBar;
        private final ImageButton buttonToggleCompleted;
        private final ImageButton buttonShare;
        private final ImageButton buttonDelete;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardShoppingList);
            textListName = itemView.findViewById(R.id.textListName);
            textItemCount = itemView.findViewById(R.id.textItemCount);
            textProgress = itemView.findViewById(R.id.textProgress);
            textDate = itemView.findViewById(R.id.textDate);
            textSource = itemView.findViewById(R.id.textSource);
            progressBar = itemView.findViewById(R.id.progressBar);
            buttonToggleCompleted = itemView.findViewById(R.id.buttonToggleCompleted);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
        
        public void bind(ShoppingList list) {
            // Nom de la liste
            textListName.setText(list.name);
            
            // Nombre d'items
            String itemsText = list.totalItems + " article" + (list.totalItems > 1 ? "s" : "");
            textItemCount.setText(itemsText);
            
            // Progression
            int progress = list.getProgressPercentage();
            progressBar.setProgress(progress);
            textProgress.setText(progress + "%");
            
            // Date de création
            if (list.createdAt != null) {
                textDate.setText(dateFormat.format(list.createdAt));
            }
            
            // Source de génération
            String sourceText = getSourceDisplayText(list.generationSource);
            textSource.setText(sourceText);
            
            // Apparence selon le statut
            if (list.isCompleted) {
                cardView.setAlpha(0.6f);
                buttonToggleCompleted.setImageResource(R.drawable.ic_undo);
                textListName.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            } else {
                cardView.setAlpha(1.0f);
                buttonToggleCompleted.setImageResource(R.drawable.ic_check);
                textListName.setTextColor(context.getResources().getColor(android.R.color.black));
            }
            
            // Listeners
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onListClicked(list);
                }
            });
            
            buttonToggleCompleted.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleCompletedClicked(list);
                }
            });
            
            buttonShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClicked(list);
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(list);
                }
            });
        }
        
        private String getSourceDisplayText(ShoppingList.GenerationSource source) {
            switch (source) {
                case MANUAL:
                    return "📝 Manuelle";
                case RECIPE:
                    return "🍳 Recette";
                case MEAL_PLAN:
                    return "📅 Planning";
                case TEMPLATE:
                    return "📋 Modèle";
                default:
                    return "📝 Autre";
            }
        }
    }
}
