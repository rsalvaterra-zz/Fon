package org.rsalvaterra.fon;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

final class LoginManager extends DefaultHttpClient {

	private static final int HTTP_TIMEOUT = 30 * 1000;

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
	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String UTF_8 = "UTF-8";

	private static final String[] VALID_SUFFIX = { ".fon.com", ".btopenzone.com", ".btfon.com", ".wifi.sfr.fr", ".hotspotsvankpn.com" };

	{
		setCookieStore(null);
		final HttpParams p = new BasicHttpParams().setParameter(LoginManager.USER_AGENT, LoginManager.USER_AGENT_STRING).setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		HttpConnectionParams.setConnectionTimeout(p, LoginManager.HTTP_TIMEOUT);
		HttpConnectionParams.setSoTimeout(p, LoginManager.HTTP_TIMEOUT);
		setParams(p);
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

	private static String replaceAmpEntities(final String s) {
		return s.replace("&amp;", "&");
	}

	private String getTestUrl() {
		return request(new HttpGet(LoginManager.CONNECTION_TEST_URL));
	}

	private String postCredentials(final String url, final String user, final String pass) {
		if (url.startsWith(LoginManager.SAFE_PROTOCOL)) {
			for (final String s : LoginManager.VALID_SUFFIX) {
				final String h = url.substring(LoginManager.SAFE_PROTOCOL.length(), url.indexOf("/", LoginManager.SAFE_PROTOCOL.length()));
				if (h.endsWith(s)) {
					try {
						final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
						p.add(new BasicNameValuePair(LoginManager.TAG_WISPR_USERNAME, LoginManager.getPrefixedUserName(h, user)));
						p.add(new BasicNameValuePair(LoginManager.TAG_WISPR_PASSWORD, pass));
						final HttpPost r = new HttpPost(LoginManager.replaceAmpEntities(url));
						r.setEntity(new UrlEncodedFormEntity(p, LoginManager.UTF_8));
						return request(r);
					} catch (final IOException e) {
						break;
					}
				}
			}
		}
		return null;
	}

	private String request(final HttpUriRequest r) {
		try {
			return EntityUtils.toString(execute(r, new BasicHttpContext()).getEntity()).trim();
		} catch (final IOException e) {
			return null;
		}
	}

	LoginResult login(final String user, final String pass) {
		int rc = Constants.WRC_ACCESS_GATEWAY_INTERNAL_ERROR;
		String rm = "";
		if ((user.length() != 0) && (pass.length() != 0)) {
			String c = getTestUrl();
			if (c != null) {
				if (!c.equals(LoginManager.CONNECTED)) {
					c = LoginManager.getElementText(c, LoginManager.TAG_WISPR);
					if ((c != null) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_MESSAGE_TYPE) == Constants.WMT_INITIAL_REDIRECT) && (LoginManager.getElementTextAsInt(c, LoginManager.TAG_RESPONSE_CODE) == Constants.WRC_NO_ERROR)) {
						c = postCredentials(LoginManager.getElementText(c, LoginManager.TAG_LOGIN_URL), user, pass);
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
