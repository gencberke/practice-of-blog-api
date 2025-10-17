package com.berkedev.practice.practiceblogapi.data.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TagCreateRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 15, message = "tag name must be between 2 - 15 characters")
    private String name;
}
