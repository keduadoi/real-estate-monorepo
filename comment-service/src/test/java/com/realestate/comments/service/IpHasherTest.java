package com.realestate.comments.service;

import com.realestate.comments.config.CommentProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class IpHasherTest {

    private final CommentProperties props = props("test-secret");
    private final IpHasher hasher = new IpHasher(props);

    @Test
    void hashesAreDeterministicAndHex() {
        String h1 = hasher.hash("1.2.3.4");
        String h2 = hasher.hash("1.2.3.4");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertTrue(h1.matches("[a-f0-9]{64}"));
    }

    @Test
    void differentIpsHashDifferently() {
        assertNotEquals(hasher.hash("1.2.3.4"), hasher.hash("1.2.3.5"));
    }

    @Test
    void differentSecretsHashDifferently() {
        IpHasher other = new IpHasher(props("other-secret"));
        assertNotEquals(hasher.hash("1.2.3.4"), other.hash("1.2.3.4"));
    }

    @Test
    void nullAndBlankReturnNull() {
        assertNull(hasher.hash(null));
        assertNull(hasher.hash(""));
    }

    @Test
    void extractClientIpPrefersForwardedFor() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 10.0.0.1");
        Mockito.when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        assertEquals("9.9.9.9", hasher.extractClientIp(req));
    }

    @Test
    void extractClientIpFallsBackToRemoteAddr() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        assertEquals("127.0.0.1", hasher.extractClientIp(req));
    }

    private static CommentProperties props(String secret) {
        CommentProperties p = new CommentProperties();
        p.setIpHashSecret(secret);
        return p;
    }
}
