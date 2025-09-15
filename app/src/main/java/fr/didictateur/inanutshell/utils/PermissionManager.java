package fr.didictateur.inanutshell.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;

/**
 * Gestionnaire de permissions avec interface utilisateur fluide
 */
public class PermissionManager {
    
    public static final int REQUEST_CODE_PERMISSIONS = 1003;
    
    private static final String[] CAMERA_PERMISSIONS = {
        Manifest.permission.CAMERA
    };
    
    private static final String[] GALLERY_PERMISSIONS = getGalleryPermissions();
    
    private static String[] getGalleryPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            // Android 13+ : Utiliser READ_MEDIA_IMAGES
            return new String[]{"android.permission.READ_MEDIA_IMAGES"};
        } else {
            // Android 12 et inférieur : Utiliser READ_EXTERNAL_STORAGE  
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }
    
    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied();
        void onPermissionsPermanentlyDenied();
    }
    
    /**
     * Demander les permissions pour l'appareil photo
     */
    public static void requestCameraPermissions(Activity activity, PermissionCallback callback) {
        requestPermissions(activity, CAMERA_PERMISSIONS, 
            "Permissions Appareil Photo", 
            "L'application a besoin d'accéder à votre appareil photo et au stockage pour prendre des photos de vos recettes.",
            callback);
    }
    
    /**
     * Demander les permissions pour la galerie
     */
    public static void requestGalleryPermissions(Activity activity, PermissionCallback callback) {
        requestPermissions(activity, GALLERY_PERMISSIONS, 
            "Permissions Galerie", 
            "L'application a besoin d'accéder à vos photos pour sélectionner des images de recettes.",
            callback);
    }
    
    /**
     * Vérifier si les permissions de l'appareil photo sont accordées
     */
    public static boolean hasCameraPermissions(Context context) {
        return hasPermissions(context, CAMERA_PERMISSIONS);
    }
    
    /**
     * Vérifier si les permissions de la galerie sont accordées
     */
    public static boolean hasGalleryPermissions(Context context) {
        return hasPermissions(context, GALLERY_PERMISSIONS);
    }
    
    private static boolean hasPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private static void requestPermissions(Activity activity, String[] permissions, String title, String message, PermissionCallback callback) {
        
        // Vérifier si on a déjà toutes les permissions
        if (hasPermissions(activity, permissions)) {
            callback.onPermissionsGranted();
            return;
        }
        
        // Toujours afficher une explication conviviale avant de demander les permissions
        showPermissionExplanationDialog(activity, title, message, permissions, callback);
    }
    
    private static void showPermissionExplanationDialog(Activity activity, String title, String message, String[] permissions, PermissionCallback callback) {
        new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setIcon(R.drawable.ic_camera)
            .setPositiveButton("Autoriser", (dialog, which) -> {
                ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_PERMISSIONS);
            })
            .setNegativeButton("Plus tard", (dialog, which) -> {
                callback.onPermissionsDenied();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Traiter la réponse des permissions
     * À appeler depuis onRequestPermissionsResult de l'activité
     */
    public static void handlePermissionResult(Activity activity, int requestCode, String[] permissions, int[] grantResults, PermissionCallback callback) {
        android.util.Log.d("PermissionManager", "handlePermissionResult - requestCode: " + requestCode);
        
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            android.util.Log.d("PermissionManager", "Wrong request code, ignoring");
            return;
        }
        
        // Vérifier si toutes les permissions ont été accordées
        boolean allGranted = true;
        for (int i = 0; i < grantResults.length; i++) {
            android.util.Log.d("PermissionManager", "Permission " + permissions[i] + " result: " + 
                (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
            }
        }
        
        if (allGranted) {
            android.util.Log.d("PermissionManager", "All permissions granted");
            callback.onPermissionsGranted();
        } else {
            // Pour simplifier, on ne traite jamais comme "refus définitif" au premier refus
            // L'utilisateur peut toujours réessayer ou aller dans les paramètres manuellement
            android.util.Log.d("PermissionManager", "Permissions denied, showing rationale");
            callback.onPermissionsDenied();
        }
    }
    
    private static boolean hasAskedPermissionsBefore(Activity activity, String[] permissions) {
        android.content.SharedPreferences prefs = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE);
        for (String permission : permissions) {
            if (prefs.getBoolean("asked_" + permission, false)) {
                return true;
            }
        }
        return false;
    }
    
    private static void markPermissionsAsAsked(Activity activity, String[] permissions) {
        android.content.SharedPreferences prefs = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (String permission : permissions) {
            editor.putBoolean("asked_" + permission, true);
        }
        editor.apply();
    }
    
    private static void showPermissionSettingsDialog(Activity activity, PermissionCallback callback) {
        new AlertDialog.Builder(activity)
            .setTitle("Permissions requises")
            .setMessage("Pour utiliser cette fonctionnalité, veuillez autoriser les permissions dans les paramètres de l'application.")
            .setIcon(R.drawable.ic_settings)
            .setPositiveButton("Ouvrir les paramètres", (dialog, which) -> {
                openAppSettings(activity);
                callback.onPermissionsPermanentlyDenied();
            })
            .setNegativeButton("Annuler", (dialog, which) -> {
                callback.onPermissionsDenied();
            })
            .show();
    }
    
    private static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
    
    /**
     * Réinitialiser l'état des permissions demandées (utile pour les tests)
     */
    public static void resetPermissionState(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("permissions", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
    
    /**
     * Debug: Afficher l'état de toutes les permissions
     */
    public static void debugPermissions(Context context) {
        android.util.Log.d("PermissionManager", "=== DEBUG PERMISSIONS ===");
        android.util.Log.d("PermissionManager", "Android SDK: " + android.os.Build.VERSION.SDK_INT);
        
        // Permissions caméra
        for (String permission : CAMERA_PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            android.util.Log.d("PermissionManager", "Camera - " + permission + ": " + (granted ? "GRANTED" : "DENIED"));
        }
        
        // Permissions galerie
        for (String permission : GALLERY_PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            android.util.Log.d("PermissionManager", "Gallery - " + permission + ": " + (granted ? "GRANTED" : "DENIED"));
        }
        
        android.util.Log.d("PermissionManager", "Camera permissions OK: " + hasCameraPermissions(context));
        android.util.Log.d("PermissionManager", "Gallery permissions OK: " + hasGalleryPermissions(context));
        android.util.Log.d("PermissionManager", "========================");
    }
}
