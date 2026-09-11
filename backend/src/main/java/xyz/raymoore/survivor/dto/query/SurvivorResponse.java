package xyz.raymoore.survivor.dto.query;

import java.util.List;

public record SurvivorResponse(String spreadsheetTitle, List<WeekView> weeks) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String spreadsheetTitle = "";
        private List<WeekView> weeks = List.of();

        public Builder spreadsheetTitle(String value) {
            spreadsheetTitle = value;
            return this;
        }

        public Builder weeks(List<WeekView> value) {
            weeks = value;
            return this;
        }

        public SurvivorResponse build() {
            return new SurvivorResponse(spreadsheetTitle, weeks);
        }
    }

    public record WeekView(int number, String label, boolean current, List<ContestantView> contestants) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int number;
            private String label = "";
            private boolean current;
            private List<ContestantView> contestants = List.of();

            public Builder number(int value) {
                number = value;
                return this;
            }

            public Builder label(String value) {
                label = value;
                return this;
            }

            public Builder current(boolean value) {
                current = value;
                return this;
            }

            public Builder contestants(List<ContestantView> value) {
                contestants = value;
                return this;
            }

            public WeekView build() {
                return new WeekView(number, label, current, contestants);
            }
        }
    }

    public record ContestantView(String name, String pick, boolean selectionExists) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name = "";
            private String pick = "";
            private boolean selectionExists;

            public Builder name(String value) {
                name = value;
                return this;
            }

            public Builder pick(String value) {
                pick = value;
                return this;
            }

            public Builder selectionExists(boolean value) {
                selectionExists = value;
                return this;
            }

            public ContestantView build() {
                return new ContestantView(name, pick, selectionExists);
            }
        }
    }
}
