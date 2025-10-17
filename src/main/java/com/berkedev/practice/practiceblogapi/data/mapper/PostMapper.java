package com.berkedev.practice.practiceblogapi.data.mapper;

import com.berkedev.practice.practiceblogapi.data.dto.request.PostCreateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.request.PostUpdateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.response.PostResponse;
import com.berkedev.practice.practiceblogapi.data.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    public PostResponse toResponse(Post post) {
        if (post == null)
            return null;

        return PostResponse.builder()
                .id(post.getId())
                .slug(post.getSlug())
                .content(post.getContent())
                .title(post.getTitle())
                .published(post.isPublished())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())

                .tags(tagMapper.toResponseList(post.getTags()))
                .category(categoryMapper.toResponse(post.getCategory()))
                .author(userMapper.toResponse(post.getAuthor()))
                .build();
    }

    public Post toEntity(PostCreateRequest createRequest) {
        if (createRequest == null)
            return null;

        return Post.builder()
                .content(createRequest.getContent())
                .published(createRequest.getPublished())
                .slug(createRequest.getSlug())
                .title(createRequest.getTitle())
                .build();
    }

    public void updateEntityFromRequest(PostUpdateRequest updateRequest, Post post) {
        if (updateRequest == null || post == null)
            return;

        if (updateRequest.getPublished() != null)
            post.setPublished(updateRequest.getPublished());

        if (updateRequest.getTitle() != null)
            post.setTitle(updateRequest.getTitle());

        if (updateRequest.getSlug() != null)
            post.setSlug(updateRequest.getSlug());

        if (updateRequest.getContent() != null)
            post.setContent(updateRequest.getContent());
    }
}