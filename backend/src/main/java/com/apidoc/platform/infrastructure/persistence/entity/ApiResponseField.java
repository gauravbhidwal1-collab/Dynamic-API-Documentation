package com.apidoc.platform.infrastructure.persistence.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "api_response_fields",
        indexes = {
            @Index(name = "idx_api_resp_fields_master_sort", columnList = "api_master_id,sort_order"),
            @Index(name = "idx_api_resp_fields_parent_sort", columnList = "parent_field_id,sort_order"),
            @Index(name = "idx_api_resp_fields_master_kind", columnList = "api_master_id,response_kind")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"apiMaster", "parent", "childFields"})
public class ApiResponseField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_master_id", nullable = false)
    private ApiMaster apiMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_field_id")
    private ApiResponseField parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 64)
    @Builder.Default
    private List<ApiResponseField> childFields = new ArrayList<>();

    @Column(name = "field_key", nullable = false, length = 255)
    private String fieldKey;

    @Column(name = "data_type", nullable = false, length = 64)
    private String dataType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Root trees use SUCCESS vs FAILURE; nested rows match their tree. Legacy rows may have null (treated as SUCCESS).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "response_kind", length = 16)
    private ResponseKind responseKind;

    public void addChildField(ApiResponseField child) {
        childFields.add(child);
        child.setParent(this);
        child.setApiMaster(this.apiMaster);
    }
}
