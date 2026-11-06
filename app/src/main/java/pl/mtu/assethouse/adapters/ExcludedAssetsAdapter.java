package pl.mtu.assethouse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.models.ExcludedAssets;

import java.util.ArrayList;
import java.util.List;

public class ExcludedAssetsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ASSET = 1;

    private static class Row {
        final String header;
        final ExcludedAssets asset;

        Row(String header, ExcludedAssets asset) {
            this.header = header;
            this.asset = asset;
        }
    }

    private List<Row> rows = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_excluded_header, parent, false));
        }
        return new ViewHolder(inflater.inflate(R.layout.item_excluded_asset, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerTextView.setText(row.header);
            return;
        }

        ViewHolder assetHolder = (ViewHolder) holder;
        ExcludedAssets asset = row.asset;

        if (asset.isScanner()) {
            assetHolder.scannerBadge.setVisibility(View.VISIBLE);
            assetHolder.assetIdTextView.setText(asset.getDisplayName());
            assetHolder.descriptionTextView.setText("Asset ID: " + asset.getAssetId());
            assetHolder.deviceIdTextView.setText("Device ID: " + asset.getDeviceId());
            assetHolder.deviceIdTextView.setVisibility(View.VISIBLE);
        } else {
            assetHolder.scannerBadge.setVisibility(View.GONE);
            assetHolder.assetIdTextView.setText("Asset ID: " + asset.getAssetId());
            assetHolder.descriptionTextView.setText("Description: " + asset.getDescription());
            assetHolder.deviceIdTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_ASSET;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public void setData(List<ExcludedAssets> newAssetsList) {
        List<Row> newRows = new ArrayList<>();

        List<ExcludedAssets> scanners = new ArrayList<>();
        List<ExcludedAssets> exclusions = new ArrayList<>();
        for (ExcludedAssets asset : newAssetsList) {
            if (asset.isScanner()) {
                scanners.add(asset);
            } else {
                exclusions.add(asset);
            }
        }

        if (!scanners.isEmpty()) {
            newRows.add(new Row("REGISTERED SCANNERS (" + scanners.size() + ")", null));
            for (ExcludedAssets scanner : scanners) {
                newRows.add(new Row(null, scanner));
            }
        }
        if (!exclusions.isEmpty()) {
            newRows.add(new Row("EXCLUDED ASSETS (" + exclusions.size() + ")", null));
            for (ExcludedAssets exclusion : exclusions) {
                newRows.add(new Row(null, exclusion));
            }
        }

        this.rows = newRows;
        notifyDataSetChanged();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final TextView headerTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.headerTextView);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView assetIdTextView;
        public final TextView descriptionTextView;
        public final TextView deviceIdTextView;
        public final TextView scannerBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            assetIdTextView = itemView.findViewById(R.id.assetIdTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            deviceIdTextView = itemView.findViewById(R.id.deviceIdTextView);
            scannerBadge = itemView.findViewById(R.id.scannerBadge);
        }
    }
}
