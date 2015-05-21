package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;

public final class WifiBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		final String a = i.getAction();
		if (a.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && ActionExecutor.isAutoConnectEnabled(c)) {
			ActionExecutor.execute(c, new Intent(c, ActionExecutor.class).setAction(Constants.ACT_CONNECT));
		} else if (a.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			final NetworkInfo ni = (NetworkInfo) i.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if ((ni.getType() == ConnectivityManager.TYPE_WIFI)) {
				final State s = ni.getState();
				if (s == State.CONNECTED) {
					ActionExecutor.execute(c, new Intent(c, ActionExecutor.class).setAction(Constants.ACT_LOGIN).putExtra(Constants.KEY_LOGIN, true));
				} else if (s == State.DISCONNECTED) {
					ActionExecutor.execute(c, new Intent(c, ActionExecutor.class).setAction(Constants.ACT_CANCEL_ALL));
				}
			}
		}
	}
}
