package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public final class WifiBroadcastReceiver extends BroadcastReceiver {

	private static boolean isAutoConnectEnabled(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_autoconnect), true);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && WifiBroadcastReceiver.isAutoConnectEnabled(context)) {
			WakefulIntentService.start(context, new Intent(context, WakefulIntentService.class).setAction(Keys.KEY_CONNECT));
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			final NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if ((ni.getType() == ConnectivityManager.TYPE_WIFI)) {
				if (ni.getState() == NetworkInfo.State.CONNECTED) {
					WakefulIntentService.start(context, new Intent(context, WakefulIntentService.class).setAction(Keys.KEY_LOGIN));
				} else if (ni.getState() == NetworkInfo.State.DISCONNECTED) {
					WakefulIntentService.start(context, new Intent(context, WakefulIntentService.class).setAction(Keys.KEY_CANCEL_SCHEDULED_ACTIONS));
				}
			}
		} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) && (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED)) {
			WakefulIntentService.start(context, new Intent(context, WakefulIntentService.class).setAction(Keys.KEY_CANCEL_NOTIFICATION));
		}
	}
}
