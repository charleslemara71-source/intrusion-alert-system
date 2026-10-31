package com.zuvo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.zuvo.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/** Uploads photos/videos to Cloudinary. Only the returned URL is stored in MySQL. */
@Service
public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    public record Uploaded(String url, String publicId, String resourceType) {}

    private final Cloudinary cloudinary;
    private final String cloudName;

    public MediaService(Cloudinary cloudinary, @Value("${cloudinary.cloud-name:}") String cloudName) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
    }

    /** kind: "image", "video" or "any" */
    public Uploaded upload(MultipartFile file, String folder, String kind) {
        if (cloudName == null || cloudName.isBlank() || cloudName.startsWith("PUT_"))
            throw new ApiException(500, "Media storage (Cloudinary) is not configured on the server.");
        if (file == null || file.isEmpty()) throw ApiException.bad("Choose a file to upload.");
        String type = file.getContentType() == null ? "" : file.getContentType();
        boolean img = type.startsWith("image/"), vid = type.startsWith("video/");
        if (kind.equals("image") && !img) throw ApiException.bad("Please choose an image file.");
        if (kind.equals("video") && !vid) throw ApiException.bad("Please choose a video file.");
        if (!img && !vid) throw ApiException.bad("Only photos and videos are allowed.");
        try {
            Map<?, ?> r = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto", "folder", "zuvo/" + folder));
            return new Uploaded((String) r.get("secure_url"), (String) r.get("public_id"), (String) r.get("resource_type"));
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new ApiException(502, "Upload failed. Check your internet connection and try again.");
        }
    }

    public void delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type",
                    resourceType == null ? "image" : resourceType));
        } catch (Exception e) {
            log.warn("Could not delete media {}: {}", publicId, e.getMessage());
        }
    }
}
