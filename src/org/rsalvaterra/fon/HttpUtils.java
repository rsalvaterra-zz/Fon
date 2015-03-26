package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class HttpUtils {

	private static final int MAX_XPROTO_REDIRECTS = 3;
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
			uc.setRequestProperty(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);
			uc.setConnectTimeout(HttpUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(HttpUtils.SOCKET_TIMEOUT);
			for (int i = 0; uc.getResponseCode() != HttpURLConnection.HTTP_OK; ++i) {
				final String target = uc.getHeaderField(HttpUtils.LOCATION);
				uc.disconnect();
				if (i == HttpUtils.MAX_XPROTO_REDIRECTS) {
					return null;
				}
				uc = (HttpURLConnection) new URL(target).openConnection();
				uc.setRequestProperty(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);
				uc.setConnectTimeout(HttpUtils.CONNECT_TIMEOUT);
				uc.setReadTimeout(HttpUtils.SOCKET_TIMEOUT);
			}
		} catch (final IOException e) {
			return null;
		}
		return HttpUtils.readConnectionStream(uc);
	}

	public static String post(final String url, final String username, final String password) {
		HttpURLConnection uc;
		final String pc;
		try {
			uc = (HttpURLConnection) new URL(url).openConnection();
			uc.setConnectTimeout(HttpUtils.CONNECT_TIMEOUT);
			uc.setReadTimeout(HttpUtils.SOCKET_TIMEOUT);
			uc.setDoOutput(true);
			uc.setRequestProperty(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);
			pc = (HttpUtils.USER_NAME + URLEncoder.encode(username, HttpUtils.UTF_8) + HttpUtils.PASSWORD + URLEncoder.encode(password, HttpUtils.UTF_8));
			uc.setRequestProperty(HttpUtils.CONTENT_LENGTH, Integer.toString(pc.getBytes().length));
			uc.setRequestProperty(HttpUtils.CONTENT_TYPE, HttpUtils.CONTENT_TYPE_STRING);
		} catch (final IOException e) {
			return null;
		}
		final BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(uc.getOutputStream()));
		} catch (final IOException e) {
			return null;
		}
		boolean error = false;
		try {
			bw.write(pc);
			bw.flush();
		} catch (final IOException e) {
			error = true;
		} finally {
			try {
				bw.close();
			} catch (final IOException e) {
				// Nothing can be done.
			}
			if (error) {
				return null;
			}
		}
		try {
			for (int i = 0; uc.getResponseCode() != HttpURLConnection.HTTP_OK; ++i) {
				final String target = uc.getHeaderField(HttpUtils.LOCATION);
				uc.disconnect();
				if (i == HttpUtils.MAX_XPROTO_REDIRECTS) {
					return null;
				}
				uc = (HttpURLConnection) new URL(target).openConnection();
				uc.setRequestProperty(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);
				uc.setConnectTimeout(HttpUtils.CONNECT_TIMEOUT);
				uc.setReadTimeout(HttpUtils.SOCKET_TIMEOUT);
			}
		} catch (final IOException e) {
			return null;
		}
		return HttpUtils.readConnectionStream(uc);
	}
}
