package fr.didictateur.inanutshell;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les thèmes dans un RecyclerView
 */
public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
    
    private Context context;
    private List<Theme> themes;
    private OnThemeClickListener listener;
    private Theme currentTheme;
    private SimpleDateFormat dateFormat;
    
    public interface OnThemeClickListener {
        void onThemeClick(Theme theme);
        void onThemeEdit(Theme theme);
        void onThemeDelete(Theme theme);
    }
    
    public ThemeAdapter(Context context, OnThemeClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.themes = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    public void updateThemes(List<Theme> newThemes) {
        this.themes.clear();
        this.themes.addAll(newThemes);
        notifyDataSetChanged();
    }
    
    public void setCurrentTheme(Theme currentTheme) {
        this.currentTheme = currentTheme;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_theme, parent, false);
        return new ThemeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        Theme theme = themes.get(position);
        holder.bind(theme);
    }
    
    @Override
    public int getItemCount() {
        return themes.size();
    }
    
    class ThemeViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private View colorPreview;
        private TextView themeName;
        private TextView themeDescription;
        private TextView themeType;
        private TextView dateCreated;
        private ImageView activeIndicator;
        private ImageView defaultIndicator;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private View colorBar;
        
        public ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.themeCard);
            colorPreview = itemView.findViewById(R.id.colorPreview);
            themeName = itemView.findViewById(R.id.themeName);
            themeDescription = itemView.findViewById(R.id.themeDescription);
            themeType = itemView.findViewById(R.id.themeType);
            dateCreated = itemView.findViewById(R.id.dateCreated);
            activeIndicator = itemView.findViewById(R.id.activeIndicator);
            defaultIndicator = itemView.findViewById(R.id.defaultIndicator);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            colorBar = itemView.findViewById(R.id.colorBar);
        }
        
        public void bind(Theme theme) {
            // Nom du thème
            themeName.setText(theme.getThemeName());
            
            // Description
            if (theme.getDescription() != null && !theme.getDescription().isEmpty()) {
                themeDescription.setText(theme.getDescription());
                themeDescription.setVisibility(View.VISIBLE);
            } else {
                themeDescription.setVisibility(View.GONE);
            }
            
            // Type de thème
            themeType.setText(getThemeTypeDisplayName(theme.getThemeType()));
            
            // Date de création
            if (theme.getDateCreated() != null) {
                dateCreated.setText("Créé le " + dateFormat.format(theme.getDateCreated()));
            } else {
                dateCreated.setText("");
            }
            
            // Indicateurs d'état
            activeIndicator.setVisibility(theme.isActive() ? View.VISIBLE : View.GONE);
            defaultIndicator.setVisibility(theme.isDefault() ? View.VISIBLE : View.GONE);
            
            // Aperçu des couleurs
            setupColorPreview(theme);
            
            // État de la carte (active ou non)
            setupCardState(theme);
            
            // Boutons d'action
            setupActionButtons(theme);
            
            // Listeners
            setupListeners(theme);
        }
        
        private void setupColorPreview(Theme theme) {
            try {
                // Couleur de fond de l'aperçu
                if (theme.getBackgroundColor() != null) {
                    int bgColor = Color.parseColor(theme.getBackgroundColor());
                    colorPreview.setBackgroundColor(bgColor);
                }
                
                // Barre de couleur avec les couleurs primaire et secondaire
                if (colorBar != null && theme.getPrimaryColor() != null && theme.getSecondaryColor() != null) {
                    // Créer un gradient ou utiliser la couleur primaire
                    int primaryColor = Color.parseColor(theme.getPrimaryColor());
                    colorBar.setBackgroundColor(primaryColor);
                }
            } catch (IllegalArgumentException e) {
                // Couleurs invalides, utiliser les valeurs par défaut
                colorPreview.setBackgroundColor(Color.WHITE);
                if (colorBar != null) {
                    colorBar.setBackgroundColor(Color.GRAY);
                }
            }
        }
        
        private void setupCardState(Theme theme) {
            if (theme.isActive()) {
                // Thème actif - bordure colorée
                try {
                    int primaryColor = Color.parseColor(theme.getPrimaryColor());
                    cardView.setStrokeColor(primaryColor);
                    cardView.setStrokeWidth(4);
                } catch (IllegalArgumentException e) {
                    cardView.setStrokeColor(Color.BLUE);
                    cardView.setStrokeWidth(4);
                }
            } else {
                // Thème inactif - pas de bordure
                cardView.setStrokeWidth(0);
            }
            
            // Élévation plus élevée pour le thème actif
            cardView.setCardElevation(theme.isActive() ? 8f : 4f);
        }
        
        private void setupActionButtons(Theme theme) {
            // Bouton d'édition - visible seulement pour les thèmes personnalisés
            boolean canEdit = theme.getThemeType() == Theme.ThemeType.CUSTOM || 
                             (theme.getUserId() > 0 && !theme.isDefault());
            editButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            
            // Bouton de suppression - visible seulement pour les thèmes non-défaut
            boolean canDelete = !theme.isDefault() && theme.getUserId() > 0;
            deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        }
        
        private void setupListeners(Theme theme) {
            // Clic sur la carte pour appliquer le thème
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeClick(theme);
                }
            });
            
            // Bouton d'édition
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeEdit(theme);
                }
            });
            
            // Bouton de suppression
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeDelete(theme);
                }
            });
        }
        
        private String getThemeTypeDisplayName(Theme.ThemeType type) {
            switch (type) {
                case LIGHT:
                    return "Clair";
                case DARK:
                    return "Sombre";
                case AUTO:
                    return "Automatique";
                case CUSTOM:
                    return "Personnalisé";
                case MATERIAL_YOU:
                    return "Material You";
                case HIGH_CONTRAST:
                    return "Contraste élevé";
                case SEASONAL:
                    return "Saisonnier";
                case RECIPE_THEMED:
                    return "Thème recette";
                default:
                    return type.name();
            }
        }
    }
}
