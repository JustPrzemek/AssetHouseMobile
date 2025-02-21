package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zebra.rfid.api3.TagData;
import com.zebra.scannercontrol.SDKHandler;

public class TestActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    public TextView statusTextViewRFID = null;
    public TextView textrfid , scanResult;
    RFIDHandler rfidHandler;
    final static String TAG = "AssetHouse";
    public static SDKHandler sdkHandler;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // RFID Handler
        statusTextViewRFID = (TextView) findViewById(R.id.textViewStatusrfid);
        textrfid = (TextView) findViewById(R.id.edittextrfid);
        scanResult = (TextView) findViewById(R.id.scanResult);
        rfidHandler = new RFIDHandler();
        //rfidHandler.onCreate(this);


        //Scanner Initializations
        //Handling Runtime BT permissions for Android 12 and higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }else{
                rfidHandler.onCreate(this);
            }

        }else{
            rfidHandler.onCreate(this);
        }
    }

    @Override
    public void handleTagdata(TagData[] tagData) {
        final StringBuilder sb = new StringBuilder();
        for (int index = 0; index < tagData.length; index++) {
            String tagId = tagData[index].getTagID();
//            int rssi = tagData[index].getPeakRSSI();

            sb.append(tagId + "\n");
//            sb.append("TAG ID: " + tagId + "  |  RSSI: " + rssi + "\n");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textrfid.append(sb.toString());
            }
        });
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (pressed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textrfid.setText("");
                }
            });
            rfidHandler.performInventory();
        } else
            rfidHandler.stopInventory();
    }

    @Override
    public void barcodeData(String val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanResult.setText("Scan Result : "+val);
            }
        });

    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TestActivity.this,val,Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                rfidHandler.onCreate(this);
            }
            else {
                Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.antenna_settings) {
            String result = rfidHandler.Test1();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.Singulation_control) {
            String result = rfidHandler.Test2();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.Default) {
            String result = rfidHandler.Defaults();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void clearTags(View view) {rfidHandler.clearTags();}

    @Override
    protected void onPause() {
        super.onPause();
        //rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        String result = rfidHandler.onResume();
        statusTextViewRFID.setText(result);
    }

    public void StartInventory(View view)
    {
        textrfid.setText("");
        rfidHandler.performInventory();
        //   rfidHandler.MultiTag();
    }

    public void scanCode(View view) {
        rfidHandler.scanCode();
    }

    public void StopInventory(View view) {
        rfidHandler.stopInventory();
    }

    public void testFunction(View view) {
        rfidHandler.testFunction();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
    }
}