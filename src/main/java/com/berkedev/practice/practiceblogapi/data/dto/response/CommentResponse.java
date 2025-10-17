package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    private UserResponse author;
}
