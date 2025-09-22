package fr.didictateur.inanutshell.api;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.ui.ApiManagementActivity;

public class ApiClientsAdapter extends RecyclerView.Adapter<ApiClientsAdapter.ClientViewHolder> {
    
    private Context context;
    private List<ApiServer.ApiClient> clients;
    private SimpleDateFormat dateFormat;
    
    public ApiClientsAdapter(Context context, List<ApiServer.ApiClient> clients) {
        this.context = context;
        this.clients = clients;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_api_client, parent, false);
        return new ClientViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        ApiServer.ApiClient client = clients.get(position);
        holder.bind(client);
    }
    
    @Override
    public int getItemCount() {
        return clients.size();
    }
    
    public void updateClients(List<ApiServer.ApiClient> newClients) {
        this.clients = newClients;
        notifyDataSetChanged();
    }
    
    class ClientViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textClientName;
        private TextView textClientDescription;
        private TextView textClientId;
        private TextView textClientPermissions;
        private TextView textClientCreated;
        private TextView textClientStatus;
        private ImageButton buttonRevokeClient;
        
        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textClientName = itemView.findViewById(R.id.text_client_name);
            textClientDescription = itemView.findViewById(R.id.text_client_description);
            textClientId = itemView.findViewById(R.id.text_client_id);
            textClientPermissions = itemView.findViewById(R.id.text_client_permissions);
            textClientCreated = itemView.findViewById(R.id.text_client_created);
            textClientStatus = itemView.findViewById(R.id.text_client_status);
            buttonRevokeClient = itemView.findViewById(R.id.button_revoke_client);
        }
        
        public void bind(ApiServer.ApiClient client) {
            textClientName.setText(client.name);
            textClientDescription.setText(client.description.isEmpty() ? 
                "Aucune description" : client.description);
            textClientId.setText("ID: " + client.clientId.substring(0, 8) + "...");
            
            // Afficher les permissions
            if (client.allowedPermissions.isEmpty()) {
                textClientPermissions.setText("Aucune permission");
                textClientPermissions.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            } else {
                StringBuilder permissions = new StringBuilder();
                for (int i = 0; i < client.allowedPermissions.size(); i++) {
                    if (i > 0) permissions.append(", ");
                    
                    String permission = client.allowedPermissions.get(i);
                    // Convertir en format lisible
                    switch (permission) {
                        case "recipes:read":
                            permissions.append("Lecture recettes");
                            break;
                        case "recipes:write":
                            permissions.append("Écriture recettes");
                            break;
                        case "comments:read":
                            permissions.append("Lecture commentaires");
                            break;
                        case "comments:write":
                            permissions.append("Écriture commentaires");
                            break;
                        case "nutrition:read":
                            permissions.append("Lecture nutrition");
                            break;
                        case "users:read":
                            permissions.append("Lecture utilisateurs");
                            break;
                        default:
                            permissions.append(permission);
                    }
                }
                textClientPermissions.setText(permissions.toString());
                textClientPermissions.setTextColor(context.getResources().getColor(android.R.color.black));
            }
            
            // Date de création
            textClientCreated.setText("Créé le " + dateFormat.format(client.createdAt));
            
            // Statut
            if (client.isActive) {
                textClientStatus.setText("🟢 Actif");
                textClientStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                buttonRevokeClient.setVisibility(View.VISIBLE);
            } else {
                textClientStatus.setText("🔴 Révoqué");
                textClientStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                buttonRevokeClient.setVisibility(View.GONE);
            }
            
            // Action de révocation
            buttonRevokeClient.setOnClickListener(v -> {
                if (context instanceof ApiManagementActivity) {
                    ((ApiManagementActivity) context).revokeClient(client.clientId);
                }
            });
        }
    }
}
