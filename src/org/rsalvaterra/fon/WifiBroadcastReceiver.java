package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public final class WifiBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		final String action = i.getAction();
		if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && WakefulIntentService.isAutoConnectEnabled(c)) {
			WakefulIntentService.start(c, Constants.KEY_CONNECT);
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			final NetworkInfo ni = (NetworkInfo) i.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if ((ni.getType() == ConnectivityManager.TYPE_WIFI)) {
				if (ni.getState() == NetworkInfo.State.CONNECTED) {
					WakefulIntentService.start(c, Constants.KEY_LOGIN);
				} else if (ni.getState() == NetworkInfo.State.DISCONNECTED) {
					WakefulIntentService.start(c, Constants.KEY_CANCEL_SCHEDULED_ACTIONS);
				}
			}
		} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) && (i.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED)) {
			WakefulIntentService.start(c, Constants.KEY_CANCEL_NOTIFICATION);
		}
	}
}
