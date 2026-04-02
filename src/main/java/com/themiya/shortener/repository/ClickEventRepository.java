package com.themiya.shortener.repository;

import com.themiya.shortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByLinkId(Long linkId);

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
            where c.country is not null
            group by c.country
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByCountryAll();

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
            where c.deviceType is not null
            group by c.deviceType
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByDeviceTypeAll();

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
            where c.referer is not null
            group by c.referer
            order by count(c.id) desc
            """)
    List<LabelCountProjection> countByRefererAll();

    @Query(value = """
            select to_char(date(clicked_at), 'YYYY-MM-DD') as label, count(*) as total
            from click_events
            where link_id = :linkId
            group by date(clicked_at)
            order by date(clicked_at)
            """, nativeQuery = true)
    List<LabelCountProjection> countByDay(@Param("linkId") Long linkId);

    @Query(value = """
            select to_char(date(clicked_at), 'YYYY-MM-DD') as label, count(*) as total
            from click_events
            group by date(clicked_at)
            order by date(clicked_at)
            """, nativeQuery = true)
    List<LabelCountProjection> countByDayAll();

    interface LabelCountProjection {
        String getLabel();
        Long getTotal();
    }
}
