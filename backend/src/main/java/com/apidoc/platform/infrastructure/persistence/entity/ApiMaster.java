package com.apidoc.platform.infrastructure.persistence.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "api_master",
        indexes = {
            @Index(name = "idx_api_master_updated_at", columnList = "updated_at"),
            @Index(name = "idx_api_master_active_updated", columnList = "is_active,updated_at"),
            @Index(name = "idx_api_master_group_updated", columnList = "api_group_id,updated_at")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_api_master_group_version",
                        columnNames = {"api_group_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"requestFields", "responseFields", "logs"})
public class ApiMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    /** Business identifier; uniqueness is enforced per {@link #apiGroupId} lineage (not globally). */
    @Column(name = "api_code", length = 128)
    private String apiCode;

    /**
     * Stable id shared by all versions of one API. For the first row in a lineage it equals {@link #id} after
     * persist.
     */
    @Column(name = "api_group_id")
    private Long apiGroupId;

    /** Version label (e.g. v1, v2) unique within {@link #apiGroupId}. */
    @Column(name = "version", nullable = false, length = 64)
    @Builder.Default
    private String version = "v1";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "activities_sequence_text", columnDefinition = "TEXT")
    private String activitiesSequenceText;

    @Column(name = "additional_notes_text", columnDefinition = "TEXT")
    private String additionalNotesText;

    @Column(name = "impact_on_system_text", columnDefinition = "TEXT")
    private String impactOnSystemText;

    /** JSON array of documented HTTP header rows (key, value, description). */
    @Column(name = "documented_headers_json", columnDefinition = "TEXT")
    private String documentedHeadersJson;

    /** JSON array of failure validation rules (validation message, scenario). */
    @Column(name = "failure_validations_json", columnDefinition = "TEXT")
    private String failureValidationsJson;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "base_url", nullable = false, length = 2048)
    private String baseUrl;

    @Column(name = "path_template", length = 2048)
    private String pathTemplate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "apiMaster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApiField> requestFields = new ArrayList<>();

    @OneToMany(mappedBy = "apiMaster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApiResponseField> responseFields = new ArrayList<>();

    @OneToMany(mappedBy = "apiMaster", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<ApiLog> logs = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (active == null) {
            active = true;
        }
        if (version == null || version.isEmpty()) {
            version = "v1";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addRequestField(ApiField field) {
        requestFields.add(field);
        field.setApiMaster(this);
    }

    public void addResponseField(ApiResponseField field) {
        responseFields.add(field);
        field.setApiMaster(this);
    }

    public void addLog(ApiLog log) {
        logs.add(log);
        log.setApiMaster(this);
    }
}
