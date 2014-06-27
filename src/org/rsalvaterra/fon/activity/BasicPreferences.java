package org.rsalvaterra.fon.activity;

import org.rsalvaterra.fon.R;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public final class BasicPreferences extends PreferenceActivity {

	private static final int CLOSE_ID = Menu.FIRST;
	private static final int ADVANCED_ID = BasicPreferences.CLOSE_ID + 1;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences_main);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(Menu.NONE, BasicPreferences.CLOSE_ID, 0, R.string.close).setIcon(android.R.drawable.ic_menu_save);
		menu.add(Menu.NONE, BasicPreferences.ADVANCED_ID, 1, R.string.advanced).setIcon(android.R.drawable.ic_menu_preferences);
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
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
}
