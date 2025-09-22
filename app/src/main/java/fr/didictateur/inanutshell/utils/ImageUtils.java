package fr.didictateur.inanutshell.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitaire pour la gestion des images dans l'application
 */
public class ImageUtils {
    
    public static final int REQUEST_CODE_GALLERY = 1001;
    public static final int REQUEST_CODE_CAMERA = 1002;
    public static final int REQUEST_CODE_PERMISSIONS = 1003;
    
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    public interface ImageSelectionListener {
        void onImageSelected(Uri imageUri, String imagePath);
        void onImageSelectionError(String error);
    }
    
    /**
     * Ouvrir la galerie pour sélectionner une image
     */
    public static void openGallery(Activity activity, ImageSelectionListener listener) {
        // Sur Android récent, essayons d'abord sans permissions (ACTION_GET_CONTENT)
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            // Android 10+ : Utiliser le sélecteur moderne sans permissions
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
        } else {
            // Android plus ancien : Demander les permissions
            fr.didictateur.inanutshell.utils.PermissionManager.requestGalleryPermissions(activity, new fr.didictateur.inanutshell.utils.PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionsGranted() {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
                }
                
                @Override
                public void onPermissionsDenied() {
                    listener.onImageSelectionError("Permission galerie refusée. Réessayez ou activez les permissions dans Paramètres > Apps > In a Nutshell > Autorisations");
                }
                
                @Override
                public void onPermissionsPermanentlyDenied() {
                    listener.onImageSelectionError("Activez les permissions dans Paramètres > Apps > In a Nutshell > Autorisations");
                }
            });
        }
    }
    
    /**
     * Ouvrir l'appareil photo pour prendre une photo
     */
    public static void openCamera(Activity activity, ImageSelectionListener listener) {
        fr.didictateur.inanutshell.utils.PermissionManager.requestCameraPermissions(activity, new fr.didictateur.inanutshell.utils.PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                
                // Créer un fichier pour stocker la photo
                File photoFile = createImageFile(activity);
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(activity,
                            "fr.didictateur.inanutshell.fileprovider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);
                    
                    // Stocker l'URI temporaire pour récupération après la photo
                    if (activity instanceof ImageSelectionListener) {
                        // Note: L'URI sera géré dans onActivityResult
                    }
                } else {
                    listener.onImageSelectionError("Impossible de créer le fichier pour la photo");
                }
            }
            
            @Override
            public void onPermissionsDenied() {
                listener.onImageSelectionError("Permission appareil photo refusée. Réessayez ou activez les permissions dans Paramètres > Apps > In a Nutshell > Autorisations");
            }
            
            @Override
            public void onPermissionsPermanentlyDenied() {
                listener.onImageSelectionError("Activez les permissions dans Paramètres > Apps > In a Nutshell > Autorisations");
            }
        });
    }
    
    /**
     * Créer un fichier temporaire pour une image
     */
    private static File createImageFile(Context context) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "RECIPE_" + timeStamp + "_";
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException ex) {
            return null;
        }
    }
    
    /**
     * Redimensionner une image pour l'upload
     */
    public static Bitmap resizeImageForUpload(Context context, Uri imageUri, int maxWidth, int maxHeight) {
        try {
            InputStream input = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            // Calculer le facteur de réduction
            int scaleFactor = calculateInSampleSize(options, maxWidth, maxHeight);
            
            // Décoder l'image avec la taille réduite
            input = context.getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            // Corriger l'orientation si nécessaire
            return rotateImageIfRequired(context, bitmap, imageUri);
            
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Calculer le facteur de réduction pour une image
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Corriger l'orientation d'une image selon les données EXIF
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        
        if (input != null) {
            ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
        return img;
    }
    
    /**
     * Faire tourner une image d'un angle donné
     */
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
    
    /**
     * Sauvegarder un bitmap dans un fichier temporaire
     */
    public static File saveBitmapToTempFile(Context context, Bitmap bitmap, String fileName) {
        try {
            File tempFile = new File(context.getCacheDir(), fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Obtenir le chemin réel d'un Uri
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        // Pour les versions récentes d'Android, on utilise directement l'URI
        // Le chemin réel n'est plus nécessaire avec les API modernes
        return uri.toString();
    }
    
    // ===================== MÉTHODES POUR L'ÉDITION D'IMAGES =====================
    
    /**
     * Créer une miniature optimisée d'une image
     */
    public static Bitmap createOptimizedThumbnail(Context context, Uri imageUri, int width, int height) {
        try {
            InputStream input = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            // Calculer le facteur de réduction pour la miniature
            int scaleFactor = calculateInSampleSize(options, width, height);
            
            // Décoder avec la taille optimisée
            input = context.getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Utiliser moins de mémoire
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            if (bitmap != null) {
                // Redimensionner exactement à la taille demandée
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                if (scaledBitmap != bitmap) {
                    bitmap.recycle(); // Libérer la mémoire de l'original
                }
                return scaledBitmap;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Sauvegarder une miniature dans le cache
     */
    public static File saveThumbnailToCache(Context context, Bitmap thumbnail, String recipeId) {
        try {
            File cacheDir = new File(context.getCacheDir(), "thumbnails");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            File thumbnailFile = new File(cacheDir, "thumb_" + recipeId + ".jpg");
            FileOutputStream out = new FileOutputStream(thumbnailFile);
            
            // Utiliser une compression plus élevée pour les miniatures
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, out);
            out.flush();
            out.close();
            
            return thumbnailFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Charger une miniature depuis le cache
     */
    public static Bitmap loadThumbnailFromCache(Context context, String recipeId) {
        try {
            File thumbnailFile = new File(context.getCacheDir(), "thumbnails/thumb_" + recipeId + ".jpg");
            if (thumbnailFile.exists()) {
                return BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Nettoyer le cache des miniatures
     */
    public static void clearThumbnailCache(Context context) {
        try {
            File cacheDir = new File(context.getCacheDir(), "thumbnails");
            if (cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Appliquer un filtre ou effet à une image
     */
    public static Bitmap applyImageFilter(Bitmap original, ImageFilter filter) {
        if (original == null) return null;
        
        Bitmap filtered = original.copy(original.getConfig(), true);
        
        switch (filter) {
            case BRIGHTNESS_UP:
                return adjustBrightness(filtered, 20);
            case BRIGHTNESS_DOWN:
                return adjustBrightness(filtered, -20);
            case CONTRAST_UP:
                return adjustContrast(filtered, 1.2f);
            case CONTRAST_DOWN:
                return adjustContrast(filtered, 0.8f);
            case SEPIA:
                return applySepia(filtered);
            case GRAYSCALE:
                return applyGrayscale(filtered);
            default:
                return filtered;
        }
    }
    
    /**
     * Ajuster la luminosité d'une image
     */
    private static Bitmap adjustBrightness(Bitmap bitmap, int brightness) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            1, 0, 0, 0, brightness,
            0, 1, 0, 0, brightness,
            0, 0, 1, 0, brightness,
            0, 0, 0, 1, 0
        });
        
        return applyColorMatrix(bitmap, colorMatrix);
    }
    
    /**
     * Ajuster le contraste d'une image
     */
    private static Bitmap adjustContrast(Bitmap bitmap, float contrast) {
        float translate = (1.0f - contrast) * 128.0f;
        
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            contrast, 0, 0, 0, translate,
            0, contrast, 0, 0, translate,
            0, 0, contrast, 0, translate,
            0, 0, 0, 1, 0
        });
        
        return applyColorMatrix(bitmap, colorMatrix);
    }
    
    /**
     * Appliquer un effet sépia
     */
    private static Bitmap applySepia(Bitmap bitmap) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
        });
        
        return applyColorMatrix(bitmap, colorMatrix);
    }
    
    /**
     * Appliquer un effet de niveaux de gris
     */
    private static Bitmap applyGrayscale(Bitmap bitmap) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.setSaturation(0);
        
        return applyColorMatrix(bitmap, colorMatrix);
    }
    
    /**
     * Appliquer une matrice de couleur à un bitmap
     */
    private static Bitmap applyColorMatrix(Bitmap bitmap, android.graphics.ColorMatrix colorMatrix) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(result);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }
    
    /**
     * Énumération des filtres d'image disponibles
     */
    public enum ImageFilter {
        NONE,
        BRIGHTNESS_UP,
        BRIGHTNESS_DOWN,
        CONTRAST_UP,
        CONTRAST_DOWN,
        SEPIA,
        GRAYSCALE
    }
    
    /**
     * Compresser une image pour réduire sa taille
     */
    public static Bitmap compressImage(Bitmap bitmap, int maxSizeKB) {
        int quality = 90;
        Bitmap compressed = bitmap;
        
        while (quality > 10) {
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            compressed.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            byte[] byteArray = stream.toByteArray();
            
            if (byteArray.length <= maxSizeKB * 1024) {
                // La taille est acceptable
                break;
            }
            
            quality -= 10;
        }
        
        return compressed;
    }
    
    /**
     * Lancer l'éditeur d'image
     */
    public static void launchImageEditor(Activity activity, Uri imageUri, int requestCode) {
        Intent intent = new Intent(activity, fr.didictateur.inanutshell.ui.image.ImageEditorActivity.class);
        intent.putExtra(fr.didictateur.inanutshell.ui.image.ImageEditorActivity.EXTRA_IMAGE_URI, imageUri);
        activity.startActivityForResult(intent, requestCode);
    }
}
