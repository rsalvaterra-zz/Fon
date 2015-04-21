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

	private static final OnPreferenceChangeListener CHECKBOX_LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			if (((Boolean) v).booleanValue()) {
				final Context c = p.getContext();
				WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.ACT_SCAN));
			}
			return true;
		}
	};

	private static final OnPreferenceChangeListener EDIT_TEXT_LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			p.setSummary(p.getContext().getString(R.string.intervalSummary, v));
			return true;
		}
	};

	private static final OnPreferenceChangeListener RINGTONE_LISTENER = new OnPreferenceChangeListener() {

		@Override
		public final boolean onPreferenceChange(final Preference p, final Object v) {
			final String t = v.toString();
			final String s;
			if (t.length() == 0) {
				s = "";
			} else {
				final Ringtone r = RingtoneManager.getRingtone(p.getContext(), Uri.parse(t));
				if (r == null) {
					s = null;
				} else {
					s = r.getTitle(p.getContext());
				}
			}
			p.setSummary(s);
			return true;
		}
	};

	private static void bindCheckBoxListener(final Preference p) {
		p.setOnPreferenceChangeListener(AdvancedPreferences.CHECKBOX_LISTENER);
	}

	private static void bindEditTextListener(final Preference p) {
		p.setOnPreferenceChangeListener(AdvancedPreferences.EDIT_TEXT_LISTENER);
		AdvancedPreferences.EDIT_TEXT_LISTENER.onPreferenceChange(p, PreferenceManager.getDefaultSharedPreferences(p.getContext()).getString(p.getKey(), "300"));
	}

	private static void bindRingtoneListener(final Preference p) {
		p.setOnPreferenceChangeListener(AdvancedPreferences.RINGTONE_LISTENER);
		AdvancedPreferences.RINGTONE_LISTENER.onPreferenceChange(p, PreferenceManager.getDefaultSharedPreferences(p.getContext()).getString(p.getKey(), ""));
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.preferences_advanced);
		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		AdvancedPreferences.bindCheckBoxListener(preferenceScreen.findPreference(getString(R.string.key_reconnect)));
		AdvancedPreferences.bindEditTextListener(preferenceScreen.findPreference(getString(R.string.key_period)));
		AdvancedPreferences.bindRingtoneListener(preferenceScreen.findPreference(getString(R.string.key_success)));
		AdvancedPreferences.bindRingtoneListener(preferenceScreen.findPreference(getString(R.string.key_failure)));
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
