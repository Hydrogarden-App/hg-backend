package com.hydrogarden;

import com.hydrogarden.common.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("test")
@RequiredArgsConstructor
public class SecuredEndpoint {

    @GetMapping
    public ResponseEntity<String> test(@AuthenticationPrincipal UserId userId) {
        return ResponseEntity.ok("Hello, %s".formatted(userId.getValue()));
    }
}
