package fr.didictateur.inanutshell.adapters;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.cloud.CloudBackupManager;

public class BackupsAdapter extends RecyclerView.Adapter<BackupsAdapter.BackupViewHolder> {
    
    private Context context;
    private List<CloudBackupManager.BackupMetadata> backups;
    private SimpleDateFormat dateFormat;
    private OnBackupActionListener actionListener;
    
    public interface OnBackupActionListener {
        void onRestoreBackup(CloudBackupManager.BackupMetadata metadata);
        void onDeleteBackup(CloudBackupManager.BackupMetadata metadata);
        void onViewBackupDetails(CloudBackupManager.BackupMetadata metadata);
    }
    
    public BackupsAdapter(Context context) {
        this.context = context;
        this.backups = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public BackupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_backup, parent, false);
        return new BackupViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BackupViewHolder holder, int position) {
        CloudBackupManager.BackupMetadata backup = backups.get(position);
        holder.bind(backup);
    }
    
    @Override
    public int getItemCount() {
        return backups.size();
    }
    
    public void updateBackups(List<CloudBackupManager.BackupMetadata> newBackups) {
        this.backups = new ArrayList<>(newBackups);
        notifyDataSetChanged();
    }
    
    public void setOnBackupActionListener(OnBackupActionListener listener) {
        this.actionListener = listener;
    }
    
    class BackupViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imageProviderIcon;
        private TextView textBackupDate;
        private TextView textBackupDescription;
        private TextView textBackupSize;
        private TextView textBackupStats;
        private ImageView imageEncrypted;
        private ImageButton buttonRestore;
        private ImageButton buttonDelete;
        private ImageButton buttonDetails;
        
        public BackupViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imageProviderIcon = itemView.findViewById(R.id.image_provider_icon);
            textBackupDate = itemView.findViewById(R.id.text_backup_date);
            textBackupDescription = itemView.findViewById(R.id.text_backup_description);
            textBackupSize = itemView.findViewById(R.id.text_backup_size);
            textBackupStats = itemView.findViewById(R.id.text_backup_stats);
            imageEncrypted = itemView.findViewById(R.id.image_encrypted);
            buttonRestore = itemView.findViewById(R.id.button_restore);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }
        
        public void bind(CloudBackupManager.BackupMetadata backup) {
            // Date
            textBackupDate.setText(dateFormat.format(backup.createdAt));
            
            // Description
            if (backup.description != null && !backup.description.isEmpty()) {
                textBackupDescription.setText(backup.description);
            } else {
                textBackupDescription.setText("Sauvegarde automatique");
            }
            
            // Taille
            String sizeText = Formatter.formatFileSize(context, backup.size);
            textBackupSize.setText(sizeText);
            
            // Statistiques
            String statsText = String.format("%d recettes • %d commentaires • %d utilisateurs", 
                backup.recipesCount, backup.commentsCount, backup.usersCount);
            textBackupStats.setText(statsText);
            
            // Icône du provider
            switch (backup.provider) {
                case GOOGLE_DRIVE:
                    imageProviderIcon.setImageResource(R.drawable.ic_google_drive);
                    break;
                case DROPBOX:
                    imageProviderIcon.setImageResource(R.drawable.ic_dropbox);
                    break;
                case ONEDRIVE:
                    imageProviderIcon.setImageResource(R.drawable.ic_onedrive);
                    break;
                default:
                    imageProviderIcon.setImageResource(R.drawable.ic_cloud);
                    break;
            }
            
            // Icône de chiffrement
            imageEncrypted.setVisibility(backup.isEncrypted ? View.VISIBLE : View.GONE);
            
            // Actions
            buttonRestore.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRestoreBackup(backup);
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteBackup(backup);
                }
            });
            
            buttonDetails.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onViewBackupDetails(backup);
                }
            });
            
            // Click sur l'item entier pour voir les détails
            itemView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onViewBackupDetails(backup);
                }
            });
        }
    }
}
