package org.example.catholicsouvenircustomorder.service.Helper;

import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductCreateDTO dto);

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
    void updateProductFromDto(ProductCreateDTO dto,
                              @MappingTarget Product entity);
}
