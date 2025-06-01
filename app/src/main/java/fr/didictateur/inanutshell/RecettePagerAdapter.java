package fr.didictateur.inanutshell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RecettePagerAdapter extends FragmentStateAdapter {
    private final Fragment[] fragments;

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
}

