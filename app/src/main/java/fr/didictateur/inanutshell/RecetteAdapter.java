package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class RecetteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Item> items;
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_RECETTE = 1;

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
            fHolder.folderIcon.setImageResource(R.drawable.appicon);

						Context context = fHolder.itemView.getContext();
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
						int colorResId = context.getResources().getIdentifier(
								colorName,
								"color",
								context.getPackageName()
						);
						int color = androidx.core.content.ContextCompat.getColor(context, colorResId);

						android.graphics.drawable.GradientDrawable bg =
							(android.graphics.drawable.GradientDrawable)
							androidx.core.content.ContextCompat.getDrawable(
									context,
									R.drawable.bg_folder
							).mutate();
						bg.setColor(color);
						fHolder.itemView.setBackground(bg);

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

        FolderViewHolder(View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            folderName = itemView.findViewById(R.id.folderName);
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

