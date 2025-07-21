package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class IngredientsFragment extends Fragment {
    private EditText editIngredients;
    private String pendingIngredients;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingredients, container, false);
        editIngredients = view.findViewById(R.id.editIngredients);
        
        // Appliquer les données en attente si elles existent
        if (pendingIngredients != null) {
            editIngredients.setText(pendingIngredients);
            pendingIngredients = null;
        }
        
        return view;
    }

    public String getIngredients() {
        return editIngredients != null ? editIngredients.getText().toString() : "";
    }
    
    public void setIngredients(String ingredients) {
        if (editIngredients != null && ingredients != null) {
            editIngredients.setText(ingredients);
        } else {
            pendingIngredients = ingredients;
        }
    }
}

