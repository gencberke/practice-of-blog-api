package com.berkedev.practice.practiceblogapi.data.repository;

import com.berkedev.practice.practiceblogapi.data.dto.response.CategoryResponse;
import com.berkedev.practice.practiceblogapi.data.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    List<Category> findByNameContainingIgnoreCase(String keyword);

    @Query("""
                select new com.berkedev.practice.practiceblogapi.data.dto.response.CategoryResponse(
                 c.id,
                  c.name,
                   c.description
                 ) from Category c where c.name=:name
            """)
    List<CategoryResponse> findAllSomeList(@Param("name") String name);
}