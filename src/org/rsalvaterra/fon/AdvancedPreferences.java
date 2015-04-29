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
import android.view.Menu;
import android.view.MenuItem;

public final class AdvancedPreferences extends PreferenceActivity {

	private static final int BASIC_ID = Menu.FIRST;

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
					final Ringtone r = RingtoneManager.getRingtone(c, Uri.parse(s));
					if (r != null) {
						s = r.getTitle(c);
					}
				}
			}
			p.setSummary(s);
			return true;
		}
	};

	private void setListener(final int id, final String v) {
		final Preference p = getPreferenceScreen().findPreference(getString(id));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, WakefulIntentService.getPreference(p.getContext(), id, v));
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.pref_advanced);
		setListener(R.string.kperiod, Constants.DEFAULT_PERIOD);
		setListener(R.string.krssi, Constants.DEFAULT_MINIMUM_RSSI);
		setListener(R.string.ksuccess, "");
		setListener(R.string.kfailure, "");
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
