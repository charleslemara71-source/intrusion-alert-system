package com.zuvo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuvo.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates LiveKit access tokens (a signed JWT). The API secret stays on the server;
 * the browser only receives the short-lived token and the wss:// URL.
 */
@Service
public class LiveKitTokenService {
    private final String url, apiKey, apiSecret;
    private final ObjectMapper mapper = new ObjectMapper();

    public LiveKitTokenService(@Value("${livekit.url:}") String url,
                               @Value("${livekit.api-key:}") String apiKey,
                               @Value("${livekit.api-secret:}") String apiSecret) {
        this.url = url;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String url() { return url; }

    public String token(String room, String identity, String name, boolean canPublish, boolean roomAdmin) {
        if (apiKey.isBlank() || apiSecret.isBlank() || apiKey.startsWith("PUT_"))
            throw new ApiException(500, "LiveKit is not configured on the server.");
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> video = new LinkedHashMap<>();
            video.put("room", room);
            video.put("roomJoin", true);
            video.put("canPublish", canPublish);
            video.put("canSubscribe", true);
            video.put("canPublishData", true);
            if (roomAdmin) video.put("roomAdmin", true);

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", apiKey);
            claims.put("sub", identity);
            claims.put("name", name);
            claims.put("nbf", now - 10);
            claims.put("exp", now + 2 * 3600);
            claims.put("video", video);

            String header = b64(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = b64(mapper.writeValueAsBytes(claims));
            String unsigned = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return unsigned + "." + b64(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "Could not create the call token.");
        }
    }

    private String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
