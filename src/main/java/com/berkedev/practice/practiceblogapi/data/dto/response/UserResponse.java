package com.berkedev.practice.practiceblogapi.data.dto.response;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String userName;
    private LocalDateTime createdAt;
    private String email;
}
