package com.example.schoolforum.util;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Slf4j
public class SslUtils {

    private SslUtils() {
    }

    /**
     * 创建信任所有证书的 SSLSocketFactory（用于局部配置，不影响全局）。
     * 可用于 HttpClient 等支持自定义 SSL 工厂的 HTTP 客户端。
     */
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

    /**
     * 创建信任所有主机名的 HostnameVerifier（用于局部配置，不影响全局）。
     */
    public static HostnameVerifier createTrustingHostnameVerifier() {
        return (hostname, session) -> true;
    }

    /**
     * 临时禁用 JVM 全局 SSL 验证，执行指定操作后自动恢复。
     * 仅在 JustAuth 等不支持自定义 HTTP 客户端的第三方库中使用。
     *
     * <p>此方法通过临时替换默认 SSLSocketFactory 和 HostnameVerifier 来绕过 SSL 验证，
     * 操作完成后立即恢复原有设置，最大程度减少全局影响。</p>
     *
     * @param action 需要在 SSL 验证禁用期间执行的操作
     * @param <T>    操作返回类型
     * @return 操作的返回值
     */
    public static <T> T withSslDisabled(java.util.function.Supplier<T> action) {
        SSLSocketFactory originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        try {
            log.warn("【安全警告】临时禁用 SSL 证书验证，仅用于 OAuth 回调");
            HttpsURLConnection.setDefaultSSLSocketFactory(createTrustingSSLSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(createTrustingHostnameVerifier());
            return action.get();
        } finally {
            // 无论成功还是异常，都立即恢复原始设置
            HttpsURLConnection.setDefaultSSLSocketFactory(originalFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(originalVerifier);
            log.debug("SSL 证书验证已恢复");
        }
    }

    /**
     * 临时禁用 JVM 全局 SSL 验证，执行指定操作后自动恢复（无返回值版本）。
     */
    public static void withSslDisabled(Runnable action) {
        withSslDisabled(() -> { action.run(); return null; });
    }
}
