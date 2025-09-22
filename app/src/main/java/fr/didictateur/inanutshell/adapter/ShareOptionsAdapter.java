package fr.didictateur.inanutshell.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.ShareOption;

public class ShareOptionsAdapter extends RecyclerView.Adapter<ShareOptionsAdapter.ShareOptionViewHolder> {
    
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
    public ShareOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_option, parent, false);
        return new ShareOptionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ShareOptionViewHolder holder, int position) {
        ShareOption option = shareOptions.get(position);
        holder.bind(option, listener);
    }
    
    @Override
    public int getItemCount() {
        return shareOptions.size();
    }
    
    static class ShareOptionViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView titleTextView;
        private TextView descriptionTextView;
        
        public ShareOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.option_icon);
            titleTextView = itemView.findViewById(R.id.option_title);
            descriptionTextView = itemView.findViewById(R.id.option_description);
        }
        
        public void bind(ShareOption option, OnShareOptionClickListener listener) {
            iconImageView.setImageResource(option.getIconResId());
            titleTextView.setText(option.getTitle());
            descriptionTextView.setText(option.getDescription());
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareOptionClick(option);
                }
            });
        }
    }
}
