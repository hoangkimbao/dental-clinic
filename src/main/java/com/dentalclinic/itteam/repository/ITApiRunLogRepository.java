package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITApiRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITApiRunLogRepository extends JpaRepository<ITApiRunLog, Long> {

    /**
     * Retrieve all API run logs ordered by run timestamp descending.
     */
    List<ITApiRunLog> findAllByOrderByRunTimestampDesc();

    /**
     * Retrieve top 50 recent API runs for the API Monitor view.
     */
    List<ITApiRunLog> findTop50ByOrderByRunTimestampDesc();

    /**
     * Paginated API run logs for history exploration.
     */
    Page<ITApiRunLog> findAllByOrderByRunTimestampDesc(Pageable pageable);

    /**
     * Search API runs by endpoint substring.
     */
    List<ITApiRunLog> findByEndpointContainingIgnoreCaseOrderByRunTimestampDesc(String endpoint);

    /**
     * Filter API runs by HTTP status code (e.g. 200, 401, 500).
     */
    List<ITApiRunLog> findByStatusCodeOrderByRunTimestampDesc(Integer statusCode);

    /**
     * Filter API runs by HTTP method (e.g. "GET", "POST", "PUT", "DELETE").
     */
    List<ITApiRunLog> findByHttpMethodOrderByRunTimestampDesc(String httpMethod);

    /**
     * Count API runs by HTTP status code.
     */
    long countByStatusCode(Integer statusCode);
}
