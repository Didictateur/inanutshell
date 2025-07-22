package fr.didictateur.inanutshell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagManager {
    
    // Tags prédéfinis
    public static final String[] DEFAULT_TAGS = {
        "Rapide", "Facile", "Difficile", "Végétarien", "Végétalien", 
        "Sans gluten", "Dessert", "Entrée", "Plat principal", 
        "Apéritif", "Petit-déjeuner", "Déjeuner", "Dîner",
        "Cuisine française", "Cuisine italienne", "Cuisine asiatique",
        "Économique", "Gastronomique", "Healthy", "Comfort food"
    };
    
    public static List<String> getDefaultTags() {
        return Arrays.asList(DEFAULT_TAGS);
    }
    
    // Extraire les tags automatiquement depuis le contenu
    public static Set<String> extractAutoTags(Recette recette) {
        Set<String> autoTags = new HashSet<>();
        
        // Analyser le temps de préparation
        if (recette.tempsPrep != null && !recette.tempsPrep.isEmpty()) {
            try {
                String cleanTime = recette.tempsPrep.replaceAll("[^0-9]", "");
                if (!cleanTime.isEmpty()) {
                    int minutes = Integer.parseInt(cleanTime);
                    if (minutes <= 15) {
                        autoTags.add("Rapide");
                    } else if (minutes >= 120) {
                        autoTags.add("Long");
                    }
                }
            } catch (NumberFormatException e) {
                // Ignorer si impossible de parser
            }
        }
        
        // Analyser les ingrédients pour détecter le type
        String ingredients = recette.ingredients != null ? recette.ingredients.toLowerCase() : "";
        String preparation = recette.preparation != null ? recette.preparation.toLowerCase() : "";
        String allText = (ingredients + " " + preparation).toLowerCase();
        
        // Détection végétarien/végétalien
        if (!containsAnimal(allText)) {
            if (!containsDairy(allText)) {
                autoTags.add("Végétalien");
            } else {
                autoTags.add("Végétarien");
            }
        }
        
        // Détection sans gluten
        if (!containsGluten(allText)) {
            autoTags.add("Sans gluten");
        }
        
        // Détection type de plat
        if (containsDessertKeywords(allText)) {
            autoTags.add("Dessert");
        }
        
        return autoTags;
    }
    
    private static boolean containsAnimal(String text) {
        String[] animalKeywords = {"viande", "porc", "bœuf", "boeuf", "agneau", 
                                 "poulet", "poisson", "crevette", "jambon", "bacon"};
        for (String keyword : animalKeywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
    
    private static boolean containsDairy(String text) {
        String[] dairyKeywords = {"lait", "fromage", "beurre", "crème", "yaourt", "yogourt"};
        for (String keyword : dairyKeywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
    
    private static boolean containsGluten(String text) {
        String[] glutenKeywords = {"farine", "pain", "pâtes", "blé", "orge", "seigle"};
        for (String keyword : glutenKeywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
    
    private static boolean containsDessertKeywords(String text) {
        String[] dessertKeywords = {"sucre", "chocolat", "gâteau", "tarte", "mousse", 
                                   "crème", "dessert", "sweet"};
        for (String keyword : dessertKeywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
    
    // Recherche par tags
    public static boolean recipeMatchesTags(Recette recette, Set<String> searchTags) {
        if (searchTags.isEmpty()) return true;
        
        Set<String> recipeTags = extractAutoTags(recette);
        
        // Chercher aussi dans le titre et les ingrédients
        String searchText = (recette.titre + " " + recette.ingredients + " " + recette.preparation).toLowerCase();
        
        for (String tag : searchTags) {
            if (recipeTags.contains(tag) || searchText.contains(tag.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}
