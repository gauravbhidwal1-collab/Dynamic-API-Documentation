package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiResponseFieldRepository extends JpaRepository<ApiResponseField, Long> {

    List<ApiResponseField> findByApiMaster_IdOrderBySortOrderAsc(Long apiMasterId);

    List<ApiResponseField> findByApiMaster_IdAndParentIsNullOrderBySortOrderAsc(Long apiMasterId);

    List<ApiResponseField> findByParent_IdOrderBySortOrderAsc(Long parentFieldId);

    void deleteByApiMaster_Id(Long apiMasterId);
}
