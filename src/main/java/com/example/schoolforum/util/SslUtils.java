package com.example.schoolforum.util;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Slf4j
public class SslUtils {

    private static volatile boolean sslDisabled = false;

    private SslUtils() {
    }

    public static synchronized void disableSslVerification() {
        if (sslDisabled) {
            return;
        }
        sslDisabled = true;
        log.warn("【安全警告】SSL证书验证已被全局禁用，这会使所有HTTPS连接面临中间人攻击风险！仅应在开发环境使用。");
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
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
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch (Exception e) {
            log.error("Failed to disable SSL verification", e);
        }
    }

    public static SSLSocketFactory createTrustingSSLSocketFactory() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create trusting SSL factory", e);
        }
    }

    public static HostnameValidator createTrustingHostnameVerifier() {
        return new HostnameValidator();
    }

    public static boolean isSslDisabled() {
        return sslDisabled;
    }

    public static class HostnameValidator implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
