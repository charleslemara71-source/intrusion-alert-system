package com.zuvo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/**
 * Cloud AI moderation using OpenAI's free moderation endpoint (omni-moderation-latest).
 * If the key is missing or the service is down, content is allowed (fail-open) and a warning is logged.
 */
@Service
public class ModerationService {
    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    private final String apiKey;
    private final RestClient client = RestClient.builder().baseUrl("https://api.openai.com").build();

    public ModerationService(@Value("${moderation.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    private boolean enabled() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("PUT_");
    }

    public boolean isTextFlagged(String text) {
        if (text == null || text.isBlank()) return false;
        return call(text);
    }

    public boolean isImageFlagged(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;
        return call(List.of(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))));
    }

    @SuppressWarnings("unchecked")
    private boolean call(Object input) {
        if (!enabled()) return false;
        try {
            Map<String, Object> res = client.post().uri("/v1/moderations")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", "omni-moderation-latest", "input", input))
                    .retrieve().body(Map.class);
            if (res == null) return false;
            List<Map<String, Object>> results = (List<Map<String, Object>>) res.get("results");
            return results != null && !results.isEmpty() && Boolean.TRUE.equals(results.get(0).get("flagged"));
        } catch (Exception e) {
            log.warn("Moderation API unavailable, allowing content: {}", e.getMessage());
            return false;
        }
    }
}
