package com.dentalclinic.analytics.repository;

import com.dentalclinic.analytics.model.AnalyticsEvent;
import com.dentalclinic.analytics.model.EventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<AnalyticsEvent, Long> {

    List<AnalyticsEvent> findByEventType(EventType eventType);

    List<AnalyticsEvent> findByClientSessionId(String clientSessionId);

    long countByEventType(EventType eventType);

    long countByOccurredAtAfter(LocalDateTime since);

    @Query("SELECT e.eventType, COUNT(e) FROM AnalyticsEvent e GROUP BY e.eventType")
    List<Object[]> countGroupedByEventType();

    @Query("SELECT e.eventName, COUNT(e) FROM AnalyticsEvent e WHERE e.eventType = :eventType GROUP BY e.eventName ORDER BY COUNT(e) DESC")
    List<Object[]> countGroupedByEventName(@Param("eventType") EventType eventType);

    @Query("SELECT e.pageUrl, COUNT(e) FROM AnalyticsEvent e WHERE e.pageUrl IS NOT NULL AND e.pageUrl <> '' GROUP BY e.pageUrl ORDER BY COUNT(e) DESC")
    List<Object[]> countTopPages(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT e.clientSessionId) FROM AnalyticsEvent e WHERE e.clientSessionId IS NOT NULL AND e.clientSessionId <> ''")
    long countDistinctSessions();

    List<AnalyticsEvent> findTop20ByOrderByOccurredAtDesc();
}
