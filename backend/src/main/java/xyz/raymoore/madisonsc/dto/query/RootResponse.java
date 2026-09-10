package xyz.raymoore.madisonsc.dto.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RootResponse(List<YearView> years) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<YearView> years = List.of();

        public Builder years(List<YearView> value) {
            years = value;
            return this;
        }

        public RootResponse build() {
            return new RootResponse(years);
        }
    }

    public record YearView(
            int year,
            String seasonLabel,
            int latestWeek,
            List<StandingView> standings
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int year;
            private String seasonLabel = "";
            private int latestWeek;
            private List<StandingView> standings = List.of();

            public Builder year(int value) {
                year = value;
                return this;
            }

            public Builder seasonLabel(String value) {
                seasonLabel = value;
                return this;
            }

            public Builder latestWeek(int value) {
                latestWeek = value;
                return this;
            }

            public Builder standings(List<StandingView> value) {
                standings = value;
                return this;
            }

            public YearView build() {
                return new YearView(year, seasonLabel, latestWeek, standings);
            }
        }
    }

    public record StandingView(
            UUID contestantId,
            String name,
            int rank,
            BigDecimal cumulativeWinPercentage,
            RecordView cumulativeRecord
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private UUID contestantId;
            private String name = "";
            private int rank;
            private BigDecimal cumulativeWinPercentage = BigDecimal.ZERO;
            private RecordView cumulativeRecord = new RecordView(0, 0, 0);

            public Builder contestantId(UUID value) {
                contestantId = value;
                return this;
            }

            public Builder name(String value) {
                name = value;
                return this;
            }

            public Builder rank(int value) {
                rank = value;
                return this;
            }

            public Builder cumulativeWinPercentage(BigDecimal value) {
                cumulativeWinPercentage = value;
                return this;
            }

            public Builder cumulativeRecord(RecordView value) {
                cumulativeRecord = value;
                return this;
            }

            public StandingView build() {
                return new StandingView(contestantId, name, rank, cumulativeWinPercentage, cumulativeRecord);
            }
        }
    }

    public record RecordView(int wins, int losses, int ties) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int wins;
            private int losses;
            private int ties;

            public Builder wins(int value) {
                wins = value;
                return this;
            }

            public Builder losses(int value) {
                losses = value;
                return this;
            }

            public Builder ties(int value) {
                ties = value;
                return this;
            }

            public RecordView build() {
                return new RecordView(wins, losses, ties);
            }
        }
    }
}
