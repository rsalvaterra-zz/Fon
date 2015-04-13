package org.rsalvaterra.fon;

import android.content.Context;
import android.content.Intent;

public final class AlarmBroadcastReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		WakefulBroadcastReceiver.startService(c, i.setClass(c, WakefulIntentService.class));
	}

}
