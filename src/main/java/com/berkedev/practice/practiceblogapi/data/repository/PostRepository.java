package com.berkedev.practice.practiceblogapi.data.repository;

import com.berkedev.practice.practiceblogapi.data.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** TODO */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

}
