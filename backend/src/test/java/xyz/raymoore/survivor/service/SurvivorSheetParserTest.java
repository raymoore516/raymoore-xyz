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
import xyz.raymoore.madisonsc.category.Team;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.ContestantView;
import xyz.raymoore.survivor.dto.query.SurvivorResponse.PickStatus;

class SurvivorSheetParserTest {

    private static final Color GREEN = new Color().setGreen(1f);
    private static final Color RED = new Color().setRed(1f);

    @Test
    void marksAffirmativeWeekOneBuybacksAndSortsByStatusTeamAndName() {
        Spreadsheet spreadsheet = spreadsheet(
                List.of(
                        row(cell("Name"), cell("Entry"), cell("Buyback"), cell("Week 1")),
                        row(cell("Alice"), cell("X"), cell(""), resultCell(Team.CLE, GREEN)),
                        row(cell("Bob"), cell("X"), cell(""), resultCell(Team.BAL, GREEN)),
                        row(cell("Charlie"), cell("X"), cell(""), cell(Team.PIT)),
                        row(cell("Derek"), cell("X"), cell(""), cell(Team.CHI)),
                        row(cell("Evan"), cell("X"), cell("X"), resultCell(Team.GB, RED)),
                        row(cell("Frank"), cell("X"), cell("Y"), resultCell(Team.CIN, RED)),
                        row(cell("George"), cell("X"), cell("YES"), resultCell(Team.CIN, RED)),
                        row(cell("Helga"), cell("X"), cell(""), resultCell(Team.BAL, RED)),
                        row(cell("Isaac"), cell("X"), cell(""), resultCell(Team.DET, RED)),
                        row(cell("John"), cell("X"), cell(""), resultCell(Team.MIN, RED))
                ),
                List.of(Team.BAL, Team.CLE, Team.CIN, Team.PIT, Team.CHI, Team.DET, Team.GB, Team.MIN)
        );

        List<ContestantView> contestants = SurvivorSheetParser.parse(spreadsheet).weeks().getFirst().contestants();

        assertEquals(
                List.of("Bob", "Alice", "Derek", "Charlie", "Helga", "Frank", "George", "Isaac", "Evan", "John"),
                contestants.stream().map(ContestantView::name).toList()
        );
        assertEquals(
                List.of(
                        PickStatus.SURVIVAL,
                        PickStatus.SURVIVAL,
                        PickStatus.PENDING,
                        PickStatus.PENDING,
                        PickStatus.ELIMINATION,
                        PickStatus.BUYBACK,
                        PickStatus.BUYBACK,
                        PickStatus.ELIMINATION,
                        PickStatus.BUYBACK,
                        PickStatus.ELIMINATION
                ),
                contestants.stream().map(ContestantView::status).toList()
        );
        assertNull(contestants.get(5).eliminationWeek());
        assertNull(contestants.get(6).eliminationWeek());
        assertNull(contestants.get(8).eliminationWeek());
    }

    @Test
    void doesNotReuseAWeekOneBuybackForTheNextLoss() {
        Spreadsheet spreadsheet = spreadsheet(
                List.of(
                        row(cell("Name"), cell("Entry"), cell("Buyback"), cell("Week 1"), cell("Week 2")),
                        row(cell("Alice"), cell("X"), cell("X"),
                                resultCell(Team.BAL, RED), resultCell(Team.CLE, RED))
                ),
                List.of(Team.BAL, Team.CLE)
        );

        SurvivorResponse response = SurvivorSheetParser.parse(spreadsheet);

        ContestantView weekOne = response.weeks().get(0).contestants().getFirst();
        ContestantView weekTwo = response.weeks().get(1).contestants().getFirst();
        assertEquals(PickStatus.BUYBACK, weekOne.status());
        assertNull(weekOne.eliminationWeek());
        assertEquals(PickStatus.ELIMINATION, weekTwo.status());
        assertEquals(2, weekTwo.eliminationWeek());
    }

    private static Spreadsheet spreadsheet(List<RowData> standings, List<Team> teamsByCode) {
        List<RowData> teams = new java.util.ArrayList<>();
        teams.add(row(cell("Team"), cell("Name")));
        teamsByCode.forEach(team -> teams.add(row(cell(team), cell("Team " + team.name()))));
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

    private static CellData cell(Team team) {
        return cell(team.name());
    }

    private static CellData resultCell(Team team, Color color) {
        return cell(team).setEffectiveFormat(new CellFormat().setBackgroundColor(color));
    }
}
