package com.fitpet.server.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.discord")
public class DiscordWebhookProperties {
    private boolean enabled;
    private String errorWebhookUrl;
    private String signupWebhookUrl;
}
