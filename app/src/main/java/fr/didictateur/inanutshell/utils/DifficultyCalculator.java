package fr.didictateur.inanutshell.utils;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.RecipeInstruction;
import fr.didictateur.inanutshell.ui.search.SearchFilters;

/**
 * Utilitaire pour calculer automatiquement le niveau de difficulté d'une recette
 * Basé sur différents facteurs comme le temps de préparation, nombre d'ingrédients, etc.
 */
public class DifficultyCalculator {
    
    /**
     * Calcule le niveau de difficulté d'une recette basé sur plusieurs facteurs
     */
    public static SearchFilters.DifficultyLevel calculateDifficulty(Recipe recipe) {
        int score = 0;
        
        // Facteur 1: Nombre d'ingrédients
        if (recipe.getRecipeIngredient() != null) {
            int ingredientCount = recipe.getRecipeIngredient().size();
            if (ingredientCount <= 5) {
                score += 1; // Facile
            } else if (ingredientCount <= 10) {
                score += 2; // Moyen
            } else {
                score += 3; // Difficile
            }
        } else {
            score += 2; // Par défaut moyen si pas d'info
        }
        
        // Facteur 2: Temps de préparation
        Integer prepTimeMinutes = parseTimeToMinutes(recipe.getPrepTime());
        if (prepTimeMinutes != null) {
            if (prepTimeMinutes <= 15) {
                score += 1; // Facile
            } else if (prepTimeMinutes <= 45) {
                score += 2; // Moyen
            } else {
                score += 3; // Difficile
            }
        } else {
            score += 2; // Par défaut moyen
        }
        
        // Facteur 3: Temps de cuisson
        Integer cookTimeMinutes = parseTimeToMinutes(recipe.getCookTime());
        if (cookTimeMinutes != null) {
            if (cookTimeMinutes <= 30) {
                score += 1; // Facile
            } else if (cookTimeMinutes <= 90) {
                score += 2; // Moyen
            } else {
                score += 3; // Difficile
            }
        } else {
            score += 1; // Par défaut facile si pas de cuisson
        }
        
        // Facteur 4: Nombre d'étapes de préparation
        if (recipe.getRecipeInstructions() != null) {
            int stepCount = recipe.getRecipeInstructions().size();
            if (stepCount <= 3) {
                score += 1; // Facile
            } else if (stepCount <= 8) {
                score += 2; // Moyen
            } else {
                score += 3; // Difficile
            }
        } else {
            score += 2; // Par défaut moyen
        }
        
        // Facteur 5: Complexité des techniques (analyse des mots-clés)
        score += analyzeCookingTechniques(recipe);
        
        // Facteur 6: Équipement spécialisé
        if (recipe.getTools() != null && !recipe.getTools().isEmpty()) {
            score += 1; // Un point supplémentaire pour équipement spécialisé
        }
        
        // Calcul final basé sur la moyenne pondérée
        double averageScore = (double) score / 6; // 6 facteurs
        
        if (averageScore <= 1.5) {
            return SearchFilters.DifficultyLevel.EASY;
        } else if (averageScore <= 2.5) {
            return SearchFilters.DifficultyLevel.MEDIUM;
        } else {
            return SearchFilters.DifficultyLevel.HARD;
        }
    }
    
    /**
     * Analyse les techniques de cuisine complexes dans la recette
     */
    private static int analyzeCookingTechniques(Recipe recipe) {
        String[] complexTechniques = {
            "flamber", "confit", "sous vide", "tempura", "clarifier", "monter au beurre",
            "émulsion", "sabayon", "pâte feuilletée", "pâte à choux", "ganache", "caramel",
            "napper", "julienne", "brunoise", "chiffonnade", "réduire", "déglacé",
            "marinade", "saumure", "fermentation", "levain", "pétrissage"
        };
        
        String[] easyTechniques = {
            "mélanger", "couper", "hacher", "éplucher", "râper", "faire bouillir",
            "faire cuire", "griller", "frire", "cuire au four", "réchauffer"
        };
        
        int complexityScore = 0;
        
        // Analyser les instructions
        if (recipe.getRecipeInstructions() != null) {
            for (RecipeInstruction instruction : recipe.getRecipeInstructions()) {
                String text = "";
                if (instruction.getText() != null) {
                    text += instruction.getText().toLowerCase();
                }
                if (instruction.getTitle() != null) {
                    text += " " + instruction.getTitle().toLowerCase();
                }
                
                // Rechercher des techniques complexes
                for (String technique : complexTechniques) {
                    if (text.contains(technique)) {
                        complexityScore += 2; // Technique complexe
                        break; // Une seule par instruction pour éviter les doublons
                    }
                }
                
                // Technique simple trouvée mais pas de complexe
                boolean hasComplexTechnique = false;
                for (String technique : complexTechniques) {
                    if (text.contains(technique)) {
                        hasComplexTechnique = true;
                        break;
                    }
                }
                
                if (!hasComplexTechnique) {
                    for (String technique : easyTechniques) {
                        if (text.contains(technique)) {
                            // Ne rien ajouter, c'est neutre
                            break;
                        }
                    }
                }
            }
        }
        
        // Analyser la description
        if (recipe.getDescription() != null) {
            String description = recipe.getDescription().toLowerCase();
            for (String technique : complexTechniques) {
                if (description.contains(technique)) {
                    complexityScore += 1;
                }
            }
        }
        
        // Limiter le score à un maximum de 3
        return Math.min(complexityScore, 3);
    }
    
