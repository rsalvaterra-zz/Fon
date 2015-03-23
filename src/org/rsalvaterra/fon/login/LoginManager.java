package org.rsalvaterra.fon.login;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.rsalvaterra.fon.HttpUtils;
import org.rsalvaterra.fon.ResponseCodes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.util.Xml;

public final class LoginManager {

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String DEFAULT_LOGOFF_URL = "http://192.168.3.1:80/logoff";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".neuf.fr", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	private static LoginResult fonLogin(final String user, final String password) {
		int responseCode = ResponseCodes.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String replyMessage = null;
		String logoffUrl = null;
		String content = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		if (content != null) {
			if (!content.equals(LoginManager.CONNECTED)) {
				content = LoginManager.getFonXML(content);
				if (content != null) {
					final FonInfoHandler wih = new FonInfoHandler();
					if (LoginManager.parseFonXML(content, wih) && (wih.getMessageType() == ResponseCodes.WISPR_MESSAGE_TYPE_INITIAL_REDIRECT) && (wih.getResponseCode() == ResponseCodes.WISPR_RESPONSE_CODE_NO_ERROR)) {
						content = LoginManager.getFonXMLByPost(wih.getLoginURL(), user, password);
						if (content != null) {
							final FonResponseHandler wrh = new FonResponseHandler();
							if (LoginManager.parseFonXML(content, wrh)) {
								responseCode = wrh.getResponseCode();
								if (responseCode == ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED) {
									logoffUrl = wrh.getLogoffURL();
								} else if (responseCode == ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_FAILED) {
									responseCode = wrh.getFonResponseCode();
									replyMessage = wrh.getReplyMessage();
								}
							}
						} else if (LoginManager.isConnected()) {
							responseCode = ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED;
							logoffUrl = LoginManager.DEFAULT_LOGOFF_URL;
						}
					}
				} else {
					responseCode = ResponseCodes.CUST_WISPR_NOT_PRESENT;
				}
			} else {
				responseCode = ResponseCodes.CUST_ALREADY_CONNECTED;
				logoffUrl = LoginManager.DEFAULT_LOGOFF_URL;
			}
		}
		return new LoginResult(responseCode, replyMessage, logoffUrl);
	}

