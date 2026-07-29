package com.datacollector.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.datacollector.app.models.DataField;
import com.datacollector.app.models.SheetConfig;
import com.datacollector.app.utils.NetworkUtils;
import com.datacollector.app.utils.PrefsManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PrefsManager prefsManager;
    private TextView tvSheetStatus, tvFieldCount, tvRecordCount, tvNetworkStatus;
    private Button btnScan, btnConfigSheet, btnSelectFields, btnHistory;
    private LinearLayout layoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PrefsManager(this);
        initViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void initViews() {
        tvSheetStatus = findViewById(R.id.tvSheetStatus);
        tvFieldCount = findViewById(R.id.tvFieldCount);
        tvRecordCount = findViewById(R.id.tvRecordCount);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        btnScan = findViewById(R.id.btnScan);
        btnConfigSheet = findViewById(R.id.btnConfigSheet);
        btnSelectFields = findViewById(R.id.btnSelectFields);
        btnHistory = findViewById(R.id.btnHistory);
        layoutStatus = findViewById(R.id.layoutStatus);
    }

    private void setupClickListeners() {
        btnScan.setOnClickListener(v -> {
            // Check prerequisites
            SheetConfig config = prefsManager.getSheetConfig();
            List<DataField> fields = prefsManager.getSelectedFields();

            if (config.getSheetId() == null || config.getSheetId().isEmpty()) {
                Toast.makeText(this, "Please configure Google Sheet first!", 
                    Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SheetConfigActivity.class));
                return;
            }

            boolean hasSelectedFields = false;
            for (DataField f : fields) {
                if (f.isSelected()) {
                    hasSelectedFields = true;
                    break;
                }
            }

            if (!hasSelectedFields) {
                Toast.makeText(this, "Please select at least one data field!", 
                    Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, FieldSelectorActivity.class));
                return;
            }

            startActivity(new Intent(this, ScanActivity.class));
        });

        btnConfigSheet.setOnClickListener(v -> {
            startActivity(new Intent(this, SheetConfigActivity.class));
        });

        btnSelectFields.setOnClickListener(v -> {
            startActivity(new Intent(this, FieldSelectorActivity.class));
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    private void updateStatus() {
        // Sheet status
        SheetConfig config = prefsManager.getSheetConfig();
        if (config.getSheetId() != null && !config.getSheetId().isEmpty()) {
            tvSheetStatus.setText("✅ Sheet Connected");
            tvSheetStatus.setTextColor(0xFF4CAF50);
        } else {
            tvSheetStatus.setText("❌ No Sheet Configured");
            tvSheetStatus.setTextColor(0xFFF44336);
        }

        // Field count
        List<DataField> fields = prefsManager.getSelectedFields();
        long selectedCount = 0;
        for (DataField f : fields) {
            if (f.isSelected()) selectedCount++;
        }
        tvFieldCount.setText("📋 " + selectedCount + " fields selected");

        // Record count
        int historyCount = prefsManager.getHistory().size();
        tvRecordCount.setText("📊 " + historyCount + " records scanned");

        // Network status
        if (NetworkUtils.isNetworkAvailable(this)) {
            tvNetworkStatus.setText("🌐 Online");
            tvNetworkStatus.setTextColor(0xFF4CAF50);
        } else {
            tvNetworkStatus.setText("📵 Offline");
            tvNetworkStatus.setTextColor(0xFFF44336);
        }
    }
}