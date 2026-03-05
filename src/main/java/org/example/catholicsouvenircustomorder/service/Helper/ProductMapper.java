package org.example.catholicsouvenircustomorder.service.Helper;

import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductCreateDTO dto);

    @Mapping(target = "images", source = "images")
    @Mapping(target = "artisanId", expression = "java(getArtisanId(product))")
    @Mapping(target = "artisanName", expression = "java(getArtisanName(product))")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    void updateProductFromDto(ProductCreateDTO dto,
                              @MappingTarget Product entity);

    @Mapping(source = "image_url", target = "imageUrl")
    ProductImageResponse toImageResponse(ProductImage image);

    List<ProductImageResponse> toImageResponseList(List<ProductImage> images);
    
    // Helper method để lấy artisan ID
    default UUID getArtisanId(Product product) {
        if (product.getAccount() == null) {
            return null;
        }
        
        // Nếu account có artisan profile, lấy artisanUuid
        if (product.getAccount().getArtisanProfile() != null) {
            return product.getAccount().getArtisanProfile().getArtisanUuid();
        }
        
        // Fallback: dùng accountId
        return product.getAccount().getAccountId();
    }
    
    // Helper method để lấy artisan name
    default String getArtisanName(Product product) {
        if (product.getAccount() == null) {
            return null;
        }
        
        // Nếu account có artisan profile, lấy artisanName từ đó
        if (product.getAccount().getArtisanProfile() != null) {
            return product.getAccount().getArtisanProfile().getArtisanName();
        }
        
        // Fallback: dùng fullName của account
        return product.getAccount().getFullName();
    }
}
