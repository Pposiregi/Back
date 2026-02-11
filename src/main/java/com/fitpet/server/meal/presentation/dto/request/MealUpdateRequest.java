package com.fitpet.server.meal.presentation.dto.request;

import com.fitpet.server.meal.application.dto.MealUpdateCommand;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealUpdateRequest {

    private String title;

    @Min(0)
    private Integer kcal;

    @Min(1)
    private Integer sequence;

    private Boolean changeImage;

    public MealUpdateCommand toCommand() {
        return MealUpdateCommand.builder()
                .title(this.title)
                .kcal(this.kcal)
                .sequence(this.sequence)
                .changeImage(this.changeImage != null ? this.changeImage : false)
                .build();
    }
}