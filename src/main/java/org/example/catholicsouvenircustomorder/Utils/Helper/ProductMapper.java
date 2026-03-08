package org.example.catholicsouvenircustomorder.Utils.Helper;

import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "images", source = "images")
    @Mapping(target = "artisanId", expression = "java(getArtisanId(product))")
    @Mapping(target = "artisanName", expression = "java(getArtisanName(product))")
    ProductResponse toResponse(Product product);

    @Mapping(target = "artisanId", source = "artisan.artisanUuid")
    List<ProductResponse> toResponseList(List<Product> products);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images", ignore = true)
    void updateProductFromDto(UpdateProductRequest dto,
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
