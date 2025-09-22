package fr.didictateur.inanutshell.service;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextPaint;
import android.text.Layout;
import android.text.StaticLayout;
import androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.RecipeInstruction;

public class ShareService {
    private static ShareService instance;
    private Context context;

    private ShareService(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ShareService getInstance(Context context) {
        if (instance == null) {
            instance = new ShareService(context);
        }
        return instance;
    }

    public void shareAsText(Recipe recipe) {
        String recipeText = formatRecipeAsText(recipe);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(Intent.createChooser(shareIntent, "Partager la recette"));
    }

    public void shareAsImage(Recipe recipe) {
        try {
            Bitmap recipeBitmap = createRecipeImage(recipe);
            File imageFile = saveImageToCache(recipeBitmap, recipe.getName() + "_recipe.png");
            
            Uri imageUri = FileProvider.getUriForFile(
                context, 
                context.getPackageName() + ".fileprovider", 
                imageFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(shareIntent, "Partager l'image de la recette"));
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback sur le partage de texte
            shareAsText(recipe);
        }
    }

    public void shareAsQRCode(Recipe recipe) {
        try {
            String recipeData = formatRecipeForQRCode(recipe);
            Bitmap qrBitmap = generateQRCode(recipeData, 512, 512);
            
            File qrFile = saveImageToCache(qrBitmap, recipe.getName() + "_qr.png");
            
            Uri qrUri = FileProvider.getUriForFile(
                context, 
                context.getPackageName() + ".fileprovider", 
                qrFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, qrUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code - Recette: " + recipe.getName());
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(shareIntent, "Partager le QR Code"));
        } catch (Exception e) {
            e.printStackTrace();
            shareAsText(recipe);
        }
    }

    public void shareViaWhatsApp(Recipe recipe) {
        if (isAppInstalled("com.whatsapp")) {
            String recipeText = formatRecipeAsText(recipe);
            
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(whatsappIntent);
        } else {
            shareAsText(recipe);
        }
    }

    public void shareViaFacebook(Recipe recipe) {
        if (isAppInstalled("com.facebook.katana")) {
            String recipeText = formatRecipeAsText(recipe);
            
            Intent facebookIntent = new Intent(Intent.ACTION_SEND);
            facebookIntent.setType("text/plain");
            facebookIntent.setPackage("com.facebook.katana");
            facebookIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
            facebookIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(facebookIntent);
        } else {
            shareAsText(recipe);
        }
    }

    public void shareViaTwitter(Recipe recipe) {
        if (isAppInstalled("com.twitter.android")) {
            String shortText = recipe.getName() + " - " + 
                (recipe.getDescription() != null && recipe.getDescription().length() > 100 ? 
                 recipe.getDescription().substring(0, 100) + "..." : recipe.getDescription());
            
            Intent twitterIntent = new Intent(Intent.ACTION_SEND);
            twitterIntent.setType("text/plain");
            twitterIntent.setPackage("com.twitter.android");
            twitterIntent.putExtra(Intent.EXTRA_TEXT, shortText);
            twitterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(twitterIntent);
        } else {
            shareAsText(recipe);
        }
    }

    public void shareViaInstagram(Recipe recipe) {
        if (isAppInstalled("com.instagram.android")) {
            try {
                Bitmap recipeBitmap = createRecipeImage(recipe);
                File imageFile = saveImageToCache(recipeBitmap, recipe.getName() + "_instagram.jpg");
                
                Uri imageUri = FileProvider.getUriForFile(
                    context, 
                    context.getPackageName() + ".fileprovider", 
                    imageFile
                );
                
                Intent instagramIntent = new Intent(Intent.ACTION_SEND);
                instagramIntent.setType("image/jpeg");
                instagramIntent.setPackage("com.instagram.android");
                instagramIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                instagramIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                instagramIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                context.startActivity(instagramIntent);
            } catch (Exception e) {
                e.printStackTrace();
                shareAsText(recipe);
            }
        } else {
            shareAsText(recipe);
        }
    }

    public void shareAsLink(Recipe recipe) {
        // Pour l'instant, on génère un lien local
        String link = "inanutshell://recipe/" + recipe.getId();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Découvre cette recette: " + recipe.getName() + "\n" + link);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(Intent.createChooser(shareIntent, "Partager le lien"));
    }

    private String formatRecipeAsText(Recipe recipe) {
        StringBuilder text = new StringBuilder();
        text.append("🍽️ ").append(recipe.getName()).append("\n");
        text.append("═══════════════════\n\n");
        
        if (recipe.getDescription() != null) {
            text.append("📝 Description:\n").append(recipe.getDescription()).append("\n\n");
        }
        
        if (recipe.getRecipeYield() != null) {
            text.append("👥 Portions: ").append(recipe.getRecipeYield()).append("\n");
        }
        
        if (recipe.getPrepTime() != null) {
            text.append("⏱️ Préparation: ").append(recipe.getPrepTime()).append("\n");
        }
        
        if (recipe.getCookTime() != null) {
            text.append("🔥 Cuisson: ").append(recipe.getCookTime()).append("\n");
        }
        
        if (recipe.getTotalTime() != null) {
            text.append("⏰ Temps total: ").append(recipe.getTotalTime()).append("\n");
        }
        
        text.append("\n");
        
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            text.append("🛒 INGRÉDIENTS:\n");
            text.append("───────────────\n");
            for (RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                text.append("• ").append(ingredient.getDisplay()).append("\n");
            }
            text.append("\n");
        }
        
        if (recipe.getRecipeInstructions() != null && !recipe.getRecipeInstructions().isEmpty()) {
            text.append("👨‍🍳 INSTRUCTIONS:\n");
            text.append("─────────────────\n");
            int stepNumber = 1;
            for (RecipeInstruction instruction : recipe.getRecipeInstructions()) {
                text.append(stepNumber).append(". ").append(instruction.getText()).append("\n\n");
                stepNumber++;
            }
        }
        
        text.append("📱 Partagé depuis In a Nutshell");
        
        return text.toString();
    }

