package fr.didictateur.inanutshell.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour afficher les insights automatiques générés par l'analytics
 */
public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {
    
    private List<InsightItem> insights;
    private Context context;
    
    public static class InsightItem {
        public String title;
        public String description;
        public String type; // "positive", "negative", "neutral", "warning"
        public int iconRes;
        
        public InsightItem(String title, String description, String type) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.iconRes = getIconForType(type);
        }
        
        private int getIconForType(String type) {
            switch (type) {
                case "positive":
                    return R.drawable.ic_trending_up;
                case "negative":
                    return R.drawable.ic_trending_down;
                case "warning":
                    return R.drawable.ic_warning;
                default:
                    return R.drawable.ic_info;
            }
        }
    }
    
    public InsightsAdapter(Context context) {
        this.context = context;
        this.insights = new ArrayList<>();
    }
    
    public void updateInsights(List<InsightItem> newInsights) {
        this.insights.clear();
        this.insights.addAll(newInsights);
        notifyDataSetChanged();
    }
    
    public void addInsight(InsightItem insight) {
        this.insights.add(insight);
        notifyItemInserted(insights.size() - 1);
    }
    
    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_insight, parent, false);
        return new InsightViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        InsightItem insight = insights.get(position);
        
        holder.textTitle.setText(insight.title);
        holder.textDescription.setText(insight.description);
        holder.iconInsight.setImageResource(insight.iconRes);
        
        // Couleur selon le type
        int backgroundColor = context.getColor(R.color.surface);
        int textColor = context.getColor(R.color.on_surface);
        
        switch (insight.type) {
            case "positive":
                backgroundColor = context.getColor(R.color.success_light);
                textColor = context.getColor(R.color.success_dark);
                break;
            case "negative":
                backgroundColor = context.getColor(R.color.error_light);
                textColor = context.getColor(R.color.error);
                break;
            case "warning":
                backgroundColor = context.getColor(R.color.warning_light);
                textColor = context.getColor(R.color.warning_dark);
                break;
        }
        
        holder.itemView.setBackgroundColor(backgroundColor);
        holder.textTitle.setTextColor(textColor);
        holder.textDescription.setTextColor(textColor);
    }
    
    @Override
    public int getItemCount() {
        return insights.size();
    }
    
    static class InsightViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textDescription;
        ImageView iconInsight;
        
        public InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_insight_title);
            textDescription = itemView.findViewById(R.id.text_insight_description);
            iconInsight = itemView.findViewById(R.id.image_insight_icon);
        }
    }
}
