package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ApercuFragment extends Fragment {
    private EditText editTitre, editTaille, editTempsPrep;
    private String pendingTitre, pendingTaille, pendingTempsPrep;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apercu, container, false);
        editTitre = view.findViewById(R.id.editTitre);
        editTaille = view.findViewById(R.id.editTaille);
        editTempsPrep = view.findViewById(R.id.editTempsPrep);
        
        // Appliquer les données en attente si elles existent
        if (pendingTitre != null) {
            editTitre.setText(pendingTitre);
            pendingTitre = null;
        }
        if (pendingTaille != null) {
            editTaille.setText(pendingTaille);
            pendingTaille = null;
        }
        if (pendingTempsPrep != null) {
            editTempsPrep.setText(pendingTempsPrep);
            pendingTempsPrep = null;
        }
        
        return view;
    }

    public String getTitre() {
        return editTitre != null ? editTitre.getText().toString() : "";
    }
    public String getTaille() {
        return editTaille != null ? editTaille.getText().toString() : "";
    }
    public String getTempsPrep() {
        return editTempsPrep != null ? editTempsPrep.getText().toString() : "";
    }
    
    public void setTitre(String titre) {
        if (editTitre != null && titre != null) {
            editTitre.setText(titre);
        } else {
            pendingTitre = titre;
        }
    }
    
    public void setTaille(String taille) {
        if (editTaille != null && taille != null) {
            editTaille.setText(taille);
        } else {
            pendingTaille = taille;
        }
    }
    
    public void setTempsPrep(String tempsPrep) {
        if (editTempsPrep != null && tempsPrep != null) {
            editTempsPrep.setText(tempsPrep);
        } else {
            pendingTempsPrep = tempsPrep;
        }
    }
}

