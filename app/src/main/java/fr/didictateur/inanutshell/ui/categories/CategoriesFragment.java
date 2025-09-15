package fr.didictateur.inanutshell.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.FragmentCategoriesBinding;

public class CategoriesFragment extends Fragment {
    
    private FragmentCategoriesBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_categories, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // TODO: Implement categories functionality
        binding.tvPlaceholder.setText("Catégories - À venir");
    }
}
