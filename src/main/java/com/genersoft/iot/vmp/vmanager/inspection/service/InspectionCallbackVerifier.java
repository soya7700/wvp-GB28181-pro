package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

@Component
public class InspectionCallbackVerifier {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String NONCE_KEY_PREFIX = "WVP:AI:INSPECTION:CALLBACK:NONCE:";

    private final InspectionProperties properties;
    private final StringRedisTemplate redis;

    public InspectionCallbackVerifier(InspectionProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    public void verify(String timestamp, String nonce, String signature, Long taskId,
                       String action, String callbackId) {
        String secret = properties.getCallbackToken();
        if (secret == null || secret.isEmpty()) {
            return;
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (RuntimeException exception) {
            throw unauthorized();
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - requestTime);
        if (skew > properties.getCallbackMaxSkewSeconds()
                || nonce == null || nonce.trim().isEmpty()
                || signature == null || signature.trim().isEmpty()) {
            throw unauthorized();
        }
        String expected = sign(secret, timestamp, nonce, taskId, action, callbackId);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized();
        }
        Boolean firstUse = redis.opsForValue().setIfAbsent(
                NONCE_KEY_PREFIX + nonce, "1", Duration.ofSeconds(properties.getCallbackNonceSeconds()));
        if (!Boolean.TRUE.equals(firstUse)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI回调已被处理");
        }
    }

    public String sign(String secret, String timestamp, String nonce, Long taskId,
                       String action, String callbackId) {
        String canonical = timestamp + "\n" + nonce + "\n" + taskId + "\n"
                + action + "\n" + (callbackId == null ? "" : callbackId);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成AI回调签名", exception);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AI回调签名无效");
    }
}
