package org.rsalvaterra.fon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

final class HttpUtils {

	private static final String ENCODING_GZIP = "gzip";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";
	private static final String USER_AGENT = "User-Agent";
	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String UTF_8 = "UTF-8";

	private static final DefaultHttpClient HTTP_CLIENT = new DefaultHttpClient();
	private static final HttpParams HTTP_PARAMETERS = new BasicHttpParams().setParameter(HttpUtils.USER_AGENT, HttpUtils.USER_AGENT_STRING);

	static {
		HttpUtils.HTTP_CLIENT.setCookieStore(null);
		HttpUtils.HTTP_CLIENT.addRequestInterceptor(new HttpRequestInterceptor() {

			@Override
			public void process(final HttpRequest request, final HttpContext context) {
				if (!request.containsHeader(HttpUtils.HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HttpUtils.HEADER_ACCEPT_ENCODING, HttpUtils.ENCODING_GZIP);
				}
			}
		});
		HttpUtils.HTTP_CLIENT.addResponseInterceptor(new HttpResponseInterceptor() {

			@Override
			public void process(final HttpResponse response, final HttpContext context) {
				// Inflate any responses compressed with gzip
				final Header encoding = response.getEntity().getContentEncoding();
				if (encoding != null) {
					final HeaderElement[] elements = encoding.getElements();
					for (final HeaderElement element : elements) {
						if (element.getName().equalsIgnoreCase(HttpUtils.ENCODING_GZIP)) {
							response.setEntity(new HttpEntityWrapper(response.getEntity()) {

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
		});
	}

	private HttpUtils() {}

	private static String request(final HttpUriRequest hr, final int t) {
		HttpConnectionParams.setConnectionTimeout(HttpUtils.HTTP_PARAMETERS, t);
		HttpConnectionParams.setSoTimeout(HttpUtils.HTTP_PARAMETERS, t);
		HttpUtils.HTTP_CLIENT.setParams(HttpUtils.HTTP_PARAMETERS);
		try {
			return EntityUtils.toString(HttpUtils.HTTP_CLIENT.execute(hr, new BasicHttpContext()).getEntity()).trim();
		} catch (final IOException e) {
			return null;
		}
	}

	static String get(final String url, final int timeout) {
		return HttpUtils.request(new HttpGet(url), timeout);
	}

	static String post(final String url, final String username, final String password) {
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
		return HttpUtils.request(r, Constants.HTTP_TIMEOUT);
	}
}
