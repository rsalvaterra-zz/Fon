package org.rsalvaterra.fon.login;

import org.rsalvaterra.fon.HttpUtils;
import org.rsalvaterra.fon.ResponseCodes;

final class SFRLogin {

	private static String getSFRFonURL(final String source) {
		final int start = source.indexOf("SFRLoginURL_JIL");
		final int end = source.indexOf("-->", start);
		if ((start == -1) || (end == -1)) {
			return null;
		}
		final String url = source.substring(start, end);
		return new String(url.substring(url.indexOf("https")).replace("&amp;", "&").replace("notyet", "smartclient"));

	}

	static LoginResult login(final String user, final String password) {
		int responseCode = ResponseCodes.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		String logoffUrl = null;
		String content = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		if (content != null) {
			if (!content.equals(LoginManager.CONNECTED)) {
				content = SFRLogin.getSFRFonURL(content);
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

}
