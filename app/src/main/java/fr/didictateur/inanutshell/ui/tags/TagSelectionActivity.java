package fr.didictateur.inanutshell.ui.tags;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.flexbox.FlexboxLayoutManager;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityTagSelectionBinding;
import fr.didictateur.inanutshell.data.manager.CategoryTagManager;
import fr.didictateur.inanutshell.data.model.Tag;
import fr.didictateur.inanutshell.ui.adapter.TagAdapter;
import java.util.List;
import java.util.ArrayList;

public class TagSelectionActivity extends AppCompatActivity implements TagAdapter.OnTagClickListener {
    
    private static final String TAG_LOG = "TagSelectionActivity";
    public static final String EXTRA_SELECTED_TAGS = "selected_tags";
    public static final String EXTRA_RESULT_TAGS = "result_tags";
    
    private ActivityTagSelectionBinding binding;
    private TagAdapter tagAdapter;
    private CategoryTagManager categoryTagManager;
    private List<Tag> selectedTags = new ArrayList<>();
    private List<Tag> allTags = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        loadInitialTags();
        loadTags();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.tag_selection);
        }
        
        binding.btnDone.setOnClickListener(v -> finishWithResult());
        
        // Gestion de la création de nouveaux tags
        binding.btnCreateTag.setOnClickListener(v -> createNewTag());
        
        // Création par appui sur "Enter"
        binding.etNewTag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                createNewTag();
                return true;
            }
            return false;
        });
    }
    
    private void setupRecyclerView() {
        categoryTagManager = CategoryTagManager.getInstance();
        tagAdapter = new TagAdapter(this);
        tagAdapter.setSelectionMode(true);
        
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        binding.recyclerViewTags.setLayoutManager(layoutManager);
        binding.recyclerViewTags.setAdapter(tagAdapter);
    }
    
    private void loadInitialTags() {
        // Récupérer les tags déjà sélectionnés depuis l'intent
        if (getIntent().hasExtra(EXTRA_SELECTED_TAGS)) {
            ArrayList<String> tagIds = getIntent().getStringArrayListExtra(EXTRA_SELECTED_TAGS);
            if (tagIds != null) {
                for (String tagId : tagIds) {
                    Tag tag = categoryTagManager.getTagById(tagId);
                    if (tag != null) {
                        selectedTags.add(tag);
                    }
                }
            }
        }
    }
    
    private void loadTags() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        
        categoryTagManager.getTags(new CategoryTagManager.TagsCallback() {
            @Override
            public void onSuccess(List<Tag> tags) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    allTags = new ArrayList<>(tags);
                    tagAdapter.setTags(tags);
                    tagAdapter.setSelectedTags(selectedTags);
                    updateDoneButton();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    Log.e(TAG_LOG, "Erreur lors du chargement des tags: " + error);
                    // TODO: Afficher un message d'erreur à l'utilisateur
                });
            }
        });
    }
    
    @Override
    public void onTagClick(Tag tag) {
        // Pas utilisé en mode sélection
    }
    
    @Override
    public void onTagSelectionChanged(List<Tag> selectedTags) {
        this.selectedTags = selectedTags;
        updateDoneButton();
    }
    
    private void updateDoneButton() {
        binding.btnDone.setText(getString(R.string.done) + 
                (selectedTags.isEmpty() ? "" : " (" + selectedTags.size() + ")"));
    }
    
    private void createNewTag() {
        String tagName = binding.etNewTag.getText().toString().trim();
        
        if (TextUtils.isEmpty(tagName)) {
            binding.etNewTag.setError("Le nom du tag ne peut pas être vide");
            return;
        }
        
        // Vérifier si le tag existe déjà
        for (Tag existingTag : allTags) {
            if (existingTag.getName().equalsIgnoreCase(tagName)) {
                binding.etNewTag.setError("Ce tag existe déjà");
                return;
            }
        }
        
        // Désactiver le bouton pendant la création
        binding.btnCreateTag.setEnabled(false);
        binding.btnCreateTag.setText("Création...");
        
        categoryTagManager.createTag(tagName, new CategoryTagManager.TagCreateCallback() {
            @Override
            public void onSuccess(Tag tag) {
                runOnUiThread(() -> {
                    // Ajouter le nouveau tag à la liste
                    allTags.add(tag);
                    
                    // Sélectionner automatiquement le nouveau tag
                    selectedTags.add(tag);
                    
                    // Mettre à jour l'adapter
                    tagAdapter.setTags(allTags);
                    tagAdapter.setSelectedTags(selectedTags);
                    
                    // Vider le champ de texte
                    binding.etNewTag.setText("");
                    binding.tilNewTag.setError(null);
                    
                    // Réactiver le bouton
                    binding.btnCreateTag.setEnabled(true);
                    binding.btnCreateTag.setText(R.string.create);
                    
                    updateDoneButton();
                    
                    Log.d(TAG_LOG, "Nouveau tag créé et sélectionné: " + tag.getName());
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.etNewTag.setError("Erreur: " + error);
                    binding.btnCreateTag.setEnabled(true);
                    binding.btnCreateTag.setText(R.string.create);
                    Log.e(TAG_LOG, "Erreur lors de la création du tag: " + error);
                });
            }
        });
    }
    
    private void finishWithResult() {
        Intent resultIntent = new Intent();
        ArrayList<String> tagIds = new ArrayList<>();
        for (Tag tag : selectedTags) {
            tagIds.add(tag.getId());
        }
        resultIntent.putStringArrayListExtra(EXTRA_RESULT_TAGS, tagIds);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
