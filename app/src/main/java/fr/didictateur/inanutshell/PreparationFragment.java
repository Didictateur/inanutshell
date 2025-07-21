package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PreparationFragment extends Fragment {
    private EditText editPreparation;
    private String pendingPreparation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preparation, container, false);
        editPreparation = view.findViewById(R.id.editPreparation);
        
        // Appliquer les données en attente si elles existent
        if (pendingPreparation != null) {
            editPreparation.setText(pendingPreparation);
            pendingPreparation = null;
        }
        
        return view;
    }

    public String getPreparation() {
        return editPreparation != null ? editPreparation.getText().toString() : "";
    }
    
    public void setPreparation(String preparation) {
        if (editPreparation != null && preparation != null) {
            editPreparation.setText(preparation);
        } else {
            pendingPreparation = preparation;
        }
    }
}

