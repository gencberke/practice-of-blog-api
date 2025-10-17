package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;
    private String fullName;
}
