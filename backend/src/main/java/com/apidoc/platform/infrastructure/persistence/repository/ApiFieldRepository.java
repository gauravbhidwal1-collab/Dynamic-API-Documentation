package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiFieldRepository extends JpaRepository<ApiField, Long> {

    List<ApiField> findByApiMaster_IdOrderBySortOrderAsc(Long apiMasterId);

    List<ApiField> findByApiMaster_IdAndParentIsNullOrderBySortOrderAsc(Long apiMasterId);

    List<ApiField> findByParent_IdOrderBySortOrderAsc(Long parentFieldId);

    void deleteByApiMaster_Id(Long apiMasterId);
}
