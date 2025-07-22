package fr.didictateur.inanutshell;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<Recette> recipes;
    private OnRecipeClickListener listener;
    private Context context;
    
    public interface OnRecipeClickListener {
        void onRecipeClick(Recette recipe);
    }
    
    public SearchResultAdapter(List<Recette> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recette recipe = recipes.get(position);
        
        holder.recipeName.setText(recipe.titre);
        
        // Afficher un aperçu des ingrédients (premiers mots)
        String ingredients = recipe.ingredients;
        if (ingredients != null && ingredients.length() > 50) {
            ingredients = ingredients.substring(0, 47) + "...";
        }
        holder.recipeIngredients.setText(ingredients != null ? ingredients : "");
        
        // Extraire et afficher le temps de cuisson
        String cookingTime = extractCookingTime(recipe.preparation);
        if (!cookingTime.isEmpty()) {
            holder.cookingTime.setText(cookingTime);
            holder.cookingTime.setVisibility(View.VISIBLE);
        } else {
            holder.cookingTime.setVisibility(View.GONE);
        }
        
        // Afficher tag végétarien si applicable
        if (isVegetarian(recipe)) {
            holder.vegetarianTag.setVisibility(View.VISIBLE);
        } else {
            holder.vegetarianTag.setVisibility(View.GONE);
        }
        
        // Charger l'image si disponible
        if (recipe.photoPath != null && !recipe.photoPath.isEmpty()) {
            // TODO: Charger l'image avec Glide ou Picasso
            // Pour l'instant, utiliser l'icône par défaut
            holder.recipeImage.setImageResource(R.drawable.appicon);
        } else {
            holder.recipeImage.setImageResource(R.drawable.appicon);
        }
        
        // Gestion du clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return recipes.size();
    }
    
    public void updateResults(List<Recette> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }
    
    private String extractCookingTime(String instructions) {
        // Recherche de patterns temporels dans les instructions
        String[] timePatterns = {"\\d+ min", "\\d+ minutes", "\\d+min"};
        
        for (String pattern : timePatterns) {
            if (instructions.matches(".*" + pattern + ".*")) {
                // Extraire le premier temps trouvé
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(instructions);
                if (m.find()) {
                    return m.group();
                }
            }
        }
        
        return "";
    }
    
    private boolean isVegetarian(Recette recipe) {
        String ingredients = recipe.ingredients != null ? recipe.ingredients.toLowerCase() : "";
        String[] meatKeywords = {"viande", "porc", "boeuf", "agneau", "poulet", "volaille", 
                               "jambon", "lard", "bacon", "saucisse", "chorizo"};
        
        for (String keyword : meatKeywords) {
            if (ingredients.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName;
        TextView recipeIngredients;
        TextView cookingTime;
        TextView vegetarianTag;
        ImageView favoriteIcon;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeName);
            recipeIngredients = itemView.findViewById(R.id.recipeIngredients);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            vegetarianTag = itemView.findViewById(R.id.vegetarianTag);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}
