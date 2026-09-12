package xyz.raymoore.survivor.service;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ColorStyle;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.ContestantView;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.PickStatus;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.WeekView;

/** Converts a fresh Sheets snapshot into weekly standings without retaining state between requests. */
final class SurvivorSheetParser {

    private static final Pattern WEEK_HEADER = Pattern.compile("^Week\\s+([1-9]\\d*)$", Pattern.CASE_INSENSITIVE);
    // A channel must exceed BOTH others by this margin (about 13/255).
    // This recognizes pale red/green fills without treating white/gray as results.
    private static final float COLOR_BIAS = 0.05f;

    private SurvivorSheetParser() {
    }

    static SurvivorResponse parse(Spreadsheet spreadsheet) {
        List<RowData> rows = worksheetRows(spreadsheet, "Standings");
        Map<String, String> teams = teamNames(worksheetRows(spreadsheet, "Teams"));
        Map<String, Color> theme = themeColors(spreadsheet);
        List<WeekView> weeks = rows.isEmpty() ? List.of() : weeks(rows, teams, theme);
        return SurvivorResponse.builder()
                .spreadsheetTitle(spreadsheet.getProperties().getTitle())
                .weeks(weeks)
                .build();
    }

    private static List<WeekView> weeks(List<RowData> rows, Map<String, String> teams, Map<String, Color> theme) {
        RowData headers = rows.getFirst();
        int nameColumn = requiredHeader(headers, "Name");
        if (nameColumn != 0) {
            throw new IllegalStateException("The Standings worksheet's Name header must be in Column A");
        }
        int entryColumn = requiredHeader(headers, "Entry");
        int buybackColumn = requiredHeader(headers, "Buyback");
        List<RowData> entrants = rows.stream().skip(1)
                .filter(row -> checked(cell(row, entryColumn)))
                .filter(row -> !text(cell(row, nameColumn)).isBlank())
                .toList();
        List<WeekView> weeks = new ArrayList<>();
        // Index by entrant row, since names are not necessarily unique.
        Map<Integer, Integer> eliminatedIn = new HashMap<>();

        for (WeekColumn week : weekColumns(headers)) {
            List<ContestantView> contestants = new ArrayList<>();
            boolean picksHidden = false;
            boolean populated = false;

            for (int index = 0; index < entrants.size(); index++) {
                RowData row = entrants.get(index);
                CellData pickCell = cell(row, week.column());
                String teamCode = text(pickCell);
                populated |= !teamCode.isBlank();
                Integer eliminationWeek = eliminatedIn.get(index);
                boolean alreadyEliminated = eliminationWeek != null;
                PickStatus status = alreadyEliminated ? PickStatus.ELIMINATED : status(pickCell, theme);
                // Reveal once everyone eligible at the start of this week has submitted.
                // A Week 1 buyback keeps an entrant eligible; result colors do not gate reveal.
                picksHidden |= !alreadyEliminated && teamCode.isBlank();

                if (status == PickStatus.ELIMINATION
                        && !(week.number() == 1 && checked(cell(row, buybackColumn)))) {
                    eliminationWeek = week.number();
                    eliminatedIn.put(index, eliminationWeek);
                }

                contestants.add(ContestantView.builder()
                        .name(text(cell(row, nameColumn)))
                        .pick(alreadyEliminated ? "" : teamCode)
                        .selectionExists(!alreadyEliminated && !teamCode.isBlank())
                        .status(status)
                        .eliminationWeek(eliminationWeek)
                        .build());
            }

            // Still process empty columns above: a red blank can record a missed-pick loss.
            if (!populated) {
                continue;
            }

            boolean hidden = picksHidden;
            List<ContestantView> views = contestants.stream()
                    .sorted(CONTESTANT_ORDER)
                    .map(contestant -> ContestantView.builder()
                            .name(contestant.name())
                            .pick(hidden ? "" : teamName(contestant.pick(), teams))
                            .selectionExists(contestant.selectionExists())
                            .status(contestant.status())
                            .eliminationWeek(contestant.eliminationWeek())
                            .build())
                    .toList();
            weeks.add(WeekView.builder()
                    .number(week.number())
                    .label("Week " + week.number())
                    .picksHidden(hidden)
                    .contestants(views)
                    .build());
        }
        return List.copyOf(weeks);
    }

    private static final Comparator<ContestantView> CONTESTANT_ORDER = Comparator
            .comparingInt((ContestantView contestant) -> switch (contestant.status()) {
                case SURVIVAL -> 0;
                case PENDING -> 1;
                case ELIMINATION -> 2;
                case ELIMINATED -> 3;
            })
            .thenComparingInt(contestant -> contestant.status() == PickStatus.ELIMINATED
                    ? -contestant.eliminationWeek() : 0)
            .thenComparing(ContestantView::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ContestantView::name);

