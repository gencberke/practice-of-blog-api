package com.berkedev.practice.practiceblogapi.data.mapper;

import com.berkedev.practice.practiceblogapi.data.dto.request.UserCreateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.request.UserUpdateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.response.UserResponse;
import com.berkedev.practice.practiceblogapi.data.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null)
            return null;

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User toEntity(UserCreateRequest createRequest) {
        if (createRequest == null)
            return null;

        return User.builder()
                .username(createRequest.getUsername())
                .email(createRequest.getEmail())
                .fullName(createRequest.getFullName())
                .password(createRequest.getPassword())
                .build();
    }

    public void updateEntityFromRequest(UserUpdateRequest updateRequest, User user) {
        if (updateRequest == null || user == null)
            return;

        if (updateRequest.getEmail() != null)
            user.setEmail(updateRequest.getEmail());

        if (updateRequest.getPassword() != null)
            user.setPassword(updateRequest.getPassword());

        if (updateRequest.getFullName() != null)
            user.setFullName(updateRequest.getFullName());
    }
}
