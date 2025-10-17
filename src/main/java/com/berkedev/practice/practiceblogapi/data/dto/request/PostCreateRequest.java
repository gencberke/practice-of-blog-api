package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class PostCreateRequest {

    @NotBlank(message = "title is required")
    @Size(min = 5, max = 200, message = "title must be between 5 - 200 characters")
    private String title;

    @NotBlank(message = "slug is required")
    @Size(min = 5, max = 200, message = "slug must be between 5 - 200 characters")
    private String slug;

    @NotBlank
    @Size(min = 50)
    private String content;
    private boolean published = false;

    @NotNull(message = "category is required")
    private Long categoryId;
    private List<Long> tagIds = new ArrayList<>();
}
