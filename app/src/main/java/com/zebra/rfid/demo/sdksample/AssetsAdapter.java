package com.zebra.rfid.demo.sdksample;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.List;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.ViewHolder> {

    private List<JSONObject> assetList;

    public AssetsAdapter(List<JSONObject> assetList) {
        this.assetList = assetList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject asset = assetList.get(position);

        holder.assetId.setText(asset.optString("assetId", "N/A"));
        holder.description.setText(asset.optString("description", "No Description"));
//        holder.inventoryStatus.setText(asset.optString("inventoryStatus", "No Status"));
//        holder.commentCount.setText("Comments: " + asset.optInt("commentCount", 0));

        String status = asset.optString("status", "UNKNOWN");

        if (status.equals("SCANNED")) {
            holder.itemView.setBackgroundColor(Color.GREEN);
        } else if (status.equals("NEW_SCANNED")) {
            holder.itemView.setBackgroundColor(Color.YELLOW);
        } else if (status.equals("NOT_SCANNED")) {
            holder.itemView.setBackgroundColor(Color.RED);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return assetList.size();
    }

    public void updateAssets(List<JSONObject> newAssets) {
        this.assetList = newAssets;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView assetId, description, inventoryStatus, commentCount;

        public ViewHolder(View itemView) {
            super(itemView);
            assetId = itemView.findViewById(R.id.assetId);
            description = itemView.findViewById(R.id.description);
//         inventoryStatus = itemView.findViewById(R.id.inventoryStatus);
//          commentCount = itemView.findViewById(R.id.commentCount);
        }
    }
}
