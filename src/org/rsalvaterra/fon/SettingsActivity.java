package org.rsalvaterra.fon;

import android.app.AlertDialog;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public final class SettingsActivity extends PreferenceActivity {

	private static final int CLOSE_ID = Menu.FIRST;
	private static final int ABOUT_ID = SettingsActivity.CLOSE_ID + 1;

	private static final OnPreferenceChangeListener LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			final Context c = p.getContext();
			final String k = p.getKey();
			String s;
			if (k.equals(c.getString(R.string.kperiod))) {
				s = c.getString(R.string.periodSummary, v);
			} else if (k.equals(c.getString(R.string.krssi))) {
				s = c.getString(R.string.rssiSummary, v);
			} else {
				s = (String) v;
				if (s.length() != 0) {
					s = RingtoneManager.getRingtone(c, Uri.parse(s)).getTitle(c);
				}
			}
			p.setSummary(s);
			return true;
		}
	};

	private void setListener(final int id, final String v) {
		final Preference p = getPreferenceScreen().findPreference(getString(id));
		p.setOnPreferenceChangeListener(SettingsActivity.LISTENER);
		SettingsActivity.LISTENER.onPreferenceChange(p, ActionExecutor.getPreference(p.getContext(), id, v));
	}

	private void showAbout() {
		((TextView) new AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name).setMessage(Html.fromHtml(getString(R.string.app_credits, getString(R.string.app_copyright), getString(R.string.app_source)))).show().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.settings);
		setListener(R.string.kperiod, Constants.DEFAULT_PERIOD);
		setListener(R.string.krssi, Constants.DEFAULT_MINIMUM_RSSI);
		setListener(R.string.ksuccess, "");
		setListener(R.string.kfailure, "");
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu m) {
		m.add(Menu.NONE, SettingsActivity.CLOSE_ID, Menu.NONE, R.string.close).setIcon(android.R.drawable.ic_menu_save);
		m.add(Menu.NONE, SettingsActivity.ABOUT_ID, Menu.NONE, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
		return super.onCreateOptionsMenu(m);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem mi) {
		switch (mi.getItemId()) {
			case CLOSE_ID:
				finish();
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
