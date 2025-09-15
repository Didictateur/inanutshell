package fr.didictateur.inanutshell;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Utilitaire pour appliquer les thèmes aux éléments de l'interface utilisateur
 */
public class ThemeApplicator {
    private static final String TAG = "ThemeApplicator";

    /**
     * Applique un thème à une activité
     */
    public static void applyThemeToActivity(Activity activity, Theme theme) {
        if (theme == null || activity == null) return;

        // Appliquer les couleurs de la barre de statut et navigation
        applySystemBarColors(activity, theme);
        
        // Appliquer la couleur de fond principale
        applyBackgroundColor(activity, theme);
        
        // Appliquer les paramètres d'accessibilité globaux
        applyAccessibilitySettings(activity, theme);
    }

    /**
     * Applique les couleurs des barres système
     */
    private static void applySystemBarColors(Activity activity, Theme theme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            
            // Couleur de la barre de statut
            int primaryColor = parseColor(theme.getPrimaryColor(), "#2196F3");
            int darkerPrimary = darkenColor(primaryColor, 0.8f);
            window.setStatusBarColor(darkerPrimary);
            
            // Couleur de la barre de navigation
            window.setNavigationBarColor(primaryColor);
            
            // Mode sombre pour les icônes de la barre de statut
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                
                if (isColorLight(parseColor(theme.getBackgroundColor(), "#FFFFFF"))) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    /**
     * Applique la couleur de fond à l'activité
     */
    private static void applyBackgroundColor(Activity activity, Theme theme) {
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null) {
            int backgroundColor = parseColor(theme.getBackgroundColor(), "#FFFFFF");
            rootView.setBackgroundColor(backgroundColor);
        }
    }

