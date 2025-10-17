package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String username;
    private LocalDateTime createdAt;
    private String email;
}
