package xyz.raymoore.madisonsc.dto.query;

import java.util.List;
import java.util.UUID;

public record ContestantPicksResponse(
        UUID contestantId,
        String name,
        List<Integer> years,
        List<TeamView> teams
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID contestantId;
        private String name = "";
        private List<Integer> years = List.of();
        private List<TeamView> teams = List.of();

        public Builder contestantId(UUID value) {
            contestantId = value;
            return this;
        }

        public Builder name(String value) {
            name = value;
            return this;
        }

        public Builder years(List<Integer> value) {
            years = value;
            return this;
        }

        public Builder teams(List<TeamView> value) {
            teams = value;
            return this;
        }

        public ContestantPicksResponse build() {
            return new ContestantPicksResponse(contestantId, name, years, teams);
        }
    }

    public record TeamView(
            String team,
            RecordView cumulativeRecord,
            List<YearRecordView> yearlyRecords
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String team = "";
            private RecordView cumulativeRecord = new RecordView(0, 0, 0);
            private List<YearRecordView> yearlyRecords = List.of();

            public Builder team(String value) {
                team = value;
                return this;
            }

            public Builder cumulativeRecord(RecordView value) {
                cumulativeRecord = value;
                return this;
            }

            public Builder yearlyRecords(List<YearRecordView> value) {
                yearlyRecords = value;
                return this;
            }

            public TeamView build() {
                return new TeamView(team, cumulativeRecord, yearlyRecords);
            }
        }
    }

    public record YearRecordView(int year, RecordView record) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int year;
            private RecordView record = new RecordView(0, 0, 0);

            public Builder year(int value) {
                year = value;
                return this;
            }

            public Builder record(RecordView value) {
                record = value;
                return this;
            }

            public YearRecordView build() {
                return new YearRecordView(year, record);
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
