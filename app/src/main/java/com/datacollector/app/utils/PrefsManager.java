package com.datacollector.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.datacollector.app.models.DataField;
import com.datacollector.app.models.ScannedRecord;
import com.datacollector.app.models.SheetConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PrefsManager {
    private static final String PREFS_NAME = "data_collector_prefs";
    private static final String KEY_SHEET_CONFIG = "sheet_config";
    private static final String KEY_SELECTED_FIELDS = "selected_fields";
    private static final String KEY_HISTORY = "scan_history";
    private static final String KEY_WEB_APP_URL = "web_app_url";
    private static final String KEY_AUTO_SEND = "auto_send";
    private static final String KEY_FIRST_RUN = "first_run";

    private SharedPreferences prefs;
    private Gson gson;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Sheet Config
    public void saveSheetConfig(SheetConfig config) {
        prefs.edit().putString(KEY_SHEET_CONFIG, gson.toJson(config)).apply();
    }

    public SheetConfig getSheetConfig() {
        String json = prefs.getString(KEY_SHEET_CONFIG, null);
        if (json == null) return new SheetConfig();
        return gson.fromJson(json, SheetConfig.class);
    }

    // Selected Fields
    public void saveSelectedFields(List<DataField> fields) {
        prefs.edit().putString(KEY_SELECTED_FIELDS, gson.toJson(fields)).apply();
    }

    public List<DataField> getSelectedFields() {
        String json = prefs.getString(KEY_SELECTED_FIELDS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<DataField>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // Scan History
    public void saveHistory(List<ScannedRecord> records) {
        // Keep only last 500 records
        if (records.size() > 500) {
            records = records.subList(records.size() - 500, records.size());
        }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(records)).apply();
    }

    public List<ScannedRecord> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<ScannedRecord>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void addToHistory(ScannedRecord record) {
        List<ScannedRecord> history = getHistory();
        history.add(record);
        saveHistory(history);
    }

    // Web App URL
    public void saveWebAppUrl(String url) {
        prefs.edit().putString(KEY_WEB_APP_URL, url).apply();
    }

    public String getWebAppUrl() {
        return prefs.getString(KEY_WEB_APP_URL, "");
    }

    // Auto Send
    public void setAutoSend(boolean autoSend) {
        prefs.edit().putBoolean(KEY_AUTO_SEND, autoSend).apply();
    }

    public boolean isAutoSend() {
        return prefs.getBoolean(KEY_AUTO_SEND, true);
    }

    // First Run
    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRunDone() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}