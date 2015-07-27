package org.rsalvaterra.fon;

interface Constants {

	static final int CRC_ALREADY_AUTHORISED = 1000;
	static final int CRC_CREDENTIALS_ERROR = 1001;
	static final int CRC_WISPR_NOT_PRESENT = 1002;

	static final int FRC_GENERAL_ERROR = 900;
	static final int FRC_NOT_ENOUGH_CREDIT = 901;
	static final int FRC_BAD_CREDENTIALS = 902;
	static final int FRC_BLACKLISTED = 903;
	static final int FRC_USER_LIMIT_EXCEEDED = 905;
	static final int FRC_HOTSPOT_LIMIT_EXCEEDED = 906;
	static final int FRC_NOT_AUTHORIZED = 907;
	static final int FRC_PARTNER_ERROR = 908;
	static final int FRC_INTERNAL_ERROR = 909;
	static final int FRC_UNKNOWN_ERROR = 910;
	static final int FRC_INVALID_TEMPORARY_CREDENTIAL = 911;
	static final int FRC_AUTHORIZATION_CONNECTION_ERROR = 912;
	static final int FRC_BANNED_REALM = 913;
	static final int FRC_BANNED_USER = 914;
	static final int FRC_FLOOD_LIMIT_EXCEEDED = 915;
	static final int FRC_DATACAP_EXCEEDED = 916;
	static final int FRC_USER_SUSPENDED = 917;
	static final int FRC_DEVICE_NOT_AUTHORIZED = 918;
	static final int FRC_NOT_CREDENTIALS = 920;
	static final int FRC_INSERT_PROMO_CODE = 922;
	static final int FRC_VIEW_PROMO_CODE = 923;
	static final int FRC_NOT_PASS_PURCHASE_AVAILABLE = 924;
	static final int FRC_NOT_ENOUGH_CREDIT_2 = 925;
	static final int FRC_INTERNAL_ERROR_2 = 926;
	static final int FRC_NOT_AUTHORIZED_OWN_HOTSPOT = 927;

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

	static final String DEFAULT_PERIOD = "300";
	static final String DEFAULT_MINIMUM_RSSI = "-80";

	static final String APP_ID = "org.rsalvaterra.fon";

	static final String ACT_CANCEL = "0";
	static final String ACT_CHECK = "1";
	static final String ACT_CONNECT = "2";
	static final String ACT_LOGIN = "3";
	static final String ACT_SCAN = "4";

}
