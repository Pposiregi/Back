package com.fitpet.server.user.application.mapper;

import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.presentation.dto.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mappings({
      @Mapping(target = "id", ignore = true),
      @Mapping(target = "provider", ignore = true),
      @Mapping(target = "providerUid", ignore = true),
      @Mapping(target = "deviceToken", ignore = true),
      @Mapping(target = "refreshToken", ignore = true),
      @Mapping(target = "registrationStatus", ignore = true),
      @Mapping(target = "password", ignore = true),
      @Mapping(target = "createdAt", ignore = true),
      @Mapping(target = "updatedAt", ignore = true),
      @Mapping(target = "dailyStepCount", ignore = true),
      @Mapping(target = "allowActivityNotification", ignore = true),
      @Mapping(target = "lastAccessedAt", ignore = true)
    })
    User toEntity(UserCreateRequest request);

    @Mapping(source = "id", target = "userId")
    UserDto toDto(User user);

}
