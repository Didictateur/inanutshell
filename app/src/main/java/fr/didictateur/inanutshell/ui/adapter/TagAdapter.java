package fr.didictateur.inanutshell.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Tag;
import java.util.List;
import java.util.ArrayList;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
    
    private List<Tag> tags = new ArrayList<>();
    private OnTagClickListener listener;
    private boolean isSelectionMode = false;
    private List<Tag> selectedTags = new ArrayList<>();
    
    public interface OnTagClickListener {
        void onTagClick(Tag tag);
        void onTagSelectionChanged(List<Tag> selectedTags);
    }
    
    public TagAdapter(OnTagClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        boolean isSelected = selectedTags.contains(tag);
        holder.bind(tag, isSelected);
    }
    
    @Override
    public int getItemCount() {
        return tags.size();
    }
    
    public void setTags(List<Tag> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addTag(Tag tag) {
        tags.add(tag);
        notifyItemInserted(tags.size() - 1);
    }
    
    public void removeTag(int position) {
        if (position >= 0 && position < tags.size()) {
            Tag removedTag = tags.remove(position);
            selectedTags.remove(removedTag);
            notifyItemRemoved(position);
        }
    }
    
    // Mode sélection pour assigner des tags aux recettes
    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedTags.clear();
        }
        notifyDataSetChanged();
    }
    
    public void setSelectedTags(List<Tag> selectedTags) {
        this.selectedTags = selectedTags != null ? selectedTags : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Tag> getSelectedTags() {
        return new ArrayList<>(selectedTags);
    }
    
    class TagViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTagName;
        private View tagContainer;
        
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagName = itemView.findViewById(R.id.tv_tag_name);
            tagContainer = itemView.findViewById(R.id.tag_container);
            
            itemView.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    Tag tag = tags.get(getAdapterPosition());
                    
                    if (isSelectionMode) {
                        // Mode sélection : basculer la sélection
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag);
                        } else {
                            selectedTags.add(tag);
                        }
                        notifyItemChanged(getAdapterPosition());
                        
                        if (listener != null) {
                            listener.onTagSelectionChanged(getSelectedTags());
                        }
                    } else {
                        // Mode normal : déclencher le clic
                        if (listener != null) {
                            listener.onTagClick(tag);
                        }
                    }
                }
            });
        }
        
        public void bind(Tag tag, boolean isSelected) {
            tvTagName.setText(tag.getName());
            
            // Styling basé sur la sélection
            if (isSelectionMode) {
                tagContainer.setSelected(isSelected);
                tagContainer.setActivated(isSelected);
            } else {
                tagContainer.setSelected(false);
                tagContainer.setActivated(false);
            }
        }
    }
}
