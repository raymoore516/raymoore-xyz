package xyz.raymoore.survivor.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;

@Service
public class SurvivorService {

    private static final Pattern WEEK_HEADER = Pattern.compile("^Week\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final Environment environment;

    public SurvivorService(Environment environment) {
        this.environment = environment;
    }

    public SurvivorResponse loadCurrentPicks() throws IOException, GeneralSecurityException {
        String spreadsheetId = environment.getRequiredProperty("project.survivor.spreadsheet-id");
        Sheets sheets = createSheetsClient();
        Spreadsheet spreadsheet = sheets.spreadsheets()
                .get(spreadsheetId)
                .setFields("properties.title")
                .execute();
        List<List<Object>> standingsRows = loadWorksheetRows(sheets, spreadsheetId, "Standings");
        Map<String, String> teamNamesByCode = parseTeamNames(
                loadWorksheetRows(sheets, spreadsheetId, "Teams")
        );
        List<SurvivorResponse.WeekView> weeks = standingsRows.isEmpty()
                ? List.of()
                : parseWeeks(standingsRows, teamNamesByCode);

        return SurvivorResponse.builder()
                .spreadsheetTitle(spreadsheet.getProperties().getTitle())
                .weeks(weeks)
                .build();
    }

    private static List<List<Object>> loadWorksheetRows(
            Sheets sheets,
            String spreadsheetId,
            String worksheetTitle
    ) throws IOException {
        ValueRange worksheet = sheets.spreadsheets()
                .values()
                .get(spreadsheetId, quoteWorksheetTitle(worksheetTitle))
                .setMajorDimension("ROWS")
                .execute();
        return worksheet.getValues() == null ? List.of() : worksheet.getValues();
    }

    private Sheets createSheetsClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(SheetsScopes.SPREADSHEETS_READONLY);

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(environment.getRequiredProperty("spring.application.name"))
                .build();
    }

    private static List<SurvivorResponse.WeekView> parseWeeks(
            List<List<Object>> rows,
            Map<String, String> teamNamesByCode
    ) {
        List<Object> headers = rows.getFirst();
        int nameColumn = findRequiredHeader(headers, "Name");
        if (nameColumn != 0) {
            throw new IllegalStateException("The survivor spreadsheet's Name header must be in Column A");
        }
        int entryColumn = findRequiredHeader(headers, "Entry");
        List<WeekColumn> weekColumns = findWeekColumns(headers).stream()
                .filter(weekColumn -> hasAnyPick(rows, weekColumn.columnIndex()))
                .toList();
        List<SurvivorResponse.WeekView> weeks = new ArrayList<>();

        for (WeekColumn weekColumn : weekColumns) {
            boolean current = weekColumn.equals(weekColumns.getLast());

            List<SurvivorResponse.ContestantView> contestants = rows.stream()
                    .skip(1)
                    .filter(row -> isChecked(cell(row, entryColumn)))
                    .filter(row -> !cellText(row, nameColumn).isBlank())
                    .map(row -> {
                        String pick = cellText(row, weekColumn.columnIndex());
                        return SurvivorResponse.ContestantView.builder()
                                .name(cellText(row, nameColumn))
                                .pick(current ? "" : findTeamName(pick, teamNamesByCode))
                                .selectionExists(!pick.isBlank())
                                .build();
                    })
                    .toList();

            weeks.add(SurvivorResponse.WeekView.builder()
                    .number(weekColumn.number())
                    .label(weekColumn.label())
                    .current(current)
                    .contestants(contestants)
                    .build());
        }

        return List.copyOf(weeks);
    }

    private static Map<String, String> parseTeamNames(List<List<Object>> rows) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("The Teams worksheet is empty");
        }

        List<Object> headers = rows.getFirst();
        int teamColumn = findRequiredHeader(headers, "Team");
        int nameColumn = findRequiredHeader(headers, "Name");
        Map<String, String> teamNamesByCode = new LinkedHashMap<>();

        for (List<Object> row : rows.subList(1, rows.size())) {
            String teamCode = cellText(row, teamColumn);
            if (teamCode.isBlank()) {
                continue;
            }

            String teamName = cellText(row, nameColumn);
            if (teamName.isBlank()) {
                throw new IllegalStateException("The Teams worksheet is missing a Name for " + teamCode);
            }

            String previousName = teamNamesByCode.putIfAbsent(normalizeTeamCode(teamCode), teamName);
            if (previousName != null) {
                throw new IllegalStateException("The Teams worksheet contains duplicate Team code " + teamCode);
            }
        }

        return Map.copyOf(teamNamesByCode);
    }

    private static String findTeamName(String teamCode, Map<String, String> teamNamesByCode) {
        if (teamCode.isBlank()) {
            return "";
        }

        String teamName = teamNamesByCode.get(normalizeTeamCode(teamCode));
        if (teamName == null) {
            throw new IllegalStateException("The Teams worksheet has no Name for Team code " + teamCode);
        }
        return teamName;
    }

    private static String normalizeTeamCode(String teamCode) {
        return teamCode.toUpperCase(Locale.ROOT);
    }

    private static int findRequiredHeader(List<Object> headers, String requiredHeader) {
        for (int index = 0; index < headers.size(); index++) {
            if (cellText(headers.get(index)).equalsIgnoreCase(requiredHeader)) {
                return index;
            }
        }
        throw new IllegalStateException("The survivor spreadsheet is missing the " + requiredHeader + " header");
    }

    private static List<WeekColumn> findWeekColumns(List<Object> headers) {
        List<WeekColumn> weekColumns = new ArrayList<>();
        Set<Integer> weekNumbers = new HashSet<>();

        for (int index = 0; index < headers.size(); index++) {
            String label = cellText(headers.get(index));
            Matcher matcher = WEEK_HEADER.matcher(label);
            if (!matcher.matches()) {
                continue;
            }

            int number = Integer.parseInt(matcher.group(1));
            if (!weekNumbers.add(number)) {
                throw new IllegalStateException("The survivor spreadsheet contains duplicate Week " + number + " headers");
            }
            weekColumns.add(new WeekColumn(number, label, index));
        }

        weekColumns.sort((left, right) -> Integer.compare(left.number(), right.number()));
        return List.copyOf(weekColumns);
    }

    private static boolean hasAnyPick(List<List<Object>> rows, int weekColumn) {
        return rows.stream()
                .skip(1)
                .anyMatch(row -> !cellText(row, weekColumn).isBlank());
    }

    private static boolean isChecked(Object value) {
        return Boolean.TRUE.equals(value)
                || cellText(value).toLowerCase(Locale.ROOT).equals("true");
    }

    private static String cellText(List<Object> row, int column) {
        return cellText(cell(row, column));
    }

    private static String cellText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static Object cell(List<Object> row, int column) {
        return column < row.size() ? row.get(column) : null;
    }

    private static String quoteWorksheetTitle(String worksheetTitle) {
        return "'" + worksheetTitle.replace("'", "''") + "'";
    }

    private record WeekColumn(int number, String label, int columnIndex) {
    }
}
