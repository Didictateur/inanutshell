package fr.didictateur.inanutshell;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptateur pour afficher les minuteries dans une RecyclerView
 */
public class TimersAdapter extends RecyclerView.Adapter<TimersAdapter.TimerViewHolder> {
    
    private List<Timer> timers;
    private final TimerClickListener clickListener;
    
    public interface TimerClickListener {
        void onTimerClick(Timer timer);
        void onStartPauseClick(Timer timer);
        void onResetClick(Timer timer);
        void onDeleteClick(Timer timer);
    }
    
    public TimersAdapter(List<Timer> timers, TimerClickListener clickListener) {
        this.timers = timers;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public TimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_timer, parent, false);
        return new TimerViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TimerViewHolder holder, int position) {
        Timer timer = timers.get(position);
        holder.bind(timer, clickListener);
    }
    
    @Override
    public int getItemCount() {
        return timers.size();
    }
    
    public void updateTimers(List<Timer> newTimers) {
        this.timers = newTimers;
        notifyDataSetChanged();
    }
    
    static class TimerViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvTimerName;
        private final TextView tvTimeRemaining;
        private final TextView tvOriginalDuration;
        private final TextView tvTimerState;
        private final ProgressBar progressBar;
        private final ImageButton btnStartPause;
        private final ImageButton btnReset;
        private final ImageButton btnDelete;
        
        public TimerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvTimerName = itemView.findViewById(R.id.tvTimerName);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            tvOriginalDuration = itemView.findViewById(R.id.tvOriginalDuration);
            tvTimerState = itemView.findViewById(R.id.tvTimerState);
            progressBar = itemView.findViewById(R.id.progressBar);
            btnStartPause = itemView.findViewById(R.id.btnStartPause);
            btnReset = itemView.findViewById(R.id.btnReset);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
        
        public void bind(Timer timer, TimerClickListener clickListener) {
            // Nom de la minuterie
            tvTimerName.setText(timer.name);
            
            // Temps restant
            tvTimeRemaining.setText(timer.getFormattedTime());
            
            // Durée originale
            tvOriginalDuration.setText("sur " + timer.getFormattedOriginalDuration());
            
            // État de la minuterie
            String stateText = getStateText(timer.state);
            tvTimerState.setText(stateText);
            tvTimerState.setTextColor(getStateColor(timer.state));
            
            // Barre de progression
            progressBar.setMax(1000); // Pour une meilleure précision
            progressBar.setProgress((int) (timer.getProgress() * 1000));
            
            // Couleur de la carte selon l'état
            cardView.setCardBackgroundColor(getCardBackgroundColor(timer.state));
            
            // Bouton Start/Pause
            if (timer.isRunning()) {
                btnStartPause.setImageResource(android.R.drawable.ic_media_pause);
                btnStartPause.setContentDescription("Pause");
            } else if (timer.isPaused() || timer.state == Timer.TimerState.CREATED) {
                btnStartPause.setImageResource(android.R.drawable.ic_media_play);
                btnStartPause.setContentDescription("Démarrer");
            } else {
                // Timer terminé ou annulé
                btnStartPause.setImageResource(android.R.drawable.ic_media_play);
                btnStartPause.setContentDescription("Recommencer");
                btnStartPause.setEnabled(false);
            }
            
            // Bouton Reset
            btnReset.setEnabled(timer.isActive() || timer.isFinished());
            
            // Click listeners
            cardView.setOnClickListener(v -> clickListener.onTimerClick(timer));
            
            btnStartPause.setOnClickListener(v -> {
                if (timer.state != Timer.TimerState.FINISHED && timer.state != Timer.TimerState.CANCELLED) {
                    clickListener.onStartPauseClick(timer);
                }
            });
            
            btnReset.setOnClickListener(v -> clickListener.onResetClick(timer));
            btnDelete.setOnClickListener(v -> clickListener.onDeleteClick(timer));
            
            // Style spécial pour les minuteries terminées
            if (timer.isFinished()) {
                tvTimeRemaining.setText("00:00");
                tvTimeRemaining.setTextColor(Color.RED);
                tvTimerName.setAlpha(0.7f);
            } else {
                tvTimeRemaining.setTextColor(Color.BLACK);
                tvTimerName.setAlpha(1.0f);
            }
        }
        
        private String getStateText(Timer.TimerState state) {
            switch (state) {
                case CREATED: return "Prêt";
                case RUNNING: return "En cours";
                case PAUSED: return "En pause";
                case FINISHED: return "Terminé";
                case CANCELLED: return "Annulé";
                default: return "Inconnu";
            }
        }
        
        private int getStateColor(Timer.TimerState state) {
            switch (state) {
                case CREATED: return Color.BLUE;
                case RUNNING: return Color.GREEN;
                case PAUSED: return Color.YELLOW;
                case FINISHED: return Color.RED;
                case CANCELLED: return Color.GRAY;
                default: return Color.BLACK;
            }
        }
        
        private int getCardBackgroundColor(Timer.TimerState state) {
            switch (state) {
                case RUNNING: 
                    return Color.parseColor("#E8F5E8"); // Vert clair
                case PAUSED: 
                    return Color.parseColor("#FFF8E1"); // Jaune clair
                case FINISHED: 
                    return Color.parseColor("#FFEBEE"); // Rouge clair
                case CANCELLED: 
                    return Color.parseColor("#F5F5F5"); // Gris clair
                default: 
                    return Color.WHITE;
            }
        }
    }
}
