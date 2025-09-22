package fr.didictateur.inanutshell.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;

/**
 * Service de partage avancé pour les recettes
 * Gère l'export en multiple formats : PDF, image, texte, QR codes
 */
public class ShareService {
    private static final String TAG = "ShareService";
    private final Context context;
    
    public ShareService(Context context) {
        this.context = context;
    }
    
    /**
     * Partage une recette sous forme de texte formaté
     */
    public void shareAsText(Recipe recipe) {
        String recipeText = formatRecipeAsText(recipe);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
        
        Intent chooser = Intent.createChooser(shareIntent, "Partager la recette");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }
    
    /**
     * Crée et partage une image de la recette
     */
    public void shareAsImage(Recipe recipe) {
        try {
            Bitmap recipeBitmap = createRecipeImage(recipe);
            File imageFile = saveImageToCache(recipeBitmap, recipe.getName());
            
            Uri imageUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                imageFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(shareIntent, "Partager l'image de la recette");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du partage d'image", e);
        }
    }
    
    /**
     * Génère un QR code contenant l'URL ou les informations de la recette
     */
    public void shareAsQRCode(Recipe recipe) {
        try {
            String qrContent = createQRContent(recipe);
            Bitmap qrBitmap = generateQRCode(qrContent, 512, 512);
            
            File qrFile = saveImageToCache(qrBitmap, recipe.getName() + "_QR");
            
            Uri qrUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                qrFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, qrUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code - Recette: " + recipe.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(shareIntent, "Partager le QR Code");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création du QR Code", e);
        }
    }
    
    /**
     * Partage sur les réseaux sociaux spécifiques
     */
    public void shareOnSocialNetwork(Recipe recipe, SocialNetwork network) {
        String recipeText = formatRecipeForSocial(recipe);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
        
        switch (network) {
            case FACEBOOK:
                shareIntent.setPackage("com.facebook.katana");
                break;
            case TWITTER:
                shareIntent.setPackage("com.twitter.android");
                break;
            case INSTAGRAM:
                shareIntent.setPackage("com.instagram.android");
                break;
            case WHATSAPP:
                shareIntent.setPackage("com.whatsapp");
                break;
        }
        
        try {
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
        } catch (Exception e) {
            // Fallback vers partage générique si l'app n'est pas installée
            shareAsText(recipe);
        }
    }
    
    /**
     * Crée un lien public partageable (simule une fonctionnalité web)
     */
    public String generatePublicLink(Recipe recipe) {
        // Dans un vrai cas, ceci ferait appel à un serveur web
        String baseUrl = "https://mealie.share/recipe/";
        String recipeId = String.valueOf(recipe.getId());
        return baseUrl + recipeId;
    }
    
    /**
     * Formate la recette en texte lisible
     */
    private String formatRecipeAsText(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("🍽️ ").append(recipe.getName()).append("\n\n");
        
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            sb.append("📝 Description:\n").append(recipe.getDescription()).append("\n\n");
        }
        
        // Informations de base
        sb.append("⏱️ Informations:\n");
        if (recipe.getPrepTime() != null) {
            sb.append("• Préparation: ").append(recipe.getPrepTime()).append("\n");
        }
        if (recipe.getCookTime() != null) {
            sb.append("• Cuisson: ").append(recipe.getCookTime()).append("\n");
        }
        if (recipe.getTotalTime() != null) {
            sb.append("• Total: ").append(recipe.getTotalTime()).append("\n");
        }
        sb.append("\n");
        
        // Ingrédients
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            sb.append("🥕 Ingrédients:\n");
            for (RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                sb.append("• ").append(ingredient.getDisplay() != null ? 
                    ingredient.getDisplay() : ingredient.getOriginalText()).append("\n");
            }
            sb.append("\n");
        }
        
        // Instructions
        if (recipe.getRecipeInstructions() != null && !recipe.getRecipeInstructions().isEmpty()) {
            sb.append("👩‍🍳 Instructions:\n");
            for (int i = 0; i < recipe.getRecipeInstructions().size(); i++) {
                sb.append((i + 1)).append(". ")
                  .append(recipe.getRecipeInstructions().get(i).getText()).append("\n");
            }
        }
        
        sb.append("\n📱 Partagé depuis iNutshell - ").append(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        
        return sb.toString();
    }
    
    /**
     * Crée une image formatée de la recette
     */
    private Bitmap createRecipeImage(Recipe recipe) {
        int width = 800;
        int height = 1200;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fond
        canvas.drawColor(Color.WHITE);
        
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(48);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setAntiAlias(true);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(36);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        headerPaint.setAntiAlias(true);
        
        float currentY = 80;
        
        // Titre
        canvas.drawText("🍽️ " + recipe.getName(), 40, currentY, titlePaint);
        currentY += 100;
        
        // Description
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            canvas.drawText("Description:", 40, currentY, headerPaint);
            currentY += 50;
            
            String[] descLines = wrapText(recipe.getDescription(), 25);
            for (String line : descLines) {
                canvas.drawText(line, 40, currentY, textPaint);
                currentY += 40;
            }
            currentY += 30;
        }
        
        // Temps
        canvas.drawText("⏱️ Temps:", 40, currentY, headerPaint);
        currentY += 50;
        
        if (recipe.getPrepTime() != null) {
            canvas.drawText("Préparation: " + recipe.getPrepTime(), 40, currentY, textPaint);
            currentY += 40;
        }
        
        if (recipe.getCookTime() != null) {
            canvas.drawText("Cuisson: " + recipe.getCookTime(), 40, currentY, textPaint);
            currentY += 40;
        }
        
        return bitmap;
    }
    
    /**
     * Génère un QR code
     */
    private Bitmap generateQRCode(String content, int width, int height) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
    
    /**
     * Sauvegarde une image dans le cache
     */
    private File saveImageToCache(Bitmap bitmap, String filename) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "shared_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        File imageFile = new File(cacheDir, filename + "_" + System.currentTimeMillis() + ".png");
        
        FileOutputStream out = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
        
        return imageFile;
    }
    
    /**
     * Crée le contenu pour le QR code
     */
    private String createQRContent(Recipe recipe) {
        return "RECIPE:" + recipe.getName() + 
               "|DESC:" + (recipe.getDescription() != null ? recipe.getDescription() : "") +
               "|URL:" + generatePublicLink(recipe);
    }
    
    /**
     * Formate pour réseaux sociaux (version courte)
     */
    private String formatRecipeForSocial(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("🍽️ ").append(recipe.getName()).append("\n\n");
        
        if (recipe.getDescription() != null && recipe.getDescription().length() > 0) {
            String shortDesc = recipe.getDescription().length() > 100 ? 
                recipe.getDescription().substring(0, 100) + "..." : recipe.getDescription();
            sb.append(shortDesc).append("\n\n");
        }
        
        sb.append("#recette #cuisine #iNutshell");
        return sb.toString();
    }
    
    /**
     * Découpe le texte en lignes
     */
    private String[] wrapText(String text, int maxCharsPerLine) {
        if (text.length() <= maxCharsPerLine) {
            return new String[]{text};
        }
        
        return text.split("(?<=\\G.{" + maxCharsPerLine + "})");
    }
    
    /**
     * Énumération des réseaux sociaux supportés
     */
    public enum SocialNetwork {
        FACEBOOK, TWITTER, INSTAGRAM, WHATSAPP
    }
}
