package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.ProductResponse;
import com.manguonmo.popworld.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "seriesName", source = "series.name")
    @Mapping(target = "mainImageUrl", expression = "java(product.getMainImageUrl())")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
