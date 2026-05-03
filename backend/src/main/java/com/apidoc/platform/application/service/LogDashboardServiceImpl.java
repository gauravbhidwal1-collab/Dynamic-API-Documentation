package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.LatencySeriesPoint;
import com.apidoc.platform.application.dto.LogDashboardRecentItem;
import com.apidoc.platform.application.dto.LogDashboardResponse;
import com.apidoc.platform.application.dto.LogsPerformanceResponse;
import com.apidoc.platform.infrastructure.persistence.entity.ApiLog;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.repository.ApiLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LogDashboardServiceImpl implements LogDashboardService {

    private final ApiLogRepository apiLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public LogDashboardResponse getDashboard(Instant from, Instant to, Long apiId, String apiName) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(7, ChronoUnit.DAYS);
        if (start.isAfter(end)) {
            Instant t = start;
            start = end;
            end = t;
        }

        Specification<ApiLog> base = baseSpecification(start, end, apiId, apiName);
        long total = apiLogRepository.count(base);
        long success = apiLogRepository.count(base.and(httpSuccessSpecification()));
        long failure = total - success;
        double avgMs = averageDurationMs(start, end, apiId, apiName);

        List<LogDashboardRecentItem> recent =
                apiLogRepository
                        .findAll(base, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "executedAt")))
                        .stream()
                        .map(this::toRecentItem)
                        .collect(Collectors.toList());

        return LogDashboardResponse.builder()
                .totalRequests(total)
                .successCount(success)
                .failureCount(failure)
                .avgResponseTimeMs(avgMs)
                .last10Requests(recent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LogsPerformanceResponse getPerformance(Instant from, Instant to, Long apiId, String apiName) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(7, ChronoUnit.DAYS);
        if (start.isAfter(end)) {
            Instant t = start;
            start = end;
            end = t;
        }
        Specification<ApiLog> base = baseSpecification(start, end, apiId, apiName);
        Specification<ApiLog> withDuration = base.and(durationPresentSpecification());
        long withDurCount = apiLogRepository.count(withDuration);
        double avgMs = averageDurationMs(start, end, apiId, apiName);
        Long minMs = withDurCount > 0 ? minDurationMs(start, end, apiId, apiName) : null;
        Long maxMs = withDurCount > 0 ? maxDurationMs(start, end, apiId, apiName) : null;

        List<LatencySeriesPoint> series = new ArrayList<>();
        if (withDurCount > 0) {
            List<ApiLog> points =
                    apiLogRepository
                            .findAll(
                                    withDuration,
                                    PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "executedAt")))
                            .getContent();
            for (ApiLog log : points) {
                series.add(
                        LatencySeriesPoint.builder()
                                .executedAt(log.getExecutedAt())
                                .responseTimeMs(log.getDurationMs())
                                .httpStatus(log.getHttpStatus())
                                .build());
            }
        }
        return LogsPerformanceResponse.builder()
                .avgLatencyMs(avgMs)
                .minLatencyMs(minMs)
                .maxLatencyMs(maxMs)
                .series(series)
                .build();
    }

    private Specification<ApiLog> durationPresentSpecification() {
        return (root, query, cb) -> cb.isNotNull(root.get("durationMs"));
    }

    private Long minDurationMs(Instant from, Instant to, Long apiId, String apiName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ApiLog> root = cq.from(ApiLog.class);
        Join<ApiLog, ApiMaster> master = root.join("apiMaster", JoinType.INNER);
        cq.select(cb.min(root.get("durationMs")));
        cq.where(
                cb.and(
                        dashboardFilters(root, master, cb, from, to, apiId, apiName),
                        cb.isNotNull(root.get("durationMs"))));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private Long maxDurationMs(Instant from, Instant to, Long apiId, String apiName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ApiLog> root = cq.from(ApiLog.class);
        Join<ApiLog, ApiMaster> master = root.join("apiMaster", JoinType.INNER);
        cq.select(cb.max(root.get("durationMs")));
        cq.where(
                cb.and(
                        dashboardFilters(root, master, cb, from, to, apiId, apiName),
                        cb.isNotNull(root.get("durationMs"))));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private LogDashboardRecentItem toRecentItem(ApiLog log) {
        ApiMaster m = log.getApiMaster();
        return LogDashboardRecentItem.builder()
                .id(log.getId())
                .apiId(m != null ? m.getId() : null)
                .apiName(m != null ? m.getName() : "—")
                .httpStatus(log.getHttpStatus())
                .durationMs(log.getDurationMs())
                .executedAt(log.getExecutedAt())
                .build();
    }

    private Specification<ApiLog> baseSpecification(
            Instant from, Instant to, Long apiId, String apiName) {
        return (root, query, cb) -> {
            Join<ApiLog, ApiMaster> master = root.join("apiMaster", JoinType.INNER);
            return dashboardFilters(root, master, cb, from, to, apiId, apiName);
        };
    }

    private Predicate dashboardFilters(
            Root<ApiLog> root,
            Join<ApiLog, ApiMaster> master,
            CriteriaBuilder cb,
            Instant from,
            Instant to,
            Long apiId,
            String apiName) {
        List<Predicate> p = new ArrayList<>();
        p.add(cb.between(root.get("executedAt"), from, to));
        if (apiId != null) {
            p.add(cb.equal(master.get("id"), apiId));
        }
        if (StringUtils.hasText(apiName)) {
            String pattern =
                    "%" + escapeLike(apiName.trim().toLowerCase(Locale.ROOT)) + "%";
            p.add(cb.like(cb.lower(master.get("name")), pattern));
        }
        return cb.and(p.toArray(new Predicate[0]));
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Specification<ApiLog> httpSuccessSpecification() {
        return (root, query, cb) ->
                cb.and(
                        cb.isNotNull(root.get("httpStatus")),
                        cb.greaterThanOrEqualTo(root.get("httpStatus"), 200),
                        cb.lessThan(root.get("httpStatus"), 300));
    }

    private double averageDurationMs(Instant from, Instant to, Long apiId, String apiName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> cq = cb.createQuery(Double.class);
        Root<ApiLog> root = cq.from(ApiLog.class);
        Join<ApiLog, ApiMaster> master = root.join("apiMaster", JoinType.INNER);
        cq.select(cb.avg(root.get("durationMs")));
        cq.where(
                cb.and(
                        dashboardFilters(root, master, cb, from, to, apiId, apiName),
                        cb.isNotNull(root.get("durationMs"))));
        Double avg = entityManager.createQuery(cq).getSingleResult();
        return avg != null ? avg : 0d;
    }
}
