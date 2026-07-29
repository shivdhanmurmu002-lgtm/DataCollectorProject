package com.datacollector.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.adapters.FieldAdapter;
import com.datacollector.app.models.DataField;
import com.datacollector.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldSelectorActivity extends AppCompatActivity 
        implements FieldAdapter.OnFieldChangeListener {

    private RecyclerView recyclerView;
    private FieldAdapter adapter;
    private Button btnSave, btnSelectAll, btnClearAll;
    private PrefsManager prefsManager;
    private List<DataField> fields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_selector);

        prefsManager = new PrefsManager(this);
        initViews();
        loadFields();
        setupRecyclerView();
        setupButtons();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvFields);
        btnSave = findViewById(R.id.btnSaveFields);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnClearAll = findViewById(R.id.btnClearAll);
    }

    private void loadFields() {
        fields = prefsManager.getSelectedFields();
        
        if (fields == null || fields.isEmpty()) {
            // Load default fields
            fields = new ArrayList<>(Arrays.asList(DataField.getCommonFields()));
        }
    }

    private void setupRecyclerView() {
        adapter = new FieldAdapter(fields, true, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> {
            prefsManager.saveSelectedFields(adapter.getFields());
            
            int selectedCount = 0;
            for (DataField f : adapter.getFields()) {
                if (f.isSelected()) selectedCount++;
            }
            
            Toast.makeText(this, selectedCount + " fields saved! ✅", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnSelectAll.setOnClickListener(v -> {
            for (DataField f : fields) {
                f.setSelected(true);
            }
            adapter.notifyDataSetChanged();
        });

        btnClearAll.setOnClickListener(v -> {
            for (DataField f : fields) {
                f.setSelected(false);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onFieldSelectionChanged(int position, boolean selected) {
        // Already handled in adapter
    }

    @Override
    public void onFieldPatternChanged(int position, String pattern) {
        // Already handled in adapter
    }
}