package org.example.catholicsouvenircustomorder.service.Helper;

import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductCreateDTO dto);

    @Mapping(target = "images", source = "images")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    void updateProductFromDto(ProductCreateDTO dto,
                              @MappingTarget Product entity);

    @Mapping(source = "image_url", target = "imageUrl")
    ProductImageResponse toImageResponse(ProductImage image);

    List<ProductImageResponse> toImageResponseList(List<ProductImage> images);
}
