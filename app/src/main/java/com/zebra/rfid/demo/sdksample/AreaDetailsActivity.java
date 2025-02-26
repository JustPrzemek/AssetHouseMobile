package com.zebra.rfid.demo.sdksample;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AreaDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);

        TextView areaNameTextView = findViewById(R.id.areaNameTextView);

        // Get the passed area name
        String areaName = getIntent().getStringExtra("AREA_NAME");
        areaNameTextView.setText(areaName != null ? areaName : "No Area Selected");
    }
}
