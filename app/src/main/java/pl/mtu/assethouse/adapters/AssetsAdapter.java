package pl.mtu.assethouse.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.api.service.AssetService;
import pl.mtu.assethouse.models.Asset;

import java.util.ArrayList;
import java.util.List;

public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.ViewHolder> {
    private List<Asset> assetList;
    private List<Asset> scannedAssetsList;
    private boolean showPlacementView = false;
    private AssetService assetService;

    public AssetsAdapter(List<Asset> assetList, List<Asset> scannedAssetsList, AssetService assetsService) {
        this.assetList = new ArrayList<>(assetList);
        this.scannedAssetsList = new ArrayList<>(scannedAssetsList);
        this.assetService = assetsService;
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
        setupTextViewBehavior(holder.description, displayText);

        holder.itemView.setOnClickListener(v -> {
            showAssetDetailsDialog(v.getContext(), asset);
        });
    }

    private void showAssetDetailsDialog(Context context, Asset asset) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.asset_details);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView assetIdView = new TextView(context);
        setBoldLabelText(assetIdView, "Asset ID: \n", getDisplayText(context, asset.getAssetId()));
        layout.addView(assetIdView);

        TextView descriptionView = new TextView(context);
        setBoldLabelText(descriptionView, "Description: \n", getDisplayText(context, asset.getDescription()));
        layout.addView(descriptionView);

        TextView systemNameView = new TextView(context);
        setBoldLabelText(systemNameView, "System Name: \n", getDisplayText(context, asset.getSystemName()));
        layout.addView(systemNameView);

        TextView locationView = new TextView(context);
        setBoldLabelText(locationView, "Expected Location: \n", getDisplayText(context, asset.getExpectedLocation()));
        layout.addView(locationView);

        Spinner commentsSpinner = new Spinner(context);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item,
                new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        commentsSpinner.setAdapter(spinnerAdapter);

        TextView commentsLabel = new TextView(context);
        commentsLabel.setText(R.string.comment_label);
        layout.addView(commentsLabel);
        layout.addView(commentsSpinner);

        builder.setView(layout);

        new Thread(() -> {
            try {
                List<String> comments = new ArrayList<>(assetService.getPredefinedComments()); // Tworzymy kopię listy
                comments.add(0, "----"); // Dodajemy domyślną wartość tylko do lokalnej kopii

                new Handler(Looper.getMainLooper()).post(() -> {
                    spinnerAdapter.clear();
                    spinnerAdapter.addAll(comments);
                    spinnerAdapter.notifyDataSetChanged();

                    commentsSpinner.setSelection(0); // Ustaw domyślną wartość

                    if (asset.getComment() != null && !asset.getComment().isEmpty() && !asset.getComment().equals("----")) {
                        int position = comments.indexOf(asset.getComment());
                        if (position >= 0) {
                            commentsSpinner.setSelection(position);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String selectedComment = (String) commentsSpinner.getSelectedItem();
            // Zapisz komentarz tylko jeśli nie jest domyślną wartością
            asset.setComment(selectedComment != null && !selectedComment.equals("----") ? selectedComment : "");
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.create().show();
    }
    private void setBoldLabelText(TextView textView, String label, String value) {
        SpannableString spannable = new SpannableString(label + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
    }
    private String getDisplayText(Context context, String value) {
        return value != null ? value : context.getString(R.string.not_available);
    }
    private String getPlacementText(Asset asset) {
        if (asset.isNew()) {
            return asset.getExpectedLocation() != null ? asset.getExpectedLocation() : "No location";
        }
        if (asset.isMissing() || asset.isOk() || asset.isUnscanned()) {
            return asset.getSystemName() != null ? asset.getSystemName() : "No system";
        }
        return asset.getDescription() != null ? asset.getDescription() : "No description";
    }

    private int getBackgroundResource(String status) {
        switch (status) {
            case "NEW": return R.drawable.rounded_item_bg_new;
            case "MISSING": return R.drawable.rounded_item_bg_missing;
            case "OK": return R.drawable.rounded_item_bg_ok;
            default: return R.drawable.rounded_item_bg;
        }
    }

    private void setupTextViewBehavior(TextView textView, String text) {
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(text);
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