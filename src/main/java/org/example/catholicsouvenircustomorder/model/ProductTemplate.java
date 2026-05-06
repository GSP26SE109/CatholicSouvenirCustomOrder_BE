package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Table(name = "product_templates")
public class ProductTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID templateId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal basePrice;
    
    @Column(length = 100)
    private String material;
    
    @Column(length = 100)
    private String style;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> baseImages = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean isActive = false; // Mặc định chờ admin duyệt
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TemplateCustomZone> customZones = new ArrayList<>();
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<OrderTemplateDetail> orderTemplateDetails = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
