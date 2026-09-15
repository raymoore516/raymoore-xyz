package xyz.raymoore.survivor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.ContestantView;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.PickStatus;

class SurvivorSheetParserTest {

    private static final Color GREEN = new Color().setGreen(1f);
    private static final Color RED = new Color().setRed(1f);

    @Test
    void marksAffirmativeWeekOneBuybacksAndSortsThemAfterSurvivals() {
        Spreadsheet spreadsheet = spreadsheet(
                List.of(
                        row(cell("Name"), cell("Entry"), cell("Buyback"), cell("Week 1")),
                        row(cell("Green"), cell("X"), cell(""), resultCell("ZZZ", GREEN)),
                        row(cell("Charlie"), cell("X"), cell("X"), resultCell("BBB", RED)),
                        row(cell("Alice"), cell("X"), cell("Y"), resultCell("AAA", RED)),
                        row(cell("Bob"), cell("X"), cell("YES"), resultCell("AAA", RED)),
                        row(cell("None"), cell("X"), cell(""), cell("AAA")),
                        row(cell("Red"), cell("X"), cell(""), resultCell("AAA", RED))
                ),
                List.of("AAA", "BBB", "ZZZ")
        );

        List<ContestantView> contestants = SurvivorSheetParser.parse(spreadsheet).weeks().getFirst().contestants();

        assertEquals(
                List.of("Green", "Alice", "Bob", "Charlie", "None", "Red"),
                contestants.stream().map(ContestantView::name).toList()
        );
        assertEquals(
                List.of(
                        PickStatus.SURVIVAL,
                        PickStatus.BUYBACK,
                        PickStatus.BUYBACK,
                        PickStatus.BUYBACK,
                        PickStatus.PENDING,
                        PickStatus.ELIMINATION
                ),
                contestants.stream().map(ContestantView::status).toList()
        );
        assertNull(contestants.get(1).eliminationWeek());
        assertNull(contestants.get(2).eliminationWeek());
        assertNull(contestants.get(3).eliminationWeek());
    }

    @Test
    void doesNotReuseAWeekOneBuybackForTheNextLoss() {
        Spreadsheet spreadsheet = spreadsheet(
                List.of(
                        row(cell("Name"), cell("Entry"), cell("Buyback"), cell("Week 1"), cell("Week 2")),
                        row(cell("Ray"), cell("X"), cell("X"), resultCell("AAA", RED), resultCell("BBB", RED))
                ),
                List.of("AAA", "BBB")
        );

        SurvivorResponse response = SurvivorSheetParser.parse(spreadsheet);

        ContestantView weekOne = response.weeks().get(0).contestants().getFirst();
        ContestantView weekTwo = response.weeks().get(1).contestants().getFirst();
        assertEquals(PickStatus.BUYBACK, weekOne.status());
        assertNull(weekOne.eliminationWeek());
        assertEquals(PickStatus.ELIMINATION, weekTwo.status());
        assertEquals(2, weekTwo.eliminationWeek());
    }

    private static Spreadsheet spreadsheet(List<RowData> standings, List<String> teamCodes) {
        List<RowData> teams = new java.util.ArrayList<>();
        teams.add(row(cell("Team"), cell("Name")));
        teamCodes.forEach(code -> teams.add(row(cell(code), cell("Team " + code))));
        return new Spreadsheet()
                .setProperties(new SpreadsheetProperties().setTitle("Survivor Test"))
                .setSheets(List.of(sheet("Standings", standings), sheet("Teams", teams)));
    }

    private static Sheet sheet(String title, List<RowData> rows) {
        return new Sheet()
                .setProperties(new SheetProperties().setTitle(title))
                .setData(List.of(new GridData().setRowData(rows)));
    }

    private static RowData row(CellData... cells) {
        return new RowData().setValues(Arrays.asList(cells));
    }

    private static CellData cell(String value) {
        return new CellData().setFormattedValue(value);
    }

    private static CellData resultCell(String value, Color color) {
        return cell(value).setEffectiveFormat(new CellFormat().setBackgroundColor(color));
    }
}
