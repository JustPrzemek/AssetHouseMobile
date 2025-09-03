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

public class ExcludedAssetsAdapter extends RecyclerView.Adapter<ExcludedAssetsAdapter.ViewHolder> {

    private List<ExcludedAssets> assetsList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_excluded_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExcludedAssets asset = assetsList.get(position);
        holder.assetIdTextView.setText("Asset ID: " + asset.getAssetId());
        holder.descriptionTextView.setText("Description: " + asset.getDescription());
    }

    @Override
    public int getItemCount() {
        return assetsList.size();
    }

    public void setData(List<ExcludedAssets> newAssetsList) {
        this.assetsList = newAssetsList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView assetIdTextView;
        public final TextView descriptionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            assetIdTextView = itemView.findViewById(R.id.assetIdTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
        }
    }
}