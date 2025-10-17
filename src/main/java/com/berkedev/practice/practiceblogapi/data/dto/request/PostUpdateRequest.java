package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {

    @Size(min = 5, max = 200)
    private String title;

    @Size(min = 5,max = 200)
    private String slug;

    @Size(min = 50)
    private String content;
    private Boolean published;

    private Long categoryId;
    private List<Long> tagIds;
}
