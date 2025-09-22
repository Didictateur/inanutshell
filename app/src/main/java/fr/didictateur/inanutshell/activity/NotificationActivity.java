package fr.didictateur.inanutshell.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.adapter.NotificationAdapter;
import fr.didictateur.inanutshell.data.dao.NotificationDao;
import fr.didictateur.inanutshell.data.AppDatabase;
import fr.didictateur.inanutshell.data.model.Notification;
import fr.didictateur.inanutshell.service.NotificationService;
import fr.didictateur.inanutshell.service.RecipeSuggestionService;
import fr.didictateur.inanutshell.utils.PreferencesManager;

/**
 * Activité pour gérer les notifications et leurs paramètres
 */
public class NotificationActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationActivity";
    
    // UI Components
    private RecyclerView recyclerViewNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewEmpty;
    private FloatingActionButton fabSettings;
    
    // Adapter
    private NotificationAdapter notificationAdapter;
    
    // Services
    private NotificationDao notificationDao;
    private NotificationService notificationService;
    private RecipeSuggestionService suggestionService;
    private ExecutorService executorService;
    
    // Préférences
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        
        initializeServices();
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadNotifications();
    }
    
    /**
     * Initialise les services
     */
    private void initializeServices() {
        notificationDao = AppDatabase.getInstance(this).notificationDao();
        notificationService = new NotificationService(this);
        suggestionService = new RecipeSuggestionService(this);
        executorService = Executors.newCachedThreadPool();
        preferencesManager = new PreferencesManager(this);
    }
    
    /**
     * Initialise les vues
     */
    private void initializeViews() {
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabSettings = findViewById(R.id.fabSettings);
    }
    
    /**
     * Configure la toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
    }
    
    /**
     * Configure le RecyclerView
     */
    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(this, new ArrayList<>());
        
        // Actions sur les notifications
        notificationAdapter.setOnNotificationClickListener(notification -> {
            if (notification.getType() == Notification.NotificationType.RECIPE_SUGGESTION ||
                notification.getType() == Notification.NotificationType.NEW_RECIPE) {
                openRecipe(notification);
            }
            markAsRead(notification);
        });
        
        notificationAdapter.setOnNotificationDeleteListener(this::deleteNotification);
        
        recyclerViewNotifications.setAdapter(notificationAdapter);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        // Dividers
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerViewNotifications.addItemDecoration(divider);
    }
    
    /**
     * Configure les listeners
     */
    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        
        // Bouton paramètres
        fabSettings.setOnClickListener(v -> showSettingsBottomSheet());
    }
    
    /**
     * Charge les notifications
     */
    private void loadNotifications() {
        swipeRefreshLayout.setRefreshing(true);
        
        executorService.execute(() -> {
            try {
                List<Notification> notifications = notificationDao.getAllNotifications();
                
                runOnUiThread(() -> {
                    notificationAdapter.updateNotifications(notifications);
                    updateEmptyState(notifications.isEmpty());
                    swipeRefreshLayout.setRefreshing(false);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement des notifications", e);
                runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
            }
        });
    }
    
    /**
     * Met à jour l'état vide
     */
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerViewNotifications.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
            textViewEmpty.setText("Aucune notification\n\nActive les notifications dans les paramètres pour recevoir des suggestions de recettes et des rappels personnalisés !");
        } else {
            recyclerViewNotifications.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
        }
    }
    
    /**
     * Ouvre une recette depuis une notification
     */
    private void openRecipe(Notification notification) {
        try {
            String actionData = notification.getActionData();
            if (actionData != null && !actionData.isEmpty()) {
                int recipeId = Integer.parseInt(actionData);
                
                // TODO: Implémenter la navigation vers ViewRecetteActivity
                // Intent intent = new Intent(this, ViewRecetteActivity.class);
                // intent.putExtra("recette_id", recipeId);
                // startActivity(intent);
                
                // Pour l'instant, afficher un Toast
                Toast.makeText(this, "Ouverture de la recette ID: " + recipeId, Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de recette invalide: " + notification.getActionData());
        }
    }
    
    /**
     * Marque une notification comme lue
     */
    private void markAsRead(Notification notification) {
        if (!notification.isRead()) {
            executorService.execute(() -> {
                notificationDao.markAsRead(notification.getNotificationId(), true);
                
                runOnUiThread(() -> {
                    notification.setRead(true);
                    notificationAdapter.notifyItemChanged(
                        notificationAdapter.getNotifications().indexOf(notification)
                    );
                });
            });
        }
    }
    
    /**
     * Supprime une notification
     */
    private void deleteNotification(Notification notification) {
        executorService.execute(() -> {
            notificationDao.deleteNotificationById(notification.getNotificationId());
            
            runOnUiThread(() -> {
                int position = notificationAdapter.getNotifications().indexOf(notification);
                if (position != -1) {
                    notificationAdapter.removeNotification(position);
                    updateEmptyState(notificationAdapter.getNotifications().isEmpty());
                }
            });
        });
    }
    
    /**
     * Affiche la feuille de paramètres
     */
    private void showSettingsBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_notification_settings, null);
        bottomSheet.setContentView(view);
        
        setupSettingsSheet(view, bottomSheet);
        bottomSheet.show();
    }
    
    /**
     * Configure la feuille de paramètres
     */
    private void setupSettingsSheet(View view, BottomSheetDialog bottomSheet) {
        // Switches pour les types de notifications
        Switch switchMealReminders = view.findViewById(R.id.switchMealReminders);
        Switch switchRecipeSuggestions = view.findViewById(R.id.switchRecipeSuggestions);
        Switch switchCookingTips = view.findViewById(R.id.switchCookingTips);
        Switch switchNewRecipes = view.findViewById(R.id.switchNewRecipes);
        
        // Slider pour la fréquence des suggestions
        Slider sliderFrequency = view.findViewById(R.id.sliderFrequency);
        TextView textFrequency = view.findViewById(R.id.textFrequency);
        
        // Chips pour les moments préférés
        ChipGroup chipGroupTimes = view.findViewById(R.id.chipGroupTimes);
        
        // Boutons
        MaterialButton buttonMorningTime = view.findViewById(R.id.buttonMorningTime);
        MaterialButton buttonEveningTime = view.findViewById(R.id.buttonEveningTime);
        MaterialButton buttonSave = view.findViewById(R.id.buttonSave);
        MaterialButton buttonTestNotification = view.findViewById(R.id.buttonTestNotification);
        
        // Charger les préférences actuelles
        loadCurrentSettings(switchMealReminders, switchRecipeSuggestions, 
                           switchCookingTips, switchNewRecipes, sliderFrequency);
        
        // Listeners
        sliderFrequency.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                updateFrequencyText(textFrequency, (int) value);
            }
        });
        
        buttonMorningTime.setOnClickListener(v -> showTimePicker("morning"));
        buttonEveningTime.setOnClickListener(v -> showTimePicker("evening"));
        
        buttonTestNotification.setOnClickListener(v -> {
            sendTestNotification();
            bottomSheet.dismiss();
        });
        
        buttonSave.setOnClickListener(v -> {
            saveSettings(switchMealReminders, switchRecipeSuggestions,
                        switchCookingTips, switchNewRecipes, sliderFrequency, chipGroupTimes);
            bottomSheet.dismiss();
        });
        
        // Initialiser le texte de fréquence
        updateFrequencyText(textFrequency, (int) sliderFrequency.getValue());
    }
    
    /**
     * Charge les paramètres actuels
     */
    private void loadCurrentSettings(Switch switchMealReminders, Switch switchRecipeSuggestions,
                                   Switch switchCookingTips, Switch switchNewRecipes, Slider sliderFrequency) {
        switchMealReminders.setChecked(preferencesManager.isMealRemindersEnabled());
        switchRecipeSuggestions.setChecked(preferencesManager.isRecipeSuggestionsEnabled());
        switchCookingTips.setChecked(preferencesManager.isCookingTipsEnabled());
        switchNewRecipes.setChecked(preferencesManager.isNewRecipeNotificationsEnabled());
        
        int frequency = preferencesManager.getSuggestionFrequency();
        sliderFrequency.setValue(frequency);
    }
    
    /**
     * Met à jour le texte de fréquence
     */
    private void updateFrequencyText(TextView textView, int frequency) {
        String text;
        switch (frequency) {
            case 1:
                text = "Quotidienne";
                break;
            case 2:
                text = "Tous les 2 jours";
                break;
            case 3:
                text = "Tous les 3 jours";
                break;
            case 7:
                text = "Hebdomadaire";
                break;
            default:
                text = "Tous les " + frequency + " jours";
                break;
        }
        textView.setText(text);
    }
    
    /**
     * Affiche le sélecteur d'heure
     */
    private void showTimePicker(String timeType) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Choisir l'heure")
                .build();
        
        timePicker.addOnPositiveButtonClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            
            if ("morning".equals(timeType)) {
                preferencesManager.setMorningReminderTime(hour, minute);
            } else {
                preferencesManager.setEveningReminderTime(hour, minute);
            }
        });
        
        timePicker.show(getSupportFragmentManager(), "time_picker");
    }
    
    /**
     * Envoie une notification de test
     */
    private void sendTestNotification() {
        Notification testNotification = new Notification(
            "Notification de test 🧪",
            "Si tu vois ce message, les notifications fonctionnent parfaitement !",
            Notification.NotificationType.SYSTEM_UPDATE
        );
        
        testNotification.setPriority(Notification.NotificationPriority.NORMAL);
        notificationService.showNotification(testNotification);
    }
    
    /**
     * Sauvegarde les paramètres
     */
    private void saveSettings(Switch switchMealReminders, Switch switchRecipeSuggestions,
                             Switch switchCookingTips, Switch switchNewRecipes,
                             Slider sliderFrequency, ChipGroup chipGroupTimes) {
        
        preferencesManager.setMealRemindersEnabled(switchMealReminders.isChecked());
        preferencesManager.setRecipeSuggestionsEnabled(switchRecipeSuggestions.isChecked());
        preferencesManager.setCookingTipsEnabled(switchCookingTips.isChecked());
        preferencesManager.setNewRecipeNotificationsEnabled(switchNewRecipes.isChecked());
        preferencesManager.setSuggestionFrequency((int) sliderFrequency.getValue());
        
        // Sauvegarder les moments sélectionnés
        List<Integer> selectedTimes = new ArrayList<>();
        for (int i = 0; i < chipGroupTimes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTimes.getChildAt(i);
            if (chip.isChecked()) {
                selectedTimes.add(i);
            }
        }
        preferencesManager.setPreferredNotificationTimes(selectedTimes);
        
        // Reprogrammer les notifications automatiques
        if (switchRecipeSuggestions.isChecked()) {
            suggestionService.scheduleAutomaticSuggestions();
        }
        
        showSuccess("Paramètres sauvegardés !");
    }
    
    /**
     * Affiche un message de succès
     */
    private void showSuccess(String message) {
        // Ici vous pouvez implémenter un Snackbar ou Toast
        runOnUiThread(() -> {
            // Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
