package com.fitpet.server.alram.application.mapper;

import com.fitpet.server.alram.domain.entity.AlramMessage;
import com.fitpet.server.alram.presentation.dto.request.AlramRequestDto;
import com.fitpet.server.alram.presentation.dto.response.AlramResponseDto;
import com.fitpet.server.user.domain.entity.User;
import com.google.firebase.messaging.Notification;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AlramMapper {
    Notification toNotification(AlramRequestDto requestDto);

    @Mapping(source = "requestDto.body", target = "message")
    @Mapping(source = "user", target = "user")
    AlramMessage toAlramMessage(AlramRequestDto requestDto, User user);

    @Mapping(source = "savedAlram.id", target = "alramId")
    AlramResponseDto toAlramResponseDto(
            AlramMessage savedAlram,
            String fcmMessageId,
            String message,
            Map<String, String> data
    );
}