    private String formatRecipeForQRCode(Recipe recipe) {
        StringBuilder qrData = new StringBuilder();
        qrData.append("RECIPE:").append(recipe.getName());
        if (recipe.getId() != null) {
            qrData.append("|ID:").append(recipe.getId());
        }
        if (recipe.getRecipeYield() != null) {
            qrData.append("|YIELD:").append(recipe.getRecipeYield());
        }
        if (recipe.getTotalTime() != null) {
            qrData.append("|TIME:").append(recipe.getTotalTime());
        }
        return qrData.toString();
    }

    private Bitmap generateQRCode(String text, int width, int height) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        
        return bitmap;
    }

    private Bitmap createRecipeImage(Recipe recipe) {
        int width = 800;
        int height = 1200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fond dégradé
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        
        // Titre
        TextPaint titlePaint = new TextPaint();
        titlePaint.setColor(Color.parseColor("#2C3E50"));
        titlePaint.setTextSize(48);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setAntiAlias(true);
        
        String title = recipe.getName();
        StaticLayout titleLayout = new StaticLayout(
            title, titlePaint, width - 80, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false
        );
        
        canvas.save();
        canvas.translate(40, 60);
        titleLayout.draw(canvas);
        canvas.restore();
        
        int currentY = 200;
        
        // Informations générales
        TextPaint infoPaint = new TextPaint();
        infoPaint.setColor(Color.parseColor("#34495E"));
        infoPaint.setTextSize(24);
        infoPaint.setAntiAlias(true);
        
        if (recipe.getRecipeYield() != null) {
            canvas.drawText("👥 Portions: " + recipe.getRecipeYield(), 40, currentY, infoPaint);
            currentY += 40;
        }
        
        if (recipe.getTotalTime() != null) {
            canvas.drawText("⏰ Temps: " + recipe.getTotalTime(), 40, currentY, infoPaint);
            currentY += 40;
        }
        
        currentY += 20;
        
        // Description
        if (recipe.getDescription() != null) {
            TextPaint descPaint = new TextPaint();
            descPaint.setColor(Color.parseColor("#7F8C8D"));
            descPaint.setTextSize(20);
            descPaint.setAntiAlias(true);
            
            StaticLayout descLayout = new StaticLayout(
                recipe.getDescription(), descPaint, width - 80, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false
            );
            
            canvas.save();
            canvas.translate(40, currentY);
            descLayout.draw(canvas);
            canvas.restore();
            
            currentY += descLayout.getHeight() + 30;
        }
        
        // Ingrédients (limités)
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            TextPaint headerPaint = new TextPaint();
            headerPaint.setColor(Color.parseColor("#E74C3C"));
            headerPaint.setTextSize(28);
            headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
            headerPaint.setAntiAlias(true);
            
            canvas.drawText("🛒 INGRÉDIENTS", 40, currentY, headerPaint);
            currentY += 50;
            
            TextPaint ingredientPaint = new TextPaint();
            ingredientPaint.setColor(Color.parseColor("#2C3E50"));
            ingredientPaint.setTextSize(20);
            ingredientPaint.setAntiAlias(true);
            
            int maxIngredients = Math.min(recipe.getRecipeIngredient().size(), 8);
            for (int i = 0; i < maxIngredients; i++) {
                String ingredient = "• " + recipe.getRecipeIngredient().get(i).getDisplay();
                canvas.drawText(ingredient, 40, currentY, ingredientPaint);
                currentY += 30;
            }
            
            if (recipe.getRecipeIngredient().size() > 8) {
                canvas.drawText("... et " + (recipe.getRecipeIngredient().size() - 8) + " autres ingrédients", 
                    40, currentY, ingredientPaint);
                currentY += 40;
            }
        }
        
        // Footer
        TextPaint footerPaint = new TextPaint();
        footerPaint.setColor(Color.parseColor("#95A5A6"));
        footerPaint.setTextSize(18);
        footerPaint.setAntiAlias(true);
        
        canvas.drawText("📱 Créé avec In a Nutshell", 40, height - 40, footerPaint);
        
        return bitmap;
    }

    private File saveImageToCache(Bitmap bitmap, String fileName) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "shared_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        File imageFile = new File(cacheDir, fileName);
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
        fos.close();
        
        return imageFile;
    }

    private boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
