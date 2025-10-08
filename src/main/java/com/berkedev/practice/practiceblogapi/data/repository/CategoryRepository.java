package com.berkedev.practice.practiceblogapi.data.repository;

import com.berkedev.practice.practiceblogapi.data.entity.Category;
import jdk.dynalink.linker.LinkerServices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    /// @param keyword
    /// @return
    /// "keyword'ü içeren category isimlerini getirir"
    List<Category> findByNameContainingIgnoreCase(String keyword);
}