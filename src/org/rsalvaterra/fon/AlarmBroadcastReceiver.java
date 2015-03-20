package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		WakefulIntentService.start(context, new Intent(context, WakefulIntentService.class).setAction(intent.getAction()));
	}

}
