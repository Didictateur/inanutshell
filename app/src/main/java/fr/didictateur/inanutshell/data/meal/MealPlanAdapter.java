package fr.didictateur.inanutshell.data.meal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import fr.didictateur.inanutshell.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les repas planifiés
 */
public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.MealPlanViewHolder> {
    
    private Context context;
    private List<MealPlan> mealPlans;
    private OnMealPlanClickListener onMealPlanClickListener;
    private SimpleDateFormat timeFormatter;
    
    public interface OnMealPlanClickListener {
        void onMealPlanClick(MealPlan mealPlan);
        void onEditClick(MealPlan mealPlan);
        void onDeleteClick(MealPlan mealPlan);
        void onMarkCompletedClick(MealPlan mealPlan);
    }
    
    public MealPlanAdapter(Context context) {
        this.context = context;
        this.mealPlans = new ArrayList<>();
        this.timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    public void setMealPlans(List<MealPlan> mealPlans) {
        this.mealPlans = mealPlans != null ? mealPlans : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setOnMealPlanClickListener(OnMealPlanClickListener listener) {
        this.onMealPlanClickListener = listener;
    }
    
    @NonNull
    @Override
    public MealPlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_plan, parent, false);
        return new MealPlanViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MealPlanViewHolder holder, int position) {
        MealPlan mealPlan = mealPlans.get(position);
        holder.bind(mealPlan);
    }
    
    @Override
    public int getItemCount() {
        return mealPlans.size();
    }
    
    class MealPlanViewHolder extends RecyclerView.ViewHolder {
        
        private MaterialCardView cardView;
        private TextView textRecipeName;
        private TextView textMealType;
        private TextView textServings;
        private TextView textNotes;
        private Chip chipCompleted;
        private ImageView iconMealType;
        private ImageButton btnEdit;
        private ImageButton btnDelete;
        private ImageButton btnMarkCompleted;
        
        public MealPlanViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardMealPlan);
            textRecipeName = itemView.findViewById(R.id.textRecipeName);
            textMealType = itemView.findViewById(R.id.textMealType);
            textServings = itemView.findViewById(R.id.textServings);
            textNotes = itemView.findViewById(R.id.textNotes);
            chipCompleted = itemView.findViewById(R.id.chipCompleted);
            iconMealType = itemView.findViewById(R.id.iconMealType);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnMarkCompleted = itemView.findViewById(R.id.btnMarkCompleted);
        }
        
        public void bind(MealPlan mealPlan) {
            // Nom de la recette
            textRecipeName.setText(mealPlan.getRecipeName());
            
            // Type de repas
            textMealType.setText(mealPlan.getMealType().getDisplayName());
            
            // Nombre de portions
            String servingsText = mealPlan.getServings() + " portion" + 
                                (mealPlan.getServings() > 1 ? "s" : "");
            textServings.setText(servingsText);
            
            // Notes (optionnel)
            if (mealPlan.getNotes() != null && !mealPlan.getNotes().trim().isEmpty()) {
                textNotes.setText(mealPlan.getNotes());
                textNotes.setVisibility(View.VISIBLE);
            } else {
                textNotes.setVisibility(View.GONE);
            }
            
            // État terminé
            if (mealPlan.isCompleted()) {
                chipCompleted.setVisibility(View.VISIBLE);
                cardView.setAlpha(0.7f);
                btnMarkCompleted.setVisibility(View.GONE);
            } else {
                chipCompleted.setVisibility(View.GONE);
                cardView.setAlpha(1.0f);
                btnMarkCompleted.setVisibility(View.VISIBLE);
            }
            
            // Icône selon le type de repas
            setMealTypeIcon(mealPlan.getMealType());
            
            // Listeners
            cardView.setOnClickListener(v -> {
                if (onMealPlanClickListener != null) {
                    onMealPlanClickListener.onMealPlanClick(mealPlan);
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                if (onMealPlanClickListener != null) {
                    onMealPlanClickListener.onEditClick(mealPlan);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (onMealPlanClickListener != null) {
                    onMealPlanClickListener.onDeleteClick(mealPlan);
                }
            });
            
            btnMarkCompleted.setOnClickListener(v -> {
                if (onMealPlanClickListener != null) {
                    onMealPlanClickListener.onMarkCompletedClick(mealPlan);
                }
            });
        }
        
        private void setMealTypeIcon(MealPlan.MealType mealType) {
            int iconRes;
            switch (mealType) {
                case BREAKFAST:
                    iconRes = R.drawable.ic_breakfast; // Icône petit-déjeuner
                    break;
                case LUNCH:
                    iconRes = R.drawable.ic_lunch; // Icône déjeuner
                    break;
                case DINNER:
                    iconRes = R.drawable.ic_dinner; // Icône dîner
                    break;
                case SNACK:
                    iconRes = R.drawable.ic_snack; // Icône collation
                    break;
                default:
                    iconRes = R.drawable.ic_restaurant; // Icône par défaut
                    break;
            }
            
            // Si les icônes n'existent pas encore, utiliser une icône par défaut
            try {
                iconMealType.setImageResource(iconRes);
            } catch (Exception e) {
                iconMealType.setImageResource(R.drawable.ic_restaurant);
            }
        }
    }
}
