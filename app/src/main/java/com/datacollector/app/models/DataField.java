package com.datacollector.app.models;

import java.io.Serializable;

public class DataField implements Serializable {
    private String fieldName;
    private String fieldKey;
    private boolean selected;
    private String extractedValue;
    private String pattern; // regex pattern for extraction

    public DataField() {}

    public DataField(String fieldName, String fieldKey, String pattern) {
        this.fieldName = fieldName;
        this.fieldKey = fieldKey;
        this.pattern = pattern;
        this.selected = false;
        this.extractedValue = "";
    }

    // Predefined common fields
    public static DataField[] getCommonFields() {
        return new DataField[]{
            new DataField("Serial Number", "serial_number", 
                "(?i)(?:serial\\s*(?:no|number|#)?\\s*[:\\-]?\\s*)([A-Za-z0-9\\-]+)"),
            new DataField("Model Number", "model_number", 
                "(?i)(?:model\\s*(?:no|number|#)?\\s*[:\\-]?\\s*)([A-Za-z0-9\\-]+)"),
            new DataField("Part Number", "part_number", 
                "(?i)(?:part\\s*(?:no|number|#)?\\s*[:\\-]?\\s*)([A-Za-z0-9\\-]+)"),
            new DataField("Date", "date", 
                "(?i)(?:date\\s*[:\\-]?\\s*)(\\d{1,4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,4})"),
            new DataField("Price", "price", 
                "(?i)(?:price|cost|amount|total)\\s*[:\\-]?\\s*[₹$€£]?\\s*([\\d,]+\\.?\\d*)"),
            new DataField("Quantity", "quantity", 
                "(?i)(?:qty|quantity)\\s*[:\\-]?\\s*(\\d+)"),
            new DataField("Batch Number", "batch_number", 
                "(?i)(?:batch\\s*(?:no|number|#)?\\s*[:\\-]?\\s*)([A-Za-z0-9\\-]+)"),
            new DataField("Manufacturer", "manufacturer", 
                "(?i)(?:mfg|manufacturer|made\\s*by)\\s*[:\\-]?\\s*(.+?)(?:\\n|$)"),
            new DataField("Expiry Date", "expiry_date", 
                "(?i)(?:exp(?:iry)?\\s*(?:date)?\\s*[:\\-]?\\s*)(\\d{1,4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,4})"),
            new DataField("Weight", "weight", 
                "(?i)(?:weight|wt)\\s*[:\\-]?\\s*([\\d.]+\\s*(?:kg|g|lb|oz)?)"),
            new DataField("Barcode/ID", "barcode_id", 
                "(?i)(?:barcode|id|code)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]+)"),
            new DataField("Name/Title", "name", 
                "(?i)(?:name|title|product)\\s*[:\\-]?\\s*(.+?)(?:\\n|$)"),
            new DataField("Phone Number", "phone", 
                "(?i)(?:ph(?:one)?|tel|mobile|contact)\\s*[:\\-]?\\s*([+]?[\\d\\-\\s()]{7,15})"),
            new DataField("Email", "email", 
                "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
            new DataField("Address", "address", 
                "(?i)(?:address|addr)\\s*[:\\-]?\\s*(.+?)(?:\\n|$)"),
            new DataField("Invoice Number", "invoice_number", 
                "(?i)(?:invoice\\s*(?:no|number|#)?\\s*[:\\-]?\\s*)([A-Za-z0-9\\-/]+)"),
            new DataField("Custom Field 1", "custom_1", ""),
            new DataField("Custom Field 2", "custom_2", ""),
            new DataField("Custom Field 3", "custom_3", ""),
        };
    }

    // Getters and Setters
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public String getExtractedValue() { return extractedValue; }
    public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }
    
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}