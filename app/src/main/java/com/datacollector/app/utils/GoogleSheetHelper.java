package com.datacollector.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleSheetHelper {
    private static final String TAG = "GoogleSheetHelper";
    
    // Using Google Apps Script Web App approach (simplest, no OAuth needed)
    // You deploy a Google Apps Script as a web app that handles the sheet operations
    
    private String webAppUrl; // Google Apps Script deployed web app URL
    private String sheetId;
    private String sheetName;
    private OkHttpClient client;
    private ExecutorService executor;
    private Handler mainHandler;

    public interface SheetCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public GoogleSheetHelper(Context context) {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void configure(String webAppUrl, String sheetId, String sheetName) {
        this.webAppUrl = webAppUrl;
        this.sheetId = sheetId;
        this.sheetName = sheetName != null ? sheetName : "Sheet1";
    }

    /**
     * Append a row of data to the Google Sheet
     */
    public void appendRow(Map<String, String> data, List<String> columnOrder, SheetCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "append");
                payload.put("sheetId", sheetId);
                payload.put("sheetName", sheetName);

                JSONArray headers = new JSONArray();
                JSONArray values = new JSONArray();

                for (String key : columnOrder) {
                    headers.put(key);
                    values.put(data.getOrDefault(key, ""));
                }

                payload.put("headers", headers);
                payload.put("values", values);
                payload.put("timestamp", System.currentTimeMillis());

                String responseStr = makePostRequest(payload.toString());
                mainHandler.post(() -> callback.onSuccess(responseStr));

            } catch (Exception e) {
                Log.e(TAG, "Error appending row", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Set up headers in the sheet
     */
    public void setupHeaders(List<String> headers, SheetCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "setupHeaders");
                payload.put("sheetId", sheetId);
                payload.put("sheetName", sheetName);

                JSONArray headerArray = new JSONArray();
                // Add timestamp header first
                headerArray.put("Timestamp");
                for (String header : headers) {
                    headerArray.put(header);
                }
                payload.put("headers", headerArray);

                String responseStr = makePostRequest(payload.toString());
                mainHandler.post(() -> callback.onSuccess(responseStr));

            } catch (Exception e) {
                Log.e(TAG, "Error setting up headers", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Append data using Google Sheets API v4 directly (with API key)
     * This is an alternative approach
     */
    public void appendRowDirect(String apiKey, Map<String, String> data, 
                                 List<String> columnOrder, SheetCallback callback) {
        executor.execute(() -> {
            try {
                String range = sheetName + "!A:Z";
                String url = String.format(
                    "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s:append" +
                    "?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS&key=%s",
                    sheetId, range, apiKey
                );

                JSONObject payload = new JSONObject();
                JSONArray valuesArray = new JSONArray();
                JSONArray rowArray = new JSONArray();

                // Add timestamp
                rowArray.put(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                    java.util.Locale.getDefault()).format(new java.util.Date()));

                for (String key : columnOrder) {
                    rowArray.put(data.getOrDefault(key, ""));
                }

                valuesArray.put(rowArray);
                payload.put("values", valuesArray);

                String responseStr = makePostRequest(url, payload.toString());
                mainHandler.post(() -> callback.onSuccess(responseStr));

            } catch (Exception e) {
                Log.e(TAG, "Error appending row directly", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Use Google Apps Script Web App URL for writing
     */
    private String makePostRequest(String jsonPayload) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        Request request = new Request.Builder()
                .url(webAppUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return "OK";
        }
    }

    private String makePostRequest(String url, String jsonPayload) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return "OK";
        }
    }

    /**
     * Test connection to the sheet
     */
    public void testConnection(SheetCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "test");
                payload.put("sheetId", sheetId);
                payload.put("sheetName", sheetName);

                String responseStr = makePostRequest(payload.toString());
                mainHandler.post(() -> callback.onSuccess(responseStr));

            } catch (Exception e) {
                Log.e(TAG, "Connection test failed", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}