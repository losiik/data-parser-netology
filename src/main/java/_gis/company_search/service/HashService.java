package _gis.company_search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class HashService {
    private final String hashKey;

    HashService(
            @Value("${hashKey}") String hashKey
    ){
        this.hashKey = hashKey;
    }


    public String hashString(String input) throws NoSuchAlgorithmException {
        String salt_input = input + hashKey;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hashBytes = digest.digest(salt_input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
