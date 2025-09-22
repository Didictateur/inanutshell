package fr.didictateur.inanutshell.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.analytics.AnalyticsManager;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {
    
    private Context context;
    private List<AnalyticsManager.Insight> insights;
    private SimpleDateFormat dateFormat;
    
    public InsightsAdapter(Context context) {
        this.context = context;
        this.insights = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_insight, parent, false);
        return new InsightViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        AnalyticsManager.Insight insight = insights.get(position);
        holder.bind(insight);
    }
    
    @Override
    public int getItemCount() {
        return insights.size();
    }
    
    public void updateInsights(List<AnalyticsManager.Insight> newInsights) {
        this.insights = new ArrayList<>(newInsights);
        notifyDataSetChanged();
    }
    
    class InsightViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imageInsightIcon;
        private TextView textInsightTitle;
        private TextView textInsightDescription;
        private TextView textInsightTime;
        
        public InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imageInsightIcon = itemView.findViewById(R.id.image_insight_icon);
            textInsightTitle = itemView.findViewById(R.id.text_insight_title);
            textInsightDescription = itemView.findViewById(R.id.text_insight_description);
            textInsightTime = itemView.findViewById(R.id.text_insight_time);
        }
        
        public void bind(AnalyticsManager.Insight insight) {
            textInsightTitle.setText(insight.title);
            textInsightDescription.setText(insight.description);
            textInsightTime.setText(dateFormat.format(insight.createdAt));
            
            // Définir l'icône et la couleur selon le type
            switch (insight.type) {
                case "info":
                    imageInsightIcon.setImageResource(R.drawable.ic_info);
                    itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                    break;
                    
                case "positive":
                    imageInsightIcon.setImageResource(R.drawable.ic_trending_up);
                    itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                    break;
                    
                case "warning":
                    imageInsightIcon.setImageResource(R.drawable.ic_warning);
                    itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                    break;
                    
                case "trending":
                    imageInsightIcon.setImageResource(R.drawable.ic_trending_up);
                    itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                    break;
                    
                default: // neutral
                    imageInsightIcon.setImageResource(R.drawable.ic_info);
                    itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                    break;
            }
        }
    }
}
