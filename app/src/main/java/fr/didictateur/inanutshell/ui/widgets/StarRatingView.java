package fr.didictateur.inanutshell.ui.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import fr.didictateur.inanutshell.R;

/**
 * Widget personnalisé pour afficher et gérer la notation par étoiles
 */
public class StarRatingView extends LinearLayout {
    
    private static final int MAX_STARS = 5;
    private ImageView[] starViews;
    private float rating = 0f;
    private boolean isInteractive = false;
    private OnRatingChangedListener listener;
    
    public interface OnRatingChangedListener {
        void onRatingChanged(float rating);
    }
    
    public StarRatingView(Context context) {
        super(context);
        init();
    }
    
    public StarRatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StarRatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        starViews = new ImageView[MAX_STARS];
        
        for (int i = 0; i < MAX_STARS; i++) {
            ImageView starView = new ImageView(getContext());
            
            // Définir la taille des étoiles
            int size = (int) (24 * getResources().getDisplayMetrics().density); // 24dp
            LayoutParams params = new LayoutParams(size, size);
            if (i > 0) {
                params.leftMargin = (int) (2 * getResources().getDisplayMetrics().density); // 2dp
            }
            starView.setLayoutParams(params);
            
            // Définir l'image par défaut
            starView.setImageResource(R.drawable.ic_star_empty);
            
            // Rendre interactive si nécessaire
            final int starIndex = i + 1;
            starView.setOnClickListener(v -> {
                if (isInteractive && listener != null) {
                    setRating(starIndex);
                    listener.onRatingChanged(starIndex);
                }
            });
            
            starViews[i] = starView;
            addView(starView);
        }
        
        updateStars();
    }
    
    /**
     * Définit la note à afficher
     * @param rating Note de 0 à 5
     */
    public void setRating(float rating) {
        this.rating = Math.max(0f, Math.min(MAX_STARS, rating));
        updateStars();
    }
    
    /**
     * Récupère la note actuelle
     * @return Note de 0 à 5
     */
    public float getRating() {
        return rating;
    }
    
    /**
     * Active ou désactive l'interactivité
     * @param interactive true pour permettre les clics
     */
    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        setClickable(interactive);
        
        // Changer le style des étoiles pour indiquer l'interactivité
        for (ImageView starView : starViews) {
            starView.setClickable(interactive);
            if (interactive) {
                starView.setBackground(ContextCompat.getDrawable(getContext(), 
                    android.R.drawable.list_selector_background));
            } else {
                starView.setBackground(null);
            }
        }
    }
    
    /**
     * Définit l'écouteur de changement de note
     * @param listener Écouteur
     */
    public void setOnRatingChangedListener(OnRatingChangedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Met à jour l'affichage des étoiles selon la note
     */
    private void updateStars() {
        for (int i = 0; i < MAX_STARS; i++) {
            ImageView starView = starViews[i];
            
            if (i < Math.floor(rating)) {
                // Étoile pleine
                starView.setImageResource(R.drawable.ic_star_filled);
            } else if (i < rating) {
                // Étoile à moitié (pour les notes décimales)
                starView.setImageResource(R.drawable.ic_star_half);
            } else {
                // Étoile vide
                starView.setImageResource(R.drawable.ic_star_empty);
            }
        }
    }
}
