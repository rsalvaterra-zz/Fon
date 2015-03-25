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
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public final class HttpUtils {

	private static final int CONNECT_TIMEOUT = 5 * 1000;
	private static final int SOCKET_TIMEOUT = 5 * 1000;

	private static final String USER_AGENT_STRING = "FONAccess; wispr; (Linux; U; Android)";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	private static final String TAG_WISPR_PASSWORD = "Password";
	private static final String TAG_WISPR_USERNAME = "UserName";

	private static DefaultHttpClient getHttpClient() {
		final HttpParams p = new BasicHttpParams().setParameter(CoreProtocolPNames.USER_AGENT, HttpUtils.USER_AGENT_STRING);
		HttpConnectionParams.setConnectionTimeout(p, HttpUtils.CONNECT_TIMEOUT);
		HttpConnectionParams.setSoTimeout(p, HttpUtils.SOCKET_TIMEOUT);
		final DefaultHttpClient client = new DefaultHttpClient(p);
		client.addRequestInterceptor(new HttpRequestInterceptor() {

			@Override
			public void process(final HttpRequest request, final HttpContext context) {
				if (!request.containsHeader(HttpUtils.HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HttpUtils.HEADER_ACCEPT_ENCODING, HttpUtils.ENCODING_GZIP);
				}
			}
		});
		client.addResponseInterceptor(new HttpResponseInterceptor() {

			@Override
			public void process(final HttpResponse response, final HttpContext context) {
				// Inflate any responses compressed with gzip
				final Header encoding = response.getEntity().getContentEncoding();
				if (encoding != null) {
					for (final HeaderElement element : encoding.getElements()) {
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
		client.setCookieStore(null);
		return client;
	}

	private static String getUrlCommon(final HttpUriRequest httpreq) {
		try {
			return EntityUtils.toString(HttpUtils.getHttpClient().execute(httpreq, new BasicHttpContext()).getEntity()).trim();
		} catch (final IOException se) {
			return null;
		}
	}

	public static String get(final String url) {
		return HttpUtils.getUrlCommon(new HttpGet(url));
	}

	public static String post(final String url, final String username, final String password) {
		final ArrayList<BasicNameValuePair> p = new ArrayList<BasicNameValuePair>();
		p.add(new BasicNameValuePair(HttpUtils.TAG_WISPR_USERNAME, username));
		p.add(new BasicNameValuePair(HttpUtils.TAG_WISPR_PASSWORD, password));
		final UrlEncodedFormEntity f;
		try {
			f = new UrlEncodedFormEntity(p, HTTP.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return null;
		}
		final HttpPost httpreq = new HttpPost(url);
		httpreq.setEntity(f);
		return HttpUtils.getUrlCommon(httpreq);
	}
}
