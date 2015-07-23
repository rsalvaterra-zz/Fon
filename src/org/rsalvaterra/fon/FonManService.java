package org.rsalvaterra.fon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

public final class FonManService extends Service implements Callback, Comparator<ScanResult> {

	private static final int NOTIFICATION_ID = 1;
	private static final int REQUEST_CODE = 1;
	private static final int CONNECTIVITY_CHECK_PERIOD = 60 * 1000;
	private static final int BLACKLIST_PERIOD = 300 * 1000;

	private static final long[] VIBRATE_PATTERN_FAILURE = { 100, 250, 100, 250 };
	private static final long[] VIBRATE_PATTERN_SUCCESS = { 100, 250 };

	private static volatile WakeLock WAKELOCK;

	private final HashMap<String, Long> blacklist = new HashMap<String, Long>();

	private final LoginManager loginManager = new LoginManager();

	private final Handler messageHandler;

	{
		final HandlerThread ht = new HandlerThread(Constants.APP_ID);
		ht.start();
		messageHandler = new Handler(ht.getLooper(), this);
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
		return FonManService.getPreferences(c).getBoolean(c.getString(id), v);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static SharedPreferences getPreferences(final Context c) {
		final int mode;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			mode = Context.MODE_MULTI_PROCESS;
		} else {
			mode = Context.MODE_PRIVATE;
		}
		return c.getSharedPreferences(Constants.PREFERENCES_NAME, mode);
	}

	private static boolean isBt(final String ssid) {
		return ssid.equals("BTFON") || ssid.equals("BTWiFi") || ssid.equals("BTWiFi-with-FON") || ssid.startsWith("BTOpenzone");
	}

	private static boolean isConnected(final SupplicantState ss) {
		return (ss == SupplicantState.COMPLETED);
	}

	private static boolean isDisconnected(final SupplicantState ss) {
		return (ss == SupplicantState.INACTIVE) || (ss == SupplicantState.DORMANT) || (ss == SupplicantState.DISCONNECTED) || (ss == SupplicantState.SCANNING);
	}

	private static boolean isDowntownBrooklyn(final String ssid) {
		return ssid.equals("DowntownBrooklynWifi_Fon");
	}

	private static boolean isDt(final String ssid) {
		return ssid.equals("Telekom_FON");
	}

	private static boolean isGenericFon(final String ssid) {
		return ssid.startsWith("FON_");
	}

	private static boolean isHt(final String ssid) {
		return ssid.equals("HotSpot Fon");
	}

	private static boolean isInsecure(final ScanResult sr) {
		return !(sr.capabilities.contains("WEP") || sr.capabilities.contains("PSK") || sr.capabilities.contains("EAP"));
	}

	private static boolean isInsecure(final WifiConfiguration wc) {
		for (final String s : wc.wepKeys) {
			if (s != null) {
				return false;
			}
		}
		return wc.allowedKeyManagement.get(KeyMgmt.NONE);
	}

	private static boolean isJt(final String ssid) {
		return ssid.equalsIgnoreCase("JT Fon");
	}

	private static boolean isKpn(final String ssid) {
		return ssid.equals("KPN Fon");
	}

	private static boolean isMweb(final String ssid) {
		return ssid.equals("@MWEB FON");
	}

	private static boolean isOi(final String ssid) {
		return ssid.equals("Oi WiFi Fon") || ssid.equals("OI_WIFI_FON");
	}

	private static boolean isOte(final String ssid) {
		return ssid.equals("OTE WiFi Fon");
	}

	private static boolean isOtherFon(final String ssid) {
		return ssid.startsWith("Fon WiFi");
	}

	private static boolean isProximus(final String ssid) {
		return ssid.equals("PROXIMUS_FON");
	}

	private static boolean isRomtelecom(final String ssid) {
		return ssid.equals("Romtelecom Fon");
	}

	private static boolean isSfr(final String ssid) {
		return ssid.equals("SFR WiFi FON");
	}

	private static boolean isSoftbank(final String ssid) {
		return ssid.equals("FON");
	}

	private static boolean isSt(final String ssid) {
		return ssid.equalsIgnoreCase("Telekom FON");
	}

