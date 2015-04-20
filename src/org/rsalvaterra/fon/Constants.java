package org.rsalvaterra.fon;

interface Constants {

	static final int CRC_ALREADY_CONNECTED = 1000;
	static final int CRC_CREDENTIALS_ERROR = 1001;
	static final int CRC_WISPR_NOT_PRESENT = 1002;

	static final int FRC_INVALID_CREDENTIALS_ALT = 900;
	static final int FRC_NOT_ENOUGH_CREDIT = 901;
	static final int FRC_INVALID_CREDENTIALS = 902;
	static final int FRC_USER_IN_BLACK_LIST = 903;
	static final int FRC_SESSION_LIMIT_EXCEEDED = 905;
	static final int FRC_SPOT_LIMIT_EXCEEDED = 906;
	static final int FRC_NOT_AUTHORIZED = 907;
	static final int FRC_CUSTOMIZED_ERROR = 908;
	static final int FRC_INTERNAL_ERROR = 909;
	static final int FRC_UNKNOWN_ERROR = 910;
	static final int FRC_INVALID_TEMPORARY_CREDENTIAL = 911;
	static final int FRC_AUTHORIZATION_CONNECTION_ERROR = 912;

	static final int WMT_INITIAL_REDIRECT = 100;
	static final int WMT_PROXY_NOTIFICATION = 110;
	static final int WMT_AUTH_NOTIFICATION = 120;
	static final int WMT_LOGOFF_NOTIFICATION = 130;
	static final int WMT_RESPONSE_AUTH_POLL = 140;
	static final int WMT_RESPONSE_ABORT_LOGIN = 150;

	static final int WRC_NO_ERROR = 0;
	static final int WRC_LOGIN_SUCCEEDED = 50;
	static final int WRC_LOGIN_FAILED = 100;
	static final int WRC_RADIUS_ERROR = 102;
	static final int WRC_NETWORK_ADMIN_ERROR = 105;
	static final int WRC_LOGOFF_SUCCEEDED = 150;
	static final int WRC_LOGING_ABORTED = 151;
	static final int WRC_PROXY_DETECTION = 200;
	static final int WRC_AUTH_PENDING = 201;
	static final int WRC_ACCESS_GATEWAY_INTERNAL_ERROR = 255;

	static final int HTTP_TIMEOUT = 30 * 1000;

	static final String APP_ID = "org.rsalvaterra.fon";

	static final String KEY_CANCEL_ALL = Constants.APP_ID + ".CANCEL_ALL";
	static final String KEY_CONNECT = Constants.APP_ID + ".CONNECT";
	static final String KEY_FIRST = Constants.APP_ID + ".FIRST";
	static final String KEY_LOGIN = Constants.APP_ID + ".LOGIN";
	static final String KEY_LOGOFF = Constants.APP_ID + ".LOGOFF";
	static final String KEY_LOGOFF_URL = Constants.APP_ID + ".LOGOFF_URL";
	static final String KEY_SCAN = Constants.APP_ID + ".SCAN";

}
