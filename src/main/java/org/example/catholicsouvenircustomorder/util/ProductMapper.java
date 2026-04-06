package org.example.catholicsouvenircustomorder.util;

import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.model.Tag;
import org.mapstruct.*;

import java.util.List;

import java.util.UUID;
@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "images", source = "images")
    @Mapping(target = "artisanId", source = "artisan.artisanUuid")
    @Mapping(target = "artisanName", source = "artisan.artisanName")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "tags", source = "tags")
    ProductResponse toResponse(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "tags", ignore = true)       
    @Mapping(target = "category", ignore = true)
    void updateProductFromDto(UpdateProductRequest dto,
                              @MappingTarget Product entity);

    ProductImageResponse toImageResponse(ProductImage image);

    List<ProductImageResponse> toImageResponseList(List<ProductImage> images);

    default List<String> map(List<Tag> tags) {
        if (tags == null) return null;
        return tags.stream()
                .map(Tag::getName)
                .toList();
    }
}
