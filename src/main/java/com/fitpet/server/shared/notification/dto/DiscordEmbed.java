package com.fitpet.server.shared.notification.dto;

import java.util.List;

public record DiscordEmbed(String title, String description, int color, List<DiscordEmbedField> fields) {
}
