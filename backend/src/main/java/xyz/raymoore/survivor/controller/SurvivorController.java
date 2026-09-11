package xyz.raymoore.survivor.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;
import xyz.raymoore.survivor.service.SurvivorService;

@RestController
public class SurvivorController {

    private final SurvivorService survivorService;

    public SurvivorController(SurvivorService survivorService) {
        this.survivorService = survivorService;
    }

    @GetMapping("/api/survivor")
    public ResponseEntity<SurvivorResponse> getSurvivorLeague() throws IOException, GeneralSecurityException {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(survivorService.loadCurrentPicks());
    }
}
