package com.gymory.domain.user.trainer.data;

import com.gymory.domain.certification.data.Certification;
import com.gymory.domain.fee.data.Fee;
import com.gymory.domain.user.userbase.UserRole;
import com.gymory.domain.user.userbase.data.UserBase;
import com.gymory.domain.user.trainer.dto.TrainerCreateDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@DiscriminatorValue("TRAINER")
public class Trainer extends UserBase {

    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    private String shortIntroduction;
    private String longIntroduction;

    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Fee> fees = new ArrayList<>();


    @Builder
    private Trainer(String username, String email, String password, UserRole role, String shortIntroduction, String longIntroduction) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.shortIntroduction = shortIntroduction;
        this.longIntroduction = longIntroduction;
    }

    public static Trainer create(TrainerCreateDto trainerDto){
        return Trainer.builder()
                .username(trainerDto.getUsername())
                .email(trainerDto.getEmail())
                .password(trainerDto.getPassword())
                .role(trainerDto.getRole())
                .shortIntroduction(trainerDto.getShortIntroduction())
                .longIntroduction(trainerDto.getLongIntroduction())
                .build();
    }
}
