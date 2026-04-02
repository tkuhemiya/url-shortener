package com.themiya.shortener.repository;

import com.themiya.shortener.entity.Link;
import com.themiya.shortener.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    long countByOwnerTokenAndUserIsNull(String ownerToken);

    long countByUserId(Long userId);

    List<Link> findAllByOwnerTokenAndUserIsNullOrderByCreatedAtDesc(String ownerToken);

    List<Link> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Link> findByIdAndOwnerTokenAndUserIsNull(Long id, String ownerToken);

    Optional<Link> findByIdAndUserId(Long id, Long userId);

    List<Link> findAllByOwnerTokenAndUserIsNull(String ownerToken);
}
