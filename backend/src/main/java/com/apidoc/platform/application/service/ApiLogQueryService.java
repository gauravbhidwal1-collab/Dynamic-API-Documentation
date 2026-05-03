package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiLogEntryResponse;
import java.util.List;

public interface ApiLogQueryService {

    List<ApiLogEntryResponse> findAllOrderByNewest();

    List<ApiLogEntryResponse> findByApiIdOrderByNewest(Long apiId);
}
