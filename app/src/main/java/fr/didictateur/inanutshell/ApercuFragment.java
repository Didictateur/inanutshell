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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apercu, container, false);
        editTitre = view.findViewById(R.id.editTitre);
        editTaille = view.findViewById(R.id.editTaille);
        editTempsPrep = view.findViewById(R.id.editTempsPrep);
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
}

