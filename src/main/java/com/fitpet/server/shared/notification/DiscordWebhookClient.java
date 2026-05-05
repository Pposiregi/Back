package com.fitpet.server.shared.notification;

import com.fitpet.server.shared.notification.dto.DiscordWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class DiscordWebhookClient {

    private final RestClient restClient = RestClient.create();

    public void send(String webhookUrl, DiscordWebhookPayload payload) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
