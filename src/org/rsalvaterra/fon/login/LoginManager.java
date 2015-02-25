package org.rsalvaterra.fon.login;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;
import org.rsalvaterra.fon.HttpUtils;
import org.rsalvaterra.fon.R;
import org.rsalvaterra.fon.ResponseCodes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Xml;

public final class LoginManager {

	private static final String FON_MAC_PREFIX = "00:18:84";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";
	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".neuf.fr", ".wifi.sfr.fr", ".hotspotsvankpn.com", ".livedoor.com" };
	static final String CONNECTED = "CONNECTED";
	static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	static final String DEFAULT_LOGOFF_URL = "http://192.168.3.1:80/logoff";
	static final String TAG_RESPONSE_CODE = "ResponseCode";

	private static String getPassword(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_password), "").trim();
	}

	private static String getUsername(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_username), "").trim();
	}

	private static boolean isBT(final String ssid) {
		return ssid.equalsIgnoreCase("BTWiFi-with-FON") || ssid.equalsIgnoreCase("BTWIFI");
	}

	private static boolean isDowntownBrooklyn(final String ssid) {
		return ssid.equalsIgnoreCase("DowntownBrooklynWifi_Fon");
	}

	private static boolean isDT(final String ssid) {
		return ssid.equalsIgnoreCase("Telekom_FON");
	}

	private static boolean isFonera(final String ssid, final String bssid) {
		return !LoginManager.isLivedoor(ssid, bssid) && ssid.toUpperCase(Locale.US).startsWith("FON_");
	}

	private static boolean isFonNetwork(final String ssid, final String bssid) {
		return LoginManager.isNOS(ssid) || LoginManager.isFonera(ssid, bssid) || LoginManager.isBT(ssid) || LoginManager.isProximus(ssid) || LoginManager.isKPN(ssid) || LoginManager.isDT(ssid) || LoginManager.isST(ssid) || LoginManager.isJT(ssid) || LoginManager.isHT(ssid) || LoginManager.isOTE(ssid) || LoginManager.isNETIA(ssid) || LoginManager.isRomtelecom(ssid) || LoginManager.isTTNET(ssid) || LoginManager.isOtherFon(ssid) || LoginManager.isOi(ssid) || LoginManager.isDowntownBrooklyn(ssid) || LoginManager.isMWEB(ssid) || LoginManager.isTelstra(ssid);
	}

	private static boolean isFonWISPrURL(final URL url) {
		return (url.getHost().contains("portal.fon.com") || url.getHost().contentEquals("www.btopenzone.com") || url.getHost().contains("wifi.sfr.fr")) && !(url.getHost().contains("belgacom") || url.getHost().contains("telekom"));
	}

	private static boolean isHT(final String ssid) {
		return ssid.equalsIgnoreCase("HotSpot Fon");
	}

	private static boolean isJT(final String ssid) {
		return ssid.equalsIgnoreCase("JT Fon");
	}

	private static boolean isKPN(final String ssid) {
		return ssid.equalsIgnoreCase("KPN Fon");
	}

	private static boolean isLivedoor(final String ssid, final String bssid) {
		return ((bssid == null) || !bssid.startsWith(LoginManager.FON_MAC_PREFIX)) && ssid.equalsIgnoreCase("FON_livedoor");
	}

	private static boolean isMWEB(final String ssid) {
		return ssid.equalsIgnoreCase("@MWEB Fon");
	}

	private static boolean isNETIA(final String ssid) {
		return ssid.equalsIgnoreCase("FON_NETIA_FREE_INTERNET");
	}

	private static boolean isNOS(final String ssid) {
		return ssid.equalsIgnoreCase("FON_ZON_FREE_INTERNET");
	}

	private static boolean isOi(final String ssid) {
		return ssid.toUpperCase(Locale.US).startsWith("OI_WIFI_FON") || ssid.equalsIgnoreCase("OI WIFI FON");
	}

	private static boolean isOTE(final String ssid) {
		return ssid.equalsIgnoreCase("OTE WiFi Fon");
	}

	private static boolean isOtherFon(final String ssid) {
		return ssid.equalsIgnoreCase("FON") || ssid.equalsIgnoreCase("Fon WiFi") || ssid.equalsIgnoreCase("Fon WiFi 5GHz") || ssid.equalsIgnoreCase("Fon WiFi 5G") || ssid.equalsIgnoreCase("Fon Free WiFi") || ssid.equalsIgnoreCase("Fon WiFi (free)");
	}

	private static boolean isProximus(final String ssid) {
		return ssid.equalsIgnoreCase("PROXIMUS_FON");
	}

	private static boolean isRomtelecom(final String ssid) {
		return ssid.equalsIgnoreCase("Romtelecom Fon");
	}

	private static boolean isSFR(final String ssid) {
		return ssid.equalsIgnoreCase("SFR WiFi FON");
	}

	private static boolean isSoftBank(final String ssid) {
		return ssid.equalsIgnoreCase("NOC_SOFTBANK");
	}

	private static boolean isST(final String ssid) {
		return ssid.equalsIgnoreCase("Telekom FON");
	}

	private static boolean isTelstra(final String ssid) {
		return ssid.equalsIgnoreCase("Telstra Air");
	}

	private static boolean isTTNET(final String ssid) {
		return ssid.equalsIgnoreCase("TTNET WiFi FON");
	}

	private static URL parseURL(final String url) {
		final URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException e) {
			return null;
		}
		if (u.getProtocol().equals("https")) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				if (u.getHost().toLowerCase(Locale.US).endsWith(s)) {
					return u;
				}
			}
		}
		return null;
	}

	static String getFonXML(final String source) {
		final int start = source.indexOf("<" + LoginManager.TAG_WISPR);
		final int end = source.indexOf("</" + LoginManager.TAG_WISPR + ">", start);
		if ((start == -1) || (end == -1)) {
			return null;
		}
		final String res = new String(source.substring(start, end + LoginManager.TAG_WISPR.length() + 3));
		if (!res.contains("&amp;")) {
			return res.replace("&", "&amp;");
		}
		return res;
	}

	static String getFonXMLByPost(final String url, final String user, final String password) {
		final URL u = LoginManager.parseURL(url);
		if (u != null) {
			final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
			final String username;
			if (LoginManager.isFonWISPrURL(u)) {
				username = LoginManager.FON_USERNAME_PREFIX + user;
			} else {
				username = user;
			}
			p.add(new BasicNameValuePair(LoginManager.TAG_WISPR_USERNAME, username));
			p.add(new BasicNameValuePair(LoginManager.TAG_WISPR_PASSWORD, password));
			final String r = HttpUtils.getUrlByPost(url, p);
			if (r != null) {
				return LoginManager.getFonXML(r);
			}
		}
		return null;
	}

	static boolean isConnected() {
		final String response = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		return (response != null) && response.equals(LoginManager.CONNECTED);
	}

	static boolean parseFonXML(final String xml, final ContentHandler handler) {
		try {
			Xml.parse(xml, handler);
		} catch (final SAXException e) {
			return false;
		}
		return true;
	}

	public static boolean isSupportedNetwork(final String ssid, final String bssid) {
		return LoginManager.isFonNetwork(ssid, bssid) || LoginManager.isSFR(ssid) || LoginManager.isSoftBank(ssid) || LoginManager.isLivedoor(ssid, bssid);
	}

	public static LoginResult login(final Context context, final String ssid, final String bssid) {
		final String user = LoginManager.getUsername(context);
		final String password = LoginManager.getPassword(context);
		final LoginResult r;
		if ((user.length() == 0) || (password.length() == 0)) {
			r = new LoginResult(ResponseCodes.CUST_CREDENTIALS_ERROR, null, null);
		} else if (LoginManager.isLivedoor(ssid, bssid)) {
			r = LivedoorLogin.login(user, password);
		} else if (LoginManager.isSFR(ssid)) {
			r = SFRLogin.login(user, password);
		} else {
			r = FonLogin.login(user, password);
		}
		return r;
	}

}