	private static boolean isSupported(final String ssid) {
		return FonManService.isGenericFon(ssid) || FonManService.isVodafoneSpain(ssid) || FonManService.isBt(ssid) || FonManService.isJt(ssid) || FonManService.isSfr(ssid) || FonManService.isVodafoneItaly(ssid) || FonManService.isProximus(ssid) || FonManService.isKpn(ssid) || FonManService.isDt(ssid) || FonManService.isSt(ssid) || FonManService.isHt(ssid) || FonManService.isOte(ssid) || FonManService.isRomtelecom(ssid) || FonManService.isTtnet(ssid) || FonManService.isOi(ssid) || FonManService.isDowntownBrooklyn(ssid) || FonManService.isMweb(ssid) || FonManService.isOtherFon(ssid) || FonManService.isSoftbank(ssid) || FonManService.isTelstra(ssid);
	}

	private static boolean isTelstra(final String ssid) {
		return ssid.equals("Telstra AIR");
	}

	private static boolean isTtnet(final String ssid) {
		return ssid.equalsIgnoreCase("TTNET WiFi FON");
	}

	private static boolean isVodafoneItaly(final String ssid) {
		return ssid.equals("Vodafone-WIFI");
	}

	private static boolean isVodafoneSpain(final String ssid) {
		return ssid.equals("_ONOWiFi");
	}

	private static String stripQuotes(final String ssid) {
		final int length = ssid.length();
		if ((ssid.charAt(0) == '"') && (ssid.charAt(length - 1) == '"')) {
			return ssid.substring(1, length - 1);
		}
		return ssid;
	}

	private static int updateOrAddFonConfiguration(final WifiConfiguration[] wca, final WifiManager wm, final ScanResult sr) {
		final String ssid = '"' + sr.SSID + '"';
		if (wca.length != 0) {
			for (final WifiConfiguration wc : wca) {
				if (FonManService.isInsecure(wc) && wc.SSID.equals(ssid)) {
					wc.BSSID = sr.BSSID;
					return wm.updateNetwork(wc);
				}
			}
		}
		final WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = ssid;
		wc.BSSID = sr.BSSID;
		wc.allowedKeyManagement.set(KeyMgmt.NONE);
		return wm.addNetwork(wc);
	}

	private static void wakeLockAcquire(final Context c) {
		if (FonManService.WAKELOCK == null) {
			synchronized (FonManService.class) {
				if (FonManService.WAKELOCK == null) {
					FonManService.WAKELOCK = ((PowerManager) c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.APP_ID);
				}
			}
		} else if (FonManService.WAKELOCK.isHeld()) {
			return;
		}
		FonManService.WAKELOCK.acquire();
	}

	private static void wakeLockRelease() {
		if ((FonManService.WAKELOCK != null) && FonManService.WAKELOCK.isHeld()) {
			FonManService.WAKELOCK.release();
		}
	}

	static void execute(final Context c, final String a) {
		FonManService.wakeLockAcquire(c);
		c.startService(new Intent(c, FonManService.class).setAction(a));
	}

	static String getPreference(final Context c, final int id, final String v) {
		return FonManService.getPreferences(c).getString(c.getString(id), v);
	}

	static boolean isAutoConnectEnabled(final Context c) {
		return FonManService.getPreference(c, R.string.kautoconnect, true);
	}

	private void addToBlacklist(final String bssid) {
		blacklist.put(bssid, Long.valueOf(SystemClock.elapsedRealtime() + FonManService.BLACKLIST_PERIOD));
	}

	private boolean areNotificationsEnabled() {
		return FonManService.getPreference(this, R.string.knotify, true);
	}

	private void cancel() {
		stopPeriodicConnectivityCheck();
		stopPeriodicScan();
		removeNotification();
	}

	private void check(final WifiManager wm) {
		login(wm, true);
	}

