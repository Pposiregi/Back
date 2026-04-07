package com.fitpet.server.shared.config;

import com.fitpet.server.shared.interceptor.AuthInterceptor;
import com.fitpet.server.shared.resolver.AuthUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthUserArgumentResolver authUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/index.html",
                        "/",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/health",
                        "/terms",
                        "/css/**",
                        "/js/**",
                        "/static/**",
                        "/images/**",
                        "/api/test/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }
}
