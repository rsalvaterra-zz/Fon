package org.rsalvaterra.fon.login;

import org.rsalvaterra.fon.Constants;
import org.rsalvaterra.fon.HttpUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.net.Uri;
import android.util.Xml;

public final class LoginManager {

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".neuf.fr", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	private static String doLogin(final String url, final String user, final String password) {
		final Uri u = Uri.parse(url);
		if (u.getScheme().equals("https")) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final String h = u.getHost();
				if (h.endsWith(s)) {
					final String username;
					if ((h.contains("portal.fon.com") || h.contentEquals("www.btopenzone.com") || h.contains("wifi.sfr.fr")) && !(h.contains("belgacom") || h.contains("telekom"))) {
						username = LoginManager.FON_USERNAME_PREFIX + user;
					} else {
						username = user;
					}
					final String r = HttpUtils.post(url, username, password);
					if (r != null) {
						return LoginManager.getXml(r);
					}
				}
			}
		}
		return null;
	}

	private static LoginResult fonLogin(final String user, final String password) {
		int rc = Constants.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String rm = null;
		String lu = null;
		String c = HttpUtils.get(LoginManager.CONNECTION_TEST_URL);
		if (c != null) {
			if (!c.equals(LoginManager.CONNECTED)) {
				c = LoginManager.getXml(c);
				if (c != null) {
					final FonInfoHandler wih = new FonInfoHandler();
					if (LoginManager.parseXml(c, wih) && (wih.getMessageType() == Constants.WISPR_MESSAGE_TYPE_INITIAL_REDIRECT) && (wih.getResponseCode() == Constants.WISPR_RESPONSE_CODE_NO_ERROR)) {
						c = LoginManager.doLogin(wih.getLoginURL(), user, password);
						if (c != null) {
							final FonResponseHandler wrh = new FonResponseHandler();
							if (LoginManager.parseXml(c, wrh)) {
								rc = wrh.getResponseCode();
								if (rc == Constants.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED) {
									lu = wrh.getLogoffURL();
								} else if (rc == Constants.WISPR_RESPONSE_CODE_LOGIN_FAILED) {
									rc = wrh.getFonResponseCode();
									rm = wrh.getReplyMessage();
								}
							}
						} else if (LoginManager.isConnected()) {
							rc = Constants.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED;
						}
					}
				} else {
					rc = Constants.CUST_WISPR_NOT_PRESENT;
				}
			} else {
				rc = Constants.CUST_ALREADY_CONNECTED;
			}
		}
		return new LoginResult(rc, rm, lu);
	}

	private static String getSfrUrl(final String source) {
		final int start = source.indexOf("SFRLoginURL_JIL");
		final int end = source.indexOf("-->", start);
		if ((start == -1) || (end == -1)) {
			return null;
		}
		final String url = source.substring(start, end);
		return new String(url.substring(url.indexOf("https")).replace("&amp;", "&").replace("notyet", "smartclient"));

	}

	private static String getXml(final String source) {
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

	private static boolean isBt(final String ssid) {
		return ssid.equals("BTWiFi") || ssid.equals("BTWiFi-with-FON") || ssid.equals("BTOpenzone") || ssid.equals("BTOpenzone-H") || ssid.equals("BTOpenzone-B") || ssid.equals("BTOpenzone-M") || ssid.equals("BTFON");
	}

	private static boolean isConnected() {
		return LoginManager.CONNECTED.equals(HttpUtils.get(LoginManager.CONNECTION_TEST_URL));
	}

	private static boolean isDowntownBrooklyn(final String ssid) {
		return ssid.equalsIgnoreCase("DowntownBrooklynWifi_Fon");
	}

	private static boolean isDt(final String ssid) {
		return ssid.equals("Telekom_FON");
	}

	private static boolean isFon(final String ssid) {
		return LoginManager.isGenericFon(ssid) || LoginManager.isBt(ssid) || LoginManager.isProximus(ssid) || LoginManager.isKpn(ssid) || LoginManager.isDt(ssid) || LoginManager.isSt(ssid) || LoginManager.isJt(ssid) || LoginManager.isHt(ssid) || LoginManager.isOte(ssid) || LoginManager.isRomtelecom(ssid) || LoginManager.isTtnet(ssid) || LoginManager.isOtherFon(ssid) || LoginManager.isOi(ssid) || LoginManager.isDowntownBrooklyn(ssid) || LoginManager.isMweb(ssid) || LoginManager.isSoftBank(ssid) || LoginManager.isTelstra(ssid);
	}

	private static boolean isGenericFon(final String ssid) {
		return ssid.startsWith("FON_");
	}

	private static boolean isHt(final String ssid) {
		return ssid.equals("HotSpot Fon");
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
		return ssid.equalsIgnoreCase("Fon WiFi") || ssid.equalsIgnoreCase("Fon WiFi 5GHz") || ssid.equalsIgnoreCase("Fon WiFi 5G") || ssid.equalsIgnoreCase("Fon Free WiFi") || ssid.equalsIgnoreCase("Fon WiFi (free)");
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

	private static boolean isSoftBank(final String ssid) {
		return ssid.equalsIgnoreCase("NOC_SOFTBANK") || ssid.equals("FON");
	}

	private static boolean isSt(final String ssid) {
		return ssid.equalsIgnoreCase("Telekom FON");
	}

	private static boolean isTelstra(final String ssid) {
		return ssid.equalsIgnoreCase("Telstra Air");
	}

	private static boolean isTtnet(final String ssid) {
		return ssid.equalsIgnoreCase("TTNET WiFi FON");
	}

	private static boolean parseXml(final String xml, final ContentHandler handler) {
		try {
			Xml.parse(xml, handler);
		} catch (final SAXException e) {
			return false;
		}
		return true;
	}

	private static LoginResult sfrLogin(final String user, final String password) {
		int rc = Constants.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String lu = null;
		String c = HttpUtils.get(LoginManager.CONNECTION_TEST_URL);
		if (c != null) {
			if (!c.equals(LoginManager.CONNECTED)) {
				c = LoginManager.getSfrUrl(c);
				if (c != null) {
					c = LoginManager.doLogin(c, user, password);
					if (c != null) {
						FonResponseHandler wrh = new FonResponseHandler();
						if (LoginManager.parseXml(c, wrh)) {
							if (wrh.getResponseCode() == Constants.WISPR_RESPONSE_CODE_AUTH_PENDING) {
								c = HttpUtils.get(wrh.getLoginResultsURL());
								if (c != null) {
									wrh = new FonResponseHandler();
									if (LoginManager.parseXml(c, wrh)) {
										rc = wrh.getResponseCode();
										lu = wrh.getLogoffURL();
									}
								}
							} else {
								rc = wrh.getResponseCode();
								lu = wrh.getLogoffURL();
							}
						}
					} else if (LoginManager.isConnected()) {
						rc = Constants.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED;
					}
				} else {
					rc = Constants.CUST_WISPR_NOT_PRESENT;
				}
			} else {
				rc = Constants.CUST_ALREADY_CONNECTED;
			}
		}
		return new LoginResult(rc, null, lu);
	}

	public static boolean isSupported(final String ssid) {
		return LoginManager.isFon(ssid) || LoginManager.isSfr(ssid);
	}

	public static LoginResult login(final String ssid, final String user, final String password) {
		final LoginResult r;
		if ((user.length() == 0) || (password.length() == 0)) {
			r = new LoginResult(Constants.CUST_CREDENTIALS_ERROR, null, null);
		} else if (LoginManager.isFon(ssid)) {
			r = LoginManager.fonLogin(user, password);
		} else {
			r = LoginManager.sfrLogin(user, password);
		}
		return r;
	}

}
