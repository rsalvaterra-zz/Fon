package org.rsalvaterra.fon;

public final class LoginResult {

	private final int responseCode;

	private final String replyMessage;
	private final String logOffUrl;

	LoginResult(final int responseCode, final String replyMessage, final String logOffUrl) {
		this.responseCode = responseCode;
		this.replyMessage = replyMessage;
		this.logOffUrl = logOffUrl;
	}

	String getLogOffUrl() {
		return logOffUrl;
	}

	String getReplyMessage() {
		return replyMessage;
	}

	int getResponseCode() {
		return responseCode;
	}

}
