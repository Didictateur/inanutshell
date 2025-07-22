package fr.didictateur.inanutshell;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MealPlanWeekAdapter extends RecyclerView.Adapter<MealPlanWeekAdapter.DayViewHolder> {
    
    private List<DayMealPlan> dayMealPlans;
    private OnMealPlanClickListener mealPlanClickListener;
    private OnAddMealClickListener addMealClickListener;
    
    public interface OnMealPlanClickListener {
        void onMealPlanClick(MealPlanWithRecette mealPlan);
    }
    
    public interface OnAddMealClickListener {
        void onAddMealClick(String date, String mealType);
    }
    
    public MealPlanWeekAdapter(List<DayMealPlan> dayMealPlans, 
                              OnMealPlanClickListener mealPlanClickListener,
                              OnAddMealClickListener addMealClickListener) {
        this.dayMealPlans = dayMealPlans;
        this.mealPlanClickListener = mealPlanClickListener;
        this.addMealClickListener = addMealClickListener;
    }
    
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_meal_plan, parent, false);
        return new DayViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayMealPlan dayMealPlan = dayMealPlans.get(position);
        holder.bind(dayMealPlan);
    }
    
    @Override
    public int getItemCount() {
        return dayMealPlans.size();
    }
    
    public void updateMealPlans(List<DayMealPlan> newDayMealPlans) {
        this.dayMealPlans = newDayMealPlans;
        notifyDataSetChanged();
    }
    
    class DayViewHolder extends RecyclerView.ViewHolder {
        private TextView dayNameText;
        private LinearLayout mealsContainer;
        
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNameText = itemView.findViewById(R.id.dayNameText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
        }
        
        public void bind(DayMealPlan dayMealPlan) {
            dayNameText.setText(dayMealPlan.getDayName());
            
            // Appliquer la couleur du thème au nom du jour
            applyThemeColorToText(dayNameText);
            
            mealsContainer.removeAllViews();
            
            // Ajouter les repas
            addMealView("Petit déjeuner", dayMealPlan.getBreakfast(), dayMealPlan.getDate(), "breakfast");
            addMealView("Déjeuner", dayMealPlan.getLunch(), dayMealPlan.getDate(), "lunch");
            addMealView("Dîner", dayMealPlan.getDinner(), dayMealPlan.getDate(), "dinner");
        }
        
        private void addMealView(String mealLabel, MealPlanWithRecette mealPlan, String date, String mealType) {
            View mealView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal_slot, mealsContainer, false);
            
            TextView mealTypeText = mealView.findViewById(R.id.mealTypeText);
            TextView recipeNameText = mealView.findViewById(R.id.recipeNameText);
            Button addButton = mealView.findViewById(R.id.addMealButton);
            
            mealTypeText.setText(mealLabel);
            
            if (mealPlan != null && mealPlan.getRecetteTitre() != null) {
                // Repas planifié
                recipeNameText.setText(mealPlan.getRecetteTitre());
                recipeNameText.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                
                mealView.setOnClickListener(v -> {
                    if (mealPlanClickListener != null) {
                        mealPlanClickListener.onMealPlanClick(mealPlan);
                    }
                });
                
                // Style pour repas planifié
                mealView.setBackgroundColor(itemView.getContext().getColor(R.color.meal_with_recipe));
            } else {
                // Emplacement vide
                recipeNameText.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                addButton.setText("Ajouter");
                
                addButton.setOnClickListener(v -> {
                    if (addMealClickListener != null) {
                        addMealClickListener.onAddMealClick(date, mealType);
                    }
                });
                
                // Style pour emplacement vide
                mealView.setBackgroundColor(itemView.getContext().getColor(R.color.meal_empty));
            }
            
            mealsContainer.addView(mealView);
        }
        
        private void applyThemeColorToText(TextView textView) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
            String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
            
            int colorResId = itemView.getContext().getResources()
                    .getIdentifier(colorName, "color", itemView.getContext().getPackageName());
            
            if (colorResId != 0) {
                int themeColor = ContextCompat.getColor(itemView.getContext(), colorResId);
                textView.setTextColor(themeColor);
            }
        }
    }
}
