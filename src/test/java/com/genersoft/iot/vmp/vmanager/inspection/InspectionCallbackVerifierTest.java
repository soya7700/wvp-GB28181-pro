package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import com.genersoft.iot.vmp.vmanager.inspection.service.InspectionCallbackVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InspectionCallbackVerifierTest {
    private InspectionProperties properties;
    private ValueOperations<String, String> values;
    private InspectionCallbackVerifier verifier;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties = new InspectionProperties();
        properties.setCallbackToken("unit-test-secret");
        properties.setCallbackMaxSkewSeconds(300);
        properties.setCallbackNonceSeconds(600);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        verifier = new InspectionCallbackVerifier(properties, redis);
    }

    @Test
    void validSignatureShouldClaimNonce() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = verifier.sign(properties.getCallbackToken(), timestamp, "nonce-1",
                7L, "result", "callback-1");
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        verifier.verify(timestamp, "nonce-1", signature, 7L, "result", "callback-1");

        verify(values).setIfAbsent(startsWith("WVP:AI:INSPECTION:CALLBACK:NONCE:"),
                eq("1"), eq(Duration.ofSeconds(600)));
    }

    @Test
    void invalidSignatureShouldNotConsumeNonce() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> verifier.verify(timestamp, "nonce-2", "bad", 7L, "result", "callback-2"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verifyNoInteractions(values);
    }

    @Test
    void staleTimestampShouldBeRejected() {
        String timestamp = String.valueOf(Instant.now().minusSeconds(301).getEpochSecond());
        String signature = verifier.sign(properties.getCallbackToken(), timestamp, "nonce-3",
                7L, "complete", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> verifier.verify(timestamp, "nonce-3", signature, 7L, "complete", ""));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verifyNoInteractions(values);
    }

    @Test
    void repeatedNonceShouldConflict() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = verifier.sign(properties.getCallbackToken(), timestamp, "nonce-4",
                7L, "complete", "");
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> verifier.verify(timestamp, "nonce-4", signature, 7L, "complete", ""));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }
}
