package org.example.catholicsouvenircustomorder.specification;

import jakarta.persistence.criteria.Join;
import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> hasCategory(UUID category) {
        return (root, query, cb) ->
                category == null ? null :
                        cb.equal(root.get("category").get("categoryId"), category);
    }

    public static Specification<Product> hasTags(List<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) return null;

            query.distinct(true);
            Join<Object, Object> tagJoin = root.join("tags");

            return tagJoin.get("name").in(tags);
        };
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;

            if (min != null && max != null)
                return cb.between(root.get("productPrice"), min, max);

            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("productPrice"), min);

            return cb.lessThanOrEqualTo(root.get("productPrice"), max);
        };
    }
}