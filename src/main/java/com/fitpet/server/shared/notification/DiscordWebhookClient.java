package com.fitpet.server.shared.notification;

import com.fitpet.server.shared.notification.dto.DiscordWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordWebhookClient {

    private final RestClient discordRestClient;

    public void send(String webhookUrl, DiscordWebhookPayload payload) {
        discordRestClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
