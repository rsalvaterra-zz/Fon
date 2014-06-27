package org.rsalvaterra.fon.login;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

final class FonResponseHandler extends DefaultHandler {

	private static final String TAG_REPLY_MESSAGE = "ReplyMessage";
	private static final String TAG_LOGOFF_URL = "LogoffURL";
	private static final String TAG_LOGIN_RESULTS_URL = "LoginResultsURL";
	private static final String TAG_FON_RESPONSE_CODE = "FONResponseCode";

	private final StringBuilder responseCode = new StringBuilder();
	private final StringBuilder logoffUrL = new StringBuilder();
	private final StringBuilder replyMessage = new StringBuilder();
	private final StringBuilder loginResultsUrl = new StringBuilder();
	private final StringBuilder fonResponseCode = new StringBuilder();

	private String currentTag;

	int getFonResponseCode() {
		return Integer.parseInt(fonResponseCode.toString().trim());
	}

	String getLoginResultsURL() {
		return loginResultsUrl.toString().trim().replace("&amp;", "&");
	}

	String getLogoffURL() {
		return logoffUrL.toString().trim();
	}

	String getReplyMessage() {
		return replyMessage.toString().trim();
	}

	int getResponseCode() {
		return Integer.parseInt(responseCode.toString().trim());
	}

	@Override
	public void characters(final char ch[], final int start, final int length) {
		if (currentTag.equals(LoginManager.TAG_RESPONSE_CODE)) {
			responseCode.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_LOGOFF_URL)) {
			logoffUrL.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_REPLY_MESSAGE)) {
			replyMessage.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_LOGIN_RESULTS_URL)) {
			loginResultsUrl.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_FON_RESPONSE_CODE)) {
			fonResponseCode.append(ch, start, start + length);
		}
	}

	@Override
	public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		currentTag = name;
	}
}
