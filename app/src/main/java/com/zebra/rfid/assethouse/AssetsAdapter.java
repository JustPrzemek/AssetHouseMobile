package com.zebra.rfid.assethouse;

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
    private List<JSONObject> scannedAssetsList;

    public AssetsAdapter(List<JSONObject> assetList, List<JSONObject> scannedAssetsList) {
        this.assetList = assetList;
        this.scannedAssetsList = scannedAssetsList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JSONObject asset;

        if (position < assetList.size()) {
            asset = assetList.get(position);
        } else {
            asset = scannedAssetsList.get(position - assetList.size());
        }

        String status = asset.optString("status", "UNSCANNED");

        if (status.equals("OK")) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_item_bg);
        } else if (status.equals("NEW")) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_item_bg_new);
        } else if (status.equals("MISSING")) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_item_bg_missing);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        holder.assetId.setText(asset.optString("assetId", "N/A"));
        holder.description.setText(asset.optString("description", "No Description"));
    }

    @Override
    public int getItemCount() {
        return assetList.size() + scannedAssetsList.size();
    }

    public void updateAssets(List<JSONObject> newAssets, List<JSONObject> newscannedAssets) {
        this.assetList.clear();
        this.scannedAssetsList.clear();
//        this.assetList = newAssets;
        this.assetList.addAll(newAssets);
        this.scannedAssetsList.addAll(newscannedAssets);
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
