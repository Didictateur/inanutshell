package fr.didictateur.inanutshell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private FragmentManager fragmentManager;
    
    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentManager = fragmentActivity.getSupportFragmentManager();
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new RecipesFragment();
            case 1:
                return new MealPlannerFragment();
            default:
                return new RecipesFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return 2; // Deux onglets : Recettes et Planificateur
    }
    
    public Fragment getFragment(int position) {
        String fragmentTag = "f" + getItemId(position);
        return fragmentManager.findFragmentByTag(fragmentTag);
    }
}
