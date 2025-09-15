package fr.didictateur.inanutshell.ui.search;

/**
 * Interface pour communiquer les filtres de recherche
 */
public interface SearchFilterListener {
    
    /**
     * Appelé quand les filtres de recherche changent
     * @param filters Les nouveaux filtres
     */
    void onFiltersChanged(SearchFilters filters);
    
    /**
     * Appelé quand la recherche doit être réinitialisée
     */
    void onClearFilters();
}
