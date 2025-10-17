package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private Boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    private UserResponse author;
    private CategoryResponse category;
    private List<TagResponse> tags = new ArrayList<>();
}
