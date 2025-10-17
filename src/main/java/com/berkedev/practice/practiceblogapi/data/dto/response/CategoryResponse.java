package com.berkedev.practice.practiceblogapi.data.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
}

