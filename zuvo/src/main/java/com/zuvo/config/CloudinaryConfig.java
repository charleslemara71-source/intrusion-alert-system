package com.zuvo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary(@Value("${cloudinary.cloud-name:}") String cloud,
                                 @Value("${cloudinary.api-key:}") String key,
                                 @Value("${cloudinary.api-secret:}") String secret) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloud, "api_key", key, "api_secret", secret, "secure", true));
    }
}
