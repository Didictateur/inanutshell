package fr.didictateur.inanutshell.ui.components;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ComponentAutoCompleteBinding;
import fr.didictateur.inanutshell.databinding.ItemSuggestionBinding;
import fr.didictateur.inanutshell.utils.AutoCompleteManager;

/**
 * Composant d'autocomplétion personnalisé avec dropdown de suggestions
 */
public class AutoCompleteSearchView extends LinearLayout {
    
    private ComponentAutoCompleteBinding binding;
    private AutoCompleteManager autoCompleteManager;
    private PopupWindow suggestionsPopup;
    private SuggestionsAdapter suggestionsAdapter;
    private Handler debounceHandler;
    private Runnable searchRunnable;
    
    private OnTextChangeListener textChangeListener;
    private OnSuggestionSelectedListener suggestionSelectedListener;
    
    private String currentQuery = "";
    private boolean showHistory = true;
    private boolean isIngredientMode = false;
    private static final int DEBOUNCE_DELAY = 300; // ms
    private static final int MAX_SUGGESTIONS = 8;
    
    public interface OnTextChangeListener {
        void onTextChanged(String text);
    }
    
    public interface OnSuggestionSelectedListener {
        void onSuggestionSelected(String suggestion);
    }
    
    public AutoCompleteSearchView(Context context) {
        super(context);
        init(context);
    }
    
    public AutoCompleteSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public AutoCompleteSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        binding = ComponentAutoCompleteBinding.inflate(LayoutInflater.from(context), this, true);
        
        autoCompleteManager = AutoCompleteManager.getInstance(context);
        debounceHandler = new Handler(Looper.getMainLooper());
        
