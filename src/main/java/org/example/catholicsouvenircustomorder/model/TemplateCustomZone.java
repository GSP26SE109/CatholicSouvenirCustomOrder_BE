package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Table(name = "template_custom_zones")
public class TemplateCustomZone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID zoneId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id")
    private ProductTemplate template;
    
    @Column(nullable = false, length = 100)
    private String zoneName;
    
    @Column(length = 500)
    private String zoneDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputType inputType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> inputConstraints;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private Boolean isRequired = false;
    
    @Column(nullable = false)
    private Integer sortOrder = 0;
}
