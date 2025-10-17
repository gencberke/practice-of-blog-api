package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    private UserResponse author;
    private CategoryResponse category;
    private List<TagResponse> tags = new ArrayList<>();
}
