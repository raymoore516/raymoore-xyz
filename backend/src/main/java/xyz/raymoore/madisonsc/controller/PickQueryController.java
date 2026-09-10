package xyz.raymoore.madisonsc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import xyz.raymoore.madisonsc.dto.query.LatestWeekResponse;
import xyz.raymoore.madisonsc.dto.query.WeeklyPicksResponse;
import xyz.raymoore.madisonsc.dto.query.YearlyPicksResponse;
import xyz.raymoore.madisonsc.service.PickQueryService;

@RestController
@RequestMapping("/api/madisonsc/picks")
public class PickQueryController {

    private final PickQueryService pickQueryService;

    public PickQueryController(PickQueryService pickQueryService) {
        this.pickQueryService = pickQueryService;
    }

    @GetMapping("/latest")
    public LatestWeekResponse getLatestWeek() {
        return pickQueryService.findLatestWeek();
    }

    @GetMapping("/{year}")
    public YearlyPicksResponse getYearlyPicks(@PathVariable("year") int year) {
        validateYear(year);
        return pickQueryService.findYearlyPicks(year);
    }

    @GetMapping("/{year}/{week}")
    public WeeklyPicksResponse getWeeklyPicks(
            @PathVariable("year") int year,
            @PathVariable("week") int week
    ) {
        validateYear(year);
        if (week < 1 || week > 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Week must be between 1 and 18");
        }
        return pickQueryService.findWeeklyPicks(year, week);
    }

    private static void validateYear(int year) {
        if (year < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be positive");
        }
    }
}