    /**
     * Applique les paramètres d'accessibilité
     */
    private static void applyAccessibilitySettings(Activity activity, Theme theme) {
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null) {
            // Échelle de texte (sera appliquée via CSS ou ressources)
            float textScale = theme.getTextScale();
            if (textScale != 1.0f) {
                applyTextScaleToViews(rootView, textScale);
            }
        }
    }

    /**
     * Applique un thème à une toolbar Material
     */
    public static void applyThemeToToolbar(MaterialToolbar toolbar, Theme theme) {
        if (toolbar == null || theme == null) return;
        
        int primaryColor = parseColor(theme.getPrimaryColor(), "#2196F3");
        int textColor = getContrastingTextColor(primaryColor);
        
        toolbar.setBackgroundColor(primaryColor);
        toolbar.setTitleTextColor(textColor);
        toolbar.setSubtitleTextColor(textColor);
        
        // Couleur des icônes
        toolbar.setNavigationIconTint(textColor);
        toolbar.setOverflowIconTint(textColor);
    }

    /**
     * Applique un thème à un bouton Material
     */
    public static void applyThemeToButton(MaterialButton button, Theme theme) {
        if (button == null || theme == null) return;
        
        int primaryColor = parseColor(theme.getPrimaryColor(), "#2196F3");
        int textColor = getContrastingTextColor(primaryColor);
        
        button.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        button.setTextColor(textColor);
        
        // Coins arrondis si activés
        if (theme.isRoundedCorners()) {
            button.setCornerRadius((int) (theme.getCornerRadius() * 2)); // Conversion dp to px nécessaire
        }
    }

    /**
     * Applique un thème à un FloatingActionButton
     */
    public static void applyThemeToFAB(FloatingActionButton fab, Theme theme) {
        if (fab == null || theme == null) return;
        
        int secondaryColor = parseColor(theme.getSecondaryColor(), "#03DAC6");
        int iconColor = getContrastingTextColor(secondaryColor);
        
        fab.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));
        fab.setColorFilter(iconColor);
        
        // Élévation personnalisée
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setElevation(theme.getElevation() * 4); // Conversion dp to px nécessaire
        }
    }

    /**
     * Applique un thème à une MaterialCardView
     */
    public static void applyThemeToCard(MaterialCardView card, Theme theme) {
        if (card == null || theme == null) return;
        
        int backgroundColor = parseColor(theme.getBackgroundColor(), "#FFFFFF");
        int cardColor = adjustColorBrightness(backgroundColor, 0.05f);
        
        card.setCardBackgroundColor(cardColor);
        
        // Coins arrondis
        if (theme.isRoundedCorners()) {
            card.setRadius(theme.getCornerRadius());
        }
        
        // Élévation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setCardElevation(theme.getElevation());
        }
        
        // Contour si contraste élevé
        if (theme.isHighContrast()) {
            int strokeColor = parseColor(theme.getPrimaryColor(), "#2196F3");
            card.setStrokeColor(strokeColor);
            card.setStrokeWidth(2); // dp to px conversion needed
        }
    }

    /**
     * Applique un thème aux TextView
     */
    public static void applyThemeToTextView(TextView textView, Theme theme) {
        if (textView == null || theme == null) return;
        
        // Couleur du texte basée sur le contraste avec l'arrière-plan
        int backgroundColor = parseColor(theme.getBackgroundColor(), "#FFFFFF");
        int textColor = getContrastingTextColor(backgroundColor);
        textView.setTextColor(textColor);
        
        // Texte en gras si activé
        if (theme.isBoldText()) {
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        }
        
        // Échelle de texte
        float textScale = theme.getTextScale();
        if (textScale != 1.0f) {
            float currentSize = textView.getTextSize();
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize * textScale);
        }
    }

    /**
     * Crée un drawable de fond avec les couleurs du thème
     */
    public static GradientDrawable createThemedBackground(Theme theme, boolean isRounded) {
        GradientDrawable drawable = new GradientDrawable();
        
        int backgroundColor = parseColor(theme.getBackgroundColor(), "#FFFFFF");
        drawable.setColor(backgroundColor);
        
        if (isRounded && theme.isRoundedCorners()) {
            drawable.setCornerRadius(theme.getCornerRadius());
        }
        
        if (theme.isHighContrast()) {
            int strokeColor = parseColor(theme.getPrimaryColor(), "#2196F3");
            drawable.setStroke(2, strokeColor); // dp to px conversion needed
        }
        
        return drawable;
    }

    /**
     * Crée un drawable de gradient avec les couleurs du thème
     */
    public static GradientDrawable createThemedGradient(Theme theme, 
                                                       GradientDrawable.Orientation orientation) {
        GradientDrawable drawable = new GradientDrawable();
        
        int primaryColor = parseColor(theme.getPrimaryColor(), "#2196F3");
        int secondaryColor = parseColor(theme.getSecondaryColor(), "#03DAC6");
        
        drawable.setOrientation(orientation);
        drawable.setColors(new int[]{primaryColor, secondaryColor});
        
        if (theme.isRoundedCorners()) {
            drawable.setCornerRadius(theme.getCornerRadius());
        }
        
        return drawable;
    }

    // ===================== Méthodes utilitaires privées =====================

    /**
     * Parse une chaîne de couleur en entier
     */
    private static int parseColor(String colorString, String defaultColor) {
        try {
            if (colorString != null && !colorString.isEmpty()) {
                return Color.parseColor(colorString);
            }
        } catch (IllegalArgumentException e) {
            // Couleur invalide, utiliser la couleur par défaut
        }
        return Color.parseColor(defaultColor);
    }

    /**
     * Détermine si une couleur est claire ou sombre
     */
    private static boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    /**
     * Obtient une couleur de texte contrastante
     */
    private static int getContrastingTextColor(int backgroundColor) {
        return isColorLight(backgroundColor) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Assombrit une couleur
     */
    private static int darkenColor(int color, float factor) {
        int red = (int) (Color.red(color) * factor);
        int green = (int) (Color.green(color) * factor);
        int blue = (int) (Color.blue(color) * factor);
        return Color.rgb(red, green, blue);
    }

    /**
     * Ajuste la luminosité d'une couleur
     */
    private static int adjustColorBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] + factor));
        return Color.HSVToColor(hsv);
    }

    /**
     * Applique l'échelle de texte à une hiérarchie de vues
     */
    private static void applyTextScaleToViews(View view, float scale) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            float currentSize = textView.getTextSize();
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize * scale);
        }
        
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyTextScaleToViews(viewGroup.getChildAt(i), scale);
            }
        }
    }

    /**
     * Convertit des dp en pixels
     */
    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            context.getResources().getDisplayMetrics()
        );
    }

    /**
     * Convertit des pixels en dp
     */
    public static float pxToDp(Context context, int px) {
        return px / context.getResources().getDisplayMetrics().density;
    }
}
