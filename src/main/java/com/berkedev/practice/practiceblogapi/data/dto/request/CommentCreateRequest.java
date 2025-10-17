package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CommentCreateRequest {

    @NotBlank(message = "Content is required")
    @Size(min = 5, max = 1000, message = "Content must be between 5 and 1000 characters")
    private String content;

    @NotNull
    private Long postId;
}
