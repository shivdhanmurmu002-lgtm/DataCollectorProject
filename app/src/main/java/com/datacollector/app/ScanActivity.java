package com.datacollector.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.adapters.ScannedDataAdapter;
import com.datacollector.app.models.DataField;
import com.datacollector.app.models.ScannedRecord;
import com.datacollector.app.models.SheetConfig;
import com.datacollector.app.utils.GoogleSheetHelper;
import com.datacollector.app.utils.NetworkUtils;
import com.datacollector.app.utils.PrefsManager;
import com.datacollector.app.utils.TextParser;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private RecyclerView rvExtractedData;
    private TextView tvRawText, tvStatus, tvScanCount;
    private Button btnCapture, btnSendToSheet, btnReScan, btnManualEntry;
    private ProgressBar progressBar;
    private View layoutResults;

    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;
    private PrefsManager prefsManager;
    private GoogleSheetHelper sheetHelper;

    private List<DataField> selectedFields;
    private List<DataField> extractedFields;
    private ScannedDataAdapter dataAdapter;
    private String currentRawText = "";
    private boolean isProcessing = false;
    private boolean isCameraActive = true;
    private int scanCount = 0;

    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        prefsManager = new PrefsManager(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        sheetHelper = new GoogleSheetHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        initViews();
        loadConfig();
        
        if (checkCameraPermission()) {
            startCamera();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        rvExtractedData = findViewById(R.id.rvExtractedData);
        tvRawText = findViewById(R.id.tvRawText);
        tvStatus = findViewById(R.id.tvStatus);
        tvScanCount = findViewById(R.id.tvScanCount);
        btnCapture = findViewById(R.id.btnCapture);
        btnSendToSheet = findViewById(R.id.btnSendToSheet);
        btnReScan = findViewById(R.id.btnReScan);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        progressBar = findViewById(R.id.progressBarScan);
        layoutResults = findViewById(R.id.layoutResults);

        extractedFields = new ArrayList<>();
        dataAdapter = new ScannedDataAdapter(extractedFields);
        rvExtractedData.setLayoutManager(new LinearLayoutManager(this));
        rvExtractedData.setAdapter(dataAdapter);

        btnCapture.setOnClickListener(v -> captureAndProcess());
        btnSendToSheet.setOnClickListener(v -> sendToSheet());
        btnReScan.setOnClickListener(v -> reScan());
        btnManualEntry.setOnClickListener(v -> showManualEntryDialog());

        layoutResults.setVisibility(View.GONE);
    }

    private void loadConfig() {
        // Load selected fields
        selectedFields = prefsManager.getSelectedFields();

        // Configure sheet helper
        SheetConfig config = prefsManager.getSheetConfig();
        String webAppUrl = prefsManager.getWebAppUrl();
        sheetHelper.configure(webAppUrl, config.getSheetId(), config.getSheetName());
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @SuppressWarnings("UnsafeOptInUsageError")
    private void processImage(ImageProxy imageProxy) {
        if (!isCameraActive || isProcessing) {
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Only process when capture button is pressed (controlled by isCameraActive flag)
        // For live preview, we could process continuously but it's resource-intensive
        imageProxy.close();
    }

    private void captureAndProcess() {
        if (isProcessing) return;
        
        isProcessing = true;
        isCameraActive = false;
        progressBar.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(false);
        tvStatus.setText("Processing image...");

        // Take a snapshot from the preview
        previewView.getBitmap();

        // Use ImageAnalysis to capture
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();
                
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    try {
                        if (imageProxy.getImage() != null) {
                            InputImage inputImage = InputImage.fromMediaImage(
                                imageProxy.getImage(), 
                                imageProxy.getImageInfo().getRotationDegrees()
                            );

                            textRecognizer.process(inputImage)
                                .addOnSuccessListener(text -> {
                                    currentRawText = text.getText();
                                    runOnUiThread(() -> processExtractedText(currentRawText));
                                    imageProxy.close();
                                    // Unbind analysis after capture
                                    runOnUiThread(() -> {
                                        provider.unbindAll();
                                        // Rebind just preview
                                        Preview preview = new Preview.Builder().build();
                                        preview.setSurfaceProvider(
                                            previewView.getSurfaceProvider());
                                        CameraSelector selector = new CameraSelector.Builder()
                                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                            .build();
                                        provider.bindToLifecycle(
                                            ScanActivity.this, selector, preview);
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Text recognition failed", e);
                                    runOnUiThread(() -> {
                                        isProcessing = false;
                                        progressBar.setVisibility(View.GONE);
                                        btnCapture.setEnabled(true);
                                        tvStatus.setText("Recognition failed. Try again.");
                                    });
                                    imageProxy.close();
                                });
                        } else {
                            imageProxy.close();
                        }
                    } catch (Exception e) {
                        imageProxy.close();
                        runOnUiThread(() -> {
                            isProcessing = false;
                            progressBar.setVisibility(View.GONE);
                            btnCapture.setEnabled(true);
                        });
                    }
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                provider.unbindAll();
                provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Error capturing", e);
                runOnUiThread(() -> {
                    isProcessing = false;
                    progressBar.setVisibility(View.GONE);
                    btnCapture.setEnabled(true);
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processExtractedText(String rawText) {
        isProcessing = false;
        progressBar.setVisibility(View.GONE);
        btnCapture.setEnabled(true);

        if (rawText == null || rawText.trim().isEmpty()) {
            tvStatus.setText("No text detected. Try again.");
            Toast.makeText(this, "No text found in image!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show raw text
        tvRawText.setText(rawText);
        tvRawText.setVisibility(View.VISIBLE);

        // Extract fields
        extractedFields.clear();
        extractedFields.addAll(TextParser.extractAllFields(rawText, selectedFields));

        dataAdapter.notifyDataSetChanged();

        // Show results section
        layoutResults.setVisibility(View.VISIBLE);
        tvStatus.setText("✅ Text scanned successfully! Review & edit data below.");

        // Check if any data was extracted
        boolean hasData = false;
        for (DataField field : extractedFields) {
            if (field.getExtractedValue() != null && !field.getExtractedValue().isEmpty()) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            tvStatus.setText("⚠️ No matching data found. You can enter values manually.");
        }
    }

    private void sendToSheet() {
        if (extractedFields.isEmpty()) {
            Toast.makeText(this, "No data to send!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection! Data saved locally.", 
                Toast.LENGTH_LONG).show();
            saveLocally(false);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendToSheet.setEnabled(false);
        tvStatus.setText("Sending to Google Sheet...");

        // Prepare data
        Map<String, String> data = new LinkedHashMap<>();
        List<String> columnOrder = new ArrayList<>();

        for (DataField field : dataAdapter.getExtractedFields()) {
            data.put(field.getFieldName(), field.getExtractedValue());
            columnOrder.add(field.getFieldName());
        }

        sheetHelper.appendRow(data, columnOrder, new GoogleSheetHelper.SheetCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);
                btnSendToSheet.setEnabled(true);
                scanCount++;
                tvScanCount.setText("Scans: " + scanCount);
                tvStatus.setText("✅ Data sent to Google Sheet successfully!");
                Toast.makeText(ScanActivity.this, "Data sent! ✅", Toast.LENGTH_SHORT).show();

                saveLocally(true);

                // Auto reset for next scan
                new android.os.Handler().postDelayed(() -> reScan(), 1500);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnSendToSheet.setEnabled(true);
                tvStatus.setText("❌ Error: " + error);
                Toast.makeText(ScanActivity.this, 
                    "Failed to send: " + error, Toast.LENGTH_LONG).show();

                saveLocally(false);
            }
        });
    }

    private void saveLocally(boolean synced) {
        ScannedRecord record = new ScannedRecord();
        record.setRawText(currentRawText);
        record.setSynced(synced);

        Map<String, String> data = new LinkedHashMap<>();
        for (DataField field : dataAdapter.getExtractedFields()) {
            data.put(field.getFieldName(), field.getExtractedValue());
        }
        record.setExtractedData(data);

        SheetConfig config = prefsManager.getSheetConfig();
        record.setSheetId(config.getSheetId());

        prefsManager.addToHistory(record);
    }

    private void reScan() {
        layoutResults.setVisibility(View.GONE);
        tvRawText.setVisibility(View.GONE);
        extractedFields.clear();
        dataAdapter.notifyDataSetChanged();
        currentRawText = "";
        isCameraActive = true;
        tvStatus.setText("Point camera at text and tap Capture");
    }

    private void showManualEntryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manual_entry, null);
        android.widget.EditText etManualText = dialogView.findViewById(R.id.etManualText);

        new AlertDialog.Builder(this)
            .setTitle("Manual Text Entry")
            .setView(dialogView)
            .setPositiveButton("Process", (dialog, which) -> {
                String manualText = etManualText.getText().toString();
                if (!manualText.isEmpty()) {
                    currentRawText = manualText;
                    processExtractedText(manualText);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        if (sheetHelper != null) {
            sheetHelper.shutdown();
        }
    }
}