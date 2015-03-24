package org.rsalvaterra.fon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.util.Log;

public final class URLUtils {

	private static final int TIMEOUT = 5 * 1000;

	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";

	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";

	public static String getUrl(final String url) {
		URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException e) {
			return null;
		}
		HttpURLConnection uc;
		try {
			uc = (HttpURLConnection) u.openConnection();
		} catch (final IOException e) {
			return null;
		}
		uc.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
		uc.setConnectTimeout(URLUtils.TIMEOUT);
		uc.setReadTimeout(URLUtils.TIMEOUT);
		int rc;
		try {
			rc = uc.getResponseCode();
		} catch (final IOException e) {
			return null;
		}
		if ((rc != HttpURLConnection.HTTP_OK) && ((rc == HttpURLConnection.HTTP_MOVED_TEMP) || (rc == HttpURLConnection.HTTP_MOVED_PERM) || (rc == HttpURLConnection.HTTP_SEE_OTHER))) {
			try {
				u = new URL(uc.getHeaderField("Location"));
			} catch (final MalformedURLException e) {
				return null;
			}
			try {
				uc = (HttpURLConnection) u.openConnection();
			} catch (final IOException e) {
				return null;
			}
			uc.setRequestProperty("User-Agent", URLUtils.USER_AGENT_STRING);
			uc.setConnectTimeout(URLUtils.TIMEOUT);
			uc.setReadTimeout(URLUtils.TIMEOUT);
		}
		final InputStream is;
		try {
			is = uc.getInputStream();
		} catch (final IOException e) {
			return null;
		}
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();
		String s;
		try {
			for (String l = br.readLine(); l != null; l = br.readLine()) {
				sb.append(l);
			}
			s = sb.toString();
		} catch (final IOException e) {
			s = null;
		} finally {
			try {
				br.close();
			} catch (final IOException e) {
				// Nothing can be done.
			}
		}
		return s;
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
