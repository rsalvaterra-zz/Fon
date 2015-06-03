package org.rsalvaterra.fon;

final class LoginResult {

	private final int rc;

	private final String rm;

	LoginResult(final int rc, final String rm) {
		this.rc = rc;
		this.rm = new String(rm);

	}

	String getReplyMessage() {
		return rm;
	}

	int getResponseCode() {
		return rc;
	}

}
