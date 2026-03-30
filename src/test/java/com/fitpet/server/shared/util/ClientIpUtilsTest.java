package com.fitpet.server.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpUtilsTest {

    @Test
    void X_Forwarded_For_헤더가_있으면_첫_번째_IP를_반환한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        assertThat(ClientIpUtils.extract(request)).isEqualTo("1.2.3.4");
    }

    @Test
    void 헤더가_없으면_RemoteAddr를_반환한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");

        assertThat(ClientIpUtils.extract(request)).isEqualTo("9.9.9.9");
    }

    @Test
    void unknown값은_건너뛰고_다음_헤더를_사용한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("Proxy-Client-IP", "2.2.2.2");

        assertThat(ClientIpUtils.extract(request)).isEqualTo("2.2.2.2");
    }
}
