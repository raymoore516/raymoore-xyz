package xyz.raymoore.madisonsc.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import xyz.raymoore.madisonsc.dto.query.ContestantPicksResponse;
import xyz.raymoore.madisonsc.service.PickQueryService;

@RestController
@RequestMapping("/api/madisonsc/contestants")
public class ContestantPicksController {

    private final PickQueryService pickQueryService;

    public ContestantPicksController(PickQueryService pickQueryService) {
        this.pickQueryService = pickQueryService;
    }

    @GetMapping("/{contestant}/picks")
    public ContestantPicksResponse getContestantPicks(@PathVariable("contestant") UUID contestantId) {
        return pickQueryService.findContestantPicks(contestantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contestant not found"));
    }
}
