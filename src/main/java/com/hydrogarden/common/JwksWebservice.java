package com.hydrogarden.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwksWebservice {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final JwtKeyCache jwtKeyCache;
    private final String jwksUrl;


    public JwksWebservice(JwtKeyCache jwtKeyCache, @Value("${hydrogarden.clerk.jwks.url}") String jwksUrl) {
        this.jwtKeyCache = jwtKeyCache;
        this.jwksUrl = jwksUrl;
    }

    @PostConstruct
    public void init() throws Exception {
        refreshJwks();
    }

    @Scheduled(fixedDelayString = "${hydrogarden.clerk.jwks.refresh-ms:3600000}")
    public void refreshJwks() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(jwksUrl, String.class);
        Map<String, Object> jwksMap = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        List<Map<String, String>> keys = (List<Map<String, String>>) jwksMap.get("keys");

        Map<String, PublicKey> kidToKeyMap = new HashMap<>();
        for (Map<String, String> key : keys) {
            try {
                String kid = key.get("kid");
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n")));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e")));
                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(spec);

                kidToKeyMap.put(kid, publicKey);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse key " + key.get("kid"), e);
            }
        }

        this.jwtKeyCache.setPublicKeys(kidToKeyMap);
    }
}
