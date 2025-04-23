package pl.mtu.assethouse.adapters;

import android.content.Context;
import android.text.Layout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.models.Asset;

import java.util.ArrayList;
import java.util.List;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.ViewHolder> {
    private List<Asset> assetList;
    private List<Asset> scannedAssetsList;
    private boolean showPlacementView = false;

    public AssetsAdapter(List<Asset> assetList, List<Asset> scannedAssetsList) {
        this.assetList = new ArrayList<>(assetList);
        this.scannedAssetsList = new ArrayList<>(scannedAssetsList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asset asset = getItem(position);
        if (asset == null) return;

        holder.itemView.setBackgroundResource(getBackgroundResource(asset.getStatus()));
        holder.assetId.setText(asset.getAssetId());

        String displayText = showPlacementView ?
                getPlacementText(asset) :
                asset.getDescription();

        holder.description.setText(displayText);
        setupTextViewBehavior(holder.description, displayText, asset.getStatus());
    }

    private String getPlacementText(Asset asset) {
        if (asset.isNew()) return asset.getExpectedLocation();
        if (asset.isMissing() || asset.isOk() || asset.isUnscanned()) return asset.getSystemName();
        return asset.getDescription();
    }

    private int getBackgroundResource(String status) {
        switch (status) {
            case "NEW": return R.drawable.rounded_item_bg_new;
            case "MISSING": return R.drawable.rounded_item_bg_missing;
            case "OK": return R.drawable.rounded_item_bg_ok;
            default: return R.drawable.rounded_item_bg;
        }
    }

    private void setupTextViewBehavior(TextView textView, String text, String status) {
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(text);

        textView.post(() -> {
            if (isTextTruncated(textView)) {
                textView.setOnClickListener(v -> showFullTextDialog(v.getContext(), text, status));
            }
        });
    }

    private boolean isTextTruncated(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int lines = layout.getLineCount();
            if (lines > 0) {
                return layout.getEllipsisCount(lines - 1) > 0;
            }
        }
        return false;
    }

    private void showFullTextDialog(Context context, String text, String status) {
        String dialogTitle = showPlacementView ?
                (status.equals("NEW") ? "Expected Location" :
                        (status.equals("MISSING") || status.equals("OK") || status.equals("UNSCANNED")) ? "System Name" : "Description")
                : "Description";

        new AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return assetList.size() + scannedAssetsList.size();
    }

    public void setShowPlacementView(boolean showPlacementView) {
        this.showPlacementView = showPlacementView;
        notifyDataSetChanged();
    }

    public void updateAssets(List<Asset> newAssets, List<Asset> newScannedAssets) {
        this.assetList = new ArrayList<>(newAssets);
        this.scannedAssetsList = new ArrayList<>(newScannedAssets);
        notifyDataSetChanged();
    }

    private Asset getItem(int position) {
        return position < assetList.size() ?
                assetList.get(position) :
                scannedAssetsList.get(position - assetList.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView assetId;
        final TextView description;

        public ViewHolder(View itemView) {
            super(itemView);
            assetId = itemView.findViewById(R.id.assetId);
            description = itemView.findViewById(R.id.description);
            description.setSingleLine(true);
        }
    }
}