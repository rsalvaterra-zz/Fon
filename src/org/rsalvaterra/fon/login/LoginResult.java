package org.rsalvaterra.fon.login;

public final class LoginResult {

	private final int responseCode;
	private final String replyMessage;
	private final String logOffUrl;

	LoginResult(final int responseCode, final String replyMessage, final String logOffUrl) {
		this.responseCode = responseCode;
		this.replyMessage = replyMessage;
		this.logOffUrl = logOffUrl;
	}

	public String getLogOffUrl() {
		return logOffUrl;
	}

	public String getReplyMessage() {
		return replyMessage;
	}

	public int getResponseCode() {
		return responseCode;
	}

}
