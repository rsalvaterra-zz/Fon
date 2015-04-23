package org.rsalvaterra.fon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

public final class AdvancedPreferences extends PreferenceActivity {

	private static final int BASIC_ID = Menu.FIRST;

	private static final OnPreferenceChangeListener LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			final Context c = p.getContext();
			final String k = p.getKey();
			if (k.equals(c.getString(R.string.key_reconnect))) {
				if (((Boolean) v).booleanValue()) {
					WakefulIntentService.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.ACT_SCAN));
				}
			} else if (k.equals(c.getString(R.string.key_period))) {
				p.setSummary(c.getString(R.string.periodSummary, v));
			} else if (k.equals(c.getString(R.string.key_rssi))) {
				p.setSummary(c.getString(R.string.rssiSummary, v));
			} else if (k.equals(c.getString(R.string.key_success)) || k.equals(c.getString(R.string.key_failure))) {
				final String t = v.toString();
				final String s;
				if (t.length() == 0) {
					s = "";
				} else {
					final Ringtone r = RingtoneManager.getRingtone(c, Uri.parse(t));
					if (r == null) {
						s = null;
					} else {
						s = r.getTitle(c);
					}
				}
				p.setSummary(s);
			}
			return true;
		}
	};

	private static String getPreferenceValue(final Preference p, final String dv) {
		return PreferenceManager.getDefaultSharedPreferences(p.getContext()).getString(p.getKey(), dv);
	}

	private void bindPreferenceListener() {
		final PreferenceScreen ps = getPreferenceScreen();
		ps.findPreference(getString(R.string.key_reconnect)).setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		Preference p = ps.findPreference(getString(R.string.key_period));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, AdvancedPreferences.getPreferenceValue(p, "300"));
		p = ps.findPreference(getString(R.string.key_rssi));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, AdvancedPreferences.getPreferenceValue(p, "-80"));
		p = ps.findPreference(getString(R.string.key_success));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, AdvancedPreferences.getPreferenceValue(p, ""));
		p = ps.findPreference(getString(R.string.key_failure));
		p.setOnPreferenceChangeListener(AdvancedPreferences.LISTENER);
		AdvancedPreferences.LISTENER.onPreferenceChange(p, AdvancedPreferences.getPreferenceValue(p, ""));
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.preferences_advanced);
		bindPreferenceListener();
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
