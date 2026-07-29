package com.datacollector.app.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.R;
import com.datacollector.app.models.DataField;

import java.util.List;

public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.FieldViewHolder> {

    private List<DataField> fields;
    private boolean showPatternEditor;
    private OnFieldChangeListener listener;

    public interface OnFieldChangeListener {
        void onFieldSelectionChanged(int position, boolean selected);
        void onFieldPatternChanged(int position, String pattern);
    }

    public FieldAdapter(List<DataField> fields, boolean showPatternEditor, 
                         OnFieldChangeListener listener) {
        this.fields = fields;
        this.showPatternEditor = showPatternEditor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_field, parent, false);
        return new FieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        DataField field = fields.get(position);
        holder.bind(field, position);
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    public List<DataField> getFields() {
        return fields;
    }

    class FieldViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView fieldName;
        TextView fieldKey;
        EditText patternEdit;

        FieldViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbField);
            fieldName = itemView.findViewById(R.id.tvFieldName);
            fieldKey = itemView.findViewById(R.id.tvFieldKey);
            patternEdit = itemView.findViewById(R.id.etPattern);
        }

        void bind(DataField field, int position) {
            fieldName.setText(field.getFieldName());
            fieldKey.setText("Key: " + field.getFieldKey());
            checkBox.setChecked(field.isSelected());

            if (showPatternEditor && field.getFieldKey().startsWith("custom")) {
                patternEdit.setVisibility(View.VISIBLE);
                patternEdit.setText(field.getPattern());
                patternEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        field.setPattern(s.toString());
                        if (listener != null) {
                            listener.onFieldPatternChanged(position, s.toString());
                        }
                    }
                });
            } else {
                patternEdit.setVisibility(View.GONE);
            }

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                field.setSelected(isChecked);
                if (listener != null) {
                    listener.onFieldSelectionChanged(position, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked());
            });
        }
    }
}