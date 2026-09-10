package xyz.raymoore.madisonsc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.raymoore.madisonsc.dto.query.RootResponse;
import xyz.raymoore.madisonsc.service.PickQueryService;

@RestController
public class RootController {

    private final PickQueryService pickQueryService;

    public RootController(PickQueryService pickQueryService) {
        this.pickQueryService = pickQueryService;
    }

    @GetMapping("/api/madisonsc")
    public RootResponse getRootSummary() {
        return pickQueryService.findRootSummary();
    }
}
