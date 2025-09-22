package fr.didictateur.inanutshell.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.analytics.AnalyticsManager;
import fr.didictateur.inanutshell.data.model.AppStatistics;
import fr.didictateur.inanutshell.adapters.InsightsAdapter;

public class StatisticsActivity extends AppCompatActivity implements AnalyticsManager.AnalyticsListener {
    
    private AnalyticsManager analyticsManager;
    
    // UI Components
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    
    // Statistiques générales
    private TextView textTotalUsers;
    private TextView textTotalRecipes;
    private TextView textTotalComments;
    private TextView textTotalSessions;
    private TextView textActiveUsersToday;
    private TextView textActiveUsersWeek;
    private TextView textActiveUsersMonth;
    
    // Graphiques
    private LineChart chartUsagetrend;
    private PieChart chartTopCategories;
    private BarChart chartTopRecipes;
    
    // Insights
    private RecyclerView recyclerInsights;
    private InsightsAdapter insightsAdapter;
    
    // Données
    private AppStatistics currentStats;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        
        initializeComponents();
        setupAnalytics();
        setupUI();
        loadStatistics();
    }
    
    private void initializeComponents() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        
        // Statistiques générales
        textTotalUsers = findViewById(R.id.text_total_users);
        textTotalRecipes = findViewById(R.id.text_total_recipes);
        textTotalComments = findViewById(R.id.text_total_comments);
        textTotalSessions = findViewById(R.id.text_total_sessions);
        textActiveUsersToday = findViewById(R.id.text_active_users_today);
        textActiveUsersWeek = findViewById(R.id.text_active_users_week);
        textActiveUsersMonth = findViewById(R.id.text_active_users_month);
        
        // Graphiques
        chartUsagetrend = findViewById(R.id.chart_usage_trend);
        chartTopCategories = findViewById(R.id.chart_top_categories);
        chartTopRecipes = findViewById(R.id.chart_top_recipes);
        
        // Insights
        recyclerInsights = findViewById(R.id.recycler_insights);
    }
    
    private void setupAnalytics() {
        analyticsManager = AnalyticsManager.getInstance(this);
        analyticsManager.addListener(this);
    }
    
    private void setupUI() {
        // Configuration du pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::loadStatistics);
        
        // Configuration des graphiques
        setupCharts();
        
        // Configuration du RecyclerView des insights
        insightsAdapter = new InsightsAdapter(this);
        recyclerInsights.setLayoutManager(new LinearLayoutManager(this));
        recyclerInsights.setAdapter(insightsAdapter);
        
        // Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📊 Statistiques");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupCharts() {
        // Configuration du graphique de tendance d'utilisation
        configureLineChart(chartUsagetrend, "Utilisation quotidienne");
        
        // Configuration du graphique en secteurs des catégories
        configurePieChart(chartTopCategories, "Catégories populaires");
        
        // Configuration du graphique en barres des recettes
        configureBarChart(chartTopRecipes, "Recettes les plus vues");
    }
    
    private void configureLineChart(LineChart chart, String title) {
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        
        // Axes
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        chart.getAxisRight().setEnabled(false);
        
        // Légende
        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(true);
    }
    
    private void configurePieChart(PieChart chart, String title) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(5, 10, 5, 5);
        
        chart.setDragDecelerationFrictionCoef(0.95f);
        
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setHoleRadius(30f);
        chart.setTransparentCircleRadius(35f);
        
        chart.setDrawCenterText(true);
        chart.setCenterText("Catégories");
        chart.setCenterTextSize(16f);
        
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        
        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);
    }
    
    private void configureBarChart(BarChart chart, String title) {
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }
    
    private void loadStatistics() {
        showLoading(true);
        
        analyticsManager.getStatistics(this);
        
        // Charger aussi les insights
        analyticsManager.generateInsights(insights -> {
            runOnUiThread(() -> {
                // Convertir Insight en InsightItem
                List<InsightsAdapter.InsightItem> insightItems = new ArrayList<>();
                for (AnalyticsManager.Insight insight : insights) {
                    insightItems.add(new InsightsAdapter.InsightItem(insight.title, insight.description, insight.type));
                }
                insightsAdapter.updateInsights(insightItems);
            });
        });
    }
    
    @Override
    public void onStatsUpdated(AppStatistics stats) {
        this.currentStats = stats;
        
        runOnUiThread(() -> {
            updateGeneralStats(stats);
            updateCharts(stats);
            showLoading(false);
        });
    }
    
    @Override
    public void onEventTracked(String eventType) {
        // Optionnel : mettre à jour en temps réel pour certains événements
    }
    
    private void updateGeneralStats(AppStatistics stats) {
        // Statistiques générales
        textTotalUsers.setText(String.valueOf(stats.totalUsers));
        textTotalRecipes.setText(String.valueOf(stats.totalRecipes));
        textTotalComments.setText(String.valueOf(stats.totalComments));
        textTotalSessions.setText(String.valueOf(stats.totalSessions));
        
        // Utilisateurs actifs
        textActiveUsersToday.setText(String.valueOf(stats.todayActiveUsers));
        textActiveUsersWeek.setText(String.valueOf(stats.weekActiveUsers));
        textActiveUsersMonth.setText(String.valueOf(stats.monthActiveUsers));
    }
    
    private void updateCharts(AppStatistics stats) {
        // Mettre à jour le graphique de tendance
        updateUsageTrendChart(convertDataPointsToMap(stats.dailyStats));
        
        // Mettre à jour le graphique des catégories
        updateCategoriesChart(stats.topCategories);
        
        // Mettre à jour le graphique des recettes
        updateRecipesChart(stats.topRecipes);
    }
    
    private void updateUsageTrendChart(Map<String, Integer> dailyStats) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : dailyStats.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Utilisateurs actifs");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_bright));
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_blue_bright));
        dataSet.setFillAlpha(50);
        
        LineData lineData = new LineData(dataSet);
        
        chartUsagetrend.setData(lineData);
        chartUsagetrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartUsagetrend.invalidate();
    }
    
    private void updateCategoriesChart(List<AppStatistics.CategoryStats> categories) {
        List<PieEntry> entries = new ArrayList<>();
        
        for (AppStatistics.CategoryStats category : categories) {
            entries.add(new PieEntry((float) category.popularity, category.category));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Catégories");
        
        // Couleurs pour les secteurs
        List<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(android.R.color.holo_blue_bright));
        colors.add(getResources().getColor(android.R.color.holo_green_light));
        colors.add(getResources().getColor(android.R.color.holo_orange_light));
        colors.add(getResources().getColor(android.R.color.holo_purple));
        colors.add(getResources().getColor(android.R.color.holo_red_light));
        
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.BLACK);
        
        PieData pieData = new PieData(dataSet);
        
        chartTopCategories.setData(pieData);
        chartTopCategories.invalidate();
    }
    
    private void updateRecipesChart(List<AppStatistics.RecipeStats> recipes) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < Math.min(recipes.size(), 5); i++) {
            AppStatistics.RecipeStats recipe = recipes.get(i);
            entries.add(new BarEntry(i, recipe.views));
            labels.add(recipe.recipeName.length() > 15 ? 
                recipe.recipeName.substring(0, 12) + "..." : recipe.recipeName);
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Vues");
        dataSet.setColor(getResources().getColor(android.R.color.holo_green_light));
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        chartTopRecipes.setData(barData);
        chartTopRecipes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartTopRecipes.invalidate();
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        swipeRefresh.setRefreshing(false);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export) {
            exportStatistics();
            return true;
        } else if (id == R.id.action_engagement_report) {
            showEngagementReport();
            return true;
        } else if (id == R.id.action_trend_analysis) {
            showTrendAnalysis();
            return true;
        } else if (id == R.id.action_settings) {
            showAnalyticsSettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void exportStatistics() {
        analyticsManager.exportStatistics(new AnalyticsManager.ExportListener() {
            @Override
            public void onExportReady(String csvData) {
                // TODO: Sauvegarder le fichier CSV et le partager
                showToast("Statistiques exportées");
            }
            
            @Override
            public void onExportError(String error) {
                showToast("Erreur lors de l'export: " + error);
            }
        });
    }
    
    private void showEngagementReport() {
        Intent intent = new Intent(this, EngagementReportActivity.class);
        startActivity(intent);
    }
    
    private void showTrendAnalysis() {
        Intent intent = new Intent(this, TrendAnalysisActivity.class);
        startActivity(intent);
    }
    
    private void showAnalyticsSettings() {
        Intent intent = new Intent(this, AnalyticsSettingsActivity.class);
        startActivity(intent);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Convertit une List<DataPoint> en Map<String,Integer>
     */
    private Map<String, Integer> convertDataPointsToMap(List<AppStatistics.DataPoint> dataPoints) {
        Map<String, Integer> map = new HashMap<>();
        if (dataPoints != null) {
            for (AppStatistics.DataPoint point : dataPoints) {
                map.put(point.label, (int) point.value);
            }
        }
        return map;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analyticsManager != null) {
            analyticsManager.removeListener(this);
        }
    }
}
