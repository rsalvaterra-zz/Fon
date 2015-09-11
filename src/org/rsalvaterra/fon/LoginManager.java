package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

final class LoginManager {

	private static final int MAX_RETRIES = 5;
	private static final int HTTP_TIMEOUT = 30 * 1000;

	private static final String CONNECTED = "CONNECTED";
	private static final String CONNECTION_TEST_URL = "http://cm.fon.mobi/android.txt";
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String CONTENT_TYPE_STRING = "application/x-www-form-urlencoded";
	private static final String FON_USERNAME_PREFIX = "FON_WISPR/";
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

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	private static String get(final String url) {
		HttpURLConnection uc;
		try {
			uc = (HttpURLConnection) new URL(url).openConnection();
			LoginManager.setCommonParameters(uc);
			int retries = 0;
			while (LoginManager.isRedirect(uc.getResponseCode()) && (retries != LoginManager.MAX_RETRIES)) {
				uc = (HttpURLConnection) new URL(uc.getHeaderField(LoginManager.LOCATION)).openConnection();
				LoginManager.setCommonParameters(uc);
				++retries;
			}
			if (retries == LoginManager.MAX_RETRIES) {
				uc.disconnect();
				return null;
			}
		} catch (final IOException e) {
			return null;
		}
		return LoginManager.readConnectionStream(uc);
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
		if ((host.contains("portal.fon.com") || host.contains("wifi.sfr.fr") || host.equals("www.btopenzone.com")) && !(host.contains("belgacom") || host.contains("telekom"))) {
			return LoginManager.FON_USERNAME_PREFIX + user;
		}
		return user;
	}

	private static String getTestUrl() {
		return LoginManager.get(LoginManager.CONNECTION_TEST_URL);
	}

	private static boolean isRedirect(final int rc) {
		return (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_SEE_OTHER);
	}

	private static String post(final String url, final String username, final String password) {
		HttpURLConnection uc;
		final byte[] pc;
		try {
			uc = (HttpURLConnection) new URL(url).openConnection();
			LoginManager.setCommonParameters(uc);
			uc.setDoOutput(true);
			pc = (LoginManager.USER_NAME + URLEncoder.encode(username, LoginManager.UTF_8) + LoginManager.PASSWORD + URLEncoder.encode(password, LoginManager.UTF_8)).getBytes();
			uc.setRequestProperty(LoginManager.CONTENT_LENGTH, Integer.toString(pc.length));
			uc.setRequestProperty(LoginManager.CONTENT_TYPE, LoginManager.CONTENT_TYPE_STRING);
		} catch (final IOException e) {
			return null;
		}
		final OutputStream os;
		try {
			os = uc.getOutputStream();
		} catch (final IOException e) {
			return null;
		}
		boolean error = false;
		try {
			os.write(pc);
		} catch (final IOException e) {
			error = true;
		} finally {
			try {
				os.close();
			} catch (final IOException e) {
				// Nothing can be done.
			}
			if (error) {
				return null;
			}
		}
		try {
			int retries = 0;
			while (LoginManager.isRedirect(uc.getResponseCode()) && (retries != LoginManager.MAX_RETRIES)) {
				uc = (HttpURLConnection) new URL(uc.getHeaderField(LoginManager.LOCATION)).openConnection();
				LoginManager.setCommonParameters(uc);
				++retries;
			}
			if (retries == LoginManager.MAX_RETRIES) {
				uc.disconnect();
				return null;
			}
		} catch (final IOException e) {
			return null;
		}
		return LoginManager.readConnectionStream(uc);
	}

	private static String postCredentials(final String url, final String user, final String pass) {
		if (url.startsWith(LoginManager.SAFE_PROTOCOL)) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final int b = LoginManager.SAFE_PROTOCOL.length();
				final int e = url.indexOf("/", b);
				if (e > b) {
					final String h = url.substring(b, e);
					if (h.endsWith(s)) {
						return LoginManager.post(LoginManager.replaceAmpEntities(url), LoginManager.getPrefixedUserName(h, user), pass);
					}
				}
			}
		}
		return null;
	}

	private static String readConnectionStream(final HttpURLConnection uc) {
		final BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		} catch (final IOException e) {
			return null;
		}
		String s;
		try {
			final StringBuilder sb = new StringBuilder();
			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			s = sb.toString();
		} catch (final IOException e) {
			s = null;
		} finally {
			try {
				br.close();
			} catch (final IOException e) {
				// Nothing can be done.
			} finally {
				uc.disconnect();
			}
		}
		return s;
	}

	private static String replaceAmpEntities(final String s) {
		return s.replace("&amp;", "&");
	}

	private static void setCommonParameters(final HttpURLConnection uc) {
		uc.setRequestProperty(LoginManager.USER_AGENT, LoginManager.USER_AGENT_STRING);
		uc.setConnectTimeout(LoginManager.HTTP_TIMEOUT);
		uc.setReadTimeout(LoginManager.HTTP_TIMEOUT);
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
						c = LoginManager.postCredentials(LoginManager.getElementText(c, LoginManager.TAG_LOGIN_URL), user, pass);
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
