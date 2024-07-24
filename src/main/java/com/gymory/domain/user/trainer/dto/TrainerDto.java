package com.gymory.domain.user.trainer.dto;

import com.gymory.domain.user.base.dto.UserDto;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrainerDto extends UserDto {

    private String shortIntroduction;
    private String longIntroduction;
}
