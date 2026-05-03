package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.ApiDefinitionResponse;
import com.apidoc.platform.application.dto.ApiDefinitionSummaryResponse;
import com.apidoc.platform.application.dto.ApiVersionSummaryResponse;
import java.util.List;

public interface ApiDefinitionService {

    ApiDefinitionResponse create(ApiDefinitionRequest request);

    ApiDefinitionResponse update(Long id, ApiDefinitionRequest request);

    ApiDefinitionResponse getById(Long id);

    /** When {@code version} is null or blank, the latest row in the API lineage (by {@code updated_at}) is returned. */
    ApiDefinitionResponse getById(Long id, String version);

    List<ApiDefinitionSummaryResponse> listSummaries();

    List<ApiVersionSummaryResponse> listVersionSummaries(Long id);

    /** Deep-copies {@code sourceId} into a new row with {@code newVersion} in the same lineage. */
    ApiDefinitionResponse cloneAsNewVersion(Long sourceId, String newVersion);

    void deleteById(Long id);
}
