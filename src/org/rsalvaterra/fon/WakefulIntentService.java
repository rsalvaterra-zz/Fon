package org.rsalvaterra.fon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.rsalvaterra.fon.activity.BasicPreferences;
import org.rsalvaterra.fon.login.LoginManager;
import org.rsalvaterra.fon.login.LoginResult;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.SparseArray;

public final class WakefulIntentService extends IntentService {

	private static final int NOTIFICATION_ID = 1;
	private static final int REQUEST_CODE = 1;
	private static final int CONNECTIVITY_CHECK_INTERVAL = 60;
	private static final int WAKELOCK_TIMEOUT = 10 * 1000;

	private static final String KEY_WAKELOCK_ID = WakefulIntentService.class.getPackage().getName();

	private static final SparseArray<PowerManager.WakeLock> ACTIVE_WAKELOCKS = new SparseArray<PowerManager.WakeLock>();

	private static final long[] VIBRATE_PATTERN_SUCCESS = { 100, 250 };
	private static final long[] VIBRATE_PATTERN_FAILURE = { 100, 250, 100, 250 };

	private static final Comparator<ScanResult> BY_DESCENDING_LEVEL = new Comparator<ScanResult>() {

		@Override
		public int compare(final ScanResult sr1, final ScanResult sr2) {
			return (sr1.level > sr2.level ? -1 : (sr1.level == sr2.level ? 0 : 1));
		}
	};

	private static int NEXT_WAKELOCK_ID = 1;

	public WakefulIntentService() {
		super(WakefulIntentService.class.getName());
	}

