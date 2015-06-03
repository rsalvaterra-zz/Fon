package org.rsalvaterra.fon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

final class HttpClient extends DefaultHttpClient implements HttpRequestInterceptor, HttpResponseInterceptor {

	private static final int HTTP_TIMEOUT = 30 * 1000;

	private static final String ENCODING_GZIP = "gzip";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String UTF_8 = "UTF-8";

	{
		setCookieStore(null);
		addRequestInterceptor(this);
		addResponseInterceptor(this);
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

	@Override
	public void process(final HttpRequest r, final HttpContext c) throws HttpException, IOException {
		if (!r.containsHeader(HttpClient.HEADER_ACCEPT_ENCODING)) {
			r.addHeader(HttpClient.HEADER_ACCEPT_ENCODING, HttpClient.ENCODING_GZIP);
		}
	}

	@Override
	public void process(final HttpResponse r, final HttpContext c) throws HttpException, IOException {
		// Inflate any responses compressed with gzip
		final Header encoding = r.getEntity().getContentEncoding();
		if (encoding != null) {
			final HeaderElement[] elements = encoding.getElements();
			for (final HeaderElement element : elements) {
				if (element.getName().equalsIgnoreCase(HttpClient.ENCODING_GZIP)) {
					r.setEntity(new HttpEntityWrapper(r.getEntity()) {

						@Override
						public InputStream getContent() throws IOException {
							return new GZIPInputStream(wrappedEntity.getContent());
						}

						@Override
						public long getContentLength() {
							return -1;
						}
					});
					break;
				}
			}
		}
	}
}
