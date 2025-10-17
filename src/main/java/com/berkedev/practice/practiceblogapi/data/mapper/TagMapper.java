package com.berkedev.practice.practiceblogapi.data.mapper;

import com.berkedev.practice.practiceblogapi.data.dto.request.TagCreateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.response.TagResponse;
import com.berkedev.practice.practiceblogapi.data.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        if (tag == null)
            return null;

        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .build();
    }

    public List<TagResponse> toResponseList(List<Tag> tags) {
        List<TagResponse> tagResponsesList = new ArrayList<>();

        for (Tag tag : tags) {
            tagResponsesList.add(toResponse(tag));
        }

        return tagResponsesList;
    }

    public Tag toEntity(TagCreateRequest createRequest) {
        if (createRequest == null)
            return null;

        return new Tag(createRequest.getName());
    }
}
