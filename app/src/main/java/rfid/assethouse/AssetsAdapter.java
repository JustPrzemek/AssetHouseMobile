package rfid.assethouse;

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

import org.json.JSONObject;
import java.util.List;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.ViewHolder> {

    private List<JSONObject> assetList;
    private List<JSONObject> scannedAssetsList;
    private boolean showPlacementView = false;

    public AssetsAdapter(List<JSONObject> assetList, List<JSONObject> scannedAssetsList) {
        this.assetList = assetList;
        this.scannedAssetsList = scannedAssetsList;
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

        JSONObject asset = getItem(position);
        if (asset == null) return;

        String status = asset.optString("status", "UNSCANNED").toUpperCase();
        holder.itemView.setBackgroundResource(getBackgroundResource(status));

        holder.assetId.setText(asset.optString("assetId", "N/A"));
        String displayText = prepareDisplayText(asset, status);
        holder.description.setText(displayText);

        setupTextViewBehavior(holder.description, displayText, status);
    }

    private int getBackgroundResource(String status) {
        switch (status) {
            case "NEW": return R.drawable.rounded_item_bg_new;
            case "MISSING": return R.drawable.rounded_item_bg_missing;
            case "OK": return R.drawable.rounded_item_bg_ok;
            default: return R.drawable.rounded_item_bg;
        }
    }

    private String prepareDisplayText(JSONObject asset, String status) {
        String text;
        if (showPlacementView) {
            if (status.equals("NEW")) {
                text = asset.optString("expectedLocation", "No location");
            } else if (status.equals("MISSING") || status.equals("OK") || status.equals("UNSCANNED")) {
                text = asset.optString("systemName", "No system");
            } else {
                text = asset.optString("description", "No Description");
            }
        } else {
            text = asset.optString("description", "No Description");
        }
        return "null".equals(text) ? "" : text;
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

    public void updateAssets(List<JSONObject> newAssets, List<JSONObject> newScannedAssets) {
        assetList.clear();
        scannedAssetsList.clear();
        assetList.addAll(newAssets);
        scannedAssetsList.addAll(newScannedAssets);
        notifyDataSetChanged();
    }

    private JSONObject getItem(int position) {
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