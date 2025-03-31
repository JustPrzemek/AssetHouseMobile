package com.zebra.rfid.assethouse;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.List;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.ViewHolder> {

    private List<JSONObject> assetList;
    private List<JSONObject> scannedAssetsList;
    private boolean showPlacementView = false;
    private int selectedPosition = -1;

    public AssetsAdapter(List<JSONObject> assetList, List<JSONObject> scannedAssetsList) {
        this.assetList = assetList;
        this.scannedAssetsList = scannedAssetsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);

        view.setClickable(true);
        view.setForeground(ContextCompat.getDrawable(view.getContext(), R.drawable.ripple_effect));
        return new ViewHolder(view);
    }

    public void setShowPlacementView(boolean showPlacementView) {
        this.showPlacementView = showPlacementView;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JSONObject asset = position < assetList.size() ?
                assetList.get(position) :
                scannedAssetsList.get(position - assetList.size());

        String status = asset.optString("status", "UNSCANNED");

        int bgResource = R.drawable.rounded_item_bg;
        if (status.equals("NEW")) {
            bgResource = R.drawable.rounded_item_bg_new;
        } else if (status.equals("MISSING")) {
            bgResource = R.drawable.rounded_item_bg_missing;
        } else if (!status.equals("OK")) {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
        holder.itemView.setBackgroundResource(bgResource);

        holder.assetId.setText(asset.optString("assetId", "N/A"));


        String displayText = prepareDisplayText(asset, status);
        holder.description.setText(displayText);

        setupTextView(holder.description, displayText);


        holder.itemView.setSelected(position == selectedPosition);
    }

    private String prepareDisplayText(JSONObject asset, String status) {
        String text;
        if (showPlacementView) {
            if (status.equals("NEW")) {
                text = asset.optString("expectedLocation", "No location");
            } else if (status.equals("MISSING")) {
                text = asset.optString("systemName", "No system");
            } else {
                text = asset.optString("description", "No Description");
            }
        } else {
            text = asset.optString("description", "No Description");
        }
        return "null".equals(text) ? "" : text;
    }

    private void setupTextView(TextView textView, String text) {
        textView.setMaxLines(1);
        textView.setEllipsize(text.length() > 30 ? TextUtils.TruncateAt.END : null);
        textView.setClickable(text.length() > 30);

        textView.setOnClickListener(v -> {
            if (text.length() > 30) {
                showFullTextDialog(v.getContext(), text);
            }
        });
    }

    private void showFullTextDialog(Context context, String text) {
        new AlertDialog.Builder(context)
                .setTitle("Full Content")
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
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
            description.setSingleLine(true);
//         inventoryStatus = itemView.findViewById(R.id.inventoryStatus);
//          commentCount = itemView.findViewById(R.id.commentCount);
        }
    }
}
