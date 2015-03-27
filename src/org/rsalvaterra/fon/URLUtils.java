package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class URLUtils {

	private static final int CONNECT_TIMEOUT = 5 * 1000;
	private static final int SOCKET_TIMEOUT = 5 * 1000;

	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String CONTENT_TYPE_STRING = "application/x-www-form-urlencoded";
	private static final String LOCATION = "Location";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String USER_NAME = "UserName=";
	private static final String UTF_8 = "UTF-8";
	private static final String PASSWORD = "&Password=";

	private static String readConnectionStream(final HttpURLConnection uc) {
		final BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		} catch (final IOException e) {
			return null;
		}
		String s;
		try {
			final StringBuilder sb = new StringBuilder();
			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			s = sb.toString();
		} catch (final IOException e) {
			s = null;
		} finally {
			try {
				br.close();
			} catch (final IOException e) {
				// Nothing can be done.
			} finally {
				uc.disconnect();
			}
		}
		return s;
	}

	public static String get(final String url) {
		HttpURLConnection uc;
		try {
			uc = (HttpURLConnection) new URL(url).openConnection();
			uc.setRequestProperty(URLUtils.USER_AGENT, URLUtils.USER_AGENT_STRING);
			uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			final int rc = uc.getResponseCode();
			if ((rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_SEE_OTHER)) {
				uc = (HttpURLConnection) new URL(uc.getHeaderField(URLUtils.LOCATION)).openConnection();
				uc.setRequestProperty(URLUtils.USER_AGENT, URLUtils.USER_AGENT_STRING);
				uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
				uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			}
		} catch (final IOException e) {
			return null;
		}
		return URLUtils.readConnectionStream(uc);
	}

	public static String post(final String url, final String username, final String password) {
		HttpURLConnection uc;
		final byte[] pc;
		try {
			uc = (HttpURLConnection) new URL(url).openConnection();
			uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			uc.setDoOutput(true);
			uc.setRequestProperty(URLUtils.USER_AGENT, URLUtils.USER_AGENT_STRING);
			pc = (URLUtils.USER_NAME + URLEncoder.encode(username, URLUtils.UTF_8) + URLUtils.PASSWORD + URLEncoder.encode(password, URLUtils.UTF_8)).getBytes();
			uc.setRequestProperty(URLUtils.CONTENT_LENGTH, Integer.toString(pc.length));
			uc.setRequestProperty(URLUtils.CONTENT_TYPE, URLUtils.CONTENT_TYPE_STRING);
		} catch (final IOException e) {
			return null;
		}
		final OutputStream os;
		try {
			os = uc.getOutputStream();
		} catch (final IOException e) {
			return null;
		}
		boolean error = false;
		try {
			os.write(pc);
		} catch (final IOException e) {
			error = true;
		} finally {
			try {
				os.close();
			} catch (final IOException e) {
				// Nothing can be done.
			}
			if (error) {
				return null;
			}
		}
		try {
			final int rc = uc.getResponseCode();
			if ((rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_SEE_OTHER)) {
				uc = (HttpURLConnection) new URL(uc.getHeaderField(URLUtils.LOCATION)).openConnection();
				uc.setRequestProperty(URLUtils.USER_AGENT, URLUtils.USER_AGENT_STRING);
				uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
				uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			}
		} catch (final IOException e) {
			return null;
		}
		return URLUtils.readConnectionStream(uc);
	}
}
