package xyz.raymoore.madisonsc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import xyz.raymoore.madisonsc.domain.Contestant;
import xyz.raymoore.madisonsc.domain.Pick;
import xyz.raymoore.madisonsc.dto.query.LatestWeekResponse;
import xyz.raymoore.madisonsc.dto.query.WeeklyPicksResponse;
import xyz.raymoore.madisonsc.dto.query.YearlyPicksResponse;
import xyz.raymoore.madisonsc.repository.ContestantRepository;
import xyz.raymoore.madisonsc.repository.PickRepository;

@Service
public class PickQueryService {

    private static final int FIRST_SEASON_START_YEAR = 2014;

    private final ContestantRepository contestantRepository;
    private final PickRepository pickRepository;

    public PickQueryService(ContestantRepository contestantRepository, PickRepository pickRepository) {
        this.contestantRepository = contestantRepository;
        this.pickRepository = pickRepository;
    }

    public LatestWeekResponse findLatestWeek() {
        return pickRepository.findMostRecent()
                .map(pick -> LatestWeekResponse.builder()
                        .year(pick.year())
                        .week(pick.week())
                        .build())
                .orElseGet(() -> LatestWeekResponse.builder().build());
    }

    public WeeklyPicksResponse findWeeklyPicks(int year, int week) {
        List<Pick> yearPicks = pickRepository.findByYear(year);
        Map<UUID, Contestant> contestantsById = loadContestants(yearPicks);
        List<WeeklyPicksResponse.ContestantView> contestantViews = buildContestantViews(
                yearPicks,
                contestantsById,
                week
        );

        return WeeklyPicksResponse.builder()
                .year(year)
                .week(week)
                .seasonLabel(seasonLabel(year))
                .availableYears(pickRepository.findAvailableYears())
                .contestants(contestantViews)
                .build();
    }

    public YearlyPicksResponse findYearlyPicks(int year) {
        List<Pick> yearPicks = pickRepository.findByYear(year);
        Map<UUID, Contestant> contestantsById = loadContestants(yearPicks);
        List<YearlyPicksResponse.WeekView> weeks = new ArrayList<>();

        for (int week = 1; week <= 18; week++) {
            weeks.add(YearlyPicksResponse.WeekView.builder()
                    .week(week)
                    .contestants(buildContestantViews(yearPicks, contestantsById, week))
                    .build());
        }

        return YearlyPicksResponse.builder()
                .year(year)
                .seasonLabel(seasonLabel(year))
                .weeks(List.copyOf(weeks))
                .build();
    }

    private List<WeeklyPicksResponse.ContestantView> buildContestantViews(
            List<Pick> yearPicks,
            Map<UUID, Contestant> contestantsById,
            int week
    ) {
        List<Candidate> candidates = buildCandidates(yearPicks, contestantsById, week);
        candidates.sort(candidateComparator());

        List<WeeklyPicksResponse.ContestantView> contestantViews = new ArrayList<>();
        Candidate previous = null;
        int previousRank = 0;

        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            int rank = previous != null && comparePercentage(candidate.cumulativeRecord(), previous.cumulativeRecord()) == 0
                    ? previousRank
                    : index + 1;

            contestantViews.add(candidate.toView(rank));
            previous = candidate;
            previousRank = rank;
        }