        setupTextWatcher();
        setupSuggestionsPopup();
    }
    
    private void setupTextWatcher() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString();
                
                // Notifier le listener externe
                if (textChangeListener != null) {
                    textChangeListener.onTextChanged(currentQuery);
                }
                
                // Annuler la recherche précédente et programmer une nouvelle
                debounceHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchSuggestions(currentQuery);
                debounceHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
            }
        });
        
        // Gérer le focus
        binding.searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentQuery.isEmpty() && showHistory) {
                // Afficher l'historique quand le champ prend le focus
                searchSuggestions("");
            } else if (!hasFocus) {
                // Masquer les suggestions quand le champ perd le focus
                hideSuggestions();
            }
        });
    }
    
    private void setupSuggestionsPopup() {
        // Créer l'adapter pour les suggestions
        suggestionsAdapter = new SuggestionsAdapter();
        
        // Créer une RecyclerView pour les suggestions
        RecyclerView suggestionsRecyclerView = new RecyclerView(getContext());
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);
        suggestionsRecyclerView.setPadding(16, 8, 16, 8);
        
        // Créer le PopupWindow
        suggestionsPopup = new PopupWindow(
            suggestionsRecyclerView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        
        suggestionsPopup.setOutsideTouchable(true);
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setElevation(8);
        suggestionsPopup.setBackgroundDrawable(getContext().getDrawable(R.drawable.bg_suggestion_popup));
    }
    
    private void searchSuggestions(String query) {
        AutoCompleteManager.AutoCompleteCallback callback = suggestions -> {
            // S'assurer d'être sur le thread principal
            debounceHandler.post(() -> {
                if (suggestions != null && !suggestions.isEmpty()) {
                    showSuggestions(suggestions);
                } else {
                    hideSuggestions();
                }
            });
        };
        
        if (isIngredientMode) {
            autoCompleteManager.getIngredientSuggestions(query, MAX_SUGGESTIONS, callback);
        } else {
            autoCompleteManager.getTextSuggestions(query, MAX_SUGGESTIONS, callback);
        }
    }
    
    private void showSuggestions(List<String> suggestions) {
        if (suggestions.isEmpty()) {
            hideSuggestions();
            return;
        }
        
        suggestionsAdapter.updateSuggestions(suggestions);
        
        if (!suggestionsPopup.isShowing()) {
            // Calculer la position et afficher le popup
            int[] location = new int[2];
            binding.searchEditText.getLocationOnScreen(location);
            
            suggestionsPopup.showAsDropDown(
                binding.searchEditText,
                0,
                0
            );
        }
    }
    
    private void hideSuggestions() {
        if (suggestionsPopup.isShowing()) {
            suggestionsPopup.dismiss();
        }
    }
    
    private void onSuggestionClicked(String suggestion) {
        // Définir le texte sans déclencher de nouvelle recherche
        binding.searchEditText.removeTextChangedListener(binding.searchEditText.getTag() instanceof TextWatcher ? 
            (TextWatcher) binding.searchEditText.getTag() : null);
        
        binding.searchEditText.setText(suggestion);
        binding.searchEditText.setSelection(suggestion.length());
        
        // Réactiver le TextWatcher
        setupTextWatcher();
        
        // Enregistrer dans l'historique
        if (isIngredientMode) {
            autoCompleteManager.recordIngredientSearch(suggestion);
        } else {
            autoCompleteManager.recordTextSearch(suggestion);
        }
        
        // Masquer les suggestions
        hideSuggestions();
        
        // Notifier le listener
        if (suggestionSelectedListener != null) {
            suggestionSelectedListener.onSuggestionSelected(suggestion);
        }
        
        // Notifier le changement de texte
        if (textChangeListener != null) {
            textChangeListener.onTextChanged(suggestion);
        }
    }
    
    /**
     * Adapter pour la liste des suggestions
     */
    private class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {
        
        private List<String> suggestions = new ArrayList<>();
        
        public void updateSuggestions(List<String> newSuggestions) {
            this.suggestions.clear();
            this.suggestions.addAll(newSuggestions);
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSuggestionBinding binding = ItemSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new SuggestionViewHolder(binding);
        }
        
        @Override
        public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
            String suggestion = suggestions.get(position);
            holder.bind(suggestion);
        }
        
        @Override
        public int getItemCount() {
            return suggestions.size();
        }
        
        class SuggestionViewHolder extends RecyclerView.ViewHolder {
            private final ItemSuggestionBinding binding;
            
            SuggestionViewHolder(ItemSuggestionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            
            void bind(String suggestion) {
                binding.suggestionText.setText(suggestion);
                
                // Mettre en évidence le texte correspondant à la requête
                if (!currentQuery.isEmpty() && suggestion.toLowerCase().contains(currentQuery.toLowerCase())) {
                    // TODO: Implémenter la mise en évidence du texte
                }
                
                binding.getRoot().setOnClickListener(v -> onSuggestionClicked(suggestion));
            }
        }
    }
    
    // Méthodes publiques
    
    public void setText(String text) {
        binding.searchEditText.setText(text);
        currentQuery = text != null ? text : "";
    }
    
    public String getText() {
        return binding.searchEditText.getText() != null ? 
            binding.searchEditText.getText().toString() : "";
    }
    
    public void setHint(String hint) {
        binding.searchInputLayout.setHint(hint);
    }
    
    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.textChangeListener = listener;
    }
    
    public void setOnSuggestionSelectedListener(OnSuggestionSelectedListener listener) {
        this.suggestionSelectedListener = listener;
    }
    
    public void setIngredientMode(boolean isIngredientMode) {
        this.isIngredientMode = isIngredientMode;
    }
    
    public void setShowHistory(boolean showHistory) {
        this.showHistory = showHistory;
    }
    
    public TextInputEditText getEditText() {
        return binding.searchEditText;
    }
    
    public void clearText() {
        binding.searchEditText.setText("");
        hideSuggestions();
    }
    
    public void requestFocusOnEditText() {
        binding.searchEditText.requestFocus();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (debounceHandler != null) {
            debounceHandler.removeCallbacks(searchRunnable);
        }
        hideSuggestions();
    }
}
