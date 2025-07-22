package fr.didictateur.inanutshell;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class AnimationUtils {
    
    // Constantes pour les durées d'animation
    public static final int DURATION_SHORT = 200;
    public static final int DURATION_MEDIUM = 400;
    public static final int DURATION_LONG = 600;
    
    // Animation de fade in
    public static void fadeIn(View view, int duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    
    // Animation de fade out
    public static void fadeOut(View view, int duration) {
        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }
    
    // Animation de slide depuis la gauche
    public static void slideInFromLeft(View view, int duration) {
        view.setTranslationX(-view.getWidth());
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationX(0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    
    // Animation de scale (zoom)
    public static void scaleAnimation(View view, float fromScale, float toScale, int duration) {
        view.setScaleX(fromScale);
        view.setScaleY(fromScale);
        view.animate()
                .scaleX(toScale)
                .scaleY(toScale)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
    
    // Animation de pulsation
    public static void pulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        
        scaleX.setDuration(DURATION_MEDIUM);
        scaleY.setDuration(DURATION_MEDIUM);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        
        scaleX.start();
        scaleY.start();
    }
}
