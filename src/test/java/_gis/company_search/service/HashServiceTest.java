package _gis.company_search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class HashServiceTest {

    private HashService hashService;

    @BeforeEach
    void setUp() {
        hashService = new HashService("testKey");
    }

    @Test
    void hashString_ShouldBeDifferent_ForDifferentInputs() throws NoSuchAlgorithmException {
        String hash1 = hashService.hashString("password1");
        String hash2 = hashService.hashString("password2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashString_ShouldBeDeterministic_ForSameInput() throws NoSuchAlgorithmException {
        String hash1 = hashService.hashString("sameInput");
        String hash2 = hashService.hashString("sameInput");

        assertEquals(hash1, hash2);
    }
}