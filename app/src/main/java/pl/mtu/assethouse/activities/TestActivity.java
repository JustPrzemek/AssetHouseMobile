package pl.mtu.assethouse.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zebra.rfid.api3.TagData;
import pl.mtu.assethouse.R;

public class TestActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "AssetHouse";
    TextView statusTextViewRFID;
    private TextView textRfid;
    private TextView scanResult;
    private Button switchModeButton;
    private RFIDHandler rfidHandler;
    private Button startButton;
    private Button stopButton;
    private Button clearButton;


    private final BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(getString(R.string.activity_intent_filter_action))) {
                try {
                    displayScanResult(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying scan result", e);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initializeViews();
        checkBluetoothPermissions();
    }

    private void initializeViews() {
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        textRfid = findViewById(R.id.edittextrfid);
        switchModeButton = findViewById(R.id.buttonSwitchMode);
        startButton = findViewById(R.id.TestButton);
        stopButton = findViewById(R.id.TestButton2);
        clearButton = findViewById(R.id.TestButton3);


        rfidHandler = new RFIDHandler();

        switchModeButton.setOnClickListener(v -> switchScanMode());

        IntentFilter filter = new IntentFilter(getString(R.string.activity_intent_filter_action));
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver, filter);
    }

    private void switchScanMode() {
        if (rfidHandler.getCurrentMode() == RFIDHandler.ScanMode.RFID) {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.BARCODE);
            switchModeButton.setText(R.string.switch_to_rfid);
            showToast(R.string.barcode_mode);

            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            clearButton.setEnabled(false);
        } else {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.RFID);
            switchModeButton.setText(R.string.switch_to_barcode);
            showToast(R.string.rfid_mode);

            startButton.setEnabled(true);
            stopButton.setEnabled(true);
            clearButton.setEnabled(true);
        }
    }

    private void displayScanResult(Intent initiatingIntent)
    {
        String data = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));
        String labelType = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_label_type));

        TextView labelTypeView = findViewById(R.id.lblScanDecoder);
        TextView scanResultView = findViewById(R.id.lblScanData);

        scanResultView.setText(getString(R.string.barcode_data) + data);
        labelTypeView.setText(getString(R.string.label_type) + labelType);

    }
    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            initializeRfidHandler();
        }
    }

    private void initializeRfidHandler() {
        rfidHandler.onCreate(this);
    }

    // RFID Interface methods
    @Override
    public void handleTagdata(TagData[] tagData) {
        StringBuilder sb = new StringBuilder();
        for (TagData tag : tagData) {
            sb.append(tag.getTagID()).append("\n");
        }
        updateRfidText(sb.toString());
    }

    private void updateRfidText(String text) {
        runOnUiThread(() -> textRfid.append(text));
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (rfidHandler.getCurrentMode() == RFIDHandler.ScanMode.RFID) {
            runOnUiThread(() -> {
                if (pressed) {
                    textRfid.setText("");
                    rfidHandler.performInventory();
                } else {
                    rfidHandler.stopInventory();
                }
            });
        }
    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> Toast.makeText(this, val, Toast.LENGTH_SHORT).show());
    }

    private void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRfidHandler();
        } else {
            Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearTags(View view) {
        textRfid.setText("");
    }

    public void StartInventory(View view)
    {
        rfidHandler.performInventory();
    }

    public void StopInventory(View view) {
        rfidHandler.stopInventory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        statusTextViewRFID.setText(rfidHandler.onResume());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
    }
}