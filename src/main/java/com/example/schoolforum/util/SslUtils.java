package com.example.schoolforum.util;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Slf4j
public class SslUtils {

    private static boolean sslDisabled = false;

    private SslUtils() {
    }

    public static synchronized void disableSslVerification() {
        if (sslDisabled) {
            return;
        }
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            sslDisabled = true;
            log.warn("SSL certificate verification has been DISABLED. This should only be used in development environment!");
        } catch (Exception e) {
            log.error("Failed to disable SSL verification", e);
        }
    }

    public static boolean isSslDisabled() {
        return sslDisabled;
    }
}
