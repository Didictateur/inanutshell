package fr.didictateur.inanutshell;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimationUtils {
    
    // Animation de fondu en entrée
    public static void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(duration);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.start();
    }
    
    // Animation de fondu en sortie
    public static void fadeOut(View view, long duration, Runnable onComplete) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setDuration(duration);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        fadeOut.start();
    }
    
    // Animation de glissement depuis le bas
    public static void slideInFromBottom(View view, long duration) {
        view.setTranslationY(view.getHeight());
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", view.getHeight(), 0f);
        slideUp.setDuration(duration);
        slideUp.setInterpolator(new DecelerateInterpolator());
        slideUp.start();
    }
    
    // Animation de zoom et rotation
    public static void zoomRotate(View view, long duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotation", -180f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        
        animatorSet.playTogether(scaleX, scaleY, rotation, alpha);
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();
    }
    
    // Animation de rebond
    public static void bounce(View view, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f);
        
        AnimatorSet bounceSet = new AnimatorSet();
        bounceSet.playTogether(scaleX, scaleY);
        bounceSet.setDuration(duration);
        bounceSet.setInterpolator(new BounceInterpolator());
        bounceSet.start();
    }
    
    // Animation de pulsation
    public static void pulse(View view, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        
        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY);
        pulseSet.setDuration(duration);
        pulseSet.setRepeatCount(ValueAnimator.INFINITE);
        pulseSet.setRepeatMode(ValueAnimator.RESTART);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.start();
    }
    
    // Animation de secousse
    public static void shake(View view, long duration) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        shake.setDuration(duration);
        shake.start();
    }
    
    // Animation de flip horizontal
    public static void flipHorizontal(View view, long duration, Runnable onMiddle) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f);
        flipOut.setDuration(duration / 2);
        
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f);
        flipIn.setDuration(duration / 2);
        
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onMiddle != null) {
                    onMiddle.run();
                }
                flipIn.start();
            }
        });
        
        flipOut.start();
    }
    
    // Animation de glissement vers la droite et disparition
    public static void slideOutRight(View view, long duration, Runnable onComplete) {
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(view, "translationX", 0f, view.getWidth());
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(slideOut, fadeOut);
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        animatorSet.start();
    }
    
    // Animation de apparition en cascade pour les listes
    public static void cascadeIn(View[] views, long delayBetween) {
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setTranslationY(50f);
            
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(alpha, translationY);
            animatorSet.setDuration(400);
            animatorSet.setStartDelay(i * delayBetween);
            animatorSet.setInterpolator(new DecelerateInterpolator());
            animatorSet.start();
        }
    }
}
