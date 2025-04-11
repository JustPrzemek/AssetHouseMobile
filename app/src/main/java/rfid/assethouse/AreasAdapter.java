package rfid.assethouse;

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
    private OnAreaClickListener listener;

    public interface OnAreaClickListener {
        void onAreaClick(String areaName);
    }

    public AreasAdapter(List<JSONObject> areaList, OnAreaClickListener listener) {
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
        JSONObject area = areaList.get(position);
        String location = area.optString("location", "Unknown Location");

        holder.locationName.setText(location);

        String scannedDate = area.optString("scannedDate", "").trim();
        holder.scannedDate.setText(scannedDate.isEmpty() || scannedDate.equalsIgnoreCase("null") ? "Unscanned" : scannedDate);

        holder.itemCount.setText("Count: " + area.optInt("count", 0));

        if (!scannedDate.isEmpty() && !scannedDate.equalsIgnoreCase("null")) {
            holder.itemView.setBackgroundResource(R.drawable.rectangle_background_scanned);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.rectangle_background);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAreaClick(location);
            }
        });
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
