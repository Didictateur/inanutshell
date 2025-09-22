package fr.didictateur.inanutshell.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import fr.didictateur.inanutshell.R;

/**
 * Utility class for creating smooth and consistent animations throughout the app
 */
public class AnimationHelper {
    
    private static final long DEFAULT_DURATION = 300;
    private static final long SHORT_DURATION = 150;
    private static final long LONG_DURATION = 500;
    
    /**
     * Animate view entrance with slide and fade
     */
    public static void animateViewEntrance(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DEFAULT_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }
    
    /**
     * Animate view exit with slide and fade
     */
    public static void animateViewExit(View view, Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(DEFAULT_DURATION)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                view.setVisibility(View.GONE);
                if (onComplete != null) onComplete.run();
            })
            .start();
    }
    
    /**
     * Create a pulse animation for buttons
     */
    public static void pulseView(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(SHORT_DURATION);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();
    }
    
    /**
     * Animate floating action button show/hide
     */
    public static void showFab(View fab) {
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(DEFAULT_DURATION)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }
    
    public static void hideFab(View fab) {
        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(SHORT_DURATION)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    /**
     * Create a shake animation for error states
     */
    public static void shakeView(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        animator.setDuration(500);
        animator.start();
    }
    
    /**
     * Animate progress bar with smooth transition
     */
    public static void animateProgress(android.widget.ProgressBar progressBar, int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), targetProgress);
        animator.setDuration(LONG_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBar.setProgress(progress);
        });
        animator.start();
    }
    
    /**
     * Create staggered animation for list items
     */
    public static void animateListItem(View view, int position) {
        view.setAlpha(0f);
        view.setTranslationY(100f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(position * 50L) // Stagger effect
            .setDuration(DEFAULT_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }
    
    /**
     * Apply transition animations between activities
     */
    public static void applyActivityTransition(Context context, boolean isEntering) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (isEntering) {
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }
    }
    
    /**
     * Create ripple effect animation
     */
    public static void createRippleEffect(View view) {
    // Utilise simplement la ressource drawable standard pour le ripple
    view.setBackgroundResource(android.R.drawable.list_selector_background);
    }
    
    /**
     * Animate card reveal with material motion
     */
    public static void animateCardReveal(View card) {
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);
        card.setAlpha(0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
        
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(DEFAULT_DURATION);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }
    
    /**
     * Create morph animation between two views
     */
    public static void morphViews(View fromView, View toView) {
        // Hide from view
        fromView.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(SHORT_DURATION)
            .withEndAction(() -> {
                fromView.setVisibility(View.GONE);
                // Show to view
                toView.setVisibility(View.VISIBLE);
                toView.setAlpha(0f);
                toView.setScaleX(0.8f);
                toView.setScaleY(0.8f);
                toView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SHORT_DURATION)
                    .start();
            })
            .start();
    }
}
