package org.example.catholicsouvenircustomorder.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ComplaintType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class ImageHelper {
    private final Cloudinary cloudinary;
    public List<String> uploadImage(List<MultipartFile> images) {
        List<String> evidences = new ArrayList<>();
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                try {
                    Map uploadResult = cloudinary.uploader().upload(
                            image.getBytes(),
                            ObjectUtils.emptyMap()
                    );

                    String imageUrl = uploadResult.get("secure_url").toString();
                    evidences.add(imageUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Upload hình ảnh thất bại");
                }
            }
        }
        return evidences;
    }
}
