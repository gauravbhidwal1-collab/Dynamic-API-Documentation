package com.apidoc.platform.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.apidoc.platform.infrastructure.persistence.entity.SampleEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
        })
class SampleEntityRepositoryTest {

    @Autowired
    private SampleEntityRepository repository;

    @Test
    void saveAndFind_roundTrip() {
        SampleEntity entity = SampleEntity.builder().name("h2-test").build();
        SampleEntity saved = repository.saveAndFlush(entity);

        Optional<SampleEntity> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("h2-test");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