    /**
     * Parse une chaîne de temps en minutes
     */
    private static Integer parseTimeToMinutes(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }
        
        try {
            // Formats supportés: "PT30M", "30 min", "1h30", "1h 30min", etc.
            timeString = timeString.toLowerCase().trim();
            
            // Format ISO 8601 (PT30M, PT1H30M)
            if (timeString.startsWith("pt")) {
                return parseISO8601Duration(timeString);
            }
            
            int minutes = 0;
            
            // Recherche des heures
            if (timeString.contains("h")) {
                String[] parts = timeString.split("h");
                if (parts.length > 0) {
                    try {
                        int hours = Integer.parseInt(parts[0].trim().replaceAll("[^0-9]", ""));
                        minutes += hours * 60;
                        
                        // Minutes après les heures
                        if (parts.length > 1) {
                            String minutePart = parts[1].trim().replaceAll("[^0-9]", "");
                            if (!minutePart.isEmpty()) {
                                minutes += Integer.parseInt(minutePart);
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else if (timeString.contains("min")) {
                // Format "30 min", "30min"
                String minutePart = timeString.replaceAll("[^0-9]", "");
                if (!minutePart.isEmpty()) {
                    minutes = Integer.parseInt(minutePart);
                }
            } else {
                // Essayer de parser comme nombre simple
                String numberPart = timeString.replaceAll("[^0-9]", "");
                if (!numberPart.isEmpty()) {
                    minutes = Integer.parseInt(numberPart);
                }
            }
            
            return minutes > 0 ? minutes : null;
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Parse un duration ISO 8601 (PT30M, PT1H30M)
     */
    private static Integer parseISO8601Duration(String duration) {
        try {
            duration = duration.toUpperCase();
            int hours = 0;
            int minutes = 0;
            
            // Extraire les heures
            if (duration.contains("H")) {
                String hourPart = duration.substring(duration.indexOf("PT") + 2, duration.indexOf("H"));
                hours = Integer.parseInt(hourPart);
            }
            
            // Extraire les minutes
            if (duration.contains("M")) {
                String minutePart;
                if (duration.contains("H")) {
                    minutePart = duration.substring(duration.indexOf("H") + 1, duration.indexOf("M"));
                } else {
                    minutePart = duration.substring(duration.indexOf("PT") + 2, duration.indexOf("M"));
                }
                minutes = Integer.parseInt(minutePart);
            }
            
            return (hours * 60) + minutes;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Assigne automatiquement la difficulté à une recette si elle n'en a pas
     */
    public static void assignDifficultyIfMissing(Recipe recipe) {
        if (recipe.getDifficulty() == null || recipe.getDifficulty() <= 0) {
            SearchFilters.DifficultyLevel difficulty = calculateDifficulty(recipe);
            recipe.setDifficulty(difficulty.getLevel());
        }
    }
    
    /**
     * Met à jour la difficulté de toutes les recettes d'une liste
     */
    public static void updateDifficultiesForRecipes(java.util.List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            assignDifficultyIfMissing(recipe);
        }
    }
    
    /**
     * Obtient une description textuelle de la difficulté
     */
    public static String getDifficultyDescription(Integer difficulty) {
        if (difficulty == null || difficulty <= 0) {
            return "Non défini";
        }
        
        SearchFilters.DifficultyLevel level = SearchFilters.DifficultyLevel.fromLevel(difficulty);
        return level != null ? level.getDisplayName() : "Non défini";
    }
}
