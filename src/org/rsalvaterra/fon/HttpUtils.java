package org.rsalvaterra.fon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public final class HttpUtils {

	private static final int TIMEOUT = 5 * 1000;

	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String UTF_8 = "UTF-8";

	private static final DefaultHttpClient HTTP_CLIENT;

	static {
		final HttpParams p = new BasicHttpParams().setParameter(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);
		HttpConnectionParams.setConnectionTimeout(p, HttpUtils.TIMEOUT);
		HttpConnectionParams.setSoTimeout(p, HttpUtils.TIMEOUT);
		HTTP_CLIENT = new DefaultHttpClient(p);
		HttpUtils.HTTP_CLIENT.setCookieStore(null);
	}

	private static String request(final HttpUriRequest httpreq) {
		try {
			return EntityUtils.toString(HttpUtils.HTTP_CLIENT.execute(httpreq, new BasicHttpContext()).getEntity());
		} catch (final IOException e) {
			return null;
		}
	}

	public static String get(final String url) {
		return HttpUtils.request(new HttpGet(url));
	}

	public static String post(final String url, final String username, final String password) {
		final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
		p.add(new BasicNameValuePair(HttpUtils.TAG_WISPR_USERNAME, username));
		p.add(new BasicNameValuePair(HttpUtils.TAG_WISPR_PASSWORD, password));
		final UrlEncodedFormEntity f;
		try {
			f = new UrlEncodedFormEntity(p, HttpUtils.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return null;
		}
		final HttpPost r = new HttpPost(url);
		r.setEntity(f);
		return HttpUtils.request(r);
	}
}
