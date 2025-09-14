package com.hydrogarden.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtKeyCache {
    private volatile Map<String, PublicKey> kidToKeyMap;

    public synchronized void setPublicKeys(Map<String, PublicKey> kidPubkeyMap) {
        this.kidToKeyMap = new HashMap<>(kidPubkeyMap);
        StringBuilder kids = new StringBuilder();
        kidToKeyMap.forEach((kid,_) -> kids.append("%s, ".formatted(kid)));

        log.info("JwtKeyCache has been updated. kid=[{}]",kids.toString());
    }

    public synchronized Map<String, PublicKey> getPublicKeys() {
        return this.kidToKeyMap;
    }

    public PublicKey getByKid(String kid) {
        return kidToKeyMap.get(kid);
    }
}
