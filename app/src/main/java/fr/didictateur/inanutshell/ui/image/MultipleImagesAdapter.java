package fr.didictateur.inanutshell.ui.image;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.utils.ImageLoader;

/**
 * Adapter pour afficher les images multiples d'une recette
 */
public class MultipleImagesAdapter extends RecyclerView.Adapter<MultipleImagesAdapter.ImageViewHolder> {
    
    private Context context;
    private List<String> imageUrls;
    private OnImageActionListener listener;
    
    public interface OnImageActionListener {
        void onImageClick(String imageUrl, int position);
        void onImageEdit(String imageUrl, int position);
        void onImageDelete(String imageUrl, int position);
    }
    
    public MultipleImagesAdapter(Context context, List<String> imageUrls, OnImageActionListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_multiple_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        holder.bind(imageUrl, position);
    }
    
    @Override
    public int getItemCount() {
        return imageUrls.size();
    }
    
    class ImageViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imageView;
        private ImageButton btnEdit;
        private ImageButton btnDelete;
        
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imageView = itemView.findViewById(R.id.iv_image);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
        
        public void bind(String imageUrl, int position) {
            // Charger l'image
            ImageLoader.loadImageFromUrl(context, imageUrl, imageView);
            
            // Click sur l'image pour affichage plein écran
            imageView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(imageUrl, position);
                }
            });
            
            // Bouton d'édition
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageEdit(imageUrl, position);
                }
            });
            
            // Bouton de suppression
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageDelete(imageUrl, position);
                }
            });
        }
    }
}
