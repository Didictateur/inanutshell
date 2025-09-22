package fr.didictateur.inanutshell.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.cloud.CloudBackupManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter pour afficher la liste des sauvegardes cloud
 */
public class BackupsAdapter extends RecyclerView.Adapter<BackupsAdapter.BackupViewHolder> {
    
    private List<CloudBackupManager.BackupInfo> backups;
    private Context context;
    private OnBackupActionListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnBackupActionListener {
        void onRestoreBackup(CloudBackupManager.BackupInfo backup);
        void onDeleteBackup(CloudBackupManager.BackupInfo backup);
        void onDownloadBackup(CloudBackupManager.BackupInfo backup);
    }
    
    public BackupsAdapter(Context context) {
        this.context = context;
        this.backups = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }
    
    public void setOnBackupActionListener(OnBackupActionListener listener) {
        this.listener = listener;
    }
    
    public void updateBackups(List<CloudBackupManager.BackupInfo> newBackups) {
        this.backups.clear();
        this.backups.addAll(newBackups);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public BackupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_backup, parent, false);
        return new BackupViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BackupViewHolder holder, int position) {
        CloudBackupManager.BackupInfo backup = backups.get(position);
        
        holder.textBackupName.setText(backup.name);
        holder.textBackupDate.setText(dateFormat.format(backup.creationDate));
        holder.textBackupSize.setText(formatFileSize(backup.size));
        holder.textBackupProvider.setText(backup.provider);
        
        // Icône selon le provider
        switch (backup.provider.toLowerCase()) {
            case "google_drive":
                holder.iconProvider.setImageResource(R.drawable.ic_google_drive);
                break;
            case "dropbox":
                holder.iconProvider.setImageResource(R.drawable.ic_dropbox);
                break;
            case "onedrive":
                holder.iconProvider.setImageResource(R.drawable.ic_onedrive);
                break;
            default:
                holder.iconProvider.setImageResource(R.drawable.ic_cloud);
                break;
        }
        
        // Actions
        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreBackup(backup);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteBackup(backup);
            }
        });
        
        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadBackup(backup);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return backups.size();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    static class BackupViewHolder extends RecyclerView.ViewHolder {
        TextView textBackupName;
        TextView textBackupDate;
        TextView textBackupSize;
        TextView textBackupProvider;
        ImageView iconProvider;
        Button btnRestore;
        Button btnDelete;
        Button btnDownload;
        
        public BackupViewHolder(@NonNull View itemView) {
            super(itemView);
            textBackupName = itemView.findViewById(R.id.textBackupName);
            textBackupDate = itemView.findViewById(R.id.textBackupDate);
            textBackupSize = itemView.findViewById(R.id.textBackupSize);
            textBackupProvider = itemView.findViewById(R.id.textBackupProvider);
            iconProvider = itemView.findViewById(R.id.iconProvider);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
}
