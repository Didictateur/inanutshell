package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import io.noties.markwon.Markwon;

public class ViewRecetteActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_recette);

		String titre = getIntent().getStringExtra("titre");
		String taille = getIntent().getStringExtra("taille");
		String tempsPrep = getIntent().getStringExtra("tempsPrep");
		String ingredients = getIntent().getStringExtra("ingredients");
		String preparation = getIntent().getStringExtra("preparation");
		String notes = getIntent().getStringExtra("notes");

		((TextView)findViewById(R.id.textTitre)).setText(titre);
		((TextView)findViewById(R.id.textTaille)).setText(taille);
		((TextView)findViewById(R.id.textTempsPrep)).setText(tempsPrep);

		Markwon markdown = Markwon.create(this);
		markdown.setMarkdown((TextView)findViewById(R.id.textIngredients), ingredients != null ? ingredients : "");
		markdown.setMarkdown((TextView)findViewById(R.id.textPreparation), preparation != null ? preparation : "");
		markdown.setMarkdown((TextView)findViewById(R.id.textNotes), notes != null ? notes : "");

		int statusBarColor = getStatusBarColor();

		TextView titreIngredients = findViewById(R.id.textTitreIngredients);
		TextView titrePreparation = findViewById(R.id.textTitrePreparation);
		TextView titreRecette = findViewById(R.id.textTitre);

		titreIngredients.setTextColor(statusBarColor);
		titrePreparation.setTextColor(statusBarColor);
		titreRecette.setTextColor(statusBarColor);
	}

	private int getStatusBarColor() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
		String statusBarColorName = colorName.replace("toolbar_", "statusbar_");
		int statusBarColorResId = getResources().getIdentifier(
				statusBarColorName,
				"color",
				getPackageName()
		);

		return ContextCompat.getColor(this, statusBarColorResId);
	}
}
