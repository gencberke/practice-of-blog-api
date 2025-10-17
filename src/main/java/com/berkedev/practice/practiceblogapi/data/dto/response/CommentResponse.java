package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    private UserResponse author;
}
