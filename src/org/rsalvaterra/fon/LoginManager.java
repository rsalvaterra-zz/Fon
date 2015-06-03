package org.rsalvaterra.fon;

final class LoginManager {

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
	private static final String SAFE_PROTOCOL = "https://";
	private static final String TAG_FON_RESPONSE_CODE = "FONResponseCode";
	private static final String TAG_LOGIN_URL = "LoginURL";
	private static final String TAG_MESSAGE_TYPE = "MessageType";
	private static final String TAG_REPLY_MESSAGE = "ReplyMessage";
	private static final String TAG_RESPONSE_CODE = "ResponseCode";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	private final HttpClient httpClient = new HttpClient();

	private static String getElementText(final String source, final String elementName) {
		final int start = source.indexOf(">", source.indexOf(elementName));
		if (start != -1) {
			final int end = source.indexOf("</" + elementName, start);
			if (end != -1) {
				return source.substring(start + 1, end);
			}
		}
		return null;
	}

	private static int getElementTextAsInt(final String source, final String elementName) {
		return Integer.parseInt(LoginManager.getElementText(source, elementName));
	}

	private static String getPrefixedUserName(final String h, final String username) {
		if ((h.contains("portal.fon.com") || h.contains("wifi.sfr.fr") || h.equals("www.btopenzone.com")) && !(h.contains("belgacom") || h.contains("telekom"))) {
			return LoginManager.FON_USERNAME_PREFIX + username;
		}
		return username;
	}

	private static String replaceAmpEntities(final String s) {
		return s.replace("&amp;", "&");
	}

	private String postCredentials(final String url, final String user, final String pass) {
		if (url.startsWith(LoginManager.SAFE_PROTOCOL)) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final String h = url.substring(LoginManager.SAFE_PROTOCOL.length(), url.indexOf("/", LoginManager.SAFE_PROTOCOL.length()));
				if (h.endsWith(s)) {
					return httpClient.post(LoginManager.replaceAmpEntities(url), LoginManager.getPrefixedUserName(h, user), pass);
				}
			}
		}
		return null;
	}

	LoginResult login(final String user, final String password) {
		int rc = Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR;
		String rm = "";
		if ((user.length() != 0) && (password.length() != 0)) {
			String c = httpClient.get(LoginManager.CONNECTION_TEST_URL);
			if (c != null) {
				if (!c.equals(LoginManager.CONNECTED)) {
					c = LoginManager.getElementText(c, LoginManager.TAG_WISPR);
					if ((c != null) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE) == Constants.WMT_INITIAL_REDIRECT) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE) == Constants.WRC_NO_ERROR)) {
						c = postCredentials(LoginManager.getElementText(c, LoginManager.TAG_LOGIN_URL), user, password);
						if (c != null) {
							c = LoginManager.getElementText(c, LoginManager.TAG_WISPR);
							if (c != null) {
								final int mt = LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE);
								if ((mt == Constants.WMT_AUTH_NOTIFICATION) || (mt == Constants.WMT_RESPONSE_AUTH_POLL)) {
									rc = LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE);
									if ((rc == Constants.WRC_LOGIN_FAILED) || (rc == Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR)) {
										rc = LoginManager.getElementTextAsInt(c, LoginManager.TAG_FON_RESPONSE_CODE);
										rm = LoginManager.getElementText(c, LoginManager.TAG_REPLY_MESSAGE);
									}
								}
							}
						}
					} else {
						rc = Constants.CRC_WISPR_NOT_PRESENT;
					}
				} else {
					rc = Constants.CRC_ALREADY_CONNECTED;
				}
			}
		} else {
			rc = Constants.CRC_CREDENTIALS_ERROR;
		}
		return new LoginResult(rc, rm);
	}

}
