package com.berkedev.practice.practiceblogapi.data.mapper;

import com.berkedev.practice.practiceblogapi.data.dto.request.CommentCreateRequest;
import com.berkedev.practice.practiceblogapi.data.dto.response.CommentResponse;
import com.berkedev.practice.practiceblogapi.data.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentResponse toResponse(Comment comment) {
        if (comment == null)
            return null;

        return CommentResponse.builder()
                .id(comment.getId())
                .createdAt(comment.getCreatedAt())
                .content(comment.getContent())

                .author(userMapper.toResponse(comment.getAuthor()))
                .build();
    }

    public Comment toEntity(CommentCreateRequest commentCreateRequest) {
        if (commentCreateRequest == null)
            return null;

        return new Comment(commentCreateRequest.getContent());
    }
}
