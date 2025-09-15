package fr.didictateur.inanutshell.ui.rating;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.utils.RatingManager;

/**
 * Dialogue pour noter une recette avec des étoiles
 */
public class RatingDialog {
    
    public interface OnRatingSetListener {
        void onRatingSet(Recipe recipe, float rating);
        void onRatingRemoved(Recipe recipe);
    }
    
    public static void show(Context context, Recipe recipe, OnRatingSetListener listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_rating, null);
        
        TextView titleText = dialogView.findViewById(R.id.tv_dialog_title);
        StarRatingView starRating = dialogView.findViewById(R.id.star_rating_dialog);
        Button btnRemove = dialogView.findViewById(R.id.btn_remove_rating);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        // Configuration initiale
        titleText.setText("Noter : " + recipe.getName());
        
        RatingManager ratingManager = RatingManager.getInstance(context);
        float currentRating = ratingManager.getRating(recipe.getId());
        starRating.setRating(currentRating);
        starRating.setInteractive(true);
        
        // Afficher le bouton "Supprimer" seulement si une note existe
        btnRemove.setVisibility(currentRating > 0f ? View.VISIBLE : View.GONE);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        // Gestion des événements
        starRating.setOnRatingChangeListener(rating -> {
            // Mettre à jour la visibilité du bouton "Supprimer"
            btnRemove.setVisibility(rating > 0f ? View.VISIBLE : View.GONE);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            float rating = starRating.getRating();
            if (rating > 0f) {
                ratingManager.setRating(recipe.getId(), rating);
                recipe.setUserRating(rating);
                if (listener != null) {
                    listener.onRatingSet(recipe, rating);
                }
            }
            dialog.dismiss();
        });
        
        btnRemove.setOnClickListener(v -> {
            ratingManager.removeRating(recipe.getId());
            recipe.setUserRating(0f);
            if (listener != null) {
                listener.onRatingRemoved(recipe);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
}
