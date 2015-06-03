package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FonManAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context c, final Intent i) {
		FonManService.execute(c, i.getAction());
	}

}
