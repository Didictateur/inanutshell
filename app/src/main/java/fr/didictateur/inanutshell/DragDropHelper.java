package fr.didictateur.inanutshell;

import android.content.ClipData;
import android.content.ClipDescription;
import android.view.DragEvent;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DragDropHelper {
    private MainActivity mainActivity;
    private AppDatabase database;
    private ExecutorService executor;

    public DragDropHelper(MainActivity activity, AppDatabase database) {
        this.mainActivity = activity;
        this.database = database;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void enableDragAndDrop(View view, Item item) {
        if (item instanceof Recette) {
            enableRecipeDrag(view, (Recette) item);
        } else if (item instanceof Folder) {
            enableFolderDrop(view, (Folder) item);
        }
    }

    private void enableRecipeDrag(View view, Recette recette) {
        view.setOnLongClickListener(v -> {
            ClipData.Item clipItem = new ClipData.Item(String.valueOf(recette.getId()));
            ClipData dragData = new ClipData(
                "recipe_id",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                clipItem
            );

            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(dragData, shadowBuilder, recette, 0);
            return true;
        });
    }

    private void enableFolderDrop(View view, Folder folder) {
        view.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.7f);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    return true;

                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f);
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    String recipeIdStr = item.getText().toString();
                    
                    try {
                        Long recipeId = Long.valueOf(recipeIdStr);
                        showMoveConfirmation(recipeId, folder);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);
                    return true;

                default:
                    return false;
            }
        });
    }

    private void showMoveConfirmation(Long recipeId, Folder targetFolder) {
        executor.execute(() -> {
            Recette recette = database.recetteDao().getRecetteById(recipeId);
            
            mainActivity.runOnUiThread(() -> {
                if (recette != null) {
                    new AlertDialog.Builder(mainActivity)
                        .setTitle("Déplacer la recette")
                        .setMessage("Voulez-vous déplacer \"" + recette.getNom() + 
                                  "\" dans le dossier \"" + targetFolder.getName() + "\" ?")
                        .setPositiveButton("Déplacer", (dialog, which) -> {
                            moveRecipeToFolder(recette, targetFolder.getId());
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
                }
            });
        });
    }

    private void moveRecipeToFolder(Recette recette, Long targetFolderId) {
        executor.execute(() -> {
            recette.setFolderId(targetFolderId);
            database.recetteDao().update(recette);
            
            mainActivity.runOnUiThread(() -> {
                // Rafraîchir l'affichage
                mainActivity.showFolder(mainActivity.getCurrentFolderId());
            });
        });
    }

    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
