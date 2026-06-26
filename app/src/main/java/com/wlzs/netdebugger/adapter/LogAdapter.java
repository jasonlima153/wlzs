package com.wlzs.netdebugger.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.model.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private final Context context;
    private final List<LogEntry> entries = new ArrayList<>();
    private int textColor;
    private int sendColor;
    private int receiveColor;
    private int errorColor;
    private int infoColor;
    private int warnColor;
    private boolean autoScroll = true;

    public LogAdapter(Context context) {
        this.context = context;
        loadColors();
    }

    private void loadColors() {
        textColor = resolveColor(R.attr.textColorPrimary, R.color.text_primary);
        sendColor = resolveColor(R.attr.logSend, R.color.log_send);
        receiveColor = resolveColor(R.attr.logReceive, R.color.log_receive);
        errorColor = resolveColor(R.attr.logError, R.color.log_error);
        infoColor = resolveColor(R.attr.logInfo, R.color.log_info);
        warnColor = resolveColor(R.attr.logWarn, R.color.log_warn);
    }

    private int resolveColor(int attr, int fallback) {
        try {
            int[] attrs = new int[]{attr};
            android.util.TypedArray ta = context.obtainStyledAttributes(attrs);
            int color = ta.getColor(0, context.getResources().getColor(fallback));
            ta.recycle();
            return color;
        } catch (Exception e) {
            return context.getResources().getColor(fallback);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).getType();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogEntry entry = entries.get(position);
        holder.tvTime.setText(entry.getFormattedTime());
        holder.tvMessage.setText(entry.getMessage());

        switch (entry.getType()) {
            case LogEntry.TYPE_SEND:
                holder.tvMessage.setTextColor(sendColor);
                holder.tvTime.setTextColor(sendColor);
                break;
            case LogEntry.TYPE_RECEIVE:
                holder.tvMessage.setTextColor(receiveColor);
                holder.tvTime.setTextColor(receiveColor);
                break;
            case LogEntry.TYPE_ERROR:
                holder.tvMessage.setTextColor(errorColor);
                holder.tvTime.setTextColor(errorColor);
                break;
            case LogEntry.TYPE_WARN:
                holder.tvMessage.setTextColor(warnColor);
                holder.tvTime.setTextColor(warnColor);
                break;
            default:
                holder.tvMessage.setTextColor(infoColor);
                holder.tvTime.setTextColor(infoColor);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void addEntry(LogEntry entry) {
        entries.add(entry);
        notifyItemInserted(entries.size() - 1);
    }

    public void clear() {
        entries.clear();
        notifyDataSetChanged();
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvMessage;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_log_time);
            tvMessage = itemView.findViewById(R.id.tv_log_message);
        }
    }
}
