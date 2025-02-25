package com.zebra.rfid.demo.sdksample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AreasAdapter extends RecyclerView.Adapter<AreasAdapter.AreaViewHolder> {

    private List<String> areas;

    public AreasAdapter(List<String> areas) {
        this.areas = areas;
    }

    @NonNull
    @Override
    public AreaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);
        return new AreaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AreaViewHolder holder, int position) {
        holder.areaTextView.setText(areas.get(position));
    }

    @Override
    public int getItemCount() {
        return areas.size();
    }

    public void updateAreas(List<String> newAreas) {
        this.areas = newAreas;
        notifyDataSetChanged();
    }

    public static class AreaViewHolder extends RecyclerView.ViewHolder {
        TextView areaTextView;

        public AreaViewHolder(@NonNull View itemView) {
            super(itemView);
            areaTextView = itemView.findViewById(R.id.areaTextView);
        }
    }
}
