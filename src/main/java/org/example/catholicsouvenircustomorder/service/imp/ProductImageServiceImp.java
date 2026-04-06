package org.example.catholicsouvenircustomorder.service.imp;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductImagesRequest;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.repository.ProductImageRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.ProductImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImp implements ProductImageService {

    private final Cloudinary cloudinary;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    @Override
    public void updateImages(UUID productId, UpdateProductImagesRequest request) {

        if (request == null) return;
        Product product = productRepository.findById(productId).get();
        if (request.getDeleteImageIds() != null) {

            product.getImages().removeIf(image -> {

                if (request.getDeleteImageIds().contains(image.getId())) {
                    try {
                        cloudinary.uploader().destroy(image.getPublicId(), ObjectUtils.emptyMap());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }

                return false;
            });
        }
        if (request.getNewImages() != null) {
            try {
                addNewImage(productId, request.getNewImages());
            } catch (IOException e) {
                throw new RuntimeException("Upload hình ảnh thất bại");
            }
        }
    }

    public void addNewImage(UUID productId, List<MultipartFile> images) throws IOException {
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                try {
                    Map uploadResult = cloudinary.uploader().upload(
                            image.getBytes(),
                            ObjectUtils.emptyMap()
                    );

                    String imageUrl = uploadResult.get("secure_url").toString();
                    String publicId = uploadResult.get("public_id").toString();

                    ProductImage productImage = new ProductImage();
                    productImage.setImage_url(imageUrl);
                    productImage.setPublicId(publicId);
                    productImage.setProduct(productRepository.findById(productId).get());

                    productImageRepository.save(productImage);

                } catch (IOException e) {
                    throw new RuntimeException("Upload hình ảnh thất bại");
                }
            }
        }
    }
}
