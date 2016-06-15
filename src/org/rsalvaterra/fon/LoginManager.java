package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

final class LoginManager {

	private static final int MAX_REDIRECTS = 5;
	private static final int HTTP_TIMEOUT = 30 * 1000;

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String CONTENT_TYPE_STRING = "application/x-www-form-urlencoded";
	private static final String FON_WISPR_PREFIX = "FON_WISPR/";
	private static final String FON_ROAM_PREFIX = "FON_ROAM/";
	private static final String LOCATION = "Location";
	private static final String SAFE_PROTOCOL = "https://";
	private static final String TAG_FON_RESPONSE_CODE = "FONResponseCode";
	private static final String TAG_LOGIN_URL = "LoginURL";
	private static final String TAG_MESSAGE_TYPE = "MessageType";
	private static final String TAG_REPLY_MESSAGE = "ReplyMessage";
	private static final String TAG_RESPONSE_CODE = "ResponseCode";
	private static final String TAG_WISPR = "WISPAccessGatewayParam";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String USER_NAME = "UserName=";
	private static final String UTF_8 = "UTF-8";
	private static final String PASSWORD = "&Password=";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".wifi.sfr.fr", ".hotspotsvankpn.com", ".portal.vodafone-wifi.com" };

	private static HttpURLConnection buildConnection(final String url) throws IOException {
		final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty(LoginManager.USER_AGENT, LoginManager.USER_AGENT_STRING);
		conn.setConnectTimeout(LoginManager.HTTP_TIMEOUT);
		conn.setReadTimeout(LoginManager.HTTP_TIMEOUT);
		return conn;
	}

	private static String getElementText(final String s, final String e) {
		final int start = s.indexOf(">", s.indexOf(e));
		if (start != -1) {
			final int end = s.indexOf("</" + e, start);
			if (end != -1) {
				return s.substring(start + 1, end);
			}
		}
		return null;
	}

	private static int getElementTextAsInt(final String s, final String e) {
		return Integer.parseInt(LoginManager.getElementText(s, e));
	}

	private static String getPrefixedUserName(final String host, final String user) {
		if (!(host.contains("belgacom") || host.contains("telekom"))) {
			if ((host.contains("portal.fon.com") || host.contains("wifi.sfr.fr") || host.equals("www.btopenzone.com"))) {
				return LoginManager.FON_WISPR_PREFIX + user;
			} else if (host.contains("portal.vodafone-wifi.com")) {
				return LoginManager.FON_WISPR_PREFIX + LoginManager.FON_ROAM_PREFIX + user;
			}
		}
		return user;
	}

	private static String getTestUrl() {
		try {
			String target = LoginManager.CONNECTION_TEST_URL;
			int redirects = 0;
			do {
				final HttpURLConnection conn = LoginManager.buildConnection(target);
				if (!LoginManager.isRedirect(conn.getResponseCode())) {
					return LoginManager.readStream(conn);
				}
				target = conn.getHeaderField(LoginManager.LOCATION);
				conn.disconnect();
			} while (++redirects != LoginManager.MAX_REDIRECTS);
		} catch (final IOException e) {
			// Nothing to do.
		}
		return null;
	}

	private static boolean isRedirect(final int rc) {
		return (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_SEE_OTHER);
	}

	private static String login(final String url, final String user, final String pass) {
		if (url.startsWith(LoginManager.SAFE_PROTOCOL)) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final int b = LoginManager.SAFE_PROTOCOL.length();
				final int e = url.indexOf("/", b);
				if (e > b) {
					final String h = url.substring(b, e);
					if (h.endsWith(s)) {
						try {
							HttpURLConnection conn = LoginManager.buildConnection(LoginManager.replaceAmpEntities(url));
							conn.setDoOutput(true);
							final byte[] pc = (LoginManager.USER_NAME + URLEncoder.encode(LoginManager.getPrefixedUserName(h, user), LoginManager.UTF_8) + LoginManager.PASSWORD + URLEncoder.encode(pass, LoginManager.UTF_8)).getBytes();
							conn.setRequestProperty(LoginManager.CONTENT_LENGTH, Integer.toString(pc.length));
							conn.setRequestProperty(LoginManager.CONTENT_TYPE, LoginManager.CONTENT_TYPE_STRING);
							final OutputStream os = conn.getOutputStream();
							os.write(pc);
							os.close();
							int redirects = 0;
							do {
								if (!LoginManager.isRedirect(conn.getResponseCode())) {
									return LoginManager.readStream(conn);
								}
								final String target = conn.getHeaderField(LoginManager.LOCATION);
								conn.disconnect();
								conn = LoginManager.buildConnection(target);
							} while (++redirects != LoginManager.MAX_REDIRECTS);
							conn.disconnect();
						} catch (final IOException e1) {
							// Nothing to do.
						}
					}
				}
			}
		}
		return null;
	}

	private static String readStream(final HttpURLConnection conn) throws IOException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String s;
		final StringBuilder sb = new StringBuilder();
		while ((s = br.readLine()) != null) {
			sb.append(s);
		}
		br.close();
		conn.disconnect();
		return sb.toString();
	}

	private static String replaceAmpEntities(final String s) {
		return s.replace("&amp;", "&");
	}

	static LoginResult login(final String user, final String pass) {
		int rc = Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR;
		String rm = "";
		if ((user.length() != 0) && (pass.length() != 0)) {
			String c = LoginManager.getTestUrl();
			if (c != null) {
				if (!c.equals(LoginManager.CONNECTED)) {
					c = LoginManager.getElementText(c, LoginManager.TAG_WISPR);
					if ((c != null) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE) == Constants.WMT_INITIAL_REDIRECT) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE) == Constants.WRC_NO_ERROR)) {
						c = LoginManager.login(LoginManager.getElementText(c, LoginManager.TAG_LOGIN_URL), user, pass);
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
					rc = Constants.CRC_ALREADY_AUTHORISED;
				}
			}
		} else {
			rc = Constants.CRC_CREDENTIALS_ERROR;
		}
		return new LoginResult(rc, rm);
	}

}
