package xyz.raymoore.survivor.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import xyz.raymoore.survivor.dto.query.SurvivorResponse;

@Service
public class SurvivorService {

    // Read values and displayed colors together, including conditional formatting.
    private static final String SPREADSHEET_FIELDS =
            "properties(title,spreadsheetTheme(themeColors)),"
            + "sheets(properties(title),data(rowData(values(effectiveValue,formattedValue,"
            + "effectiveFormat(backgroundColor,backgroundColorStyle)))))";

    private final Environment environment;

    public SurvivorService(Environment environment) {
        this.environment = environment;
    }

    public SurvivorResponse loadCurrentPicks() throws IOException, GeneralSecurityException {
        String spreadsheetId = environment.getRequiredProperty("project.survivor.spreadsheet-id");
        Spreadsheet spreadsheet = createSheetsClient().spreadsheets()
                .get(spreadsheetId)
                .setRanges(List.of("'Standings'", "'Teams'"))
                .setFields(SPREADSHEET_FIELDS)
                .execute();
        return SurvivorSheetParser.parse(spreadsheet);
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
}
