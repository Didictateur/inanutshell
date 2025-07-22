package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RecetteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Item> items;
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_RECETTE = 1;
    
    // Variables pour la rotation des images
    private Handler handler = new Handler(Looper.getMainLooper());
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final long ROTATION_INTERVAL = 3000; // 3 secondes

    // Listener pour le clic sur un dossier
    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }
    private OnFolderClickListener onFolderClickListener;
    public void setOnFolderClickListener(OnFolderClickListener listener) {
        this.onFolderClickListener = listener;
    }

    // Listener pour le clic long sur une recette
    public interface OnRecetteActionListener {
        void onRecetteAction(View anchor, Recette recette);
    }
    private OnRecetteActionListener onRecetteActionListener;
    public void setOnRecetteActionListener(OnRecetteActionListener listener) {
        this.onRecetteActionListener = listener;
    }

    public RecetteAdapter(ArrayList<Item> items) {
        this.items = items;
    }
    
    // Méthode pour obtenir les images des recettes d'un dossier
    private List<String> getFolderRecipeImages(Context context, Folder folder) {
        List<String> imagesPaths = new ArrayList<>();
        
        // Accéder à la base de données pour obtenir les recettes du dossier
        executor.execute(() -> {
            try {
                AppDatabase db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "database-name").build();
                
                List<Recette> recettes = db.recetteDao().getRecettesByParent(folder.getId());
                
                for (Recette recette : recettes) {
                    if (recette.photoPath != null && !recette.photoPath.isEmpty()) {
                        File file = new File(recette.photoPath);
                        if (file.exists()) {
                            imagesPaths.add(recette.photoPath);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("RecetteAdapter", "Erreur lors de la récupération des images", e);
            }
        });
        
        return imagesPaths;
    }
    
    // Méthode pour démarrer la rotation des images d'un dossier
    private void startImageRotation(Context context, FolderViewHolder holder, Folder folder) {
        Log.d("RecetteAdapter", "Starting image rotation for folder: " + folder.getTitle());
        
        executor.execute(() -> {
            try {
                AppDatabase db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "database-name").build();
                
                List<Recette> recettes = db.recetteDao().getRecettesByParent(folder.getId());
                List<String> validImagePaths = new ArrayList<>();
                
                Log.d("RecetteAdapter", "Found " + recettes.size() + " recettes in folder");
                
                // Vérifier quelles recettes ont des images valides
                for (Recette recette : recettes) {
                    if (recette.photoPath != null && !recette.photoPath.isEmpty()) {
                        File file = new File(recette.photoPath);
                        Log.d("RecetteAdapter", "Checking image: " + recette.photoPath + " exists: " + file.exists());
                        if (file.exists()) {
                            validImagePaths.add(recette.photoPath);
                        }
                    }
                }
                
                Log.d("RecetteAdapter", "Found " + validImagePaths.size() + " valid images");
                
                // Retour sur le thread principal pour mettre à jour l'UI
                handler.post(() -> {
                    if (validImagePaths.isEmpty()) {
                        // Pas d'images, utiliser l'image par défaut
                        Log.d("RecetteAdapter", "No images found, using default");
                        holder.folderIcon.setImageResource(R.drawable.folder_empty);
                        holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    } else {
                        // Commencer par la première image immédiatement
                        Log.d("RecetteAdapter", "Loading first image: " + validImagePaths.get(0));
                        loadImageIntoHolder(holder, validImagePaths.get(0));
                        
                        // Si on a plusieurs images, démarrer la rotation
                        if (validImagePaths.size() > 1) {
                            startRotationCycle(holder, validImagePaths);
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e("RecetteAdapter", "Erreur lors du chargement des images du dossier", e);
                handler.post(() -> {
                    holder.folderIcon.setImageResource(R.drawable.folder_empty);
                    holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                });
            }
        });
    }
    
    // Méthode pour gérer le cycle de rotation (UNIQUEMENT les photos des recettes)
    private void startRotationCycle(FolderViewHolder holder, List<String> imagePaths) {
        if (imagePaths.isEmpty()) {
            // Pas d'images disponibles, utiliser une image par défaut pour dossier vide
            holder.folderIcon.setImageResource(R.drawable.folder_empty);
            holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            return;
        }
        
        final int[] currentIndex = {0}; // Commencer par la première image
        
        holder.rotationRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] >= imagePaths.size()) {
                    // Retour à la première image (cycle continu)
                    currentIndex[0] = 0;
                }
                
                // Charger l'image de la recette actuelle
                String imagePath = imagePaths.get(currentIndex[0]);
                loadImageIntoHolder(holder, imagePath);
                
                currentIndex[0]++;
                
                // Programmer la prochaine rotation
                holder.rotationHandler.postDelayed(this, ROTATION_INTERVAL);
            }
        };
        
        // Commencer immédiatement par la première image
        String firstImagePath = imagePaths.get(0);
        loadImageIntoHolder(holder, firstImagePath);
        
        // Si on a plusieurs images, démarrer la rotation
        if (imagePaths.size() > 1) {
            currentIndex[0] = 1; // La prochaine sera l'index 1
            holder.rotationHandler.postDelayed(holder.rotationRunnable, ROTATION_INTERVAL);
        }
    }
    
    // Méthode utilitaire pour charger une image dans le holder
    private void loadImageIntoHolder(FolderViewHolder holder, String imagePath) {
        Log.d("RecetteAdapter", "Loading image: " + imagePath);
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                Log.d("RecetteAdapter", "Image loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                holder.folderIcon.setImageBitmap(bitmap);
                holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.folderIcon.setPadding(0, 0, 0, 0); // Supprimer le padding pour les photos
            } else {
                Log.w("RecetteAdapter", "Failed to decode image: " + imagePath);
                holder.folderIcon.setImageResource(R.drawable.folder_empty);
                holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.folderIcon.setPadding(8, 8, 8, 8); // Garder le padding pour l'icône
            }
        } catch (Exception e) {
            Log.e("RecetteAdapter", "Erreur lors du chargement de l'image: " + imagePath, e);
            holder.folderIcon.setImageResource(R.drawable.folder_empty);
            holder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.folderIcon.setPadding(8, 8, 8, 8);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isFolder() ? TYPE_FOLDER : TYPE_RECETTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOLDER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder, parent, false);
            return new FolderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recipe, parent, false);
            return new RecetteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);

        if (holder instanceof FolderViewHolder) {
            Folder folder = (Folder) item;
            FolderViewHolder fHolder = (FolderViewHolder) holder;

            fHolder.folderName.setText(folder.getTitle());
            
            // Arrêter toute rotation précédente
            fHolder.stopRotation();

            Context context = fHolder.itemView.getContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
            
            // Choisir le cercle coloré approprié selon le thème
            int backgroundResId;
            switch (colorName) {
                case "toolbar_bg_orange":
                    backgroundResId = R.drawable.bg_folder_circle_orange;
                    break;
                case "toolbar_bg_blue":
                    backgroundResId = R.drawable.bg_folder_circle_blue;
                    break;
                case "toolbar_bg_green":
                    backgroundResId = R.drawable.bg_folder_circle_green;
                    break;
                case "toolbar_bg_red":
                    backgroundResId = R.drawable.bg_folder_circle_red;
                    break;
                case "toolbar_bg_purple":
                    backgroundResId = R.drawable.bg_folder_circle_purple;
                    break;
                default:
                    backgroundResId = R.drawable.bg_folder_circle_brown;
                    break;
            }
            
            // Appliquer le cercle coloré en arrière-plan
            fHolder.folderBackground.setBackgroundResource(backgroundResId);
            
            // Charger une image simple - juste le logo pour l'instant
            fHolder.folderIcon.setImageResource(R.drawable.appicon);
            fHolder.folderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            fHolder.itemView.setOnClickListener(v -> {
                if (onFolderClickListener != null) {
                    onFolderClickListener.onFolderClick(folder);
                }
            });

        } else if (holder instanceof RecetteViewHolder) {
            Recette recette = (Recette) item;
            RecetteViewHolder rHolder = (RecetteViewHolder) holder;

            rHolder.title.setText(recette.getTitle());
            
            // Utiliser l'image personnalisée si elle existe, sinon l'icône par défaut
            if (recette.photoPath != null && !recette.photoPath.isEmpty() && new File(recette.photoPath).exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(recette.photoPath);
                rHolder.image.setImageBitmap(bitmap);
            } else {
                rHolder.image.setImageResource(recette.imageResId);
            }

            rHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(
                        rHolder.itemView.getContext(),
                        ViewRecetteActivity.class
                );
                intent.putExtra("titre", recette.titre);
                intent.putExtra("taille", recette.taille);
                intent.putExtra("tempsPrep", recette.tempsPrep);
                intent.putExtra("ingredients", recette.ingredients);
                intent.putExtra("preparation", recette.preparation);
                intent.putExtra("notes", recette.notes);
                rHolder.itemView.getContext().startActivity(intent);
            });

            rHolder.itemView.setOnLongClickListener(v -> {
                if (onRecetteActionListener != null) {
                    onRecetteActionListener.onRecetteAction(v, recette);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder pour les dossiers
    static class FolderViewHolder extends RecyclerView.ViewHolder {
        ImageView folderIcon;
        TextView folderName;
        View folderBackground;
        Handler rotationHandler;
        Runnable rotationRunnable;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            folderName = itemView.findViewById(R.id.folderName);
            folderBackground = itemView.findViewById(R.id.folderBackground);
            rotationHandler = new Handler(Looper.getMainLooper());
        }
        
        void stopRotation() {
            if (rotationHandler != null && rotationRunnable != null) {
                rotationHandler.removeCallbacks(rotationRunnable);
            }
        }
    }

    // ViewHolder pour les recettes
    static class RecetteViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;

        RecetteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recipeTitle);
            image = itemView.findViewById(R.id.recipeImage);
        }
    }
}
