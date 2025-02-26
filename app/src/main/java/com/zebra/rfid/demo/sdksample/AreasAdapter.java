package com.zebra.rfid.demo.sdksample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.json.JSONObject;

public class AreasAdapter extends RecyclerView.Adapter<AreasAdapter.ViewHolder> {

    private List<JSONObject> areaList;

    public AreasAdapter(List<JSONObject> areaList) {
        this.areaList = areaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject area = areaList.get(position);

        holder.locationName.setText(area.optString("location", "Unknown Location"));
        holder.scannedDate.setText(area.optString("scannedDate", "No Date Available"));
        holder.itemCount.setText("Count: " + area.optInt("count", 0));
    }

    @Override
    public int getItemCount() {
        return areaList.size();
    }

    public void updateAreas(List<JSONObject> newAreas) {
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