	private static boolean areNotificationsEnabled(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_notify), true);
	}

	private static void cancelNotification(final Context context) {
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(WakefulIntentService.NOTIFICATION_ID);
	}

	private static void cancelScheduledActions(final Context context) {
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(PendingIntent.getBroadcast(context, WakefulIntentService.REQUEST_CODE, new Intent(context, BroadcastIntentReceiver.class).setAction(Keys.KEY_SCAN), PendingIntent.FLAG_UPDATE_CURRENT));
		alarmManager.cancel(PendingIntent.getBroadcast(context, WakefulIntentService.REQUEST_CODE, new Intent(context, BroadcastIntentReceiver.class).setAction(Keys.KEY_LOGIN), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private static boolean completeWakefulIntent(final Intent intent) {
		final int id = intent.getIntExtra(WakefulIntentService.KEY_WAKELOCK_ID, 0);
		if (id == 0) {
			return false;
		}
		synchronized (WakefulIntentService.ACTIVE_WAKELOCKS) {
			final PowerManager.WakeLock wl = WakefulIntentService.ACTIVE_WAKELOCKS.get(id);
			if (wl != null) {
				wl.release();
				WakefulIntentService.ACTIVE_WAKELOCKS.remove(id);
			}
			return true;
		}
	}

	private static void connect(final Context context, final WifiManager wm) {
		final WifiInfo wi = wm.getConnectionInfo();
		if (wi == null) {
			return;
		}
		final SupplicantState ss = wi.getSupplicantState();
		if (WakefulIntentService.isDisconnected(ss)) {
			final WifiConfiguration[] wca = WakefulIntentService.getConfiguredNetworks(wm);
			final ScanResult[] sra = WakefulIntentService.getScanResults(wm);
			int id = WakefulIntentService.getOtherId(wca, sra, false);
			if (id == -1) {
				id = WakefulIntentService.getFonId(wca, sra, wm);
				if (id == -1) {
					WakefulIntentService.cancelNotification(context);
				} else if (wm.enableNetwork(id, true) && WakefulIntentService.isReconnectEnabled(context)) {
					WakefulIntentService.scheduleScan(context);
				}
			} else if (wm.enableNetwork(id, true)) {
				WakefulIntentService.cancelNotification(context);
			}
		} else if (WakefulIntentService.isConnected(ss) && WakefulIntentService.isReconnectEnabled(context) && LoginManager.isSupportedNetwork(WakefulIntentService.stripQuotes(wi.getSSID()))) {
			final int id = WakefulIntentService.getOtherId(WakefulIntentService.getConfiguredNetworks(wm), WakefulIntentService.getScanResults(wm), WakefulIntentService.isSecureEnabled(context));
			if (id != -1) {
				wm.enableNetwork(id, true);
			} else {
				WakefulIntentService.scheduleScan(context);
			}
		}
	}

	private static WifiConfiguration[] getConfiguredNetworks(final WifiManager wm) {
		final List<WifiConfiguration> wcl = wm.getConfiguredNetworks();
		if (wcl == null) {
			return null;
		}
		return wcl.toArray(new WifiConfiguration[wcl.size()]);
	}

	private static String getFailureTone(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_failure), "");
	}

	private static int getFonId(final WifiConfiguration[] wca, final ScanResult[] sra, final WifiManager wm) {
		if (wca == null) {
			return -1;
		}
		final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
		for (final WifiConfiguration wc : wca) {
			if (!WakefulIntentService.isSecure(wc)) {
				final String ssid = WakefulIntentService.stripQuotes(wc.SSID);
				if (LoginManager.isSupportedNetwork(ssid)) {
					wcm.put(ssid, Integer.valueOf(wc.networkId));
				}
			}
		}
		for (final ScanResult sr : sra) {
			if (LoginManager.isSupportedNetwork(sr.SSID)) {
				final Integer id = wcm.get(sr.SSID);
				if (id == null) {
					final WifiConfiguration wc = new WifiConfiguration();
					wc.SSID = '"' + sr.SSID + '"';
					wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					return wm.addNetwork(wc);
				}
				return id.intValue();
			}
		}
		return -1;
	}

	private static int getOtherId(final WifiConfiguration[] wca, final ScanResult[] sra, final boolean secureOnly) {
		if ((wca == null) || (wca.length == 0)) {
			return -1;
		}
		final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
		for (final WifiConfiguration wc : wca) {
			final String ssid = WakefulIntentService.stripQuotes(wc.SSID);
			if ((!secureOnly || (secureOnly && WakefulIntentService.isSecure(wc))) && !LoginManager.isSupportedNetwork(ssid)) {
				wcm.put(ssid, Integer.valueOf(wc.networkId));
			}
		}
		for (final ScanResult sr : sra) {
			final Integer id = wcm.get(sr.SSID);
			if (id != null) {
				return id.intValue();
			}
		}
		return -1;
	}

	private static int getPeriod(final Context context) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_interval), "300"));
	}

	private static ScanResult[] getScanResults(final WifiManager wm) {
		final List<ScanResult> srl = wm.getScanResults();
		final ScanResult[] sra = srl.toArray(new ScanResult[srl.size()]);
		Arrays.sort(sra, WakefulIntentService.BY_DESCENDING_LEVEL);
		return sra;
	}

	private static String getSuccessTone(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_success), "");
	}

	private static void handleSuccess(final Context context, final String ssid, final int flags, final String logoffUrl) {
		WakefulIntentService.notifySuccess(context, ssid, flags, logoffUrl);
		WakefulIntentService.scheduleConnectivityCheck(context);
	}

	private static boolean isConnected(final SupplicantState ss) {
		return (ss == SupplicantState.COMPLETED);
	}

	private static boolean isDisconnected(final SupplicantState ss) {
		return (ss == SupplicantState.INACTIVE) || (ss == SupplicantState.DISCONNECTED) || (ss == SupplicantState.SCANNING);
	}

	private static boolean isReconnectEnabled(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_reconnect), false);
	}

	private static boolean isSecure(final WifiConfiguration wc) {
		return wc.allowedKeyManagement.get(KeyMgmt.WPA_PSK) || wc.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || wc.allowedKeyManagement.get(KeyMgmt.IEEE8021X) || (wc.wepKeys[0] != null);
	}

	private static boolean isSecureEnabled(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_secure), true);
	}

	private static boolean isVibrationEnabled(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_vibration), false);
	}

	private static void login(final Context context, final WifiManager wm) {
		final WifiInfo wi = wm.getConnectionInfo();
		if ((wi == null) || (wi.getSSID() == null)) {
			return;
		}
		final String ssid = WakefulIntentService.stripQuotes(wi.getSSID());
		if (!LoginManager.isSupportedNetwork(ssid)) {
			WakefulIntentService.cancelNotification(context);
			WakefulIntentService.purgeFonNetworks(wm);
			return;
		}
		final LoginResult result = LoginManager.login(context, ssid);
		final int responseCode = result.getResponseCode();
		switch (responseCode) {
			case ResponseCodes.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR:
			case ResponseCodes.CUST_WISPR_NOT_PRESENT:
				WakefulIntentService.tryToRecover(context, wm, wi);
				break;
			case ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED:
				WakefulIntentService.handleSuccess(context, ssid, 0, result.getLogOffUrl());
				break;
			case ResponseCodes.CUST_ALREADY_CONNECTED:
				WakefulIntentService.handleSuccess(context, ssid, Notification.FLAG_ONLY_ALERT_ONCE, result.getLogOffUrl());
				break;
			case ResponseCodes.CUST_CREDENTIALS_ERROR:
				WakefulIntentService.notifyCredentialsError(context);
				break;
			case ResponseCodes.FON_INVALID_CREDENTIALS_ALT:
			case ResponseCodes.FON_NOT_ENOUGH_CREDIT:
			case ResponseCodes.FON_INVALID_CREDENTIALS:
			case ResponseCodes.FON_USER_IN_BLACK_LIST:
			case ResponseCodes.FON_SESSION_LIMIT_EXCEEDED:
			case ResponseCodes.FON_SPOT_LIMIT_EXCEEDED:
			case ResponseCodes.FON_NOT_AUTHORIZED:
			case ResponseCodes.FON_CUSTOMIZED_ERROR:
			case ResponseCodes.FON_INTERNAL_ERROR:
			case ResponseCodes.FON_UNKNOWN_ERROR:
			case ResponseCodes.FON_INVALID_TEMPORARY_CREDENTIAL:
			case ResponseCodes.FON_AUTHORIZATION_CONNECTION_ERROR:
				WakefulIntentService.notifyFonError(context, result.getReplyMessage(), responseCode);
				break;
			default:
				break;
		}

	}

	private static void logoff(final Context context, final String url, final WifiManager wm) {
		if ((url != null) && (url.length() != 0)) {
			HttpUtils.getUrl(url);
		}
		wm.removeNetwork(wm.getConnectionInfo().getNetworkId());
		WakefulIntentService.cancelNotification(context);
	}

	private static void notify(final Context context, final String title, final long[] vibratePattern, final int flags, final String ringtone, final String text, final PendingIntent pendingIntent) {
		final Notification notification = new Notification(R.drawable.ic_stat_fon, title, System.currentTimeMillis());
		if (WakefulIntentService.areNotificationsEnabled(context)) {
			if (WakefulIntentService.isVibrationEnabled(context)) {
				notification.vibrate = vibratePattern;
			}
			notification.sound = Uri.parse(ringtone);
		}
		notification.flags |= flags;
		notification.setLatestEventInfo(context, title, text, pendingIntent);
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(WakefulIntentService.NOTIFICATION_ID, notification);
	}

	private static void notifyCredentialsError(final Context context) {
		WakefulIntentService.notify(context, context.getString(R.string.notif_title_10001), WakefulIntentService.VIBRATE_PATTERN_FAILURE, 0, WakefulIntentService.getFailureTone(context), context.getString(R.string.notif_text_config), PendingIntent.getActivity(context, WakefulIntentService.REQUEST_CODE, new Intent(context, BasicPreferences.class), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private static void notifyFonError(final Context context, final String replyMessage, final int responseCode) {
		WakefulIntentService.notify(context, context.getString(R.string.notif_title_9xx, Integer.valueOf(responseCode)), WakefulIntentService.VIBRATE_PATTERN_FAILURE, 0, WakefulIntentService.getFailureTone(context), '"' + replyMessage + '"', PendingIntent.getActivity(context, WakefulIntentService.REQUEST_CODE, new Intent(context, BasicPreferences.class), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private static void notifySuccess(final Context context, final String ssid, final int flags, final String logoffUrl) {
		WakefulIntentService.notify(context, context.getString(R.string.notif_title_conn, ssid), WakefulIntentService.VIBRATE_PATTERN_SUCCESS, Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | flags, WakefulIntentService.getSuccessTone(context), context.getString(R.string.notif_text_logoff), PendingIntent.getService(context, WakefulIntentService.REQUEST_CODE, new Intent(context, WakefulIntentService.class).setAction(Keys.KEY_LOGOFF).putExtra(Keys.KEY_LOGOFF_URL, logoffUrl), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private static void purgeFonNetworks(final WifiManager wm) {
		final WifiConfiguration[] wca = WakefulIntentService.getConfiguredNetworks(wm);
		if (wca == null) {
			return;
		}
		boolean configurationChanged = false;
		for (final WifiConfiguration wc : wca) {
			if (wc != null) {
				final String ssid = WakefulIntentService.stripQuotes(wc.SSID);
				if (LoginManager.isSupportedNetwork(ssid) && wm.removeNetwork(wc.networkId)) {
					configurationChanged = true;
				}
			}
		}
		if (configurationChanged) {
			wm.saveConfiguration();
		}
	}

	private static void scheduleAction(final Context context, final String action, final int seconds) {
		((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (seconds * 1000), PendingIntent.getBroadcast(context, WakefulIntentService.REQUEST_CODE, new Intent(context, BroadcastIntentReceiver.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private static void scheduleConnectivityCheck(final Context context) {
		WakefulIntentService.scheduleAction(context, Keys.KEY_LOGIN, WakefulIntentService.CONNECTIVITY_CHECK_INTERVAL);
	}

	private static void scheduleScan(final Context context) {
		WakefulIntentService.scheduleAction(context, Keys.KEY_SCAN, WakefulIntentService.getPeriod(context));
	}

	private static String stripQuotes(final String ssid) {
		final int length = ssid.length();
		if ((length > 2) && (ssid.charAt(0) == '"') && (ssid.charAt(length - 1) == '"')) {
			return new String(ssid.substring(1, length - 1));
		}
		return ssid;
	}

	private static void tryToRecover(final Context context, final WifiManager wm, final WifiInfo wi) {
		wm.removeNetwork(wi.getNetworkId());
		WakefulIntentService.cancelNotification(context);
	}

	public static ComponentName start(final Context context, final Intent intent) {
		synchronized (WakefulIntentService.ACTIVE_WAKELOCKS) {
			final int id = WakefulIntentService.NEXT_WAKELOCK_ID;
			if (++WakefulIntentService.NEXT_WAKELOCK_ID <= 0) {
				WakefulIntentService.NEXT_WAKELOCK_ID = 1;
			}
			intent.putExtra(WakefulIntentService.KEY_WAKELOCK_ID, id);
			final ComponentName comp = context.startService(intent);
			if (comp == null) {
				return null;
			}
			final PowerManager.WakeLock wl = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:" + comp.flattenToShortString());
			wl.setReferenceCounted(false);
			wl.acquire(WakefulIntentService.WAKELOCK_TIMEOUT);
			WakefulIntentService.ACTIVE_WAKELOCKS.put(id, wl);
			return comp;
		}
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		final String action = intent.getAction();
		if (action.equals(Keys.KEY_LOGOFF)) {
			WakefulIntentService.logoff(this, intent.getStringExtra(Keys.KEY_LOGOFF_URL), (WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Keys.KEY_SCAN)) {
			((WifiManager) getSystemService(Context.WIFI_SERVICE)).startScan();
		} else if (action.equals(Keys.KEY_CONNECT)) {
			WakefulIntentService.connect(this, (WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Keys.KEY_LOGIN)) {
			WakefulIntentService.login(this, (WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Keys.KEY_CANCEL_NOTIFICATION)) {
			WakefulIntentService.cancelNotification(this);
		} else if (action.equals(Keys.KEY_CANCEL_SCHEDULED_ACTIONS)) {
			WakefulIntentService.cancelScheduledActions(this);
		}
		WakefulIntentService.completeWakefulIntent(intent);
	}

}
