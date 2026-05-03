package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.ApiTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTemplateRepository extends JpaRepository<ApiTemplate, Long> {

    List<ApiTemplate> findAllByOrderByNameAsc();
}
