package pl.mtu.assethouse.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
    private RFIDHandler rfidHandler;
    private Button switchModeButton;

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
        rfidHandler = new RFIDHandler();

        switchModeButton = findViewById(R.id.buttonSwitchMode);
        rfidHandler = new RFIDHandler();

        switchModeButton.setOnClickListener(v -> switchScanMode());

        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.activity_intent_filter_action));
        registerReceiver(myBroadcastReceiver, filter);
    }

    private void switchScanMode() {
        if (rfidHandler.getCurrentMode() == RFIDHandler.ScanMode.RFID) {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.BARCODE);
            switchModeButton.setText(R.string.switch_to_rfid);
            Toast.makeText(this, R.string.barcode_mode, Toast.LENGTH_SHORT).show();
        } else {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.RFID);
            switchModeButton.setText(R.string.switch_to_barcode);
            Toast.makeText(this, R.string.rfid_mode, Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();

            assert action != null;
            if (action.equals(getResources().getString(R.string.activity_intent_filter_action))) {
                //  Received a barcode scan
                try {
                    displayScanResult(intent);
                } catch (Exception e) {
                    //  Catch if the UI does not exist when we receive the broadcast
                }
            }
        }
    };

    private void displayScanResult(Intent initiatingIntent)
    {
        String decodedData = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));
        String decodedLabelType = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_label_type));

        final TextView lblScanData = (TextView) findViewById(R.id.lblScanData);
        final TextView lblScanLabelType = (TextView) findViewById(R.id.lblScanDecoder);

        lblScanData.setText(R.string.barcode_data + decodedData);
        lblScanLabelType.setText(R.string.label_type + decodedLabelType);

    }
    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                initializeRfidHandler();
            }
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
    public void barcodeData(String val) {
        runOnUiThread(() -> scanResult.setText("Scan Result : " + val));
    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> Toast.makeText(this, val, Toast.LENGTH_SHORT).show());
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


    public void clearTags(View view) {
        textRfid.setText("");
    }

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