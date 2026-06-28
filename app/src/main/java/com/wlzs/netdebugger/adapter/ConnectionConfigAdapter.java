package com.wlzs.netdebugger.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.util.ConnectionManager;

import java.util.List;

public class ConnectionConfigAdapter extends RecyclerView.Adapter<ConnectionConfigAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ConnectionManager.ConnectionItem item, int position);
        void onDeleteClick(ConnectionManager.ConnectionItem item, int position);
    }

    private final Context context;
    private final List<ConnectionManager.ConnectionItem> items;
    private final OnItemClickListener listener;

    public ConnectionConfigAdapter(Context context, List<ConnectionManager.ConnectionItem> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_connection_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectionManager.ConnectionItem item = items.get(position);
        holder.tvName.setText(item.name);

        String detail;
        if ("http".equals(item.type) || "ping".equals(item.type)) {
            detail = item.host;
        } else {
            detail = item.host + (item.port != null && !item.port.isEmpty() ? ":" + item.port : "");
        }
        holder.tvDetail.setText(detail);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDetail;
        ImageView ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_config_name);
            tvDetail = itemView.findViewById(R.id.tv_config_detail);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}
