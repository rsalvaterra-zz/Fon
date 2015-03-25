package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public final class URLUtils {

	private static final int CONNECT_TIMEOUT = 5 * 1000;
	private static final int SOCKET_TIMEOUT = 5 * 1000;

	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";

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
			uc.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
			uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			final int rc = uc.getResponseCode();
			if ((rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_SEE_OTHER)) {
				uc = (HttpURLConnection) new URL(uc.getHeaderField("Location")).openConnection();
				uc.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
				uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
				uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			}
		} catch (final IOException e) {
			return null;
		}
		return URLUtils.readConnectionStream(uc);
	}

	public static String post(final String url, final String username, final String password) {
		final HttpsURLConnection uc;
		final byte[] pc;
		try {
			uc = (HttpsURLConnection) new URL(url).openConnection();
			uc.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
			uc.setConnectTimeout(URLUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(URLUtils.SOCKET_TIMEOUT);
			uc.setDoOutput(true);
			pc = URLEncoder.encode("UserName=" + username + "&Password=" + password, "UTF-8").getBytes();
			uc.setRequestProperty("Content-Length", Integer.toString(pc.length));
			uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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
		return URLUtils.readConnectionStream(uc);
	}
}
