package fr.didictateur.inanutshell.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Tool;
import java.util.List;
import java.util.ArrayList;

public class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.ToolViewHolder> {
    
    private List<Tool> tools = new ArrayList<>();
    private OnToolClickListener listener;
    private boolean isSelectionMode = false;
    private List<Tool> selectedTools = new ArrayList<>();
    
    public interface OnToolClickListener {
        void onToolClick(Tool tool);
        void onToolSelectionChanged(List<Tool> selectedTools);
    }
    
    public ToolAdapter(OnToolClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool, parent, false);
        return new ToolViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        Tool tool = tools.get(position);
        boolean isSelected = selectedTools.contains(tool);
        holder.bind(tool, isSelected);
    }
    
    @Override
    public int getItemCount() {
        return tools.size();
    }
    
    public void setTools(List<Tool> tools) {
        this.tools = tools != null ? tools : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addTool(Tool tool) {
        tools.add(tool);
        notifyItemInserted(tools.size() - 1);
    }
    
    public void removeTool(int position) {
        if (position >= 0 && position < tools.size()) {
            Tool removedTool = tools.remove(position);
            selectedTools.remove(removedTool);
            notifyItemRemoved(position);
        }
    }
    
    // Mode sélection pour assigner des outils aux recettes
    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedTools.clear();
        }
        notifyDataSetChanged();
    }
    
    public void setSelectedTools(List<Tool> selectedTools) {
        this.selectedTools = selectedTools != null ? selectedTools : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Tool> getSelectedTools() {
        return new ArrayList<>(selectedTools);
    }
    
    class ToolViewHolder extends RecyclerView.ViewHolder {
        private TextView tvToolName;
        private View toolContainer;
        
        public ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvToolName = itemView.findViewById(R.id.tv_tool_name);
            toolContainer = itemView.findViewById(R.id.tool_container);
            
            itemView.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    Tool tool = tools.get(getAdapterPosition());
                    
                    if (isSelectionMode) {
                        // Mode sélection : basculer la sélection
                        if (selectedTools.contains(tool)) {
                            selectedTools.remove(tool);
                        } else {
                            selectedTools.add(tool);
                        }
                        notifyItemChanged(getAdapterPosition());
                        
                        if (listener != null) {
                            listener.onToolSelectionChanged(getSelectedTools());
                        }
                    } else {
                        // Mode normal : déclencher le clic
                        if (listener != null) {
                            listener.onToolClick(tool);
                        }
                    }
                }
            });
        }
        
        public void bind(Tool tool, boolean isSelected) {
            tvToolName.setText(tool.getName());
            
            // Styling basé sur la sélection
            if (isSelectionMode) {
                toolContainer.setSelected(isSelected);
                toolContainer.setActivated(isSelected);
            } else {
                toolContainer.setSelected(false);
                toolContainer.setActivated(false);
            }
        }
    }
}
