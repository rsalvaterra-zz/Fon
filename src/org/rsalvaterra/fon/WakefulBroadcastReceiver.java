package org.rsalvaterra.fon;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.SparseArray;

public abstract class WakefulBroadcastReceiver extends BroadcastReceiver {

	private static final int WAKELOCK_TIMEOUT = 60 * 1000;

	private static final String KEY_WAKELOCK_ID = WakefulBroadcastReceiver.class.getPackage().getName();

	private static final SparseArray<WakeLock> ACTIVE_WAKELOCKS = new SparseArray<WakeLock>();

	private static int NEXT_WAKELOCK_ID = 1;

	static void releaseWakeLock(final Intent i) {
		final int id = i.getIntExtra(WakefulBroadcastReceiver.KEY_WAKELOCK_ID, 0);
		if (id != 0) {
			synchronized (WakefulBroadcastReceiver.ACTIVE_WAKELOCKS) {
				final WakeLock wl = WakefulBroadcastReceiver.ACTIVE_WAKELOCKS.get(id);
				if (wl != null) {
					wl.release();
					WakefulBroadcastReceiver.ACTIVE_WAKELOCKS.remove(id);
				}
			}
		}
	}

	public static ComponentName startService(final Context context, final String action) {
		synchronized (WakefulBroadcastReceiver.ACTIVE_WAKELOCKS) {
			final int id = WakefulBroadcastReceiver.NEXT_WAKELOCK_ID++;
			if (WakefulBroadcastReceiver.NEXT_WAKELOCK_ID <= 0) {
				WakefulBroadcastReceiver.NEXT_WAKELOCK_ID = 1;
			}
			final ComponentName cn = context.startService(new Intent(context, WakefulIntentService.class).setAction(action).putExtra(WakefulBroadcastReceiver.KEY_WAKELOCK_ID, id));
			if (cn != null) {
				final WakeLock wl = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WakefulBroadcastReceiver.KEY_WAKELOCK_ID);
				wl.setReferenceCounted(false);
				wl.acquire(WakefulBroadcastReceiver.WAKELOCK_TIMEOUT);
				WakefulBroadcastReceiver.ACTIVE_WAKELOCKS.put(id, wl);
			}
			return cn;
		}
	}

}
