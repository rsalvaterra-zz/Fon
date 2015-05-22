package org.rsalvaterra.fon;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public final class SettingsActivity extends PreferenceActivity {

	private static final OnPreferenceClickListener OC_LISTENER = new OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(final Preference p) {
			final Context c = p.getContext();
			((TextView) new Builder(c).setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name).setMessage(Html.fromHtml(c.getString(R.string.app_credits, c.getString(R.string.app_copyright), c.getString(R.string.app_source)))).setNeutralButton(R.string.accept, new OnClickListener() {

				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					dialog.dismiss();

				}
			}).show().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			return true;
		}
	};

	private static final OnPreferenceChangeListener OPC_LISTENER = new OnPreferenceChangeListener() {

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

	private Preference getPreference(final int id) {
		return getPreferenceScreen().findPreference(getString(id));
	}

	private void setOnPreferenceChangeListener(final int id, final String v) {
		final Preference p = getPreference(id);
		p.setOnPreferenceChangeListener(SettingsActivity.OPC_LISTENER);
		SettingsActivity.OPC_LISTENER.onPreferenceChange(p, WakefulService.getPreference(p.getContext(), id, v));
	}

	private void setOnPreferenceClickListener(final int id) {
		getPreference(id).setOnPreferenceClickListener(SettingsActivity.OC_LISTENER);
	}

	@Override
	public void onCreate(final Bundle b) {
		super.onCreate(b);
		addPreferencesFromResource(R.layout.settings);
		setOnPreferenceChangeListener(R.string.kperiod, Constants.DEFAULT_PERIOD);
		setOnPreferenceChangeListener(R.string.krssi, Constants.DEFAULT_MINIMUM_RSSI);
		setOnPreferenceChangeListener(R.string.ksuccess, "");
		setOnPreferenceChangeListener(R.string.kfailure, "");
		setOnPreferenceClickListener(R.string.kabout);
	}
}
