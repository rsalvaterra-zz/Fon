package org.rsalvaterra.fon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.rsalvaterra.fon.activity.BasicPreferences;
import org.rsalvaterra.fon.blacklist.BlacklistProvider;
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

	private static WifiConfiguration[] getConfiguredNetworks(final WifiManager wm) {
		final List<WifiConfiguration> wcl = wm.getConfiguredNetworks();
		if (wcl == null) {
			return null;
		}
		return wcl.toArray(new WifiConfiguration[wcl.size()]);
	}

	private static int getOtherId(final WifiConfiguration[] wca, final ScanResult[] sra, final boolean secureOnly) {
		if ((wca == null) || (wca.length == 0)) {
			return -1;
		}
		final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
		for (final WifiConfiguration wc : wca) {
			final String ssid = WakefulIntentService.stripQuotes(wc.SSID);
			if ((!secureOnly || (secureOnly && WakefulIntentService.isSecure(wc))) && !LoginManager.isSupported(ssid)) {
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

	private static ScanResult[] getScanResults(final WifiManager wm) {
		final List<ScanResult> srl = wm.getScanResults();
		final ScanResult[] sra = srl.toArray(new ScanResult[srl.size()]);
		Arrays.sort(sra, WakefulIntentService.BY_DESCENDING_LEVEL);
		return sra;
	}

	private static boolean isConnected(final SupplicantState ss) {
		return (ss == SupplicantState.COMPLETED);
	}

	private static boolean isDisconnected(final SupplicantState ss) {
		return (ss == SupplicantState.INACTIVE) || (ss == SupplicantState.DISCONNECTED) || (ss == SupplicantState.SCANNING);
	}

	private static boolean isSecure(final WifiConfiguration wc) {
		return wc.allowedKeyManagement.get(KeyMgmt.WPA_PSK) || wc.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || wc.allowedKeyManagement.get(KeyMgmt.IEEE8021X) || (wc.wepKeys[0] != null);
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
				if (LoginManager.isSupported(ssid) && wm.removeNetwork(wc.networkId)) {
					configurationChanged = true;
				}
			}
		}
		if (configurationChanged) {
			wm.saveConfiguration();
		}
	}

	private static String stripQuotes(final String ssid) {
		final int length = ssid.length();
		if ((length > 2) && (ssid.charAt(0) == '"') && (ssid.charAt(length - 1) == '"')) {
			return new String(ssid.substring(1, length - 1));
		}
		return ssid;
	}

	public static ComponentName start(final Context context, final Intent intent) {
		synchronized (WakefulIntentService.ACTIVE_WAKELOCKS) {
			final int id = WakefulIntentService.NEXT_WAKELOCK_ID;
			if (++WakefulIntentService.NEXT_WAKELOCK_ID <= 0) {
				WakefulIntentService.NEXT_WAKELOCK_ID = 1;
			}
			intent.putExtra(WakefulIntentService.KEY_WAKELOCK_ID, id);
			final PowerManager.WakeLock wl = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:" + WakefulIntentService.KEY_WAKELOCK_ID);
			wl.setReferenceCounted(false);
			wl.acquire(WakefulIntentService.WAKELOCK_TIMEOUT);
			WakefulIntentService.ACTIVE_WAKELOCKS.put(id, wl);
			return context.startService(intent);
		}
	}

	private boolean areNotificationsEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_notify), true);
	}

	private void cancelNotification() {
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(WakefulIntentService.NOTIFICATION_ID);
	}

	private void cancelScheduledActions() {
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(PendingIntent.getBroadcast(this, WakefulIntentService.REQUEST_CODE, new Intent(this, AlarmBroadcastReceiver.class).setAction(Constants.KEY_SCAN), PendingIntent.FLAG_UPDATE_CURRENT));
		alarmManager.cancel(PendingIntent.getBroadcast(this, WakefulIntentService.REQUEST_CODE, new Intent(this, AlarmBroadcastReceiver.class).setAction(Constants.KEY_LOGIN), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void connect(final WifiManager wm) {
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
				id = getFonId(wca, sra, wm);
				if (id == -1) {
					cancelNotification();
				} else if (wm.enableNetwork(id, true) && isReconnectEnabled()) {
					scheduleScan();
				}
			} else if (wm.enableNetwork(id, true)) {
				cancelNotification();
			}
		} else if (WakefulIntentService.isConnected(ss) && isReconnectEnabled() && LoginManager.isSupported(WakefulIntentService.stripQuotes(wi.getSSID()))) {
			final int id = WakefulIntentService.getOtherId(WakefulIntentService.getConfiguredNetworks(wm), WakefulIntentService.getScanResults(wm), isSecureEnabled());
			if (id != -1) {
				wm.enableNetwork(id, true);
			} else {
				scheduleScan();
			}
		}
	}

	private void disconnect(final WifiManager wm, final WifiInfo wi) {
		wm.removeNetwork(wi.getNetworkId());
		cancelNotification();
	}

	private String getFailureTone() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_failure), "");
	}

	private int getFonId(final WifiConfiguration[] wca, final ScanResult[] sra, final WifiManager wm) {
		if (wca == null) {
			return -1;
		}
		final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
		for (final WifiConfiguration wc : wca) {
			if (!WakefulIntentService.isSecure(wc)) {
				final String ssid = WakefulIntentService.stripQuotes(wc.SSID);
				if (LoginManager.isSupported(ssid)) {
					wcm.put(ssid, Integer.valueOf(wc.networkId));
				}
			}
		}
		for (final ScanResult sr : sra) {
			if (LoginManager.isSupported(sr.SSID) && !BlacklistProvider.isBlacklisted(getContentResolver(), sr.BSSID)) {
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

	private String getPassword() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_password), "").trim();
	}

	private int getPeriod() {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_interval), "300"));
	}

	private String getSuccessTone() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_success), "");
	}

	private String getUsername() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_username), "").trim();
	}

	private void handleSuccess(final String ssid, final int flags, final String logoffUrl) {
		notifySuccess(ssid, flags, logoffUrl);
		scheduleConnectivityCheck();
	}

	private boolean isReconnectEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_reconnect), false);
	}

	private boolean isSecureEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_secure), true);
	}

	private boolean isVibrationEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_vibration), false);
	}

	private void login(final WifiManager wm) {
		final WifiInfo wi = wm.getConnectionInfo();
		if ((wi == null) || (wi.getSSID() == null)) {
			return;
		}
		final String ssid = WakefulIntentService.stripQuotes(wi.getSSID());
		if (!LoginManager.isSupported(ssid)) {
			cancelNotification();
			WakefulIntentService.purgeFonNetworks(wm);
			return;
		}
		final LoginResult result = LoginManager.login(ssid, getUsername(), getPassword());
		final int responseCode = result.getResponseCode();
		switch (responseCode) {
			case Constants.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED:
				handleSuccess(ssid, 0, result.getLogOffUrl());
				break;
			case Constants.CUST_ALREADY_CONNECTED:
				handleSuccess(ssid, Notification.FLAG_ONLY_ALERT_ONCE, result.getLogOffUrl());
				break;
			case Constants.FON_INVALID_CREDENTIALS_ALT:
			case Constants.FON_NOT_ENOUGH_CREDIT:
			case Constants.FON_USER_IN_BLACK_LIST:
			case Constants.FON_NOT_AUTHORIZED:
			case Constants.FON_CUSTOMIZED_ERROR:
			case Constants.FON_INTERNAL_ERROR:
			case Constants.FON_UNKNOWN_ERROR:
			case Constants.FON_INVALID_TEMPORARY_CREDENTIAL:
			case Constants.FON_AUTHORIZATION_CONNECTION_ERROR:
				notifyFonError(result.getReplyMessage(), responseCode);
				break;
			case Constants.FON_INVALID_CREDENTIALS:
			case Constants.FON_SESSION_LIMIT_EXCEEDED:
			case Constants.FON_SPOT_LIMIT_EXCEEDED:
			case Constants.CUST_WISPR_NOT_PRESENT:
				BlacklistProvider.addToBlacklist(getContentResolver(), wi.getBSSID());
				//$FALL-THROUGH$
			case Constants.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR:
				disconnect(wm, wi);
				break;
			case Constants.CUST_CREDENTIALS_ERROR:
				notifyCredentialsError();
				break;
			default:
				break;
		}
	}

	private void logoff(final String url, final WifiManager wm) {
		if ((url != null) && (url.length() != 0)) {
			HttpUtils.get(url);
		}
		wm.removeNetwork(wm.getConnectionInfo().getNetworkId());
		cancelNotification();
	}

	private void notify(final String title, final long[] vibratePattern, final int flags, final String ringtone, final String text, final PendingIntent pendingIntent) {
		final Notification notification = new Notification(R.drawable.ic_stat_fon, title, System.currentTimeMillis());
		if (areNotificationsEnabled()) {
			if (isVibrationEnabled()) {
				notification.vibrate = vibratePattern;
			}
			notification.sound = Uri.parse(ringtone);
		}
		notification.flags |= flags;
		notification.setLatestEventInfo(this, title, text, pendingIntent);
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(WakefulIntentService.NOTIFICATION_ID, notification);
	}

	private void notifyCredentialsError() {
		notify(getString(R.string.notif_title_10001), WakefulIntentService.VIBRATE_PATTERN_FAILURE, 0, getFailureTone(), getString(R.string.notif_text_config), PendingIntent.getActivity(this, WakefulIntentService.REQUEST_CODE, new Intent(this, BasicPreferences.class), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void notifyFonError(final String replyMessage, final int responseCode) {
		notify(getString(R.string.notif_title_9xx, Integer.valueOf(responseCode)), WakefulIntentService.VIBRATE_PATTERN_FAILURE, 0, getFailureTone(), '"' + replyMessage + '"', PendingIntent.getActivity(this, WakefulIntentService.REQUEST_CODE, new Intent(this, BasicPreferences.class), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void notifySuccess(final String ssid, final int flags, final String logoffUrl) {
		notify(getString(R.string.notif_title_conn, ssid), WakefulIntentService.VIBRATE_PATTERN_SUCCESS, Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | flags, getSuccessTone(), getString(R.string.notif_text_logoff), PendingIntent.getService(this, WakefulIntentService.REQUEST_CODE, new Intent(this, WakefulIntentService.class).setAction(Constants.KEY_LOGOFF).putExtra(Constants.KEY_LOGOFF_URL, logoffUrl), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void scheduleAction(final String action, final int seconds) {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (seconds * 1000), PendingIntent.getBroadcast(this, WakefulIntentService.REQUEST_CODE, new Intent(this, AlarmBroadcastReceiver.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void scheduleConnectivityCheck() {
		scheduleAction(Constants.KEY_LOGIN, WakefulIntentService.CONNECTIVITY_CHECK_INTERVAL);
	}

	private void scheduleScan() {
		scheduleAction(Constants.KEY_SCAN, getPeriod());
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		final String action = intent.getAction();
		if (action.equals(Constants.KEY_LOGOFF)) {
			logoff(intent.getStringExtra(Constants.KEY_LOGOFF_URL), (WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Constants.KEY_SCAN)) {
			((WifiManager) getSystemService(Context.WIFI_SERVICE)).startScan();
		} else if (action.equals(Constants.KEY_CONNECT)) {
			connect((WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Constants.KEY_LOGIN)) {
			login((WifiManager) getSystemService(Context.WIFI_SERVICE));
		} else if (action.equals(Constants.KEY_CANCEL_NOTIFICATION)) {
			cancelNotification();
		} else if (action.equals(Constants.KEY_CANCEL_SCHEDULED_ACTIONS)) {
			cancelScheduledActions();
		}
		WakefulIntentService.completeWakefulIntent(intent);
	}

}
