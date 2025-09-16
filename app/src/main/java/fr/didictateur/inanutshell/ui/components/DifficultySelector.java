package fr.didictateur.inanutshell.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ComponentDifficultySelectBinding;
import fr.didictateur.inanutshell.ui.search.SearchFilters;

/**
 * Composant personnalisé pour la sélection du niveau de difficulté
 */
public class DifficultySelector extends LinearLayout {
    
    private ComponentDifficultySelectBinding binding;
    private OnDifficultySelectedListener listener;
    private SearchFilters.DifficultyLevel selectedDifficulty = null;
    
    public interface OnDifficultySelectedListener {
        void onDifficultySelected(SearchFilters.DifficultyLevel difficulty);
        void onDifficultyCleared();
    }
    
    public DifficultySelector(Context context) {
        super(context);
        init(context);
    }
    
    public DifficultySelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public DifficultySelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(HORIZONTAL);
        binding = ComponentDifficultySelectBinding.inflate(LayoutInflater.from(context), this, true);
        
        setupChips();
    }
    
    private void setupChips() {
        // Créer les chips pour chaque niveau de difficulté
        for (SearchFilters.DifficultyLevel difficulty : SearchFilters.DifficultyLevel.values()) {
            Chip chip = createDifficultyChip(difficulty);
            binding.difficultyChipGroup.addView(chip);
        }
        
        // Gérer la sélection
        binding.difficultyChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedDifficulty = null;
                if (listener != null) {
                    listener.onDifficultyCleared();
                }
            } else {
                int checkedId = checkedIds.get(0);
                Chip checkedChip = findViewById(checkedId);
                if (checkedChip != null) {
                    SearchFilters.DifficultyLevel difficulty = (SearchFilters.DifficultyLevel) checkedChip.getTag();
                    selectedDifficulty = difficulty;
                    if (listener != null) {
                        listener.onDifficultySelected(difficulty);
                    }
                }
            }
        });
        
        // Bouton pour effacer la sélection
        binding.clearDifficultyButton.setOnClickListener(v -> {
            binding.difficultyChipGroup.clearCheck();
            selectedDifficulty = null;
            if (listener != null) {
                listener.onDifficultyCleared();
            }
        });
    }
    
    private Chip createDifficultyChip(SearchFilters.DifficultyLevel difficulty) {
        Chip chip = new Chip(getContext());
        chip.setText(difficulty.getDisplayName());
        chip.setCheckable(true);
        chip.setTag(difficulty);
        
        // Styling basé sur le niveau de difficulté
        switch (difficulty) {
            case EASY:
                chip.setChipBackgroundColorResource(R.color.difficulty_easy);
                break;
            case MEDIUM:
                chip.setChipBackgroundColorResource(R.color.difficulty_medium);
                break;
            case HARD:
                chip.setChipBackgroundColorResource(R.color.difficulty_hard);
                break;
        }
        
        return chip;
    }
    
    /**
     * Définir le listener pour les changements de sélection
     */
    public void setOnDifficultySelectedListener(OnDifficultySelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Obtenir la difficulté sélectionnée
     */
    public SearchFilters.DifficultyLevel getSelectedDifficulty() {
        return selectedDifficulty;
    }
    
    /**
     * Définir la difficulté sélectionnée
     */
    public void setSelectedDifficulty(SearchFilters.DifficultyLevel difficulty) {
        this.selectedDifficulty = difficulty;
        
        // Mettre à jour l'interface
        binding.difficultyChipGroup.clearCheck();
        if (difficulty != null) {
            for (int i = 0; i < binding.difficultyChipGroup.getChildCount(); i++) {
                View child = binding.difficultyChipGroup.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.getTag() == difficulty) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Effacer la sélection
     */
    public void clearSelection() {
        binding.difficultyChipGroup.clearCheck();
        selectedDifficulty = null;
    }
    
    /**
     * Activer ou désactiver le composant
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        binding.difficultyChipGroup.setEnabled(enabled);
        binding.clearDifficultyButton.setEnabled(enabled);
        
        for (int i = 0; i < binding.difficultyChipGroup.getChildCount(); i++) {
            View child = binding.difficultyChipGroup.getChildAt(i);
            child.setEnabled(enabled);
        }
    }
    
    /**
     * Obtenir le texte descriptif de la difficulté sélectionnée
     */
    public String getSelectedDifficultyText() {
        if (selectedDifficulty != null) {
            return selectedDifficulty.getDisplayName();
        }
        return null;
    }
    
    /**
     * Vérifier si une difficulté est sélectionnée
     */
    public boolean hasSelection() {
        return selectedDifficulty != null;
    }
}
