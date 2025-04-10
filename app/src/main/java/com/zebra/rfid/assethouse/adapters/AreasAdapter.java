package com.zebra.rfid.assethouse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zebra.rfid.assethouse.R;
import com.zebra.rfid.assethouse.models.Area;

import java.util.List;
import org.json.JSONObject;

public class AreasAdapter extends RecyclerView.Adapter<AreasAdapter.ViewHolder> {
    private List<Area> areaList;
    private OnAreaClickListener listener;

    public interface OnAreaClickListener {
        void onAreaClick(String areaName);
    }

    public AreasAdapter(List<Area> areaList, OnAreaClickListener listener) {
        this.areaList = areaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Area area = areaList.get(position);
        holder.locationName.setText(area.getLocation());

        String scannedDate = area.getScannedDate();
        holder.scannedDate.setText(scannedDate.isEmpty() || scannedDate.equalsIgnoreCase("null") ? "Unscanned" : scannedDate);

        holder.itemCount.setText("Count: " + area.getCount());

        int backgroundRes = area.isScanned() ?
                R.drawable.rectangle_background_scanned :
                R.drawable.rectangle_background;
        holder.itemView.setBackgroundResource(backgroundRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAreaClick(area.getLocation());
            }
        });
    }

    @Override
    public int getItemCount() {
        return areaList.size();
    }

    public void updateAreas(List<Area> newAreas) {
        this.areaList = newAreas;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationName, scannedDate, itemCount;

        public ViewHolder(View itemView) {
            super(itemView);
            locationName = itemView.findViewById(R.id.locationName);
            scannedDate = itemView.findViewById(R.id.scannedDate);
            itemCount = itemView.findViewById(R.id.itemCount);
        }
    }
}
