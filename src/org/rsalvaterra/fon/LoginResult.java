package org.rsalvaterra.fon;

final class LoginResult {

	private final int rc;

	private final String rm;
	private final String lu;

	LoginResult(final int rc, final String rm, final String lu) {
		this.rc = rc;
		this.rm = new String(rm);
		this.lu = new String(lu);
	}

	String getLogOffUrl() {
		return lu;
	}

	String getReplyMessage() {
		return rm;
	}

	int getResponseCode() {
		return rc;
	}

}
