package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.ApiLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long>, JpaSpecificationExecutor<ApiLog> {

    @EntityGraph(attributePaths = {"apiMaster"})
    List<ApiLog> findAllByOrderByExecutedAtDesc();

    List<ApiLog> findByApiMaster_IdOrderByExecutedAtDesc(Long apiMasterId);

    Page<ApiLog> findByApiMaster_Id(Long apiMasterId, Pageable pageable);

    List<ApiLog> findByApiMaster_IdAndExecutedAtBetweenOrderByExecutedAtDesc(
            Long apiMasterId, Instant from, Instant to);
}