    private static PickStatus status(CellData cell, Map<String, Color> theme) {
        if (cell == null || cell.getEffectiveFormat() == null) {
            return PickStatus.PENDING;
        }
        var format = cell.getEffectiveFormat();
        Color color = resolveColor(format.getBackgroundColorStyle(), theme);
        if (color == null) {
            color = format.getBackgroundColor();
        }
        if (color == null) {
            return PickStatus.PENDING;
        }
        float red = channel(color.getRed());
        float green = channel(color.getGreen());
        float blue = channel(color.getBlue());
        if (green - Math.max(red, blue) >= COLOR_BIAS) {
            return PickStatus.SURVIVAL;
        }
        if (red - Math.max(green, blue) >= COLOR_BIAS) {
            return PickStatus.ELIMINATION;
        }
        return PickStatus.PENDING;
    }

    private static float channel(Float value) {
        return value == null ? 0 : value;
    }

    private static Color resolveColor(ColorStyle style, Map<String, Color> theme) {
        if (style == null) {
            return null;
        }
        return style.getRgbColor() != null ? style.getRgbColor() : theme.get(style.getThemeColor());
    }

    private static Map<String, Color> themeColors(Spreadsheet spreadsheet) {
        Map<String, Color> colors = new HashMap<>();
        var theme = spreadsheet.getProperties().getSpreadsheetTheme();
        if (theme != null && theme.getThemeColors() != null) {
            for (var pair : theme.getThemeColors()) {
                if (pair.getColor() != null && pair.getColor().getRgbColor() != null) {
                    colors.put(pair.getColorType(), pair.getColor().getRgbColor());
                }
            }
        }
        return colors;
    }

    private static List<RowData> worksheetRows(Spreadsheet spreadsheet, String title) {
        Sheet sheet = spreadsheet.getSheets().stream()
                .filter(candidate -> title.equals(candidate.getProperties().getTitle()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing worksheet: " + title));
        // The service requests each whole worksheet once, starting at A1.
        if (sheet.getData() == null || sheet.getData().isEmpty()
                || sheet.getData().getFirst().getRowData() == null) {
            return List.of();
        }
        return sheet.getData().getFirst().getRowData();
    }

    private static Map<String, String> teamNames(List<RowData> rows) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("The Teams worksheet is empty");
        }
        int codeColumn = requiredHeader(rows.getFirst(), "Team");
        int nameColumn = requiredHeader(rows.getFirst(), "Name");
        Map<String, String> teams = new HashMap<>();
        for (RowData row : rows.subList(1, rows.size())) {
            String code = text(cell(row, codeColumn));
            if (code.isBlank()) {
                continue;
            }
            String name = text(cell(row, nameColumn));
            if (name.isBlank()) {
                throw new IllegalStateException("The Teams worksheet is missing a Name for " + code);
            }
            if (teams.putIfAbsent(code.toUpperCase(Locale.ROOT), name) != null) {
                throw new IllegalStateException("The Teams worksheet contains duplicate Team code " + code);
            }
        }
        return teams;
    }

    private static String teamName(String code, Map<String, String> teams) {
        if (code.isBlank()) {
            return "";
        }
        String name = teams.get(code.toUpperCase(Locale.ROOT));
        if (name == null) {
            throw new IllegalStateException("The Teams worksheet has no Name for Team code " + code);
        }
        return name;
    }

    private static List<WeekColumn> weekColumns(RowData headers) {
        List<WeekColumn> weeks = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int column = 0; column < headers.getValues().size(); column++) {
            Matcher matcher = WEEK_HEADER.matcher(text(cell(headers, column)));
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                if (!seen.add(number)) {
                    throw new IllegalStateException("The Standings worksheet contains duplicate Week " + number);
                }
                weeks.add(new WeekColumn(number, column));
            }
        }
        weeks.sort(Comparator.comparingInt(WeekColumn::number));
        return weeks;
    }

    private static int requiredHeader(RowData headers, String name) {
        if (headers.getValues() != null) {
            for (int column = 0; column < headers.getValues().size(); column++) {
                if (name.equalsIgnoreCase(text(cell(headers, column)))) {
                    return column;
                }
            }
        }
        throw new IllegalStateException("The survivor spreadsheet is missing the " + name + " header");
    }

    private static CellData cell(RowData row, int column) {
        return row.getValues() == null || column >= row.getValues().size() ? null : row.getValues().get(column);
    }

    private static boolean checked(CellData cell) {
        return cell != null && ((cell.getEffectiveValue() != null
                && Boolean.TRUE.equals(cell.getEffectiveValue().getBoolValue()))
                || "TRUE".equalsIgnoreCase(text(cell)));
    }

    private static String text(CellData cell) {
        return cell == null || cell.getFormattedValue() == null ? "" : cell.getFormattedValue().trim();
    }

    private record WeekColumn(int number, int column) {
    }
}
