package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * Utilitaire pour améliorer l'accessibilité de l'application
 */
public class AccessibilityHelper {
    
    /**
     * Vérifie si les services d'accessibilité sont activés
     */
    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager accessibilityManager = 
            (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager != null && accessibilityManager.isEnabled();
    }
    
    /**
     * Configure l'accessibilité pour un élément de recette
     */
    public static void configureRecipeItemAccessibility(View itemView, 
            String recipeName, String recipeDescription, boolean isFavorite, float rating) {
        
        // Description complète pour les lecteurs d'écran
        StringBuilder contentDescription = new StringBuilder();
        contentDescription.append("Recette: ").append(recipeName);
        
        if (recipeDescription != null && !recipeDescription.isEmpty()) {
            contentDescription.append(". Description: ").append(recipeDescription);
        }
        
        if (isFavorite) {
            contentDescription.append(". Dans les favoris");
        }
        
        if (rating > 0) {
            contentDescription.append(". Note: ").append(rating).append(" étoiles sur 5");
        }
        
        contentDescription.append(". Toucher deux fois pour ouvrir");
        
        itemView.setContentDescription(contentDescription.toString());
        
        // Rendre l'élément focusable
        ViewCompat.setImportantForAccessibility(itemView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        
        // Rendre l'élément cliquable pour l'accessibilité
        itemView.setClickable(true);
        itemView.setLongClickable(true);
    }
    
    /**
     * Configure l'accessibilité pour un bouton avec état
     */
    public static void configureButtonAccessibility(View button, String action, boolean isActive) {
        StringBuilder description = new StringBuilder();
        description.append(action);
        
        if (isActive) {
            description.append(", activé");
        }
        
        button.setContentDescription(description.toString());
        ViewCompat.setImportantForAccessibility(button, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    
    /**
     * Configure l'accessibilité pour un champ de recherche
     */
    public static void configureSearchFieldAccessibility(View field, String hint, String currentValue) {
        StringBuilder description = new StringBuilder();
        description.append("Champ de recherche: ").append(hint);
        
        if (currentValue != null && !currentValue.isEmpty()) {
            description.append(". Valeur actuelle: ").append(currentValue);
        }
        
        field.setContentDescription(description.toString());
    }
    
    /**
     * Annonce un message à l'utilisateur via les services d'accessibilité
     */
    public static void announceForAccessibility(View view, String message) {
        if (view != null && message != null) {
            view.announceForAccessibility(message);
        }
    }
    
    /**
     * Configure l'accessibilité pour un élément de navigation
     */
    public static void configureNavigationAccessibility(View navItem, String tabName, boolean isSelected) {
        StringBuilder description = new StringBuilder();
        description.append("Onglet ").append(tabName);
        
        if (isSelected) {
            description.append(", sélectionné");
        } else {
            description.append(", toucher pour sélectionner");
        }
        
        navItem.setContentDescription(description.toString());
        navItem.setSelected(isSelected);
    }
    
    /**
     * Configure l'accessibilité pour une barre de progression/chargement
     */
    public static void configureProgressAccessibility(View progressView, String status) {
        progressView.setContentDescription("Chargement en cours: " + status);
        ViewCompat.setImportantForAccessibility(progressView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    
    /**
     * Configure l'accessibilité pour un état vide
     */
    public static void configureEmptyStateAccessibility(View emptyView, String title, String message) {
        StringBuilder description = new StringBuilder();
        description.append(title);
        if (message != null && !message.isEmpty()) {
            description.append(". ").append(message);
        }
        
        emptyView.setContentDescription(description.toString());
        ViewCompat.setImportantForAccessibility(emptyView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    
    /**
     * Masque un élément des lecteurs d'écran
     */
    public static void hideFromAccessibility(View view) {
        ViewCompat.setImportantForAccessibility(view, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    
    /**
     * Configure l'accessibilité pour les gestes de swipe
     */
    public static void configureSwipeAccessibility(View view, String leftAction, String rightAction) {
        StringBuilder description = new StringBuilder();
        description.append(view.getContentDescription());
        description.append(". Glisser à droite pour ").append(rightAction);
        description.append(", glisser à gauche pour ").append(leftAction);
        
        view.setContentDescription(description.toString());
        
        // Les actions de swipe seront gérées par les gestes existants
    }
}
