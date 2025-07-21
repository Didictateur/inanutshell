package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NotesFragment extends Fragment {
    private EditText editNotes;
    private String pendingNotes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        editNotes = view.findViewById(R.id.editNotes);
        
        // Appliquer les données en attente si elles existent
        if (pendingNotes != null) {
            editNotes.setText(pendingNotes);
            pendingNotes = null;
        }
        
        return view;
    }

    public String getNotes() {
        return editNotes != null ? editNotes.getText().toString() : "";
    }
    
    public void setNotes(String notes) {
        if (editNotes != null && notes != null) {
            editNotes.setText(notes);
        } else {
            pendingNotes = notes;
        }
    }
}

