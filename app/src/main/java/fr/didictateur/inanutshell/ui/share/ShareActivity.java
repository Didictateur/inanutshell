package fr.didictateur.inanutshell.ui.share;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityShareBinding;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.services.ShareService;

/**
 * Activité de partage avancé pour les recettes
 */
public class ShareActivity extends AppCompatActivity implements ShareOptionsAdapter.OnShareOptionClickListener {
    
    private ActivityShareBinding binding;
    private Recipe recipe;
    private ShareService shareService;
    private ShareOptionsAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        loadRecipe();
        setupShareService();
        setupShareOptions();
        setupRecipePreview();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Partager la recette");
        }
    }
    
    private void loadRecipe() {
        // Récupérer la recette depuis l'intent
        long recipeId = getIntent().getLongExtra("recipe_id", -1);
        if (recipeId != -1) {
            // TODO: Charger la recette depuis la base de données
            // Pour l'instant, création d'une recette d'exemple
            recipe = createSampleRecipe();
        }
        
        if (recipe == null) {
            finish();
        }
    }
    
    private void setupShareService() {
        shareService = new ShareService(this);
    }
    
    private void setupShareOptions() {
        List<ShareOption> options = createShareOptions();
        
        adapter = new ShareOptionsAdapter(options, this);
        binding.shareOptionsRecyclerView.setAdapter(adapter);
        binding.shareOptionsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    private void setupRecipePreview() {
        if (recipe != null) {
            binding.recipeTitle.setText(recipe.getName());
            binding.recipeDescription.setText(recipe.getDescription());
            
            // Afficher les informations de base
            StringBuilder info = new StringBuilder();
            if (recipe.getPrepTime() != null) {
                info.append("⏱️ Préparation: ").append(recipe.getPrepTime()).append("\\n");
            }
            if (recipe.getCookTime() != null) {
                info.append("🔥 Cuisson: ").append(recipe.getCookTime()).append("\\n");
            }
            if (recipe.getYield() != null) {
                info.append("👥 Portions: ").append(recipe.getYield());
            }
            
            binding.recipeInfo.setText(info.toString());
        }
    }
    
    private List<ShareOption> createShareOptions() {
        List<ShareOption> options = new ArrayList<>();
        
        options.add(new ShareOption(
            "Texte",
            "Partager comme texte formaté",
            R.drawable.ic_text_fields,
            ShareOption.Type.TEXT
        ));
        
        options.add(new ShareOption(
            "Image",
            "Créer et partager une image",
            R.drawable.ic_image,
            ShareOption.Type.IMAGE
        ));
        
        options.add(new ShareOption(
            "QR Code",
            "Générer un QR code",
            R.drawable.ic_qr_code,
            ShareOption.Type.QR_CODE
        ));
        
        options.add(new ShareOption(
            "Lien public",
            "Créer un lien partageable",
            R.drawable.ic_link,
            ShareOption.Type.PUBLIC_LINK
        ));
        
        options.add(new ShareOption(
            "WhatsApp",
            "Partager sur WhatsApp",
            R.drawable.ic_whatsapp,
            ShareOption.Type.WHATSAPP
        ));
        
        options.add(new ShareOption(
            "Facebook",
            "Partager sur Facebook",
            R.drawable.ic_facebook,
            ShareOption.Type.FACEBOOK
        ));
        
        options.add(new ShareOption(
            "Twitter",
            "Partager sur Twitter",
            R.drawable.ic_twitter,
            ShareOption.Type.TWITTER
        ));
        
        options.add(new ShareOption(
            "Instagram",
            "Partager sur Instagram",
            R.drawable.ic_instagram,
            ShareOption.Type.INSTAGRAM
        ));
        
        return options;
    }
    
    @Override
    public void onShareOptionClick(ShareOption option) {
        if (recipe == null) {
            Toast.makeText(this, "Erreur: recette introuvable", Toast.LENGTH_SHORT).show();
            return;
        }
        
        switch (option.getType()) {
            case TEXT:
                shareService.shareAsText(recipe);
                break;
                
            case IMAGE:
                binding.loadingIndicator.setVisibility(android.view.View.VISIBLE);
                // Exécuter en arrière-plan
                new Thread(() -> {
                    shareService.shareAsImage(recipe);
                    runOnUiThread(() -> binding.loadingIndicator.setVisibility(android.view.View.GONE));
                }).start();
                break;
                
            case QR_CODE:
                binding.loadingIndicator.setVisibility(android.view.View.VISIBLE);
                new Thread(() -> {
                    shareService.shareAsQRCode(recipe);
                    runOnUiThread(() -> binding.loadingIndicator.setVisibility(android.view.View.GONE));
                }).start();
                break;
                
            case PUBLIC_LINK:
                String publicLink = shareService.generatePublicLink(recipe);
                copyToClipboard("Lien de la recette", publicLink);
                Toast.makeText(this, "Lien copié dans le presse-papier", Toast.LENGTH_SHORT).show();
                break;
                
            case WHATSAPP:
                shareService.shareOnSocialNetwork(recipe, ShareService.SocialNetwork.WHATSAPP);
                break;
                
            case FACEBOOK:
                shareService.shareOnSocialNetwork(recipe, ShareService.SocialNetwork.FACEBOOK);
                break;
                
            case TWITTER:
                shareService.shareOnSocialNetwork(recipe, ShareService.SocialNetwork.TWITTER);
                break;
                
            case INSTAGRAM:
                shareService.shareOnSocialNetwork(recipe, ShareService.SocialNetwork.INSTAGRAM);
                break;
        }
    }
    
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }
    
    private Recipe createSampleRecipe() {
        // TODO: Remplacer par le chargement réel depuis la base de données
        Recipe sample = new Recipe();
        sample.setId("1");
        sample.setName("Pasta Carbonara");
        sample.setDescription("Un classique italien délicieux et crémeux");
        sample.setPrepTime("15 min");
        sample.setCookTime("20 min");
        sample.setYield("4 portions");
        return sample;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
