package com.zebra.rfid.assethouse;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

/**
 * AssetHouse app to connect to the reader,to do inventory and barcode scan
 * App can also set antenna settings and singulation control
 * */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openTest(View view) {
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);
    }

    public void openRooms(View view) {
        Intent intent = new Intent(this, RoomsActivity.class);
        startActivity(intent);
    }
}