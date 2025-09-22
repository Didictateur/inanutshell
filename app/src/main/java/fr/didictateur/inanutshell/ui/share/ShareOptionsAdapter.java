package fr.didictateur.inanutshell.ui.share;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.didictateur.inanutshell.R;

/**
 * Adapter pour les options de partage
 */
public class ShareOptionsAdapter extends RecyclerView.Adapter<ShareOptionsAdapter.ViewHolder> {
    
    private List<ShareOption> shareOptions;
    private OnShareOptionClickListener listener;
    
    public interface OnShareOptionClickListener {
        void onShareOptionClick(ShareOption option);
    }
    
    public ShareOptionsAdapter(List<ShareOption> shareOptions, OnShareOptionClickListener listener) {
        this.shareOptions = shareOptions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_option, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShareOption option = shareOptions.get(position);
        holder.bind(option);
    }
    
    @Override
    public int getItemCount() {
        return shareOptions.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView titleTextView;
        private TextView descriptionTextView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.option_icon);
            titleTextView = itemView.findViewById(R.id.option_title);
            descriptionTextView = itemView.findViewById(R.id.option_description);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onShareOptionClick(shareOptions.get(position));
                    }
                }
            });
        }
        
        public void bind(ShareOption option) {
            iconImageView.setImageResource(option.getIconResId());
            titleTextView.setText(option.getTitle());
            descriptionTextView.setText(option.getDescription());
        }
    }
}