        return List.copyOf(contestantViews);
    }

    private static String seasonLabel(int year) {
        int seasonStartYear = FIRST_SEASON_START_YEAR + year - 1;
        return seasonStartYear + "-" + (seasonStartYear + 1) + " NFL season";
    }

    private Map<UUID, Contestant> loadContestants(List<Pick> picks) {
        Map<UUID, Contestant> contestantsById = new HashMap<>();
        List<UUID> contestantIds = picks.stream().map(Pick::contestantId).distinct().toList();
        contestantRepository.findAllById(contestantIds)
                .forEach(contestant -> contestantsById.put(contestant.contestantId(), contestant));
        return contestantsById;
    }

    private List<Candidate> buildCandidates(
            List<Pick> yearPicks,
            Map<UUID, Contestant> contestantsById,
            int selectedWeek
    ) {
        List<Candidate> candidates = new ArrayList<>();

        for (Contestant contestant : contestantsById.values()) {
            List<Pick> contestantPicks = yearPicks.stream()
                    .filter(pick -> pick.contestantId().equals(contestant.contestantId()))
                    .toList();
            List<Pick> cumulativePicks = contestantPicks.stream()
                    .filter(pick -> pick.week() <= selectedWeek)
                    .toList();
            List<Pick> weeklyPicks = contestantPicks.stream()
                    .filter(pick -> pick.week() == selectedWeek)
                    .toList();

            candidates.add(new Candidate(
                    contestant,
                    calculateRecord(cumulativePicks),
                    calculateRecord(weeklyPicks),
                    weeklyPicks.stream().map(PickQueryService::toPickView).toList()
            ));
        }

        return candidates;
    }

    private static Comparator<Candidate> candidateComparator() {
        return (left, right) -> {
            int percentageComparison = comparePercentage(right.cumulativeRecord(), left.cumulativeRecord());
            if (percentageComparison != 0) {
                return percentageComparison;
            }

            int nameComparison = String.CASE_INSENSITIVE_ORDER.compare(
                    left.contestant().name(),
                    right.contestant().name()
            );
            if (nameComparison != 0) {
                return nameComparison;
            }

            return left.contestant().contestantId().compareTo(right.contestant().contestantId());
        };
    }

    private static WeeklyPicksResponse.PickView toPickView(Pick pick) {
        return WeeklyPicksResponse.PickView.builder()
                .team(pick.team())
                .underdog(pick.underdog())
                .line(pick.line())
                .result(normalizeResult(pick.result()))
                .build();
    }

    private static String normalizeResult(String result) {
        if (result == null) {
            return null;
        }

        String normalized = result.toLowerCase(Locale.ROOT);
        if (!normalized.equals("win") && !normalized.equals("loss") && !normalized.equals("tie")) {
            throw new IllegalStateException("Unknown pick result stored in the database: " + result);
        }
        return normalized;
    }

    private static RecordTotals calculateRecord(List<Pick> picks) {
        int wins = 0;
        int losses = 0;
        int ties = 0;

        for (Pick pick : picks) {
            String result = normalizeResult(pick.result());
            if (result == null) {
                continue;
            }

            switch (result) {
                case "win" -> wins++;
                case "loss" -> losses++;
                case "tie" -> ties++;
                default -> throw new IllegalStateException("Unexpected normalized result: " + result);
            }
        }

        return new RecordTotals(wins, losses, ties);
    }

    private static int comparePercentage(RecordTotals left, RecordTotals right) {
        if (left.completedGames() == 0 && right.completedGames() == 0) {
            return 0;
        }
        if (left.completedGames() == 0) {
            return Long.compare(0, right.scoreNumerator());
        }
        if (right.completedGames() == 0) {
            return Long.compare(left.scoreNumerator(), 0);
        }

        return Long.compare(
                left.scoreNumerator() * right.completedGames(),
                right.scoreNumerator() * left.completedGames()
        );
    }

    private record RecordTotals(int wins, int losses, int ties) {

        int completedGames() {
            return wins + losses + ties;
        }

        long scoreNumerator() {
            return 2L * wins + ties;
        }

        BigDecimal winPercentage() {
            if (completedGames() == 0) {
                return BigDecimal.ZERO.setScale(4);
            }
            return BigDecimal.valueOf(scoreNumerator())
                    .divide(BigDecimal.valueOf(2L * completedGames()), 4, RoundingMode.HALF_UP);
        }

        WeeklyPicksResponse.RecordView toView() {
            return WeeklyPicksResponse.RecordView.builder()
                    .wins(wins)
                    .losses(losses)
                    .ties(ties)
                    .build();
        }
    }

    private record Candidate(
            Contestant contestant,
            RecordTotals cumulativeRecord,
            RecordTotals weeklyRecord,
            List<WeeklyPicksResponse.PickView> picks
    ) {

        WeeklyPicksResponse.ContestantView toView(int rank) {
            return WeeklyPicksResponse.ContestantView.builder()
                    .contestantId(contestant.contestantId())
                    .name(contestant.name())
                    .rank(rank)
                    .cumulativeWinPercentage(cumulativeRecord.winPercentage())
                    .cumulativeRecord(cumulativeRecord.toView())
                    .weeklyRecord(weeklyRecord.toView())
                    .picks(picks)
                    .build();
        }
    }
}
