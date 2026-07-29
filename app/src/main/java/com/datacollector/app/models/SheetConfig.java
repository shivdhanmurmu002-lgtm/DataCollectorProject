package com.datacollector.app.models;

import java.io.Serializable;

public class SheetConfig implements Serializable {
    private String sheetUrl;
    private String sheetId;
    private String sheetName;
    private String apiKey;
    private boolean useServiceAccount;
    private String serviceAccountJson;

    public SheetConfig() {
        this.sheetName = "Sheet1";
    }

    public String extractSheetId(String url) {
        // Extract sheet ID from URL
        // Format: https://docs.google.com/spreadsheets/d/SHEET_ID/edit
        if (url == null || url.isEmpty()) return "";
        
        String[] parts = url.split("/d/");
        if (parts.length > 1) {
            String idPart = parts[1];
            int slashIndex = idPart.indexOf('/');
            if (slashIndex > 0) {
                return idPart.substring(0, slashIndex);
            }
            return idPart;
        }
        return url; // assume it's already a sheet ID
    }

    // Getters and Setters
    public String getSheetUrl() { return sheetUrl; }
    public void setSheetUrl(String sheetUrl) { 
        this.sheetUrl = sheetUrl;
        this.sheetId = extractSheetId(sheetUrl);
    }

    public String getSheetId() { return sheetId; }
    public void setSheetId(String sheetId) { this.sheetId = sheetId; }

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public boolean isUseServiceAccount() { return useServiceAccount; }
    public void setUseServiceAccount(boolean useServiceAccount) { 
        this.useServiceAccount = useServiceAccount; 
    }

    public String getServiceAccountJson() { return serviceAccountJson; }
    public void setServiceAccountJson(String serviceAccountJson) { 
        this.serviceAccountJson = serviceAccountJson; 
    }
}