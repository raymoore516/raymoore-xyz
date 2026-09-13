package xyz.raymoore.survivor.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;

@Service
public class SurvivorCache {

    private final SurvivorService survivorService;
    private volatile SurvivorResponse currentPicks;

    public SurvivorCache(SurvivorService survivorService) {
        this.survivorService = survivorService;
    }

    public SurvivorResponse getCurrentPicks(boolean refresh) throws IOException, GeneralSecurityException {
        if (refresh) {
            refresh();
        }
        return currentPicks;
    }

    @PostConstruct
    @Scheduled(fixedRate = 60, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
    public synchronized void refresh() throws IOException, GeneralSecurityException {
        currentPicks = survivorService.loadCurrentPicks();
    }
}
