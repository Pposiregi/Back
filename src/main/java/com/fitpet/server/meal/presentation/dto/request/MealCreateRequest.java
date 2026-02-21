package com.fitpet.server.meal.presentation.dto.request;

import com.fitpet.server.meal.application.dto.MealCreateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealCreateRequest {

    @NotNull
    private LocalDate day;

    @NotBlank
    private String title;

    @Min(0)
    private Integer kcal;

    @Min(1)
    private Integer sequence;

    public MealCreateCommand toCommand() {
        return MealCreateCommand.builder()
                .day(this.day)
                .title(this.title)
                .kcal(this.kcal)
                .sequence(this.sequence)
                .build();
    }
}