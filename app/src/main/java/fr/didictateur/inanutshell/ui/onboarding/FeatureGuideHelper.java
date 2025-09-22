package fr.didictateur.inanutshell.ui.onboarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.utils.AnimationHelper;

/**
 * Guide utilisateur contextuel pour présenter les nouvelles fonctionnalités
 */
public class FeatureGuideHelper {
    
    private static final String PREFS_NAME = "feature_guide_prefs";
    private static final String KEY_ADVANCED_EDITOR_SHOWN = "advanced_editor_guide_shown";
    private static final String KEY_FULLSCREEN_TIMER_SHOWN = "fullscreen_timer_guide_shown";
    private static final String KEY_SYNC_GUIDE_SHOWN = "sync_guide_shown";
    
    private Context context;
    private SharedPreferences prefs;
    
    public FeatureGuideHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Affiche le guide pour l'éditeur d'images avancé
     */
    public void showAdvancedEditorGuide() {
        if (prefs.getBoolean(KEY_ADVANCED_EDITOR_SHOWN, false)) {
            return; // Déjà affiché
        }
        
        showGuideDialog(
            "🎨 Éditeur d'Images Avancé",
            "Découvrez votre nouvel éditeur professionnel !\n\n" +
            "• 🔄 Transformer : Rotation, retournement, redimensionnement\n" +
            "• ✂️ Recadrer : Découpe précise avec poignées ajustables\n" +
            "• ⚙️ Ajuster : Luminosité, contraste, saturation\n" +
            "• 🎭 Filtres : N&B, sépia, vintage et plus\n" +
            "• ✏️ Annoter : Dessinez et ajoutez du texte\n\n" +
            "Naviguez entre les onglets pour accéder aux différents outils !",
            () -> prefs.edit().putBoolean(KEY_ADVANCED_EDITOR_SHOWN, true).apply()
        );
    }
    
    /**
     * Affiche le guide pour le timer plein écran
     */
    public void showFullscreenTimerGuide() {
        if (prefs.getBoolean(KEY_FULLSCREEN_TIMER_SHOWN, false)) {
            return; // Déjà affiché
        }
        
        showGuideDialog(
            "⏱️ Timer Plein Écran",
            "Mode immersif pour votre cuisine !\n\n" +
            "• 👆 Tapez pour afficher/masquer les contrôles\n" +
            "• ↕️ Balayez vers le haut/bas pour ajuster le temps\n" +
            "• ⏸️ Double-tap pour pause/lecture\n" +
            "• 📱 Interface optimisée pour les mains sales\n" +
            "• 🔊 Alarmes visuelles et sonores\n\n" +
            "Parfait pour suivre vos recettes sans interruption !",
            () -> prefs.edit().putBoolean(KEY_FULLSCREEN_TIMER_SHOWN, true).apply()
        );
    }
    
    /**
     * Affiche le guide pour la synchronisation
     */
    public void showSyncGuide() {
        if (prefs.getBoolean(KEY_SYNC_GUIDE_SHOWN, false)) {
            return; // Déjà affiché
        }
        
        showGuideDialog(
            "🔄 Synchronisation Temps Réel",
            "Vos recettes partout, tout le temps !\n\n" +
            "• ☁️ Sync automatique entre appareils\n" +
            "• 📱 Mode hors ligne intelligent\n" +
            "• 🔧 Résolution automatique des conflits\n" +
            "• 📊 Suivi en temps réel des modifications\n" +
            "• 🔒 Sauvegarde sécurisée\n\n" +
            "Configurez vos serveurs Mealie pour commencer !",
            () -> prefs.edit().putBoolean(KEY_SYNC_GUIDE_SHOWN, true).apply()
        );
    }
    
    /**
     * Affiche un guide personnalisé
     */
    private void showGuideDialog(String title, String message, Runnable onDismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_InANutshell);
        
        // Inflate custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feature_guide, null);
        
        TextView titleView = dialogView.findViewById(R.id.tv_guide_title);
        TextView messageView = dialogView.findViewById(R.id.tv_guide_message);
        MaterialButton gotItButton = dialogView.findViewById(R.id.btn_got_it);
        MaterialCardView cardView = dialogView.findViewById(R.id.card_guide);
        
        titleView.setText(title);
        messageView.setText(message);
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        // Animate dialog entrance
        cardView.setScaleX(0.8f);
        cardView.setScaleY(0.8f);
        cardView.setAlpha(0f);
        
        gotItButton.setOnClickListener(v -> {
            // Animate exit
            AnimationHelper.animateViewExit(cardView, () -> {
                dialog.dismiss();
                if (onDismiss != null) {
                    onDismiss.run();
                }
            });
        });
        
        // Set transparent background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        dialog.show();
        
        // Animate entrance
        AnimationHelper.animateCardReveal(cardView);
    }
    
    /**
     * Réinitialise tous les guides (utile pour les tests)
     */
    public void resetAllGuides() {
        prefs.edit()
            .putBoolean(KEY_ADVANCED_EDITOR_SHOWN, false)
            .putBoolean(KEY_FULLSCREEN_TIMER_SHOWN, false)
            .putBoolean(KEY_SYNC_GUIDE_SHOWN, false)
            .apply();
    }
    
    /**
     * Affiche un guide personnalisé pour n'importe quelle fonctionnalité
     */
    public void showCustomGuide(String title, String message, String featureKey) {
        String prefKey = "guide_" + featureKey + "_shown";
        if (prefs.getBoolean(prefKey, false)) {
            return; // Déjà affiché
        }
        
        showGuideDialog(title, message, () -> 
            prefs.edit().putBoolean(prefKey, true).apply());
    }
    
    /**
     * Vérifie si c'est la première utilisation d'une fonctionnalité
     */
    public boolean shouldShowGuide(String feature) {
        switch (feature) {
            case "advanced_editor":
                return !prefs.getBoolean(KEY_ADVANCED_EDITOR_SHOWN, false);
            case "fullscreen_timer":
                return !prefs.getBoolean(KEY_FULLSCREEN_TIMER_SHOWN, false);
            case "sync":
                return !prefs.getBoolean(KEY_SYNC_GUIDE_SHOWN, false);
            case "advanced_search":
                return !prefs.getBoolean("guide_advanced_search_shown", false);
            default:
                return !prefs.getBoolean("guide_" + feature + "_shown", false);
        }
    }
}
