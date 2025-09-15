package fr.didictateur.inanutshell.data.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import fr.didictateur.inanutshell.utils.ImageUtils;

/**
 * Service pour l'upload d'images vers l'API Mealie
 */
public class ImageUploadService {
    
    private static final String TAG = "ImageUploadService";
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");
    
    public interface ImageUploadListener {
        void onUploadSuccess(String imageUrl);
        void onUploadError(String error);
        void onUploadProgress(int progress);
    }
    
    /**
     * Upload une image vers Mealie
     */
    public static void uploadRecipeImage(Context context, String recipeId, Uri imageUri, ImageUploadListener listener) {
        
        NetworkManager networkManager = NetworkManager.getInstance();
        
        // Vérifier si nous avons une connexion
        if (!networkManager.isConnected()) {
            listener.onUploadError("Pas de connexion réseau");
            return;
        }
        
        // Vérifier les credentials
        if (networkManager.getAuthToken() == null) {
            listener.onUploadError("Token d'authentification manquant");
            return;
        }
        
        try {
            // Redimensionner l'image pour l'upload
            Bitmap resizedBitmap = ImageUtils.resizeImageForUpload(context, imageUri, 1024, 768);
            if (resizedBitmap == null) {
                listener.onUploadError("Erreur lors du redimensionnement de l'image");
                return;
            }
            
            // Convertir en bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Créer la requête multipart
            RequestBody imageBody = RequestBody.create(MEDIA_TYPE_IMAGE, imageBytes);
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "recipe_image.jpg", imageBody);
            
            // Si nous avons un recipe ID, l'ajouter
            if (recipeId != null && !recipeId.isEmpty()) {
                builder.addFormDataPart("recipe_id", recipeId);
            }
            
            RequestBody requestBody = builder.build();
            
            // Construire l'URL d'upload
            String uploadUrl = networkManager.getBaseUrl() + "/api/media/recipes/images";
            
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer " + networkManager.getAuthToken())
                    .post(requestBody)
                    .build();
                    
            // Exécuter la requête
            networkManager.getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur upload image: " + e.getMessage());
                    listener.onUploadError("Erreur réseau: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            Log.d(TAG, "Upload réussi: " + responseBody);
                            
                            // Parser la réponse pour extraire l'URL de l'image
                            String imageUrl = parseImageUrlFromResponse(responseBody);
                            if (imageUrl != null) {
                                listener.onUploadSuccess(imageUrl);
                            } else {
                                listener.onUploadSuccess(responseBody); // Fallback
                            }
                        } else {
                            String error = "Erreur serveur: " + response.code();
                            if (response.body() != null) {
                                error += " - " + response.body().string();
                            }
                            Log.e(TAG, error);
                            listener.onUploadError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur traitement réponse: " + e.getMessage());
                        listener.onUploadError("Erreur traitement réponse: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation upload: " + e.getMessage());
            listener.onUploadError("Erreur préparation upload: " + e.getMessage());
        }
    }
    
    /**
     * Parser l'URL de l'image depuis la réponse JSON
     */
    private static String parseImageUrlFromResponse(String responseBody) {
        try {
            // Simple parsing pour extraire l'URL de l'image
            // La réponse Mealie contient généralement l'URL dans un champ comme "image_url"
            if (responseBody.contains("\"image_url\"")) {
                int start = responseBody.indexOf("\"image_url\"") + 13;
                int end = responseBody.indexOf("\"", start);
                if (end > start) {
                    return responseBody.substring(start, end);
                }
            }
            
            // Fallback: chercher toute URL qui ressemble à une image
            if (responseBody.contains("http")) {
                int start = responseBody.indexOf("http");
                int end = responseBody.indexOf("\"", start);
                if (end > start) {
                    return responseBody.substring(start, end);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing URL image: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Upload une image depuis un fichier temporaire
     */
    public static void uploadImageFile(File imageFile, String recipeId, ImageUploadListener listener) {
        
        NetworkManager networkManager = NetworkManager.getInstance();
        
        if (!networkManager.isConnected()) {
            listener.onUploadError("Pas de connexion réseau");
            return;
        }
        
        if (networkManager.getAuthToken() == null) {
            listener.onUploadError("Token d'authentification manquant");
            return;
        }
        
        try {
            // Lire le fichier
            FileInputStream fis = new FileInputStream(imageFile);
            byte[] imageBytes = new byte[(int) imageFile.length()];
            fis.read(imageBytes);
            fis.close();
            
            // Créer la requête
            RequestBody imageBody = RequestBody.create(MEDIA_TYPE_IMAGE, imageBytes);
            
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", imageFile.getName(), imageBody)
                    .addFormDataPart("recipe_id", recipeId != null ? recipeId : "")
                    .build();
            
            String uploadUrl = networkManager.getBaseUrl() + "/api/media/recipes/images";
            
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer " + networkManager.getAuthToken())
                    .post(requestBody)
                    .build();
                    
            networkManager.getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onUploadError("Erreur réseau: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            String imageUrl = parseImageUrlFromResponse(responseBody);
                            listener.onUploadSuccess(imageUrl != null ? imageUrl : responseBody);
                        } else {
                            String error = "Erreur serveur: " + response.code();
                            listener.onUploadError(error);
                        }
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            listener.onUploadError("Erreur lecture fichier: " + e.getMessage());
        }
    }
}
