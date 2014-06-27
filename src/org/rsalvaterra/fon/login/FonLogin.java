package org.rsalvaterra.fon.login;

import org.rsalvaterra.fon.HttpUtils;
import org.rsalvaterra.fon.ResponseCodes;

final class FonLogin {

	static LoginResult login(final String user, final String password) {
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

}
