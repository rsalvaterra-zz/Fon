package org.rsalvaterra.fon.activity;

import org.rsalvaterra.fon.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public final class BasicPreferences extends PreferenceActivity {

	private static final int CLOSE_ID = Menu.FIRST;
	private static final int ADVANCED_ID = BasicPreferences.CLOSE_ID + 1;
	private static final int ABOUT_ID = BasicPreferences.ADVANCED_ID + 1;

	private void showAbout() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.app_name);
		builder.setView(getLayoutInflater().inflate(R.layout.about, null, false));
		builder.create();
		builder.show();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences_main);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(Menu.NONE, BasicPreferences.CLOSE_ID, 0, R.string.close).setIcon(android.R.drawable.ic_menu_save);
		menu.add(Menu.NONE, BasicPreferences.ADVANCED_ID, 1, R.string.advanced).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, BasicPreferences.ABOUT_ID, 1, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case CLOSE_ID:
				finish();
				break;
			case ADVANCED_ID:
				startActivity(new Intent(this, AdvancedPreferences.class));
				break;
			case ABOUT_ID:
				showAbout();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
}
