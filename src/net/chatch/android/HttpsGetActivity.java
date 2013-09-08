
package net.chatch.android;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class HttpsGetActivity extends Activity
{
    private TextView statusText;
    private TextView urlText;
    private boolean trustAll = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        statusText = (TextView) findViewById(R.id.status);
        urlText = (TextView) findViewById(R.id.url);
    }

    public void doGet(View button) {
        String statusString = "";
        String url = (String) urlText.getText().toString();

        try {
            final X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

            /* Setup SSLSocketFactory */
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            final SSLSocketFactory trustAllSocketFactory = new TrustAllSSLSocketFactory(trustStore);
            trustAllSocketFactory.setHostnameVerifier(hostnameVerifier);

            /* Setup HTTP Client */
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            if (trustAll) {
                registry.register(new Scheme("https", trustAllSocketFactory, 443));
            } else {
                registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
            }

            HttpParams httpParams = new BasicHttpParams();
            SingleClientConnManager mgr = new SingleClientConnManager(httpParams, registry);
            DefaultHttpClient client = new DefaultHttpClient(mgr, httpParams);

            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            statusString = "HTTP: " + response.getStatusLine().getStatusCode();
            // HttpEntity resEntity = response.getEntity();
        } catch (Exception e) {
            e.printStackTrace();
            statusString = "ERROR: " + e.toString();
        }

        statusText.setText(statusString);
    }

    public void onRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.trustAll:
                trustAll = true;
                break;
            case R.id.trustStore:
                trustAll = false;
                break;
        }
    }

    class TrustAllSSLSocketFactory extends SSLSocketFactory {
        private SSLContext sslContext = SSLContext.getInstance("TLS");

        public TrustAllSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);
            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext.init(null, new TrustManager[] {
                tm
            }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

}
