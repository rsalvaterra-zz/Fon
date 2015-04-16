package org.rsalvaterra.fon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public final class BasicPreferences extends PreferenceActivity {

	private static final int CLOSE_ID = Menu.FIRST;
	private static final int ADVANCED_ID = BasicPreferences.CLOSE_ID + 1;
	private static final int ABOUT_ID = BasicPreferences.ADVANCED_ID + 1;

	private void showAbout() {
		((TextView) new AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name).setMessage(Html.fromHtml(getString(R.string.app_credits, getString(R.string.app_copyright), getString(R.string.app_source)))).show().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.preferences_main);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu m) {
		m.add(Menu.NONE, BasicPreferences.CLOSE_ID, Menu.NONE, R.string.close).setIcon(android.R.drawable.ic_menu_save);
		m.add(Menu.NONE, BasicPreferences.ADVANCED_ID, Menu.NONE, R.string.advanced).setIcon(android.R.drawable.ic_menu_preferences);
		m.add(Menu.NONE, BasicPreferences.ABOUT_ID, Menu.NONE, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
		return super.onCreateOptionsMenu(m);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem mi) {
		switch (mi.getItemId()) {
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
				return super.onOptionsItemSelected(mi);
		}
		return true;
	}
}
