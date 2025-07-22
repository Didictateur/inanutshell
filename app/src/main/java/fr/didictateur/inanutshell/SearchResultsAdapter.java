package fr.didictateur.inanutshell;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    
    private List<Recette> recettes;
    private String currentSearchQuery = "";
    
    public SearchResultsAdapter(List<Recette> recettes) {
        this.recettes = recettes;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Utiliser le layout personnalisé pour les résultats de recherche
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recette recette = recettes.get(position);
        
        // Configurer l'image de la recette
        holder.image.setImageResource(R.drawable.appicon);
        
        // Mettre en gras les lettres correspondant à la recherche dans le titre
        if (recette.titre != null) {
            SpannableString spannableTitle = highlightSearchQuery(recette.titre, currentSearchQuery);
            holder.title.setText(spannableTitle);
        } else {
            holder.title.setText("Recette sans titre");
        }
        
        // Afficher les ingrédients comme sous-titre
        if (recette.ingredients != null && !recette.ingredients.trim().isEmpty()) {
            holder.subtitle.setText(recette.ingredients);
            holder.subtitle.setVisibility(View.VISIBLE);
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }
        
        // Masquer le temps de cuisson si présent
        if (holder.cookingTime != null) {
            holder.cookingTime.setVisibility(View.GONE);
        }
    }
    
    private SpannableString highlightSearchQuery(String text, String query) {
        SpannableString spannableString = new SpannableString(text);
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerText = text.toLowerCase();
            String lowerQuery = query.toLowerCase();
            
            // Trouver toutes les positions des lettres de la requête dans l'ordre
            int textIndex = 0;
            for (int i = 0; i < lowerQuery.length(); i++) {
                char searchChar = lowerQuery.charAt(i);
                
                // Chercher cette lettre à partir de la position actuelle
                while (textIndex < lowerText.length()) {
                    if (lowerText.charAt(textIndex) == searchChar) {
                        // Mettre en gras cette lettre
                        spannableString.setSpan(
                            new StyleSpan(Typeface.BOLD),
                            textIndex,
                            textIndex + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        textIndex++; // Passer à la lettre suivante
                        break;
                    }
                    textIndex++;
                }
                
                // Si on n'a pas trouvé cette lettre, arrêter
                if (textIndex >= lowerText.length()) {
                    break;
                }
            }
        }
        
        return spannableString;
    }
    
    @Override
    public int getItemCount() {
        return recettes.size();
    }
    
    
    public void updateData(List<Recette> newRecettes) {
        this.recettes = newRecettes;
        notifyDataSetChanged();
    }
    
    public void updateData(List<Recette> newRecettes, String searchQuery) {
        this.recettes = newRecettes;
        this.currentSearchQuery = searchQuery;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView subtitle;
        TextView cookingTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.recipeImage);
            title = itemView.findViewById(R.id.recipeName);
            subtitle = itemView.findViewById(R.id.recipeIngredients);
            cookingTime = itemView.findViewById(R.id.cookingTime);
        }
    }
}
