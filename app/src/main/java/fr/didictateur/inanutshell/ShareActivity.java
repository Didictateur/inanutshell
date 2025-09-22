package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.service.ShareService;
import fr.didictateur.inanutshell.adapter.ShareOptionsAdapter;
import fr.didictateur.inanutshell.data.model.ShareOption;

public class ShareActivity extends AppCompatActivity implements ShareOptionsAdapter.OnShareOptionClickListener {
    
    public static final String EXTRA_RECIPE = "extra_recipe";
    
    private Recipe recipe;
    private ShareService shareService;
    private RecyclerView shareOptionsRecyclerView;
    private ShareOptionsAdapter shareOptionsAdapter;
    
    // Vues pour l'aperçu de la recette
    private TextView recipeTitleText;
    private TextView recipeDescriptionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        
        // Récupération de la recette depuis l'Intent
        recipe = (Recipe) getIntent().getSerializableExtra(EXTRA_RECIPE);
        if (recipe == null) {
            Toast.makeText(this, "Erreur: recette non trouvée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        shareService = ShareService.getInstance(this);
        
        initViews();
        setupToolbar();
        setupRecipePreview();
        setupShareOptions();
    }
    
    private void initViews() {
        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Aperçu de la recette (on utilise les IDs du layout existant)
        recipeTitleText = findViewById(R.id.recipe_title);
        recipeDescriptionText = findViewById(R.id.recipe_description);
        
        // RecyclerView pour les options de partage
        shareOptionsRecyclerView = findViewById(R.id.share_options_recycler_view);
        shareOptionsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Partager la recette");
        }
    }
    
    private void setupRecipePreview() {
        recipeTitleText.setText(recipe.getName());
        
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            recipeDescriptionText.setText(recipe.getDescription());
            recipeDescriptionText.setVisibility(View.VISIBLE);
        } else {
            recipeDescriptionText.setVisibility(View.GONE);
        }
    }
    
    private void setupShareOptions() {
        List<ShareOption> shareOptions = createShareOptions();
        shareOptionsAdapter = new ShareOptionsAdapter(shareOptions, this);
        shareOptionsRecyclerView.setAdapter(shareOptionsAdapter);
    }
    
    private List<ShareOption> createShareOptions() {
        List<ShareOption> options = new ArrayList<>();
        
        options.add(new ShareOption(
            "text", 
            "Texte", 
            "Partager comme texte", 
            R.drawable.ic_text_fields
        ));
        
        options.add(new ShareOption(
            "image", 
            "Image", 
            "Partager comme image", 
            R.drawable.ic_image
        ));
        
        options.add(new ShareOption(
            "qr", 
            "QR Code", 
            "Générer un QR Code", 
            R.drawable.ic_qr_code
        ));
        
        options.add(new ShareOption(
            "link", 
            "Lien", 
            "Partager un lien", 
            R.drawable.ic_link
        ));
        
        options.add(new ShareOption(
            "whatsapp", 
            "WhatsApp", 
            "Partager sur WhatsApp", 
            R.drawable.ic_whatsapp
        ));
        
        options.add(new ShareOption(
            "facebook", 
            "Facebook", 
            "Partager sur Facebook", 
            R.drawable.ic_facebook
        ));
        
        options.add(new ShareOption(
            "twitter", 
            "Twitter", 
            "Partager sur Twitter", 
            R.drawable.ic_twitter
        ));
        
        options.add(new ShareOption(
            "instagram", 
            "Instagram", 
            "Partager sur Instagram", 
            R.drawable.ic_instagram
        ));
        
        return options;
    }
    
    @Override
    public void onShareOptionClick(ShareOption option) {
        try {
            switch (option.getId()) {
                case "text":
                    shareService.shareAsText(recipe);
                    break;
                case "image":
                    shareService.shareAsImage(recipe);
                    break;
                case "qr":
                    shareService.shareAsQRCode(recipe);
                    break;
                case "link":
                    shareService.shareAsLink(recipe);
                    break;
                case "whatsapp":
                    shareService.shareViaWhatsApp(recipe);
                    break;
                case "facebook":
                    shareService.shareViaFacebook(recipe);
                    break;
                case "twitter":
                    shareService.shareViaTwitter(recipe);
                    break;
                case "instagram":
                    shareService.shareViaInstagram(recipe);
                    break;
                default:
                    Toast.makeText(this, "Option de partage non supportée", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du partage: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
