package com.datacollector.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.adapters.HistoryAdapter;
import com.datacollector.app.models.ScannedRecord;
import com.datacollector.app.models.SheetConfig;
import com.datacollector.app.utils.GoogleSheetHelper;
import com.datacollector.app.utils.NetworkUtils;
import com.datacollector.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity 
        implements HistoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView tvEmpty, tvStats;
    private Button btnClearHistory, btnSyncAll;
    private PrefsManager prefsManager;
    private GoogleSheetHelper sheetHelper;
    private List<ScannedRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        prefsManager = new PrefsManager(this);
        sheetHelper = new GoogleSheetHelper(this);

        // Configure sheet helper
        SheetConfig config = prefsManager.getSheetConfig();
        String webAppUrl = prefsManager.getWebAppUrl();
        sheetHelper.configure(webAppUrl, config.getSheetId(), config.getSheetName());

        initViews();
        loadHistory();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvHistory);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvStats = findViewById(R.id.tvStats);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnSyncAll = findViewById(R.id.btnSyncAll);

        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all scan history?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    prefsManager.saveHistory(new ArrayList<>());
                    loadHistory();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnSyncAll.setOnClickListener(v -> syncUnsyncedRecords());
    }

    private void loadHistory() {
        records = prefsManager.getHistory();
        Collections.reverse(records); // Show newest first

        if (records.isEmpty()) {
            tvEmpty.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }

        adapter = new HistoryAdapter(records, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Update stats
        int total = records.size();
        int synced = 0;
        for (ScannedRecord r : records) {
            if (r.isSynced()) synced++;
        }
        tvStats.setText(String.format("Total: %d | Synced: %d | Pending: %d", 
            total, synced, total - synced));
    }

    private void syncUnsyncedRecords() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScannedRecord> unsynced = new ArrayList<>();
        for (ScannedRecord r : records) {
            if (!r.isSynced()) unsynced.add(r);
        }

        if (unsynced.isEmpty()) {
            Toast.makeText(this, "All records are synced!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Syncing " + unsynced.size() + " records...", 
            Toast.LENGTH_SHORT).show();

        for (ScannedRecord record : unsynced) {
            Map<String, String> data = record.getExtractedData();
            List<String> columnOrder = new ArrayList<>(data.keySet());

            sheetHelper.appendRow(data, columnOrder, new GoogleSheetHelper.SheetCallback() {
                @Override
                public void onSuccess(String response) {
                    record.setSynced(true);
                    prefsManager.saveHistory(new ArrayList<>(records));
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        loadHistory();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> 
                        Toast.makeText(HistoryActivity.this, 
                            "Sync error: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    @Override
    public void onItemClick(ScannedRecord record) {
        StringBuilder details = new StringBuilder();
        details.append("📝 Raw Text:\n").append(record.getRawText()).append("\n\n");
        details.append("📊 Extracted Data:\n");
        
        Map<String, String> data = record.getExtractedData();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                details.append("• ").append(entry.getKey())
                    .append(": ").append(entry.getValue()).append("\n");
            }
        }

        details.append("\n").append(record.isSynced() ? "✅ Synced" : "⏳ Pending sync");

        new AlertDialog.Builder(this)
            .setTitle("Scan Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    @Override
    public void onResendClick(ScannedRecord record, int position) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> data = record.getExtractedData();
        List<String> columnOrder = new ArrayList<>(data.keySet());

        sheetHelper.appendRow(data, columnOrder, new GoogleSheetHelper.SheetCallback() {
            @Override
            public void onSuccess(String response) {
                record.setSynced(true);
                prefsManager.saveHistory(new ArrayList<>(records));
                adapter.notifyItemChanged(position);
                Toast.makeText(HistoryActivity.this, "Resent successfully! ✅", 
                    Toast.LENGTH_SHORT).show();
                loadHistory();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HistoryActivity.this, 
                    "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sheetHelper != null) {
            sheetHelper.shutdown();
        }
    }
}