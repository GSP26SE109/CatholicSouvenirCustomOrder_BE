package org.example.catholicsouvenircustomorder.Utils.Helper;

import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "images", source = "images")
    @Mapping(target = "artisanId", source = "artisan.artisanUuid")
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
}
