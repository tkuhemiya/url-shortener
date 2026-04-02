package com.themiya.shortener.repository;

import com.themiya.shortener.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 where l.id = :linkId")
    int incrementClickCount(@Param("linkId") Long linkId);
}
