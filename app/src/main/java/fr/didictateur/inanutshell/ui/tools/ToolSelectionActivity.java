package fr.didictateur.inanutshell.ui.tools;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.flexbox.FlexboxLayoutManager;
import fr.didictateur.inanutshell.databinding.ActivityToolSelectionBinding;
import fr.didictateur.inanutshell.utils.ToolsManager;
import fr.didictateur.inanutshell.data.model.Tool;
import fr.didictateur.inanutshell.ui.adapter.ToolAdapter;
import java.util.List;
import java.util.ArrayList;

public class ToolSelectionActivity extends AppCompatActivity implements ToolAdapter.OnToolClickListener {
    
    private static final String TAG_LOG = "ToolSelectionActivity";
    public static final String EXTRA_SELECTED_TOOLS = "selected_tools";
    public static final String EXTRA_RESULT_TOOLS = "result_tools";
    
    private ActivityToolSelectionBinding binding;
    private ToolAdapter toolAdapter;
    private ToolsManager toolsManager;
    private List<Tool> selectedTools = new ArrayList<>();
    private List<Tool> allTools = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityToolSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        loadInitialTools();
        loadTools();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sélection des outils");
        }
        
        binding.btnDone.setOnClickListener(v -> finishWithResult());
    }
    
    private void setupRecyclerView() {
        toolsManager = ToolsManager.getInstance(this);
        toolAdapter = new ToolAdapter(this);
        toolAdapter.setSelectionMode(true);
        
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        binding.recyclerViewTools.setLayoutManager(layoutManager);
        binding.recyclerViewTools.setAdapter(toolAdapter);
    }
    
    private void loadInitialTools() {
        // Récupérer les outils déjà sélectionnés depuis l'intent
        if (getIntent().hasExtra(EXTRA_SELECTED_TOOLS)) {
            ArrayList<String> toolIds = getIntent().getStringArrayListExtra(EXTRA_SELECTED_TOOLS);
            if (toolIds != null) {
                // Pour l'instant on ne peut pas récupérer les outils par ID depuis le cache
                // On les récupérera lors du chargement complet
            }
        }
    }
    
    private void loadTools() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        
        toolsManager.getTools(new ToolsManager.ToolsCallback() {
            @Override
            public void onSuccess(List<Tool> tools) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    allTools = new ArrayList<>(tools);
                    toolAdapter.setTools(tools);
                    
                    // Maintenant qu'on a tous les outils, on peut récupérer les sélectionnés
                    loadSelectedToolsFromIntent();
                    
                    toolAdapter.setSelectedTools(selectedTools);
                    updateDoneButton();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    Log.e(TAG_LOG, "Erreur lors du chargement des outils: " + error);
                    // TODO: Afficher un message d'erreur à l'utilisateur
                });
            }
        });
    }
    
    private void loadSelectedToolsFromIntent() {
        if (getIntent().hasExtra(EXTRA_SELECTED_TOOLS)) {
            ArrayList<String> toolIds = getIntent().getStringArrayListExtra(EXTRA_SELECTED_TOOLS);
            if (toolIds != null) {
                for (String toolId : toolIds) {
                    for (Tool tool : allTools) {
                        if (tool.getId().equals(toolId)) {
                            selectedTools.add(tool);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onToolClick(Tool tool) {
        // Pas utilisé en mode sélection
    }
    
    @Override
    public void onToolSelectionChanged(List<Tool> selectedTools) {
        this.selectedTools = selectedTools;
        updateDoneButton();
    }
    
    private void updateDoneButton() {
        binding.btnDone.setText("Terminé" + 
                (selectedTools.isEmpty() ? "" : " (" + selectedTools.size() + ")"));
    }
    
    private void finishWithResult() {
        Intent resultIntent = new Intent();
        ArrayList<String> toolIds = new ArrayList<>();
        for (Tool tool : selectedTools) {
            toolIds.add(tool.getId());
        }
        resultIntent.putStringArrayListExtra(EXTRA_RESULT_TOOLS, toolIds);
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
