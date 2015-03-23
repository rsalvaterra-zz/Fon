package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.util.Log;

public final class URLUtils {

	private static final int TIMEOUT = 5 * 1000;

	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";

	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";

	public static String getUrl(final String url) {
		final URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException e) {
			return null;
		}
		BufferedReader r = null;
		try {
			final URLConnection c = u.openConnection();
			c.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
			c.setConnectTimeout(URLUtils.TIMEOUT);
			c.setReadTimeout(URLUtils.TIMEOUT);
			r = new BufferedReader(new InputStreamReader(c.getInputStream()));
			final StringBuilder b = new StringBuilder();
			for (String l = r.readLine(); l != null; l = r.readLine()) {
				b.append(l);
			}
			return b.toString();
		} catch (final IOException e) {
			Log.e(URLUtils.class.getName(), e.getMessage(), e);
		} finally {
			try {
				if (r != null) {
					r.close();
				}
			} catch (final IOException e) {
				Log.e(URLUtils.class.getName(), e.getMessage(), e);
			}
		}
		return null;
	}

	public static String getUrlByPost(final String url, final String username, final String password) {
		final URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException e) {
			return null;
		}
		BufferedReader r = null;
		BufferedWriter w = null;
		HttpURLConnection c = null;
		String s = null;
		try {
			c = (HttpURLConnection) u.openConnection();
			c.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
			c.setConnectTimeout(URLUtils.TIMEOUT);
			c.setReadTimeout(URLUtils.TIMEOUT);
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			final String parameters = URLEncoder.encode(URLUtils.TAG_WISPR_USERNAME + "=" + username + "&" + URLUtils.TAG_WISPR_PASSWORD + "=" + password, "UTF-8");
			c.setRequestProperty("Content-Length", String.valueOf(parameters.length()));
			w = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
			w.write(parameters);
			r = new BufferedReader(new InputStreamReader(c.getInputStream()));
			final StringBuilder b = new StringBuilder();
			for (String l = r.readLine(); l != null; l = r.readLine()) {
				b.append(l);
			}
			s = b.toString();
		} catch (final IOException e) {
			Log.e(URLUtils.class.getName(), e.getMessage(), e);
		} finally {
			try {
				if (w != null) {
					w.close();
				}
				if (r != null) {
					r.close();
				}
			} catch (final IOException e) {
				Log.e(URLUtils.class.getName(), e.getMessage(), e);
			}
			if (c != null) {
				c.disconnect();
			}
		}
		return s;
	}

}
