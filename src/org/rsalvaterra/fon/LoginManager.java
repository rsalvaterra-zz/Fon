package org.rsalvaterra.fon;

public final class LoginManager {

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
	private static final String SAFE_PROTOCOL = "https://";
	private static final String TAG_FON_RESPONSE_CODE = "FONResponseCode";
	private static final String TAG_LOGIN_URL = "LoginURL";
	private static final String TAG_LOGOFF_URL = "LogoffURL";
	private static final String TAG_MESSAGE_TYPE = "MessageType";
	private static final String TAG_REPLY_MESSAGE = "ReplyMessage";
	private static final String TAG_RESPONSE_CODE = "ResponseCode";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	private static String doLogin(final String url, final String user, final String pass) {
		final String u = LoginManager.replaceAmpEntities(url);
		if (u.startsWith(LoginManager.SAFE_PROTOCOL)) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final String h = u.substring(LoginManager.SAFE_PROTOCOL.length(), u.indexOf("/", LoginManager.SAFE_PROTOCOL.length()));
				if (h.endsWith(s)) {
					final String r = HttpUtils.post(u, LoginManager.getPrefixedUserName(h, user), pass);
					if (r != null) {
						return LoginManager.getWisprMessage(r);
					}
				}
			}
		}
		return null;
	}

	private static String getElementText(final String source, final String elementName) {
		final int start = source.indexOf(">", source.indexOf('<' + elementName));
		if (start != -1) {
			final int end = source.indexOf("</" + elementName + ">", start);
			if (end != -1) {
				return source.substring(start + 1, end);
			}
		}
		return "";
	}

	private static int getElementTextAsInt(final String source, final String elementName) {
		return Integer.parseInt(LoginManager.getElementText(source, elementName));
	}

	private static String getPrefixedUserName(final String h, final String username) {
		if ((h.endsWith("portal.fon.com") || h.endsWith("wifi.sfr.fr") || h.equals("www.btopenzone.com")) && !(h.contains("belgacom") || h.contains("telekom"))) {
			return LoginManager.FON_USERNAME_PREFIX + username;
		}
		return username;
	}

	private static String getTestUrlContent() {
		return HttpUtils.get(LoginManager.CONNECTION_TEST_URL, Constants.HTTP_TIMEOUT);
	}

	private static String getWisprMessage(final String source) {
		final String res = LoginManager.getElementText(source, LoginManager.TAG_WISPR);
		if (res.length() != 0) {
			return res.trim().replaceAll(">\\s+<", "><");
		}
		return null;
	}

	private static boolean isBt(final String ssid) {
		return ssid.equals("BTWiFi") || ssid.equals("BTWiFi-with-FON") || ssid.equals("BTOpenzone") || ssid.equals("BTOpenzone-H") || ssid.equals("BTOpenzone-B") || ssid.equals("BTOpenzone-M") || ssid.equals("BTFON");
	}

	private static boolean isDowntownBrooklyn(final String ssid) {
		return ssid.equalsIgnoreCase("DowntownBrooklynWifi_Fon");
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

	private static String replaceAmpEntities(final String s) {
		return s.replace("&amp;", "&");
	}

	static boolean isSupported(final String ssid) {
		return LoginManager.isGenericFon(ssid) || LoginManager.isBt(ssid) || LoginManager.isSfr(ssid) || LoginManager.isProximus(ssid) || LoginManager.isKpn(ssid) || LoginManager.isDt(ssid) || LoginManager.isSt(ssid) || LoginManager.isJt(ssid) || LoginManager.isHt(ssid) || LoginManager.isOte(ssid) || LoginManager.isRomtelecom(ssid) || LoginManager.isTtnet(ssid) || LoginManager.isOtherFon(ssid) || LoginManager.isOi(ssid) || LoginManager.isDowntownBrooklyn(ssid) || LoginManager.isMweb(ssid) || LoginManager.isSoftBank(ssid) || LoginManager.isTelstra(ssid);
	}

	static LoginResult login(final String user, final String password) {
		int rc = Constants.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String rm = null;
		String lu = null;
		if ((user.length() != 0) && (password.length() != 0)) {
			String c = LoginManager.getTestUrlContent();
			if (c != null) {
				if (!c.equals(LoginManager.CONNECTED)) {
					c = LoginManager.getWisprMessage(c);
					if ((c != null) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE) == Constants.WISPR_MESSAGE_TYPE_INITIAL_REDIRECT) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE) == Constants.WISPR_RESPONSE_CODE_NO_ERROR)) {
						c = LoginManager.doLogin(LoginManager.getElementText(c, LoginManager.TAG_LOGIN_URL), user, password);
						if (c != null) {
							final int mt = LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE);
							if ((mt == Constants.WISPR_MESSAGE_TYPE_AUTH_NOTIFICATION) || (mt == Constants.WISPR_MESSAGE_TYPE_RESPONSE_AUTH_POLL)) {
								rc = LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE);
								if (rc == Constants.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED) {
									lu = LoginManager.getElementText(c, LoginManager.TAG_LOGOFF_URL);
								} else if (rc == Constants.WISPR_RESPONSE_CODE_LOGIN_FAILED) {
									rc = LoginManager.getElementTextAsInt(c, LoginManager.TAG_FON_RESPONSE_CODE);
									rm = LoginManager.getElementText(c, LoginManager.TAG_REPLY_MESSAGE);
								}
							}
						}
					} else {
						rc = Constants.CUST_WISPR_NOT_PRESENT;
					}
				} else {
					rc = Constants.CUST_ALREADY_CONNECTED;
				}
			}
		} else {
			rc = Constants.CUST_CREDENTIALS_ERROR;
		}
		return new LoginResult(rc, rm, lu);
	}

}
