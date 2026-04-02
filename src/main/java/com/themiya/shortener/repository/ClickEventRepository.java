package com.themiya.shortener.repository;

import com.themiya.shortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByLinkId(Long linkId);

    long countByLinkIdIn(Collection<Long> linkIds);

    @Query("""
            select c.country as label, count(c.id) as total
            from ClickEvent c
            where c.link.id = :linkId and c.country is not null
            group by c.country
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByCountry(@Param("linkId") Long linkId);

    @Query("""
            select c.country as label, count(c.id) as total
            from ClickEvent c
            where c.link.id in :linkIds and c.country is not null
            group by c.country
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByCountryIn(@Param("linkIds") Collection<Long> linkIds);

    @Query("""
            select c.deviceType as label, count(c.id) as total
            from ClickEvent c
            where c.link.id = :linkId and c.deviceType is not null
            group by c.deviceType
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByDeviceType(@Param("linkId") Long linkId);

    @Query("""
            select c.deviceType as label, count(c.id) as total
            from ClickEvent c
            where c.link.id in :linkIds and c.deviceType is not null
            group by c.deviceType
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByDeviceTypeIn(@Param("linkIds") Collection<Long> linkIds);

    @Query("""
            select c.referer as label, count(c.id) as total
            from ClickEvent c
            where c.link.id = :linkId and c.referer is not null
            group by c.referer
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByReferer(@Param("linkId") Long linkId);

    @Query("""
            select c.referer as label, count(c.id) as total
            from ClickEvent c
            where c.link.id in :linkIds and c.referer is not null
            group by c.referer
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByRefererIn(@Param("linkIds") Collection<Long> linkIds);

    @Query("""
            select cast(function('date', c.clickedAt) as string) as label, count(c.id) as total
            from ClickEvent c
            where c.link.id = :linkId
            group by function('date', c.clickedAt)
            order by function('date', c.clickedAt)
            """)
    List<LabelCountProjection> countByDay(@Param("linkId") Long linkId);

    @Query("""
            select cast(function('date', c.clickedAt) as string) as label, count(c.id) as total
            from ClickEvent c
            where c.link.id in :linkIds
            group by function('date', c.clickedAt)
            order by function('date', c.clickedAt)
            """)
    List<LabelCountProjection> countByDayIn(@Param("linkIds") Collection<Long> linkIds);

    interface LabelCountProjection {
        String getLabel();
        Long getTotal();
    }
}
