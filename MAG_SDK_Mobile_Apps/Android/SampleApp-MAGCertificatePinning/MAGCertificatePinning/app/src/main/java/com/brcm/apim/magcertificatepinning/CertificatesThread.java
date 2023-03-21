package com.brcm.apim.magcertificatepinning;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CertificatesThread implements Runnable{

    URL url;
    Certificate[] serverCertificates;

    public CertificatesThread(URL url) {
        this.url = url;
    }

    public Certificate[] getCertificates()
    {
        return serverCertificates;
    }

    @Override
    public void run() {
        SSLContext sslCtx = null;
        try {
            sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, new TrustManager[]{new X509TrustManager() {
                private X509Certificate[] accepted;

                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                    throw new CertificateException("This trust manager is only for clients");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                    accepted = xcs;
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return accepted;
                }
            }}, null);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setSSLSocketFactory(sslCtx.getSocketFactory());
            connection.connect();
            serverCertificates = connection.getServerCertificates();
            connection.disconnect();
        }catch (Exception e){

        }
    }
}
