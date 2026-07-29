package com.datacollector.app.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.R;
import com.datacollector.app.models.DataField;

import java.util.List;

public class ScannedDataAdapter extends RecyclerView.Adapter<ScannedDataAdapter.DataViewHolder> {

    private List<DataField> extractedFields;

    public ScannedDataAdapter(List<DataField> extractedFields) {
        this.extractedFields = extractedFields;
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scanned_data, parent, false);
        return new DataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        DataField field = extractedFields.get(position);
        holder.bind(field);
    }

    @Override
    public int getItemCount() {
        return extractedFields.size();
    }

    public List<DataField> getExtractedFields() {
        return extractedFields;
    }

    class DataViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel;
        EditText etValue;

        DataViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvDataLabel);
            etValue = itemView.findViewById(R.id.etDataValue);
        }

        void bind(DataField field) {
            tvLabel.setText(field.getFieldName());
            etValue.setText(field.getExtractedValue());

            etValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    field.setExtractedValue(s.toString());
                }
            });
        }
    }
}