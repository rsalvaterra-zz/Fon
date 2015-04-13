package org.rsalvaterra.fon;

import android.content.Context;
import android.content.Intent;

public final class AlarmBroadcastReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		final String a = i.getAction();
		if (a.equals(Constants.KEY_LOGIN)) {
			WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.KEY_LOGIN).putExtra(Constants.KEY_FIRST, false));
		} else if (a.equals(Constants.KEY_SCAN)) {
			WakefulBroadcastReceiver.startService(c, new Intent(c, WakefulIntentService.class).setAction(Constants.KEY_SCAN));
		}
	}

}
