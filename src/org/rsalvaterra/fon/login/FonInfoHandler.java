package org.rsalvaterra.fon.login;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

final class FonInfoHandler extends DefaultHandler {

	private static final String TAG_MESSAGE_TYPE = "MessageType";
	private static final String TAG_LOGIN_URL = "LoginURL";

	static final String TAG_RESPONSE_CODE = "ResponseCode";

	private final StringBuilder loginURL = new StringBuilder();
	private final StringBuilder messageType = new StringBuilder();
	private final StringBuilder responseCode = new StringBuilder();

	private String currentTag;

	String getLoginURL() {
		return loginURL.toString().trim();
	}

	int getMessageType() {
		return Integer.parseInt(messageType.toString().trim());
	}

	int getResponseCode() {
		return Integer.parseInt(responseCode.toString().trim());
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (currentTag.equals(FonInfoHandler.TAG_LOGIN_URL)) {
			loginURL.append(ch, start, start + length);
		} else if (currentTag.equals(FonInfoHandler.TAG_MESSAGE_TYPE)) {
			messageType.append(ch, start, start + length);
		} else if (currentTag.equals(FonInfoHandler.TAG_RESPONSE_CODE)) {
			responseCode.append(ch, start, start + length);
		}
	}

	@Override
	public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		currentTag = name;
	}
}
