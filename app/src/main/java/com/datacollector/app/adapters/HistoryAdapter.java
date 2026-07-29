package com.datacollector.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.datacollector.app.R;
import com.datacollector.app.models.ScannedRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<ScannedRecord> records;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnItemClickListener {
        void onItemClick(ScannedRecord record);
        void onResendClick(ScannedRecord record, int position);
    }

    public HistoryAdapter(List<ScannedRecord> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScannedRecord record = records.get(position);
        holder.bind(record, position);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestamp, tvPreview, tvSyncStatus;
        ImageView ivSyncStatus, ivResend;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvSyncStatus = itemView.findViewById(R.id.tvSyncStatus);
            ivSyncStatus = itemView.findViewById(R.id.ivSyncStatus);
            ivResend = itemView.findViewById(R.id.ivResend);
        }

        void bind(ScannedRecord record, int position) {
            tvTimestamp.setText(dateFormat.format(new Date(record.getTimestamp())));

            // Build preview
            StringBuilder preview = new StringBuilder();
            Map<String, String> data = record.getExtractedData();
            if (data != null) {
                int count = 0;
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    if (count >= 3) {
                        preview.append("...");
                        break;
                    }
                    if (count > 0) preview.append(" | ");
                    preview.append(entry.getKey()).append(": ").append(entry.getValue());
                    count++;
                }
            }
            tvPreview.setText(preview.toString());

            if (record.isSynced()) {
                tvSyncStatus.setText("Synced ✓");
                tvSyncStatus.setTextColor(0xFF4CAF50);
                ivResend.setVisibility(View.GONE);
            } else {
                tvSyncStatus.setText("Pending ⏳");
                tvSyncStatus.setTextColor(0xFFFF9800);
                ivResend.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(record);
            });

            ivResend.setOnClickListener(v -> {
                if (listener != null) listener.onResendClick(record, position);
            });
        }
    }
}