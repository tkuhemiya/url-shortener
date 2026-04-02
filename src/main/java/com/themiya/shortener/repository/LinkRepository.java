package com.themiya.shortener.repository;

import com.themiya.shortener.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);
}