	private void connect(final WifiManager wm) {
		final WifiInfo wi = wm.getConnectionInfo();
		final SupplicantState ss = wi.getSupplicantState();
		if (FonManService.isDisconnected(ss)) {
			final WifiConfiguration[] wca = FonManService.getConfiguredNetworks(wm);
			final ScanResult[] sra = getScanResults(wm);
			int id = getOtherId(wca, sra, false);
			if (id == -1) {
				id = getFonId(wca, sra, wm);
				if ((id != -1) && wm.enableNetwork(id, true) && isReconnectEnabled()) {
					startPeriodicScan();
				}
			}
		} else if (FonManService.isConnected(ss) && isReconnectEnabled() && FonManService.isSupported(FonManService.stripQuotes(wi.getSSID()))) {
			final int id = getOtherId(FonManService.getConfiguredNetworks(wm), getScanResults(wm), isSecureEnabled());
			if (id != -1) {
				wm.enableNetwork(id, true);
			}
		}
	}

	private String getFailureTone() {
		return FonManService.getPreference(this, R.string.kfailure, "");
	}

	private int getFonId(final WifiConfiguration[] wca, final ScanResult[] sra, final WifiManager wm) {
		final int mr = getMinimumRssi();
		for (final ScanResult sr : sra) {
			if (sr.level < mr) {
				break;
			}
			if (FonManService.isSupported(sr.SSID) && FonManService.isInsecure(sr) && !isBlacklisted(sr.BSSID)) {
				return FonManService.updateOrAddFonConfiguration(wca, wm, sr);
			}
		}
		return -1;
	}

	private int getMinimumRssi() {
		if (FonManService.getPreference(this, R.string.kreject, false)) {
			return Integer.parseInt(FonManService.getPreference(this, R.string.krssi, Constants.DEFAULT_MINIMUM_RSSI));
		}
		return Integer.MIN_VALUE;
	}

