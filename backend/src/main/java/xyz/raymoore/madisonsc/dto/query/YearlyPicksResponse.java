package xyz.raymoore.madisonsc.dto.query;

import java.util.List;

public record YearlyPicksResponse(
        int year,
        String seasonLabel,
        List<WeekView> weeks
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int year;
        private String seasonLabel = "";
        private List<WeekView> weeks = List.of();

        public Builder year(int value) {
            year = value;
            return this;
        }

        public Builder seasonLabel(String value) {
            seasonLabel = value;
            return this;
        }

        public Builder weeks(List<WeekView> value) {
            weeks = value;
            return this;
        }

        public YearlyPicksResponse build() {
            return new YearlyPicksResponse(year, seasonLabel, weeks);
        }
    }

    public record WeekView(
            int week,
            List<WeeklyPicksResponse.ContestantView> contestants
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int week;
            private List<WeeklyPicksResponse.ContestantView> contestants = List.of();

            public Builder week(int value) {
                week = value;
                return this;
            }

            public Builder contestants(List<WeeklyPicksResponse.ContestantView> value) {
                contestants = value;
                return this;
            }

            public WeekView build() {
                return new WeekView(week, contestants);
            }
        }
    }
}
