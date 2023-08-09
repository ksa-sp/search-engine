package searchengine.dto.indexing;

import lombok.Getter;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;

/**
 * Downloading http page class.
 */
@Getter
public class HttpPage {
    private String request;             // For log purposes
    private int code;
    private byte[] bodyAsBytes;
    private String body;

    /**
     * String page URI constructor.
     * <br>
     * Downloads the page and fills the fields of the class with the page's content.
     *
     * @param uri Page URI to download from.
     * @param headers List of http request headers in the form of "header-name:header-value".
     *
     * @throws ParseException - the page can't be indexed due to mime type or other reasons.
     * @throws IOException subclasses - in the case of network or http protocol errors.
     */
    public HttpPage(String uri, String[] headers) throws Exception {
        this(new URI(uri), headers);
    }

    /**
     * URI class page URI constructor.
     * <br>
     * Downloads the page and fills the fields of the class with the page's content.
     *
     * @param uri Page URI to download from.
     * @param headers List of http request headers in the form of "header-name:header-value".
     *
     * @throws ParseException - the page can't be indexed due to mime type or other reasons.
     * @throws IOException subclasses - in the case of network or http protocol errors.
     */
    public HttpPage(URI uri, String[] headers) throws Exception {
        try (
                // Allow invalid server SSL certificate
                final CloseableHttpClient httpclient = HttpClients
                        .custom()
                        .setSSLContext(
                                new SSLContextBuilder()
                                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                                        .build()
                        )
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build()
        ) {
            final HttpGet httpget = new HttpGet(uri);

            for (String header : headers) {
                String[] str = header.split("\\s*:\\s*", 2);

                if (str.length == 2 && !str[0].isBlank() && !str[1].isBlank()) {
                    httpget.addHeader(str[0], str[1]);
                }
            }

            request = httpget.getRequestLine().toString()
                    + "\r\n" + Arrays.toString(httpget.getAllHeaders());

            httpclient.execute(httpget,
                    resp -> {
                        code = resp.getStatusLine().getStatusCode();
                        HttpEntity entity = resp.getEntity();

                        if (entity == null) {                           // Response of status line only (304)
                            return this;
                        }

                        ContentType contentType = ContentType.getOrDefault(entity);

                        Charset charset = contentType.getCharset();
                        String mimeType = contentType.getMimeType();

                        if (charset == null && "text/plain".equals(mimeType)) {
                            charset = StandardCharsets.UTF_8;           // Default charset for text
                        }

                        if (charset == null
                                || mimeType == null
                                || !(
                                        mimeType.matches("^text/.*")
                                        || mimeType.matches(".*xml$")
                                )
                        ) {                                             // Not indexed mime/charset
                            EntityUtils.consume(entity);
                            throw new ParseException(mimeType + " " + charset);
                        }

                        bodyAsBytes = EntityUtils.toByteArray(entity);
                        body = new String(bodyAsBytes, charset);

                        return this;
                    }
            );
        }
    }
}
