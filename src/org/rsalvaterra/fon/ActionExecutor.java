package org.rsalvaterra.fon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.SparseArray;

public final class ActionExecutor extends Service {

	private static final int NOTIFICATION_ID = 1;
	private static final int REQUEST_CODE = 1;
	private static final int CONNECTIVITY_CHECK_PERIOD = 60 * 1000;
	private static final int LOGOFF_HTTP_TIMEOUT = 2 * 1000;
	private static final int WAKELOCK_TIMEOUT = 60 * 1000;
	private static final int BLACKLIST_PERIOD = 300 * 1000;

	private static final long[] VIBRATE_PATTERN_FAILURE = { 100, 250, 100, 250 };
	private static final long[] VIBRATE_PATTERN_SUCCESS = { 100, 250 };

	private static final String KEY_WAKELOCK_ID = Constants.APP_ID + ".wakelock";

	private static final SparseArray<WakeLock> ACTIVE_WAKELOCKS = new SparseArray<WakeLock>();

	private static final HashMap<String, Long> BLACKLIST = new HashMap<String, Long>();

	private static final Comparator<ScanResult> BY_DESCENDING_SIGNAL_LEVEL = new Comparator<ScanResult>() {

		@Override
		public int compare(final ScanResult sr1, final ScanResult sr2) {
			return sr2.level - sr1.level;
		}
	};

	private static int NEXT_WAKELOCK_ID = 1;

	private final Looper looper;
	private final Handler handler;

	{
		final HandlerThread thread = new HandlerThread(Constants.APP_ID);
		thread.start();
		looper = thread.getLooper();
		handler = new Handler(looper) {

			@Override
			public void handleMessage(final Message msg) {
				executeAction((Intent) msg.obj);
			}
		};
	}

	private static void addToBlacklist(final String bssid) {
		synchronized (ActionExecutor.BLACKLIST) {
			ActionExecutor.BLACKLIST.put(bssid, Long.valueOf(SystemClock.elapsedRealtime() + ActionExecutor.BLACKLIST_PERIOD));
		}
	}

	private static WifiConfiguration[] getConfiguredNetworks(final WifiManager wm) {
		final List<WifiConfiguration> wcl = wm.getConfiguredNetworks();
		final WifiConfiguration[] wca;
		if (wcl == null) {
			wca = new WifiConfiguration[] {};
		} else {
			wca = wcl.toArray(new WifiConfiguration[wcl.size()]);
		}
		return wca;
	}

	private static boolean getPreference(final Context c, final int id, final boolean v) {
		return ActionExecutor.getPreferences(c).getBoolean(c.getString(id), v);
	}

