package fr.didictateur.inanutshell.ui.rating;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import fr.didictateur.inanutshell.R;

/**
 * Widget de notation avec 5 étoiles personnalisable
 */
public class StarRatingView extends LinearLayout {
    
    private ImageView[] stars = new ImageView[5];
    private float rating = 0f;
    private boolean isInteractive = true;
    private OnRatingChangeListener listener;
    
    public interface OnRatingChangeListener {
        void onRatingChanged(float rating);
    }
    
    public StarRatingView(Context context) {
        super(context);
        init();
    }
    
    public StarRatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StarRatingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setOrientation(HORIZONTAL);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        for (int i = 0; i < 5; i++) {
            ImageView star = new ImageView(getContext());
            star.setImageResource(R.drawable.ic_star_empty);
            star.setPadding(4, 4, 4, 4);
            
            // Paramètres de layout pour un espacement régulier
            LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            star.setLayoutParams(params);
            star.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            
            final int starIndex = i;
            star.setOnClickListener(v -> {
                if (isInteractive) {
                    setRating(starIndex + 1);
                    if (listener != null) {
                        listener.onRatingChanged(rating);
                    }
                }
            });
            
            stars[i] = star;
            addView(star);
        }
        
        updateStarDisplay();
    }
    
    public void setRating(float rating) {
        this.rating = Math.max(0f, Math.min(5f, rating));
        updateStarDisplay();
    }
    
    public float getRating() {
        return rating;
    }
    
    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        for (ImageView star : stars) {
            star.setClickable(interactive);
        }
    }
    
    public boolean isInteractive() {
        return isInteractive;
    }
    
    public void setOnRatingChangeListener(OnRatingChangeListener listener) {
        this.listener = listener;
    }
    
    private void updateStarDisplay() {
        for (int i = 0; i < 5; i++) {
            float starValue = rating - i;
            
            if (starValue >= 1f) {
                // Étoile pleine
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else if (starValue > 0f) {
                // Étoile demi (on peut l'implémenter plus tard)
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else {
                // Étoile vide
                stars[i].setImageResource(R.drawable.ic_star_empty);
            }
        }
    }
    
    public void clearRating() {
        setRating(0f);
    }
}
