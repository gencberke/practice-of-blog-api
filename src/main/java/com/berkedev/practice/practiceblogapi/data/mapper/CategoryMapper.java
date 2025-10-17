package com.berkedev.practice.practiceblogapi.data.mapper;

import com.berkedev.practice.practiceblogapi.data.dto.response.CategoryResponse;
import com.berkedev.practice.practiceblogapi.data.entity.Category;

public record CategoryMapper () {

   public CategoryResponse toResponse(Category category){
       if (category == null) {
           return null;
       }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
