package com.fitpet.server.shared.notification.dto;

import java.util.List;

public record DiscordWebhookPayload(List<DiscordEmbed> embeds) {

    public static DiscordWebhookPayload of(DiscordEmbed embed) {
        return new DiscordWebhookPayload(List.of(embed));
    }
}
