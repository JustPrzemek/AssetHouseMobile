package rfid.assethouse.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import rfid.assethouse.R;
import rfid.assethouse.models.Area;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AreasAdapter extends RecyclerView.Adapter<AreasAdapter.ViewHolder> {
    private List<Area> areaList;
    private OnAreaClickListener listener;

    public interface OnAreaClickListener {
        void onAreaClick(String areaName);
    }

    public AreasAdapter(List<Area> areaList, OnAreaClickListener listener) {
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
        Area area = areaList.get(position);
        holder.locationName.setText(area.getLocation());

        String displayDate = formatScannedDate(area.getScannedDate());
        holder.scannedDate.setText(displayDate);

        holder.itemCount.setText("Count: " + area.getCount());

        int backgroundRes = area.isScanned() ?
                R.drawable.rectangle_background_scanned :
                R.drawable.rectangle_background;
        holder.itemView.setBackgroundResource(backgroundRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAreaClick(area.getLocation());
            }
        });
    }

    private String formatScannedDate(String dateString) {
        if (dateString == null || dateString.isEmpty() ||
                dateString.equalsIgnoreCase("null") ||
                dateString.equalsIgnoreCase("undefined")) {
            return "Unscanned";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e("DateParse", "Error parsing date: " + dateString, e);
            return dateString;
        }
    }

    @Override
    public int getItemCount() {
        return areaList.size();
    }

    public void updateAreas(List<Area> newAreas) {
        this.areaList.clear();
        if (newAreas != null) {
            this.areaList.addAll(newAreas);
        }
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
