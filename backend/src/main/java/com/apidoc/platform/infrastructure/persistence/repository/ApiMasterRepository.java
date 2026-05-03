package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiMasterRepository extends JpaRepository<ApiMaster, Long> {

    List<ApiMaster> findAllByApiCode(String apiCode);

    Optional<ApiMaster> findFirstByApiGroupIdOrderByUpdatedAtDesc(Long apiGroupId);

    Optional<ApiMaster> findByApiGroupIdAndVersion(Long apiGroupId, String version);

    List<ApiMaster> findAllByApiGroupIdOrderByUpdatedAtDesc(Long apiGroupId);

    List<ApiMaster> findAllByOrderByUpdatedAtDesc();
}
