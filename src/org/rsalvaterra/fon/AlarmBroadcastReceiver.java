package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		WakefulIntentService.start(c, i.getAction());
	}

}
