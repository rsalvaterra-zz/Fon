package org.rsalvaterra.fon.login;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

final class FonResponseHandler extends DefaultHandler {

	private static final String TAG_REPLY_MESSAGE = "ReplyMessage";
	private static final String TAG_LOGOFF_URL = "LogoffURL";
	private static final String TAG_FON_RESPONSE_CODE = "FONResponseCode";

	private final StringBuilder messageType = new StringBuilder();
	private final StringBuilder responseCode = new StringBuilder();
	private final StringBuilder logoffUrl = new StringBuilder();
	private final StringBuilder replyMessage = new StringBuilder();
	private final StringBuilder fonResponseCode = new StringBuilder();

	private String currentTag;

	int getFonResponseCode() {
		return Integer.parseInt(fonResponseCode.toString().trim());
	}

	String getLogoffURL() {
		return logoffUrl.toString().trim();
	}

	int getMessageType() {
		return Integer.parseInt(messageType.toString().trim());
	}

	String getReplyMessage() {
		return replyMessage.toString().trim();
	}

	int getResponseCode() {
		return Integer.parseInt(responseCode.toString().trim());
	}

	@Override
	public void characters(final char ch[], final int start, final int length) {
		if (currentTag.equals(FonInfoHandler.TAG_MESSAGE_TYPE)) {
			messageType.append(ch, start, start + length);
		} else if (currentTag.equals(FonInfoHandler.TAG_RESPONSE_CODE)) {
			responseCode.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_LOGOFF_URL)) {
			logoffUrl.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_REPLY_MESSAGE)) {
			replyMessage.append(ch, start, start + length);
		} else if (currentTag.equals(FonResponseHandler.TAG_FON_RESPONSE_CODE)) {
			fonResponseCode.append(ch, start, start + length);
		}
	}

	@Override
	public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		currentTag = name;
	}
}
