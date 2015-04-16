package org.rsalvaterra.fon;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public final class WifiBroadcastReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		final String a = i.getAction();
		if (a.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && WakefulIntentService.isAutoConnectEnabled(c)) {
			WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.KEY_CONNECT));
		} else if (a.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			final NetworkInfo ni = (NetworkInfo) i.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if ((ni.getType() == ConnectivityManager.TYPE_WIFI)) {
				if (ni.getState() == NetworkInfo.State.CONNECTED) {
					final String ssid = WakefulIntentService.stripQuotes(((WifiManager) c.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID());
					if (LoginManager.isSupported(ssid)) {
						WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.KEY_LOGIN).putExtra(Constants.KEY_FIRST, true));
					}
				} else if (ni.getState() == NetworkInfo.State.DISCONNECTED) {
					WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.KEY_CANCEL_ALL));
				}
			}
		}
	}
}
