package org.rsalvaterra.fon.login;

import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;
import org.rsalvaterra.fon.HttpUtils;
import org.rsalvaterra.fon.ResponseCodes;

final class LivedoorLogin {

	private static final String LIVEDOOR_TARGET_URL = "https://vauth.lw.livedoor.com/fauth/index";

	private static ArrayList<BasicNameValuePair> getLoginParameters(final String user, final String password) {
		final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
		final String sn;
		final String res = HttpUtils.getUrl(LoginManager.CONNECTION_TEST_URL);
		if (res == null) {
			sn = "001";
		} else {
			sn = new String(res.substring(res.indexOf("name=\"sn\" value=\"") + 17, res.indexOf("\"", res.indexOf("name=\"sn\" value=\"") + 17)));
		}
		p.add(new BasicNameValuePair("sn", sn));
		p.add(new BasicNameValuePair("original_url", LoginManager.CONNECTION_TEST_URL));
		p.add(new BasicNameValuePair("name", user + "@fon"));
		p.add(new BasicNameValuePair("password", password));
		// Click coordinates on image button, really not needed
		p.add(new BasicNameValuePair("x", "66"));
		p.add(new BasicNameValuePair("y", "15"));
		return p;
	}

	static LoginResult login(final String user, final String password) {
		int res = ResponseCodes.WISPR_RESPONSE_CODE_ACCESS_GATEWAY_INTERNAL_ERROR;
		if (!LoginManager.isConnected()) {
			HttpUtils.getUrlByPost(LivedoorLogin.LIVEDOOR_TARGET_URL, LivedoorLogin.getLoginParameters(user, password));
			if (LoginManager.isConnected()) {
				res = ResponseCodes.WISPR_RESPONSE_CODE_LOGIN_SUCCEEDED;
			}
		} else {
			res = ResponseCodes.CUST_ALREADY_CONNECTED;
		}
		return new LoginResult(res, null, null);
	}

}
