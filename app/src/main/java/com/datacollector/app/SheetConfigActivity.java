package com.datacollector.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.datacollector.app.models.SheetConfig;
import com.datacollector.app.utils.GoogleSheetHelper;
import com.datacollector.app.utils.PrefsManager;

public class SheetConfigActivity extends AppCompatActivity {

    private EditText etSheetUrl, etSheetName, etWebAppUrl;
    private TextView tvSheetId, tvInstructions;
    private Button btnSave, btnTest, btnShowInstructions;
    private ProgressBar progressBar;
    private PrefsManager prefsManager;
    private SheetConfig sheetConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_config);

        prefsManager = new PrefsManager(this);
        initViews();
        loadConfig();
        setupListeners();
    }

    private void initViews() {
        etSheetUrl = findViewById(R.id.etSheetUrl);
        etSheetName = findViewById(R.id.etSheetName);
        etWebAppUrl = findViewById(R.id.etWebAppUrl);
        tvSheetId = findViewById(R.id.tvSheetId);
        tvInstructions = findViewById(R.id.tvInstructions);
        btnSave = findViewById(R.id.btnSave);
        btnTest = findViewById(R.id.btnTest);
        btnShowInstructions = findViewById(R.id.btnShowInstructions);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadConfig() {
        sheetConfig = prefsManager.getSheetConfig();
        if (sheetConfig.getSheetUrl() != null) {
            etSheetUrl.setText(sheetConfig.getSheetUrl());
        }
        if (sheetConfig.getSheetName() != null) {
            etSheetName.setText(sheetConfig.getSheetName());
        }
        String webAppUrl = prefsManager.getWebAppUrl();
        if (!webAppUrl.isEmpty()) {
            etWebAppUrl.setText(webAppUrl);
        }
    }

    private void setupListeners() {
        etSheetUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                SheetConfig temp = new SheetConfig();
                temp.setSheetUrl(s.toString());
                tvSheetId.setText("Sheet ID: " + temp.getSheetId());
            }
        });

        btnSave.setOnClickListener(v -> saveConfig());
        btnTest.setOnClickListener(v -> testConnection());
        btnShowInstructions.setOnClickListener(v -> showSetupInstructions());
    }

    private void saveConfig() {
        String url = etSheetUrl.getText().toString().trim();
        String name = etSheetName.getText().toString().trim();
        String webAppUrl = etWebAppUrl.getText().toString().trim();

        if (url.isEmpty()) {
            etSheetUrl.setError("Please enter Google Sheet URL");
            return;
        }

        if (webAppUrl.isEmpty()) {
            etWebAppUrl.setError("Please enter Web App URL");
            return;
        }

        sheetConfig = new SheetConfig();
        sheetConfig.setSheetUrl(url);
        sheetConfig.setSheetName(name.isEmpty() ? "Sheet1" : name);

        prefsManager.saveSheetConfig(sheetConfig);
        prefsManager.saveWebAppUrl(webAppUrl);

        Toast.makeText(this, "Configuration saved! ✅", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void testConnection() {
        String webAppUrl = etWebAppUrl.getText().toString().trim();
        String url = etSheetUrl.getText().toString().trim();

        if (webAppUrl.isEmpty() || url.isEmpty()) {
            Toast.makeText(this, "Please fill all fields first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnTest.setEnabled(false);

        SheetConfig tempConfig = new SheetConfig();
        tempConfig.setSheetUrl(url);
        String sheetName = etSheetName.getText().toString().trim();

        GoogleSheetHelper helper = new GoogleSheetHelper(this);
        helper.configure(webAppUrl, tempConfig.getSheetId(), 
            sheetName.isEmpty() ? "Sheet1" : sheetName);

        helper.testConnection(new GoogleSheetHelper.SheetCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);
                btnTest.setEnabled(true);
                Toast.makeText(SheetConfigActivity.this, 
                    "Connection successful! ✅", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnTest.setEnabled(true);
                Toast.makeText(SheetConfigActivity.this, 
                    "Connection failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSetupInstructions() {
        String instructions = "📋 SETUP INSTRUCTIONS:\n\n" +
            "1️⃣ Create a Google Sheet\n" +
            "   • Go to sheets.google.com\n" +
            "   • Create a new spreadsheet\n" +
            "   • Copy the URL\n\n" +
            "2️⃣ Create Google Apps Script\n" +
            "   • In your sheet, go to Extensions → Apps Script\n" +
            "   • Delete any code and paste the script below\n" +
            "   • Click Deploy → New Deployment\n" +
            "   • Select 'Web app'\n" +
            "   • Set 'Execute as' to 'Me'\n" +
            "   • Set 'Who has access' to 'Anyone'\n" +
            "   • Click Deploy and copy the URL\n\n" +
            "3️⃣ Paste the Web App URL in this app\n\n" +
            "━━━━━━━━━━━━━━━━━━━\n" +
            "📜 APPS SCRIPT CODE:\n" +
            "━━━━━━━━━━━━━━━━━━━\n\n" +
            getAppsScriptCode();

        new AlertDialog.Builder(this)
            .setTitle("Setup Instructions")
            .setMessage(instructions)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy Script", (dialog, which) -> {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = 
                    android.content.ClipData.newPlainText("Apps Script", getAppsScriptCode());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Script copied to clipboard!", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private String getAppsScriptCode() {
        return "function doPost(e) {\n" +
            "  try {\n" +
            "    var data = JSON.parse(e.postData.contents);\n" +
            "    var action = data.action;\n" +
            "    var sheetId = data.sheetId;\n" +
            "    var sheetName = data.sheetName || 'Sheet1';\n" +
            "    \n" +
            "    var ss = SpreadsheetApp.openById(sheetId);\n" +
            "    var sheet = ss.getSheetByName(sheetName);\n" +
            "    \n" +
            "    if (!sheet) {\n" +
            "      sheet = ss.insertSheet(sheetName);\n" +
            "    }\n" +
            "    \n" +
            "    if (action === 'test') {\n" +
            "      return ContentService.createTextOutput(\n" +
            "        JSON.stringify({status: 'success', message: 'Connected'})\n" +
            "      ).setMimeType(ContentService.MimeType.JSON);\n" +
            "    }\n" +
            "    \n" +
            "    if (action === 'setupHeaders') {\n" +
            "      var headers = data.headers;\n" +
            "      var headerRow = sheet.getRange(1, 1, 1, headers.length);\n" +
            "      headerRow.setValues([headers]);\n" +
            "      headerRow.setFontWeight('bold');\n" +
            "      return ContentService.createTextOutput(\n" +
            "        JSON.stringify({status: 'success'})\n" +
            "      ).setMimeType(ContentService.MimeType.JSON);\n" +
            "    }\n" +
            "    \n" +
            "    if (action === 'append') {\n" +
            "      var headers = data.headers;\n" +
            "      var values = data.values;\n" +
            "      var timestamp = new Date().toLocaleString();\n" +
            "      \n" +
            "      // Check if headers exist\n" +
            "      var lastCol = sheet.getLastColumn();\n" +
            "      if (lastCol === 0) {\n" +
            "        var allHeaders = ['Timestamp'];\n" +
            "        for (var i = 0; i < headers.length; i++) {\n" +
            "          allHeaders.push(headers[i]);\n" +
            "        }\n" +
            "        sheet.getRange(1, 1, 1, allHeaders.length)\n" +
            "          .setValues([allHeaders]).setFontWeight('bold');\n" +
            "      }\n" +
            "      \n" +
            "      var rowData = [timestamp];\n" +
            "      for (var j = 0; j < values.length; j++) {\n" +
            "        rowData.push(values[j]);\n" +
            "      }\n" +
            "      \n" +
            "      sheet.appendRow(rowData);\n" +
            "      \n" +
            "      return ContentService.createTextOutput(\n" +
            "        JSON.stringify({status: 'success', row: sheet.getLastRow()})\n" +
            "      ).setMimeType(ContentService.MimeType.JSON);\n" +
            "    }\n" +
            "    \n" +
            "  } catch (error) {\n" +
            "    return ContentService.createTextOutput(\n" +
            "      JSON.stringify({status: 'error', message: error.toString()})\n" +
            "    ).setMimeType(ContentService.MimeType.JSON);\n" +
            "  }\n" +
            "}\n\n" +
            "function doGet(e) {\n" +
            "  return ContentService.createTextOutput(\n" +
            "    JSON.stringify({status: 'ok', message: 'Data Collector API is running'})\n" +
            "  ).setMimeType(ContentService.MimeType.JSON);\n" +
            "}";
    }
}