	private int getOtherId(final WifiConfiguration[] wca, final ScanResult[] sra, final boolean secure) {
		if (wca.length != 0) {
			final HashMap<String, Integer> wcm = new HashMap<String, Integer>();
			for (final WifiConfiguration wc : wca) {
				final String ssid = FonManService.stripQuotes(wc.SSID);
				if ((!secure || (secure && !FonManService.isInsecure(wc))) && !FonManService.isSupported(ssid)) {
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
		return FonManService.getPreference(this, R.string.kpassword, "");
	}

	private int getPeriod() {
		return Integer.parseInt(FonManService.getPreference(this, R.string.kperiod, Constants.DEFAULT_PERIOD));
	}

	private ScanResult[] getScanResults(final WifiManager wm) {
		final List<ScanResult> srl = wm.getScanResults();
		final ScanResult[] sra = srl.toArray(new ScanResult[srl.size()]);
		Arrays.sort(sra, this);
		return sra;
	}

	private String getSuccessTone() {
		return FonManService.getPreference(this, R.string.ksuccess, "");
	}

	private String getUsername() {
		return FonManService.getPreference(this, R.string.kusername, "");
	}

	private void handleError(final WifiManager wm, final WifiInfo wi, final LoginResult lr) {
		if (FonManService.isAutoConnectEnabled(this)) {
			addToBlacklist(wi.getBSSID());
			wm.removeNetwork(wi.getNetworkId());
		} else {
			notifyFonError(lr);
		}
	}

	private void handleSuccess(final String ssid, final boolean check) {
		if (check) {
			return;
		}
		notify(getString(R.string.started), FonManService.VIBRATE_PATTERN_SUCCESS, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_ONGOING_EVENT, getSuccessTone(), getString(R.string.connected, ssid), PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT));
		startPeriodicConnectivityCheck();
	}

	private boolean isBlacklisted(final String bssid) {
		final Long t = blacklist.get(bssid);
		if (t != null) {
			if (t.longValue() > SystemClock.elapsedRealtime()) {
				return true;
			}
			blacklist.remove(bssid);
		}
		return false;
	}

	private boolean isReconnectEnabled() {
		return FonManService.getPreference(this, R.string.kreconnect, false);
	}

	private boolean isSecureEnabled() {
		return FonManService.getPreference(this, R.string.ksecure, true);
	}

	private boolean isVibrationEnabled() {
		return FonManService.getPreference(this, R.string.kvibrate, false);
	}

	private void login(final WifiManager wm) {
		login(wm, false);
	}

	private void login(final WifiManager wm, final boolean check) {
		final WifiInfo wi = wm.getConnectionInfo();
		String ssid = wi.getSSID();
		if (ssid == null) {
			return;
		}
		ssid = FonManService.stripQuotes(ssid);
		if (!FonManService.isSupported(ssid)) {
			return;
		}
		final LoginResult lr = loginManager.login(getUsername(), getPassword());
		switch (lr.getResponseCode()) {
			case Constants.WRC_LOGIN_SUCCEEDED:
			case Constants.CRC_ALREADY_AUTHORISED:
				handleSuccess(ssid, check);
				break;
			case Constants.WRC_RADIUS_ERROR:
			case Constants.WRC_NETWORK_ADMIN_ERROR:
			case Constants.FRC_HOTSPOT_LIMIT_EXCEEDED:
			case Constants.FRC_UNKNOWN_ERROR:
			case Constants.CRC_WISPR_NOT_PRESENT:
				handleError(wm, wi, lr);
				break;
			case Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR:
				wm.removeNetwork(wi.getNetworkId());
				break;
			case Constants.FRC_BAD_CREDENTIALS:
			case Constants.CRC_CREDENTIALS_ERROR:
				notifyCredentialsError();
				break;
			default:
				notifyFonError(lr);
				break;
		}
	}

	private void notify(final String title, final long[] vibratePattern, final int flags, final String ringtone, final String text, final PendingIntent pi) {
		final Notification n = new Notification();
		n.flags |= flags;
		n.icon = R.drawable.ic_stat_fon;
		if (areNotificationsEnabled()) {
			n.sound = Uri.parse(ringtone);
			if (isVibrationEnabled()) {
				n.vibrate = vibratePattern;
			}
		}
		n.setLatestEventInfo(this, title, text, pi);
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(FonManService.NOTIFICATION_ID, n);
	}

	private void notifyCredentialsError() {
		notifyError(getString(R.string.cred_error));
	}

	private void notifyError(final String title) {
		notify(title, FonManService.VIBRATE_PATTERN_FAILURE, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT, getFailureTone(), getString(R.string.configure), PendingIntent.getActivity(this, FonManService.REQUEST_CODE, new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void notifyFonError(final LoginResult lr) {
		notifyError(getString(R.string.fon_error, Integer.valueOf(lr.getResponseCode()), lr.getReplyMessage()));
	}

	private void removeNotification() {
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(FonManService.NOTIFICATION_ID);
	}

	private void startPeriodicAction(final int milliseconds, final String action) {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + milliseconds, milliseconds, PendingIntent.getBroadcast(this, FonManService.REQUEST_CODE, new Intent(this, FonManAlarmReceiver.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void startPeriodicConnectivityCheck() {
		startPeriodicAction(FonManService.CONNECTIVITY_CHECK_PERIOD, Constants.ACT_CHECK);
	}

	private void startPeriodicScan() {
		startPeriodicAction(getPeriod() * 1000, Constants.ACT_SCAN);
	}

	private void stopPeriodicAction(final String action) {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, FonManService.REQUEST_CODE, new Intent(this, FonManAlarmReceiver.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void stopPeriodicConnectivityCheck() {
		stopPeriodicAction(Constants.ACT_CHECK);
	}

	private void stopPeriodicScan() {
		stopPeriodicAction(Constants.ACT_SCAN);
	}

	@Override
	public int compare(final ScanResult sr1, final ScanResult sr2) {
		return sr2.level - sr1.level;
	}

	@Override
	public boolean handleMessage(final Message m) {
		final String s = (String) m.obj;
		if (s.equals(Constants.ACT_CANCEL)) {
			cancel();
		} else {
			final WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (s.equals(Constants.ACT_CHECK)) {
				check(wm);
			} else if (s.equals(Constants.ACT_CONNECT)) {
				connect(wm);
			} else if (s.equals(Constants.ACT_LOGIN)) {
				login(wm);
			} else if (s.equals(Constants.ACT_SCAN)) {
				wm.startScan();
			}
		}
		FonManService.wakeLockRelease();
		return true;
	}

	@Override
	public IBinder onBind(final Intent i) {
		return null;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onDestroy() {
		final Looper l = messageHandler.getLooper();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			l.quit();
		} else {
			l.quitSafely();
		}
	}

	@Override
	public int onStartCommand(final Intent i, final int f, final int id) {
		final Message m = Message.obtain();
		m.obj = i.getAction();
		messageHandler.sendMessage(m);
		return Service.START_STICKY;
	}
}
