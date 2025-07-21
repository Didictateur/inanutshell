package fr.didictateur.inanutshell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RecettePagerAdapter extends FragmentStateAdapter {
    private final Fragment[] fragments;
    private Recette recetteData; // Pour stocker les données temporairement

    public RecettePagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
        fragments = new Fragment[] {
            new ApercuFragment(),
            new IngredientsFragment(),
            new PreparationFragment(),
            new NotesFragment()
        };
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }

    // Pour accéder aux fragments depuis l'activité
    public ApercuFragment getApercuFragment() { return (ApercuFragment) fragments[0]; }
    public IngredientsFragment getIngredientsFragment() { return (IngredientsFragment) fragments[1]; }
    public PreparationFragment getPreparationFragment() { return (PreparationFragment) fragments[2]; }
    public NotesFragment getNotesFragment() { return (NotesFragment) fragments[3]; }
    
    // Pour stocker et appliquer les données de recette
    public void setRecetteData(Recette recette) {
        this.recetteData = recette;
        // Essayer d'appliquer les données si les fragments sont déjà créés
        applyRecetteDataIfPossible();
    }
    
    private void applyRecetteDataIfPossible() {
        if (recetteData != null) {
            try {
                ApercuFragment apercuFragment = getApercuFragment();
                if (apercuFragment != null && apercuFragment.getView() != null) {
                    apercuFragment.setTitre(recetteData.titre);
                    apercuFragment.setTaille(recetteData.taille);
                    apercuFragment.setTempsPrep(recetteData.tempsPrep);
                }
                
                IngredientsFragment ingredientsFragment = getIngredientsFragment();
                if (ingredientsFragment != null && ingredientsFragment.getView() != null) {
                    ingredientsFragment.setIngredients(recetteData.ingredients);
                }
                
                PreparationFragment preparationFragment = getPreparationFragment();
                if (preparationFragment != null && preparationFragment.getView() != null) {
                    preparationFragment.setPreparation(recetteData.preparation);
                }
                
                NotesFragment notesFragment = getNotesFragment();
                if (notesFragment != null && notesFragment.getView() != null) {
                    notesFragment.setNotes(recetteData.notes);
                }
            } catch (Exception e) {
                // Les fragments ne sont pas encore prêts, on réessaiera plus tard
            }
        }
    }
}

