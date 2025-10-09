package com.berkedev.practice.practiceblogapi.data.repository;

import com.berkedev.practice.practiceblogapi.data.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // Senin use-case'ine göre: Published ve draft'ları ayrı getir
    List<Post> findByAuthorIdAndPublishedTrue(Long authorId);
    List<Post> findByAuthorIdAndPublishedFalse(Long authorId);

    // Tüm post'ları getir (owner için)
    List<Post> findByAuthorId(Long authorId);

    // Published ve draft sayılarını say
    long countByAuthorIdAndPublishedTrue(Long authorId);
    long countByAuthorIdAndPublishedFalse(Long authorId);

    // Category bazlı sorgular
    List<Post> findByCategoryId(Long categoryId);
    List<Post> findByPublishedAndCategoryId(boolean published, Long categoryId);

    // Arama
    List<Post> findByTitleContainingIgnoreCase(String title);

    // Ek: Published post'ları bul (ana projede var)
    List<Post> findByPublished(boolean published);

    // Ek: Published post'ları tarih sırasına göre getir
    @Query("SELECT p FROM Post p WHERE p.published = :published ORDER BY p.publishedAt DESC")
    List<Post> findPublishedPostsOrderedByDate(@Param("published") boolean published);
}
