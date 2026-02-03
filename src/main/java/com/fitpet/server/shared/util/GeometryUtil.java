package com.fitpet.server.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeometryUtil {

    private static final double EARTH_RADIUS_METERS = 6371000; // 지구 반지름 (미터)

    /**
     * 두 지점 간의 거리 계산 (Haversine Formula)
     *
     * @return 거리 (미터 단위, 소수점 2자리 반올림)
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BigDecimal.ZERO;
        }

        // 계산의 효율성을 위해 double로 변환하여 삼각함수 수행
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lon1Rad = Math.toRadians(lon1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lon2Rad = Math.toRadians(lon2.doubleValue());

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceMeters = EARTH_RADIUS_METERS * c;

        // 결과는 다시 BigDecimal로 변환하여 정밀도 유지 (소수점 2자리, 미터 단위)
        return BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP);
    }
}