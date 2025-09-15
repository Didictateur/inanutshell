package fr.didictateur.inanutshell.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.ui.widgets.StarRatingView;
import fr.didictateur.inanutshell.utils.RatingManager;

/**
 * Boîte de dialogue pour noter une recette
 */
public class RatingDialog {
    
    private final Context context;
    private final Recipe recipe;
    private final OnRatingSetListener listener;
    private AlertDialog dialog;
    private StarRatingView starRating;
    private float currentRating;
    
    public interface OnRatingSetListener {
        void onRatingSet(Recipe recipe, float rating);
    }
    
    public RatingDialog(Context context, Recipe recipe, OnRatingSetListener listener) {
        this.context = context;
        this.recipe = recipe;
        this.listener = listener;
        this.currentRating = recipe.getUserRating();
    }
    
    /**
     * Affiche la boîte de dialogue de notation
     */
    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null);
        
        // Références vers les vues
        TextView tvRecipeName = dialogView.findViewById(R.id.tv_dialog_title);
        starRating = dialogView.findViewById(R.id.star_rating_dialog);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_confirm);
        Button btnRemove = dialogView.findViewById(R.id.btn_remove_rating);
        
        // Configurer la vue
        tvRecipeName.setText("Noter : " + recipe.getName());
        
        // Configurer les étoiles
        starRating.setRating(currentRating);
        starRating.setInteractive(true);
        starRating.setOnRatingChangedListener(rating -> {
            currentRating = rating;
        });
        
        // Afficher/masquer le bouton de suppression
        btnRemove.setVisibility(currentRating > 0 ? View.VISIBLE : View.GONE);
        
        // Créer la boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(true);
        
        dialog = builder.create();
        
        // Configurer les boutons
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (currentRating > 0) {
                saveRating(currentRating);
            }
            dialog.dismiss();
        });
        
        btnRemove.setOnClickListener(v -> {
            removeRating();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    

    
    /**
     * Sauvegarde la note
     */
    private void saveRating(float rating) {
        RatingManager ratingManager = RatingManager.getInstance(context);
        ratingManager.setRating(recipe.getId(), rating);
        recipe.setUserRating(rating);
        
        if (listener != null) {
            listener.onRatingSet(recipe, rating);
        }
    }
    
    /**
     * Supprime la note
     */
    private void removeRating() {
        RatingManager ratingManager = RatingManager.getInstance(context);
        ratingManager.removeRating(recipe.getId());
        recipe.setUserRating(0f);
        
        if (listener != null) {
            listener.onRatingSet(recipe, 0f);
        }
    }
}