	private static String getFonXML(final String source) {
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

	private static String getFonXMLByPost(final String url, final String user, final String password) {
		final URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException e) {
			return null;
		}
		if (u.getProtocol().equals("https")) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				if (u.getHost().toLowerCase(Locale.US).endsWith(s)) {
					final String username;
					if (LoginManager.isFonWISPrURL(u)) {
						username = LoginManager.FON_USERNAME_PREFIX + user;
					} else {
						username = user;
					}
					final String r = HttpUtils.getUrlByPost(url, username, password);
					if (r != null) {
						return LoginManager.getFonXML(r);
					}
				}
			}
		}
		return null;
	}

	private static String getSFRFonURL(final String source) {
		final int start = source.indexOf("SFRLoginURL_JIL");
		final int end = source.indexOf("-->", start);
		if ((start == -1) || (end == -1)) {
			return null;
		}
		final String url = source.substring(start, end);
		return new String(url.substring(url.indexOf("https")).replace("&amp;", "&").replace("notyet", "smartclient"));

	}

	private static boolean isBT(final String ssid) {
		return ssid.equals("BTWiFi") || ssid.equals("BTWiFi-with-FON") || ssid.equals("BTOpenzone") || ssid.equals("BTOpenzone-H") || ssid.equals("BTOpenzone-B") || ssid.equals("BTOpenzone-M") || ssid.equals("BTFON");
	}

	private static boolean isConnected() {
		final String response = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		return (response != null) && response.equals(LoginManager.CONNECTED);
	}

	private static boolean isDowntownBrooklyn(final String ssid) {
		return ssid.equalsIgnoreCase("DowntownBrooklynWifi_Fon");
	}

	private static boolean isDT(final String ssid) {
		return ssid.equals("Telekom_FON");
	}

	private static boolean isFon(final String ssid) {
		return LoginManager.isGenericFon(ssid) || LoginManager.isBT(ssid) || LoginManager.isProximus(ssid) || LoginManager.isKPN(ssid) || LoginManager.isDT(ssid) || LoginManager.isST(ssid) || LoginManager.isJT(ssid) || LoginManager.isHT(ssid) || LoginManager.isOTE(ssid) || LoginManager.isRomtelecom(ssid) || LoginManager.isTTNET(ssid) || LoginManager.isOtherFon(ssid) || LoginManager.isOi(ssid) || LoginManager.isDowntownBrooklyn(ssid) || LoginManager.isMWEB(ssid) || LoginManager.isSoftBank(ssid) || LoginManager.isTelstra(ssid);
	}

	private static boolean isFonWISPrURL(final URL url) {
		return (url.getHost().contains("portal.fon.com") || url.getHost().contentEquals("www.btopenzone.com") || url.getHost().contains("wifi.sfr.fr")) && !(url.getHost().contains("belgacom") || url.getHost().contains("telekom"));
	}

	private static boolean isGenericFon(final String ssid) {
		return ssid.startsWith("FON_");
	}

	private static boolean isHT(final String ssid) {
		return ssid.equals("HotSpot Fon");
	}

	private static boolean isJT(final String ssid) {
		return ssid.equalsIgnoreCase("JT Fon");
	}

	private static boolean isKPN(final String ssid) {
		return ssid.equals("KPN Fon");
	}

	private static boolean isMWEB(final String ssid) {
		return ssid.equals("@MWEB FON");
	}

	private static boolean isOi(final String ssid) {
		return ssid.equals("Oi WiFi Fon") || ssid.equals("OI_WIFI_FON");
	}

	private static boolean isOTE(final String ssid) {
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

	private static boolean isSFR(final String ssid) {
		return ssid.equals("SFR WiFi FON");
	}

	private static boolean isSoftBank(final String ssid) {
		return ssid.equalsIgnoreCase("NOC_SOFTBANK") || ssid.equals("FON");
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

	private static boolean parseFonXML(final String xml, final ContentHandler handler) {
		try {
			Xml.parse(xml, handler);
		} catch (final SAXException e) {
			return false;
		}
		return true;
	}

	private static LoginResult sfrLogin(final String user, final String password) {
		int responseCode = ResponseCodes.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String logoffUrl = null;
		String content = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		if (content != null) {
			if (!content.equals(LoginManager.CONNECTED)) {
				content = LoginManager.getSFRFonURL(content);
				if (content != null) {
					content = LoginManager.getFonXMLByPost(content, user, password);
					if (content != null) {
						FonResponseHandler wrh = new FonResponseHandler();
						if (LoginManager.parseFonXML(content, wrh)) {
							if (wrh.getResponseCode() == ResponseCodes.WISPR_RESPONSE_CODE_AUTH_PENDING) {
								content = HttpUtils.getUrl(wrh.getLoginResultsURL());
								if (content != null) {
									wrh = new FonResponseHandler();
									if (LoginManager.parseFonXML(content, wrh)) {
										responseCode = wrh.getResponseCode();
										logoffUrl = wrh.getLogoffURL();
									}
								}
							} else {
								responseCode = wrh.getResponseCode();
								logoffUrl = wrh.getLogoffURL();
							}
						}
					} else if (LoginManager.isConnected()) {
						responseCode = ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED;
						logoffUrl = LoginManager.DEFAULT_LOGOFF_URL;
					}
				} else {
					responseCode = ResponseCodes.CUST_WISPR_NOT_PRESENT;
				}
			} else {
				responseCode = ResponseCodes.CUST_ALREADY_CONNECTED;
				logoffUrl = LoginManager.DEFAULT_LOGOFF_URL;
			}
		}
		return new LoginResult(responseCode, null, logoffUrl);
	}

	public static boolean isSupported(final String ssid) {
		return LoginManager.isFon(ssid) || LoginManager.isSFR(ssid);
	}

	public static LoginResult login(final String ssid, final String user, final String password) {
		final LoginResult r;
		if ((user.length() == 0) || (password.length() == 0)) {
			r = new LoginResult(ResponseCodes.CUST_CREDENTIALS_ERROR, null, null);
		} else if (LoginManager.isSFR(ssid)) {
			r = LoginManager.sfrLogin(user, password);
		} else {
			r = LoginManager.fonLogin(user, password);
		}
		return r;
	}

}
