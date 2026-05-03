package com.apidoc.platform.application.service;

import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves which {@link ApiMaster} row to use for a logical API family: optional {@code version} picks a row; otherwise
 * the most recently updated row in the group is used.
 */
@Component
@RequiredArgsConstructor
public class ApiMasterVersionResolver {

    private final ApiMasterRepository apiMasterRepository;

    public ApiMaster resolve(Long id, String versionParam) {
        ApiMaster anchor =
                apiMasterRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "API definition not found: " + id));
        long gid = groupId(anchor);
        if (!StringUtils.hasText(versionParam)) {
            return apiMasterRepository
                    .findFirstByApiGroupIdOrderByUpdatedAtDesc(gid)
                    .orElse(anchor);
        }
        String v = versionParam.trim();
        return apiMasterRepository
                .findByApiGroupIdAndVersion(gid, v)
                .orElseThrow(
                        () ->
                                new DomainException(
                                        HttpStatus.NOT_FOUND,
                                        "API version not found for id " + id + ": " + v));
    }

    public static long groupId(ApiMaster m) {
        return m.getApiGroupId() != null ? m.getApiGroupId() : m.getId();
    }
}
