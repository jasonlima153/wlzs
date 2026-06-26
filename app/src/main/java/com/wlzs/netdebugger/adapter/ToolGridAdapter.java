package com.wlzs.netdebugger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.model.ToolItem;

import java.util.List;

public class ToolGridAdapter extends RecyclerView.Adapter<ToolGridAdapter.ViewHolder> {

    private final Context context;
    private final List<ToolItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ToolItem item, int position);
    }

    public ToolGridAdapter(Context context, List<ToolItem> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tool_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToolItem item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());
        holder.ivIcon.setImageResource(item.getIcon());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvDesc;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_tool_icon);
            tvName = itemView.findViewById(R.id.tv_tool_name);
            tvDesc = itemView.findViewById(R.id.tv_tool_desc);
        }
    }
}
