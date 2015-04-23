package org.rsalvaterra.fon;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public final class AdvancedPreferences extends PreferenceActivity {

	private static final int BASIC_ID = Menu.FIRST;

	private static final OnPreferenceChangeListener LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			final Context c = p.getContext();
			final String k = p.getKey();
			if (k.equals(c.getString(R.string.key_period))) {
				p.setSummary(c.getString(R.string.periodSummary, v));
			} else if (k.equals(c.getString(R.string.key_rssi))) {
				p.setSummary(c.getString(R.string.rssiSummary, v));
			} else if (k.equals(c.getString(R.string.key_success)) || k.equals(c.getString(R.string.key_failure))) {
				String s = v.toString();
				if (s.length() != 0) {
					final Ringtone r = RingtoneManager.getRingtone(c, Uri.parse(s));
					if (r != null) {
						s = r.getTitle(c);
					}
				}
				p.setSummary(s);
			}
			return true;
		}
	};

	private void bindListenerToPreferences() {
		setListener(R.string.key_period, "300");
		setListener(R.string.key_rssi, "-80");
		setListener(R.string.key_success, "");
		setListener(R.string.key_failure, "");
	}

	private void setListener(final int k, final String v) {
		final Preference p = getPreferenceScreen().findPreference(getString(k));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, PreferenceManager.getDefaultSharedPreferences(p.getContext()).getString(p.getKey(), v));
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.preferences_advanced);
		bindListenerToPreferences();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu m) {
		m.add(Menu.NONE, AdvancedPreferences.BASIC_ID, Menu.NONE, R.string.basic).setIcon(android.R.drawable.ic_menu_revert);
		return super.onCreateOptionsMenu(m);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem mi) {
		switch (mi.getItemId()) {
			case BASIC_ID:
				setResult(Activity.RESULT_OK);
				finish();
				break;
			default:
				return super.onOptionsItemSelected(mi);
		}
		return true;
	}

}
