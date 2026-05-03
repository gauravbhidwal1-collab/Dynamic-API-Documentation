package com.apidoc.platform.config;

import com.apidoc.platform.infrastructure.persistence.entity.SampleEntity;
import com.apidoc.platform.infrastructure.persistence.repository.SampleEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies MySQL connectivity by persisting and reading a row on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionSampleRunner implements CommandLineRunner {

    private final SampleEntityRepository sampleEntityRepository;

    @Override
    @Transactional
    public void run(String... args) {
        SampleEntity probe = SampleEntity.builder().name("mysql-connection-probe").build();
        SampleEntity saved = sampleEntityRepository.saveAndFlush(probe);
        sampleEntityRepository
                .findById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Sample row not visible after insert"));
        sampleEntityRepository.deleteById(saved.getId());
        log.info("MySQL connection verified (sample_entities round-trip succeeded, probe row removed).");
    }
}
