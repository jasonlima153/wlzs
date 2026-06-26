package com.wlzs.netdebugger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.model.LanDevice;

import java.util.List;

public class LanDeviceAdapter extends RecyclerView.Adapter<LanDeviceAdapter.ViewHolder> {

    private final Context context;
    private final List<LanDevice> devices;

    public LanDeviceAdapter(Context context, List<LanDevice> devices) {
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lan_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanDevice device = devices.get(position);
        holder.tvIp.setText(device.getIp());
        holder.tvMac.setText(device.getMac());
        holder.tvHostname.setText(device.getHostname());
        holder.tvStatus.setText(device.isReachable() ? "在线" : "离线");
        holder.tvStatus.setTextColor(device.isReachable() ?
                context.getResources().getColor(R.color.success) :
                context.getResources().getColor(R.color.error));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIp;
        TextView tvMac;
        TextView tvHostname;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvIp = itemView.findViewById(R.id.tv_device_ip);
            tvMac = itemView.findViewById(R.id.tv_device_mac);
            tvHostname = itemView.findViewById(R.id.tv_device_name);
            tvStatus = itemView.findViewById(R.id.tv_device_status);
        }
    }
}
