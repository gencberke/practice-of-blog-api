package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class PostUpdateRequest {

    @Size(min = 5, max = 200)
    private String title;

    @Size(min = 5,max = 200)
    private String slug;

    @Size(min = 50)
    private String content;
    private boolean published;

    private Long categoryId;
    private List<Long> tagIds;
}
