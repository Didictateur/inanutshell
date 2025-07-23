package fr.didictateur.inanutshell;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MealPlanWeekAdapter extends RecyclerView.Adapter<MealPlanWeekAdapter.DayViewHolder> {
    
    private List<DayMealPlan> dayMealPlans;
    private OnMealPlanClickListener mealPlanClickListener;
    private OnAddMealClickListener addMealClickListener;
    private OnDeleteMealClickListener deleteMealClickListener;
    
    public interface OnMealPlanClickListener {
        void onMealPlanClick(MealPlanWithRecette mealPlan);
    }
    
    public interface OnAddMealClickListener {
        void onAddMealClick(String date, String mealType);
    }
    
    public interface OnDeleteMealClickListener {
        void onDeleteMealClick(MealPlanWithRecette mealPlan);
    }
    
    public MealPlanWeekAdapter(List<DayMealPlan> dayMealPlans, 
                              OnMealPlanClickListener mealPlanClickListener,
                              OnAddMealClickListener addMealClickListener,
                              OnDeleteMealClickListener deleteMealClickListener) {
        this.dayMealPlans = dayMealPlans;
        this.mealPlanClickListener = mealPlanClickListener;
        this.addMealClickListener = addMealClickListener;
        this.deleteMealClickListener = deleteMealClickListener;
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
        private CardView cardView;
        
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNameText = itemView.findViewById(R.id.dayNameText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
            cardView = (CardView) itemView;
        }
        
        public void bind(DayMealPlan dayMealPlan) {
            dayNameText.setText(dayMealPlan.getDayName());
            
            // Vérifier si c'est le jour actuel
            boolean isToday = isToday(dayMealPlan.getDate());
            
            if (isToday) {
                // Mettre en évidence le jour actuel
                highlightCurrentDay();
            } else {
                // Style normal
                resetDayStyle();
            }
            
            // Appliquer la couleur du thème au nom du jour
            applyThemeColorToText(dayNameText);

            mealsContainer.removeAllViews();

            // Ajouter les repas avec support multiple
            addMealTypeSection("Petit déjeuner", dayMealPlan.getBreakfast(), dayMealPlan.getDate(), "breakfast");
            addMealTypeSection("Déjeuner", dayMealPlan.getLunch(), dayMealPlan.getDate(), "lunch");
            addMealTypeSection("Dîner", dayMealPlan.getDinner(), dayMealPlan.getDate(), "dinner");
        }
        
        private boolean isToday(String date) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar today = Calendar.getInstance();
                String todayString = dateFormat.format(today.getTime());
                return todayString.equals(date);
            } catch (Exception e) {
                return false;
            }
        }
        
        private void highlightCurrentDay() {
            // Appliquer un style spécial pour le jour actuel
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
            String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
            
            int colorResId = itemView.getContext().getResources()
                    .getIdentifier(colorName, "color", itemView.getContext().getPackageName());
            
            if (colorResId != 0) {
                int themeColor = ContextCompat.getColor(itemView.getContext(), colorResId);
                
                // Changer la couleur de fond de la carte pour le jour actuel
                int lightThemeColor = adjustAlpha(themeColor, 0.1f);
                cardView.setCardBackgroundColor(lightThemeColor);
                cardView.setCardElevation(8f);
                
                // Nom du jour en gras et plus grand
                dayNameText.setTypeface(dayNameText.getTypeface(), android.graphics.Typeface.BOLD);
                dayNameText.setTextSize(20f);
                dayNameText.setTextColor(themeColor);
            }
        }
        
        private void resetDayStyle() {
            // Style normal
            cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            cardView.setCardElevation(4f);
            dayNameText.setTextSize(18f);
            dayNameText.setTypeface(dayNameText.getTypeface(), android.graphics.Typeface.NORMAL);
        }
        
        private int adjustAlpha(int color, float factor) {
            int alpha = Math.round(255 * factor);
            int red = android.graphics.Color.red(color);
            int green = android.graphics.Color.green(color);
            int blue = android.graphics.Color.blue(color);
            return android.graphics.Color.argb(alpha, red, green, blue);
        }
        
        private void addMealTypeSection(String mealLabel, List<MealPlanWithRecette> meals, String date, String mealType) {
            // Créer une section pour ce type de repas
            View sectionView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.meal_type_section, mealsContainer, false);
            
            TextView sectionTitle = sectionView.findViewById(R.id.mealTypeSectionTitle);
            LinearLayout sectionContainer = sectionView.findViewById(R.id.mealSectionContainer);
            
            sectionTitle.setText(mealLabel);
            
            // Ajouter tous les repas de cette catégorie
            if (meals != null && !meals.isEmpty()) {
                for (MealPlanWithRecette meal : meals) {
                    addMealView(null, meal, date, mealType, sectionContainer);
                }
            }
            
            // Toujours ajouter un bouton pour ajouter un nouveau repas
            addAddMealButton(date, mealType, sectionContainer);
            
            mealsContainer.addView(sectionView);
        }
        
        private void addAddMealButton(String date, String mealType, LinearLayout container) {
            View addView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal_slot, container, false);
            
            TextView recipeNameText = addView.findViewById(R.id.recipeNameText);
            Button addButton = addView.findViewById(R.id.addMealButton);
            TextView mealTypeText = addView.findViewById(R.id.mealTypeText);
            
            // Cacher le type de repas pour le bouton d'ajout
            if (mealTypeText != null) mealTypeText.setVisibility(View.GONE);
            
            recipeNameText.setVisibility(View.GONE);
            addButton.setVisibility(View.VISIBLE);
            addButton.setText("+ Ajouter");
            
            addButton.setOnClickListener(v -> {
                if (addMealClickListener != null) {
                    addMealClickListener.onAddMealClick(date, mealType);
                }
            });
            
            // Style pour bouton d'ajout
            addView.setBackgroundColor(itemView.getContext().getColor(R.color.meal_empty));
            
            container.addView(addView);
        }
        
        private void addMealView(String mealLabel, MealPlanWithRecette mealPlan, String date, String mealType, LinearLayout container) {
            View mealView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal_slot, container, false);
            
            TextView mealTypeText = mealView.findViewById(R.id.mealTypeText);
            TextView recipeNameText = mealView.findViewById(R.id.recipeNameText);
            Button addButton = mealView.findViewById(R.id.addMealButton);
            ImageButton deleteButton = mealView.findViewById(R.id.deleteMealButton);
            
            // Cacher le type de repas dans les sections (déjà affiché en titre)
            if (mealTypeText != null) mealTypeText.setVisibility(View.GONE);
            
            if (mealPlan != null && mealPlan.getRecetteTitre() != null) {
                // Repas planifié
                recipeNameText.setText(mealPlan.getRecetteTitre());
                recipeNameText.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                
                mealView.setOnClickListener(v -> {
                    if (mealPlanClickListener != null) {
                        mealPlanClickListener.onMealPlanClick(mealPlan);
                    }
                });
                
                deleteButton.setOnClickListener(v -> {
                    if (deleteMealClickListener != null) {
                        deleteMealClickListener.onDeleteMealClick(mealPlan);
                    }
                });
                
                // Style pour repas planifié
                mealView.setBackgroundColor(itemView.getContext().getColor(R.color.meal_with_recipe));
            } else {
                // S'assurer que le bouton de suppression est caché pour les emplacements vides
                deleteButton.setVisibility(View.GONE);
            }
            
            container.addView(mealView);
        }
        
        private void addMealView(String mealLabel, MealPlanWithRecette mealPlan, String date, String mealType) {
            // Méthode de compatibilité pour l'ancien système
            View mealView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal_slot, mealsContainer, false);
            
            TextView mealTypeText = mealView.findViewById(R.id.mealTypeText);
            TextView recipeNameText = mealView.findViewById(R.id.recipeNameText);
            Button addButton = mealView.findViewById(R.id.addMealButton);
            
            if (mealTypeText != null) {
                mealTypeText.setText(mealLabel);
            }
            
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
