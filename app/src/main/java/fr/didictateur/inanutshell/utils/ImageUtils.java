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
}
