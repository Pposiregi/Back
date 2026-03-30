package com.fitpet.server.shared.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    public static String extract(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValid(ip)) {
                return firstIp(ip);
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

    private static String firstIp(String ip) {
        return ip.split(",")[0].trim();
    }

    private ClientIpUtils() {}
}
