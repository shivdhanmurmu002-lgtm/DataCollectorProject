package com.datacollector.app.utils;

import com.datacollector.app.models.DataField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {

    /**
     * Parse scanned text and extract values based on selected fields
     */
    public static Map<String, String> parseText(String rawText, List<DataField> selectedFields) {
        Map<String, String> result = new LinkedHashMap<>();

        if (rawText == null || rawText.isEmpty()) {
            return result;
        }

        for (DataField field : selectedFields) {
            if (!field.isSelected()) continue;

            String value = extractValue(rawText, field);
            result.put(field.getFieldKey(), value);
        }

        return result;
    }

    /**
     * Extract a specific field value from text using its pattern
     */
    private static String extractValue(String text, DataField field) {
        String pattern = field.getPattern();

        if (pattern == null || pattern.isEmpty()) {
            return ""; // Custom fields without patterns
        }

        try {
            Pattern p = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                // Return the first capturing group if exists, otherwise the whole match
                if (m.groupCount() > 0) {
                    return m.group(1).trim();
                }
                return m.group(0).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: Try key-value pair matching
        return tryKeyValueExtraction(text, field.getFieldName());
    }

    /**
     * Fallback extraction: look for "Key: Value" or "Key - Value" patterns
     */
    private static String tryKeyValueExtraction(String text, String fieldName) {
        String[] lines = text.split("\n");
        String lowerFieldName = fieldName.toLowerCase().trim();

        // Generate possible key variations
        List<String> keyVariations = generateKeyVariations(lowerFieldName);

        for (String line : lines) {
            String lowerLine = line.toLowerCase().trim();
            for (String key : keyVariations) {
                if (lowerLine.contains(key)) {
                    // Try to extract value after the key
                    int keyIndex = lowerLine.indexOf(key);
                    String afterKey = line.substring(keyIndex + key.length()).trim();

                    // Remove leading separators
                    afterKey = afterKey.replaceFirst("^[:\\-=\\s]+", "").trim();

                    if (!afterKey.isEmpty()) {
                        return afterKey;
                    }
                }
            }
        }

        return "";
    }

    /**
     * Generate variations of a field name for matching
     */
    private static List<String> generateKeyVariations(String fieldName) {
        List<String> variations = new ArrayList<>();
        variations.add(fieldName);

        // Remove common suffixes/prefixes
        String simplified = fieldName
                .replace("number", "no")
                .replace("num", "no");
        variations.add(simplified);

        // Add abbreviated forms
        String[] words = fieldName.split("\\s+");
        if (words.length > 1) {
            StringBuilder abbr = new StringBuilder();
            for (String word : words) {
                abbr.append(word.charAt(0));
            }
            variations.add(abbr.toString());
        }

        // Add without spaces
        variations.add(fieldName.replace(" ", ""));

        // Add with common separators
        variations.add(fieldName.replace(" ", "_"));
        variations.add(fieldName.replace(" ", "-"));

        return variations;
    }

    /**
     * Auto-detect all key-value pairs in scanned text
     */
    public static Map<String, String> autoDetectFields(String rawText) {
        Map<String, String> detected = new LinkedHashMap<>();

        if (rawText == null || rawText.isEmpty()) return detected;

        String[] lines = rawText.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Try different separators: : - = |
            String[] separators = {":", "-", "=", "|"};
            for (String sep : separators) {
                int sepIndex = line.indexOf(sep);
                if (sepIndex > 0 && sepIndex < line.length() - 1) {
                    String key = line.substring(0, sepIndex).trim();
                    String value = line.substring(sepIndex + 1).trim();

                    if (!key.isEmpty() && !value.isEmpty() && key.length() < 50) {
                        detected.put(key, value);
                        break;
                    }
                }
            }
        }

        return detected;
    }

    /**
     * Extract all data matching selected fields and return as DataField list
     */
    public static List<DataField> extractAllFields(String rawText, List<DataField> selectedFields) {
        List<DataField> result = new ArrayList<>();

        for (DataField field : selectedFields) {
            if (!field.isSelected()) continue;

            DataField extracted = new DataField(
                field.getFieldName(), 
                field.getFieldKey(), 
                field.getPattern()
            );
            extracted.setSelected(true);
            extracted.setExtractedValue(extractValue(rawText, field));
            result.add(extracted);
        }

        return result;
    }
}