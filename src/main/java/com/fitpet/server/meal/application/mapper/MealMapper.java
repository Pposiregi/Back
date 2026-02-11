package com.fitpet.server.meal.application.mapper;

import com.fitpet.server.meal.application.dto.MealCreateCommand;
import com.fitpet.server.meal.application.dto.MealDetailInfo;
import com.fitpet.server.meal.application.dto.MealResult;
import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.domain.entity.User;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "title", source = "command.title")
    @Mapping(target = "kcal", source = "command.kcal")
    @Mapping(target = "sequence", source = "command.sequence")
    @Mapping(target = "day", source = "command.day")
    Meal toEntity(MealCreateCommand command, User user);

    @Mapping(source = "meal.id", target = "mealId")
    @Mapping(source = "meal.imageUrl", target = "imageUrl")
    @Mapping(source = "uploadUrl", target = "uploadUrl")
    MealResult toResult(Meal meal, String uploadUrl);

    @Mapping(source = "meal.id", target = "mealId")
    @Mapping(source = "meal.imageUrl", target = "imageUrl", qualifiedByName = "generateGetUrl")
    MealDetailInfo toDetailInfo(Meal meal, @Context S3Service s3Service);

    @Named("generateGetUrl")
    default String generateGetUrl(String objectKey, @Context S3Service s3Service) {
        if (s3Service == null || objectKey == null) {
            return null;
        }
        return s3Service.generatePresignedGetUrl(objectKey);
    }
}