	private static SharedPreferences getPreferences(final Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c);
	}

	private static ScanResult[] getScanResults(final WifiManager wm) {
		final List<ScanResult> srl = wm.getScanResults();
		final ScanResult[] sra = srl.toArray(new ScanResult[srl.size()]);
		Arrays.sort(sra, ActionExecutor.BY_DESCENDING_SIGNAL_LEVEL);
		return sra;
	}

	private static boolean isBlacklisted(final String bssid) {
		synchronized (ActionExecutor.BLACKLIST) {
			final Long time = ActionExecutor.BLACKLIST.get(bssid);
			if (time != null) {
				if (time.longValue() > SystemClock.elapsedRealtime()) {
					return true;
				}
				ActionExecutor.BLACKLIST.remove(bssid);
			}
		}
		return false;
	}

	private static boolean isConnected(final SupplicantState ss) {
		return (ss == SupplicantState.COMPLETED);
	}

	private static boolean isDisconnected(final SupplicantState ss) {
		return (ss == SupplicantState.INACTIVE) || (ss == SupplicantState.DORMANT) || (ss == SupplicantState.DISCONNECTED) || (ss == SupplicantState.SCANNING);
	}

	private static boolean isInsecure(final ScanResult sr) {
		return !(sr.capabilities.contains("WEP") || sr.capabilities.contains("PSK") || sr.capabilities.contains("EAP"));
	}

	private static boolean isInsecure(final WifiConfiguration wc) {
		return wc.allowedKeyManagement.get(KeyMgmt.NONE) && (wc.wepKeys[0] == null);
	}

	private static void logoff(final String url, final WifiManager wm) {
		if ((url != null) && (url.length() != 0)) {
			HttpUtils.get(url, ActionExecutor.LOGOFF_HTTP_TIMEOUT);
		}
		wm.disconnect();
	}

	private static void releaseWakeLock(final Intent i) {
		final int id = i.getIntExtra(ActionExecutor.KEY_WAKELOCK_ID, 0);
		if (id != 0) {
			synchronized (ActionExecutor.ACTIVE_WAKELOCKS) {
				final WakeLock wl = ActionExecutor.ACTIVE_WAKELOCKS.get(id);
				if (wl != null) {
					wl.release();
					ActionExecutor.ACTIVE_WAKELOCKS.remove(id);
				}
			}
		}
	}

	private static void removeConfiguration(final WifiConfiguration[] wca, final WifiManager wm, final String ssid) {
		if (wca.length != 0) {
			for (final WifiConfiguration wc : wca) {
				if (ActionExecutor.isInsecure(wc) && wc.SSID.equals(ssid)) {
					wm.removeNetwork(wc.networkId);
					break;
				}
			}
		}
	}

	private static String stripQuotes(final String ssid) {
		final int length = ssid.length();
		if ((ssid.charAt(0) == '"') && (ssid.charAt(length - 1) == '"')) {
			return ssid.substring(1, length - 1);
		}
		return ssid;
	}

	static ComponentName execute(final Context context, final Intent intent) {
		synchronized (ActionExecutor.ACTIVE_WAKELOCKS) {
			final int id = ActionExecutor.NEXT_WAKELOCK_ID++;
			if (ActionExecutor.NEXT_WAKELOCK_ID <= 0) {
				ActionExecutor.NEXT_WAKELOCK_ID = 1;
			}
			final ComponentName cn = context.startService(intent.putExtra(ActionExecutor.KEY_WAKELOCK_ID, id));
			if (cn != null) {
				final WakeLock wl = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ActionExecutor.KEY_WAKELOCK_ID);
				wl.setReferenceCounted(false);
				wl.acquire(ActionExecutor.WAKELOCK_TIMEOUT);
				ActionExecutor.ACTIVE_WAKELOCKS.put(id, wl);
			}
			return cn;
		}
	}

	static String getPreference(final Context c, final int id, final String v) {
		return ActionExecutor.getPreferences(c).getString(c.getString(id), v);
	}

	static boolean isAutoConnectEnabled(final Context c) {
		return ActionExecutor.getPreference(c, R.string.kautoconnect, true);
	}

	private boolean areNotificationsEnabled() {
		return ActionExecutor.getPreference(this, R.string.knotify, true);
	}

	private void cancelAll() {
		final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final Intent i = new Intent(this, AlarmBroadcastReceiver.class);
		am.cancel(PendingIntent.getBroadcast(this, ActionExecutor.REQUEST_CODE, i.setAction(Constants.ACT_SCAN), PendingIntent.FLAG_UPDATE_CURRENT));
		am.cancel(PendingIntent.getBroadcast(this, ActionExecutor.REQUEST_CODE, i.setAction(Constants.ACT_LOGIN), PendingIntent.FLAG_UPDATE_CURRENT));
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ActionExecutor.NOTIFICATION_ID);
	}

	private void connect(final WifiManager wm) {
		final WifiInfo wi = wm.getConnectionInfo();
		final SupplicantState ss = wi.getSupplicantState();
		if (ActionExecutor.isDisconnected(ss)) {
			final WifiConfiguration[] wca = ActionExecutor.getConfiguredNetworks(wm);
			final ScanResult[] sra = ActionExecutor.getScanResults(wm);
			int id = getOtherId(wca, sra, false);
			if (id == -1) {
				id = getFonId(wca, sra, wm);
				if ((id != -1) && wm.enableNetwork(id, true) && isReconnectEnabled()) {
					scheduleScan();
				}
			}
		} else if (ActionExecutor.isConnected(ss) && isReconnectEnabled() && LoginManager.isSupported(ActionExecutor.stripQuotes(wi.getSSID()))) {
			final int id = getOtherId(ActionExecutor.getConfiguredNetworks(wm), ActionExecutor.getScanResults(wm), isSecureEnabled());
			if (id != -1) {
				wm.enableNetwork(id, true);
			} else {
				scheduleScan();
			}
		}
	}

	private String getFailureTone() {
		return ActionExecutor.getPreference(this, R.string.kfailure, "");
	}

	private int getFonId(final WifiConfiguration[] wca, final ScanResult[] sra, final WifiManager wm) {
		final int mr = getMinimumRssi();
		for (final ScanResult sr : sra) {
			if (sr.level < mr) {
				break;
			}
			if (LoginManager.isSupported(sr.SSID) && ActionExecutor.isInsecure(sr) && !ActionExecutor.isBlacklisted(sr.BSSID)) {
				final String ssid = '"' + sr.SSID + '"';
				ActionExecutor.removeConfiguration(wca, wm, ssid);
				final WifiConfiguration wc = new WifiConfiguration();
				wc.SSID = ssid;
				wc.BSSID = sr.BSSID;
				wc.allowedKeyManagement.set(KeyMgmt.NONE);
				return wm.addNetwork(wc);
			}
		}
		return -1;
	}

	private int getMinimumRssi() {
		if (ActionExecutor.getPreference(this, R.string.kreject, false)) {
			return Integer.parseInt(ActionExecutor.getPreference(this, R.string.krssi, Constants.DEFAULT_MINIMUM_RSSI));
		}
		return Integer.MIN_VALUE;
	}

	private int getOtherId(final WifiConfiguration[] wca, final ScanResult[] sra, final boolean secure) {
		if (wca.length != 0) {
			final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
			for (final WifiConfiguration wc : wca) {
				final String ssid = ActionExecutor.stripQuotes(wc.SSID);
				if ((!secure || (secure && !ActionExecutor.isInsecure(wc))) && !LoginManager.isSupported(ssid)) {
					wcm.put(ssid, Integer.valueOf(wc.networkId));
				}
			}
			final int mr = getMinimumRssi();
			for (final ScanResult sr : sra) {
				if (sr.level < mr) {
					break;
				}
				final Integer id = wcm.get(sr.SSID);
				if (id != null) {
					return id.intValue();
				}
			}
		}
		return -1;
	}

	private String getPassword() {
		return ActionExecutor.getPreference(this, R.string.kpassword, "");
	}

	private int getPeriod() {
		return Integer.parseInt(ActionExecutor.getPreference(this, R.string.kperiod, Constants.DEFAULT_PERIOD));
	}

	private String getSuccessTone() {
		return ActionExecutor.getPreference(this, R.string.ksuccess, "");
	}

	private String getUsername() {
		return ActionExecutor.getPreference(this, R.string.kusername, "");
	}

	private void handleError(final WifiManager wm, final WifiInfo wi, final LoginResult lr) {
		if (ActionExecutor.isAutoConnectEnabled(this)) {
			ActionExecutor.addToBlacklist(wi.getBSSID());
			wm.removeNetwork(wi.getNetworkId());
		} else {
			notifyFonError(lr);
		}
	}

	private void handleSuccess(final String ssid, final LoginResult lr, final boolean isLogin) {
		if (isLogin) {
			final Intent i = new Intent();
			final PendingIntent pi;
			final String text;
			if (ActionExecutor.isAutoConnectEnabled(this)) {
				pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
				text = getString(R.string.connected, ssid);
			} else {
				pi = PendingIntent.getService(this, ActionExecutor.REQUEST_CODE, i.setClass(this, ActionExecutor.class).setAction(Constants.ACT_LOGOFF).putExtra(Constants.KEY_LOGOFF_URL, lr.getLogOffUrl()), PendingIntent.FLAG_UPDATE_CURRENT);
				text = getString(R.string.logoff);
			}
			notify(getString(R.string.started), ActionExecutor.VIBRATE_PATTERN_SUCCESS, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_ONGOING_EVENT, getSuccessTone(), text, pi);
		}
		scheduleConnectivityCheck();
	}

	private boolean isReconnectEnabled() {
		return ActionExecutor.getPreference(this, R.string.kreconnect, false);
	}

	private boolean isSecureEnabled() {
		return ActionExecutor.getPreference(this, R.string.ksecure, true);
	}

	private boolean isVibrationEnabled() {
		return ActionExecutor.getPreference(this, R.string.kvibrate, false);
	}

	private void login(final WifiManager wm, final boolean isLogin) {
		final WifiInfo wi = wm.getConnectionInfo();
		final String ssid = ActionExecutor.stripQuotes(wi.getSSID());
		if (LoginManager.isSupported(ssid)) {
			final LoginResult lr = LoginManager.login(getUsername(), getPassword());
			switch (lr.getResponseCode()) {
				case Constants.WRC_LOGIN_SUCCEEDED:
				case Constants.CRC_ALREADY_CONNECTED:
					handleSuccess(ssid, lr, isLogin);
					break;
				case Constants.WRC_RADIUS_ERROR:
				case Constants.WRC_NETWORK_ADMIN_ERROR:
				case Constants.FRC_SPOT_LIMIT_EXCEEDED:
				case Constants.FRC_UNKNOWN_ERROR:
				case Constants.CRC_WISPR_NOT_PRESENT:
					handleError(wm, wi, lr);
					break;
				case Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR:
					wm.removeNetwork(wi.getNetworkId());
					break;
				case Constants.FRC_INVALID_CREDENTIALS_ALT:
				case Constants.FRC_INVALID_CREDENTIALS:
				case Constants.CRC_CREDENTIALS_ERROR:
					notifyCredentialsError();
					break;
				default:
					notifyFonError(lr);
					break;
			}
		}
	}

	private void notify(final String title, final long[] vibratePattern, final int flags, final String ringtone, final String text, final PendingIntent pendingIntent) {
		final Notification notification = new Notification();
		notification.flags |= flags;
		notification.icon = R.drawable.ic_stat_fon;
		if (areNotificationsEnabled()) {
			notification.sound = Uri.parse(ringtone);
			if (isVibrationEnabled()) {
				notification.vibrate = vibratePattern;
			}
		}
		notification.setLatestEventInfo(this, title, text, pendingIntent);
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(ActionExecutor.NOTIFICATION_ID, notification);
	}

	private void notifyCredentialsError() {
		notifyError(getString(R.string.cred_error));
	}

	private void notifyError(final String title) {
		notify(title, ActionExecutor.VIBRATE_PATTERN_FAILURE, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT, getFailureTone(), getString(R.string.configure), PendingIntent.getActivity(this, ActionExecutor.REQUEST_CODE, new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void notifyFonError(final LoginResult lr) {
		notifyError(getString(R.string.fon_error, Integer.valueOf(lr.getResponseCode()), lr.getReplyMessage()));
	}

	private void scheduleAction(final String action, final int milliseconds) {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + milliseconds, PendingIntent.getBroadcast(this, ActionExecutor.REQUEST_CODE, new Intent(this, AlarmBroadcastReceiver.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void scheduleConnectivityCheck() {
		scheduleAction(Constants.ACT_LOGIN, ActionExecutor.CONNECTIVITY_CHECK_PERIOD);
	}

	private void scheduleScan() {
		scheduleAction(Constants.ACT_SCAN, getPeriod() * 1000);
	}

	protected void executeAction(final Intent i) {
		final String a = i.getAction();
		if (a.equals(Constants.ACT_CANCEL_ALL)) {
			cancelAll();
		} else {
			final WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (a.equals(Constants.ACT_CONNECT)) {
				connect(wm);
			} else if (a.equals(Constants.ACT_LOGIN)) {
				login(wm, i.getBooleanExtra(Constants.KEY_LOGIN, false));
			} else if (a.equals(Constants.ACT_LOGOFF)) {
				ActionExecutor.logoff(i.getStringExtra(Constants.KEY_LOGOFF_URL), wm);
			} else if (a.equals(Constants.ACT_SCAN)) {
				wm.startScan();
			}
		}
		ActionExecutor.releaseWakeLock(i);
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		looper.quit();
	}

	@Override
	public void onStart(final Intent i, final int id) {
		final Message m = Message.obtain();
		m.arg1 = id;
		m.obj = i;
		handler.sendMessage(m);
	}

}
