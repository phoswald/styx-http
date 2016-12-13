package phoswald.http.server.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.rules.ExternalResource;

public class TrustAllCertsRule extends ExternalResource {

    private final SSLSocketFactory initialFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

    @Override
    protected void before() {
        HttpsURLConnection.setDefaultSSLSocketFactory(createFactory());
    }

    @Override
    protected void after() {
        HttpsURLConnection.setDefaultSSLSocketFactory(initialFactory);
    }

    private static SSLSocketFactory createFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
            };
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllCerts, new SecureRandom());
            return context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Create SSL socket factory.", e);
        }
    }
}
