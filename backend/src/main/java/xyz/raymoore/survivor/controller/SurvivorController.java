package xyz.raymoore.survivor.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;
import xyz.raymoore.survivor.service.SurvivorCache;

@RestController
public class SurvivorController {

    private final SurvivorCache survivorCache;

    public SurvivorController(SurvivorCache survivorCache) {
        this.survivorCache = survivorCache;
    }

    @GetMapping("/api/survivor")
    public ResponseEntity<SurvivorResponse> getSurvivorLeague(
            @RequestParam(defaultValue = "false") boolean refresh)
            throws IOException, GeneralSecurityException {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(survivorCache.getCurrentPicks(refresh));
    }
}
