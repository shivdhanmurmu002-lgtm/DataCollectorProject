package com.datacollector.app.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScannedRecord implements Serializable {
    private long id;
    private String rawText;
    private Map<String, String> extractedData;
    private long timestamp;
    private boolean synced;
    private String sheetId;

    public ScannedRecord() {
        this.extractedData = new LinkedHashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.synced = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Map<String, String> getExtractedData() { return extractedData; }
    public void setExtractedData(Map<String, String> extractedData) { 
        this.extractedData = extractedData; 
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public String getSheetId() { return sheetId; }
    public void setSheetId(String sheetId) { this.sheetId = sheetId; }
}