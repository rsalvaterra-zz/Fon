package org.rsalvaterra.fon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

final class HttpClient extends DefaultHttpClient {

	private static final int HTTP_TIMEOUT = 30 * 1000;

	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String UTF_8 = "UTF-8";

	{
		setCookieStore(null);
		final HttpParams p = new BasicHttpParams().setParameter(HttpClient.USER_AGENT, HttpClient.USER_AGENT_STRING).setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		HttpConnectionParams.setConnectionTimeout(p, HttpClient.HTTP_TIMEOUT);
		HttpConnectionParams.setSoTimeout(p, HttpClient.HTTP_TIMEOUT);
		setParams(p);
	}

	private String request(final HttpUriRequest r) {
		try {
			return EntityUtils.toString(execute(r, new BasicHttpContext()).getEntity()).trim();
		} catch (final IOException e) {
			return null;
		}
	}

	String get(final String url) {
		return request(new HttpGet(url));
	}

	String post(final String url, final String username, final String password) {
		final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
		p.add(new BasicNameValuePair(HttpClient.TAG_WISPR_USERNAME, username));
		p.add(new BasicNameValuePair(HttpClient.TAG_WISPR_PASSWORD, password));
		final UrlEncodedFormEntity f;
		try {
			f = new UrlEncodedFormEntity(p, HttpClient.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return null;
		}
		final HttpPost r = new HttpPost(url);
		r.setEntity(f);
		return request(r);
	}

}
