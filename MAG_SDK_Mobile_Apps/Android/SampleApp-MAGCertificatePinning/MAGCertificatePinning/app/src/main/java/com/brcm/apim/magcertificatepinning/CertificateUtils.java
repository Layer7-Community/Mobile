package com.brcm.apim.magcertificatepinning;

import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CertificateUtils {

    public static Certificate[] getCertificates(URL url) throws Exception{

        CertificatesThread runnable = new CertificatesThread(url);
        Thread thread = new Thread(runnable);

        thread.start();
        thread.join();
        return runnable.getCertificates();
    }
}
