package com.apidoc.platform.infrastructure.persistence.repository;

import com.apidoc.platform.infrastructure.persistence.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleEntityRepository extends JpaRepository<SampleEntity, Long> {}
