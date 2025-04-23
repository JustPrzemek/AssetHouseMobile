package pl.mtu.assethouse.activities;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.FILTER_ACTION;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.IRFIDLogger;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.LOCK_DATA_FIELD;
import com.zebra.rfid.api3.LOCK_PRIVILEGE;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.PreFilters;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATE_AWARE_ACTION;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TARGET;
import com.zebra.rfid.api3.TRUNCATE_ACTION;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;
import pl.mtu.assethouse.R;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class RFIDHandler implements IDcsSdkApiDelegate, Readers.RFIDReaderEventHandler {

    final static String TAG = "AssetHouse";
    private Readers readers;
    private ArrayList<ReaderDevice> availableRFIDReaderList;
    private ReaderDevice readerDevice;
    private RFIDReader reader;
    TextView textView;
    private EventHandler eventHandler;
    private ResponseHandlerInterface context;
    private SDKHandler sdkHandler;
    private ArrayList<DCSScannerInfo> scannerList;
    private int scannerID;
    static MyAsyncTask cmdExecTask = null;
    private int MAX_POWER = 297;
    private int DEVICE_STD_MODE = 0;
    private int DEVICE_PREMIUM_PLUS_MODE = 1;

    // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
    // If barcode scan is available in RFD8500, for barcode scanning change mode using mode button on RFD8500 device. By default it is set to RFID mode
    String readerNamebt = "RFD40+_211545201D0011";
    String readerName = "RFD4031-G10B700-US";
    String RFD8500 = "RFD8500161755230D5038";

    void onCreate(ResponseHandlerInterface activity) {
        context = activity;
        if (activity instanceof TestActivity) {
            textView = ((TestActivity) activity).statusTextViewRFID;
        } else if (activity instanceof AreaDetailsActivity) {
            textView = ((AreaDetailsActivity) activity).findViewById(R.id.textViewStatusrfid);
        }

        scannerList = new ArrayList<>();
        InitSDK();
    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventScannerDisappeared(int i) {

    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int i) {

    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        String s = new String(barcodeData);
        context.barcodeData(s);
        Log.d(TAG,"barcode ="+ s);
    }

    @Override
    public void dcssdkEventImage(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventVideo(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventBinaryData(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {

    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo dcsScannerInfo, DCSScannerInfo dcsScannerInfo1) {

    }



// TEST BUTTON functionality
    // following two tests are to try out different configurations features

    public String Test1() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // set antenna configurations - reducing power to 200
        try {
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(100);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Antenna power Set to 220";
    }



    public String Test2() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // Set the singulation control to S2 which will read each tag once only
        try {
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S2);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Session set to S2";
    }

    public String Defaults() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";;
        try {
            // Power to 270
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            // singulation to S0
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S0);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Default settings applied";
    }

    private boolean isReaderConnected() {
        if (reader != null && reader.isConnected())
            return true;
        else {
            Log.d(TAG, "reader is not connected");
            return false;
        }
    }

    //
    //  Activity life cycle behavior
    //

    String onResume() {
        return connect();
    }

    void onPause() {
        disconnect();
    }

     void onDestroy() {
        dispose();
    }

    //
    // RFID SDK
    //
    private void InitSDK() {
        Log.d(TAG, "InitSDK");
        if (readers == null) {
            new CreateInstanceTask().execute();
        } else
            connectReader();
    }

    public void testFunction() {
        setPreFilters();
        //testReadevent();
    }

    private void testReadevent() {
        TagAccess tagAccess = new TagAccess();
        TagAccess.LockAccessParams lockAccessParams =  tagAccess.new LockAccessParams();
        lockAccessParams.setLockPrivilege(LOCK_DATA_FIELD.LOCK_USER_MEMORY, LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE);
        lockAccessParams.setAccessPassword(Long.decode("0X" + "12341234"));
        try {
            reader.Actions.TagAccess.lockEvent(lockAccessParams,null,null);
        } catch (InvalidUsageException e) {
            throw new RuntimeException(e);
        } catch (OperationFailureException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPreFilters() {
        Log.d("setPrefilter", "setPrefilter...");
        PreFilters.PreFilter[] preFilterArray = new PreFilters.PreFilter[3];

        PreFilters filters = new PreFilters();
        PreFilters.PreFilter filter = filters.new PreFilter();
        filter.setAntennaID((short) 1);// Set this filter for Antenna ID 1
        filter.setTagPattern("000000000000000000000282");// Tags which starts with passed pattern
        filter.setTagPatternBitCount(96);
        filter.setBitOffset(32); // skip PC bits (always it should be in bit length)
        filter.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
        filter.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
        filter.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
        filter.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
        filter.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
        preFilterArray[0] = filter;

        PreFilters filters1 = new PreFilters();
        PreFilters.PreFilter filter1 = filters1.new PreFilter();
        filter1.setAntennaID((short) 1);// Set this filter for Antenna ID 1
        filter1.setTagPattern("010000000000000000000296");// Tags which starts with passed pattern
        filter1.setTagPatternBitCount(96);
        filter1.setBitOffset(32); // skip PC bits (always it should be in bit length)
        filter1.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
        filter1.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
        filter1.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
        filter1.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
        filter1.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
        preFilterArray[1] = filter1;

        PreFilters filters2 = new PreFilters();
        PreFilters.PreFilter filter2 = filters2.new PreFilter();
        filter2.setAntennaID((short) 1);// Set this filter for Antenna ID 1
        filter2.setTagPattern("101010101010444455556666");// Tags which starts with passed pattern
        filter2.setTagPatternBitCount(96);
        filter2.setBitOffset(32); // skip PC bits (always it should be in bit length)
        filter2.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
        filter2.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
        filter2.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
        filter2.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
        filter2.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
        preFilterArray[2] = filter2;

//        PreFilters filters3 = new PreFilters();
//        PreFilters.PreFilter filter3 = filters3.new PreFilter();
//        filter3.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter3.setTagPattern("03000000000000000000029A");// Tags which starts with passed pattern
//        filter3.setTagPatternBitCount(96);
//        filter3.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter3.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter3.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter3.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter3.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter3.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[3] = filter3;
//
//        PreFilters filters4 = new PreFilters();
//        PreFilters.PreFilter filter4 = filters4.new PreFilter();
//        filter4.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter4.setTagPattern("00000000000000000000027C");// Tags which starts with passed pattern
//        filter4.setTagPatternBitCount(96);
//        filter4.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter4.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter4.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter4.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter4.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter4.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[4] = filter4;
//
//        PreFilters filters5 = new PreFilters();
//        PreFilters.PreFilter filter5 = filters5.new PreFilter();
//        filter5.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter5.setTagPattern("AD93150047F311970E0000FC");// Tags which starts with passed pattern
//        filter5.setTagPatternBitCount(96);
//        filter5.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter5.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter5.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter5.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter5.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter5.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[5] = filter5;
//
//        PreFilters filters6 = new PreFilters();
//        PreFilters.PreFilter filter6 = filters6.new PreFilter();
//        filter6.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter6.setTagPattern("301203447334C3100002EBB4");// Tags which starts with passed pattern
//        filter6.setTagPatternBitCount(96);
//        filter6.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter6.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter6.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter6.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter6.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter6.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[6] = filter6;
//
//        PreFilters filters7 = new PreFilters();
//        PreFilters.PreFilter filter7 = filters7.new PreFilter();
//        filter7.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter7.setTagPattern("302203447334C3100002ED06");// Tags which starts with passed pattern
//        filter7.setTagPatternBitCount(96);
//        filter7.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter7.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter7.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter7.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter7.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter7.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[7] = filter7;
//
//        PreFilters filters8 = new PreFilters();
//        PreFilters.PreFilter filter8 = filters8.new PreFilter();
//        filter8.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter8.setTagPattern("AD93150047F3218C090000FE");// Tags which starts with passed pattern
//        filter8.setTagPatternBitCount(96);
//        filter8.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter8.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter8.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter8.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter8.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter8.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[8] = filter8;
//
//        PreFilters filters9 = new PreFilters();
//        PreFilters.PreFilter filter9 = filters9.new PreFilter();
//        filter9.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter9.setTagPattern("00000000000000000000027D");// Tags which starts with passed pattern
//        filter9.setTagPatternBitCount(96);
//        filter9.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter9.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter9.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter9.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter9.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter9.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[9] = filter9;
//
//        PreFilters filters10 = new PreFilters();
//        PreFilters.PreFilter filter10 = filters10.new PreFilter();
//        filter10.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter10.setTagPattern("00000000000000000000027A");// Tags which starts with passed pattern
//        filter10.setTagPatternBitCount(96);
//        filter10.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter10.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter10.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter10.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter10.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter10.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[10] = filter10;
//
//        PreFilters filters11 = new PreFilters();
//        PreFilters.PreFilter filter11 = filters11.new PreFilter();
//        filter11.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter11.setTagPattern("AD93150047F309850C0000FB");// Tags which starts with passed pattern
//        filter11.setTagPatternBitCount(96);
//        filter11.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter11.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter11.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter11.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter11.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter11.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[11] = filter11;
//
//        PreFilters filters12 = new PreFilters();
//        PreFilters.PreFilter filter12 = filters12.new PreFilter();
//        filter12.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter12.setTagPattern("E280689400004001B043B0FD");// Tags which starts with passed pattern
//        filter12.setTagPatternBitCount(96);
//        filter12.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter12.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter12.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter12.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter12.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter12.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[12] = filter12;
//
//        PreFilters filters13 = new PreFilters();
//        PreFilters.PreFilter filter13 = filters13.new PreFilter();
//        filter13.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter13.setTagPattern("308011700000020D2A413321");// Tags which starts with passed pattern
//        filter13.setTagPatternBitCount(96);
//        filter13.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter13.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter13.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter13.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter13.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter13.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[13] = filter13;
//
//        PreFilters filters14 = new PreFilters();
//        PreFilters.PreFilter filter14 = filters14.new PreFilter();
//        filter14.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter14.setTagPattern("00000000000000000000027F");// Tags which starts with passed pattern
//        filter14.setTagPatternBitCount(96);
//        filter14.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter14.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter14.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter14.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter14.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter14.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[14] = filter14;
//
//        PreFilters filters15 = new PreFilters();
//        PreFilters.PreFilter filter15 = filters15.new PreFilter();
//        filter15.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter15.setTagPattern("020000000000000000000298");// Tags which starts with passed pattern
//        filter15.setTagPatternBitCount(96);
//        filter15.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter15.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter15.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter15.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter15.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter15.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[15] = filter15;
//
//        PreFilters filters16 = new PreFilters();
//        PreFilters.PreFilter filter16 = filters16.new PreFilter();
//        filter16.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter16.setTagPattern("000000000000000000000280");// Tags which starts with passed pattern
//        filter16.setTagPatternBitCount(96);
//        filter16.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter16.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter16.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter16.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter16.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter16.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[16] = filter16;
//
//        PreFilters filters17 = new PreFilters();
//        PreFilters.PreFilter filter17 = filters17.new PreFilter();
//        filter17.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter17.setTagPattern("E280689400004001B043B0FB");// Tags which starts with passed pattern
//        filter17.setTagPatternBitCount(96);
//        filter17.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter17.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter17.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter17.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter17.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter17.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[17] = filter17;
//
//        PreFilters filters18 = new PreFilters();
//        PreFilters.PreFilter filter18 = filters18.new PreFilter();
//        filter18.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter18.setTagPattern("000000000000");// Tags which starts with passed pattern
//        filter18.setTagPatternBitCount(48);
//        filter18.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter18.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter18.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter18.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter18.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter18.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[18] = filter18;
//
//        PreFilters filters19 = new PreFilters();
//        PreFilters.PreFilter filter19 = filters19.new PreFilter();
//        filter19.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter19.setTagPattern("12121313141415150000072500000000");// Tags which starts with passed pattern
//        filter19.setTagPatternBitCount(96);
//        filter19.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter19.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter19.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter19.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter19.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter19.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[19] = filter19;
//
//        PreFilters filters20 = new PreFilters();
//        PreFilters.PreFilter filter20 = filters20.new PreFilter();
//        filter20.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter20.setTagPattern("21000000000000000000073100000000");// Tags which starts with passed pattern
//        filter20.setTagPatternBitCount(96);
//        filter20.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter20.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter20.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter20.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter20.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter20.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[20] = filter20;
//
//        PreFilters filters21 = new PreFilters();
//        PreFilters.PreFilter filter21 = filters21.new PreFilter();
//        filter21.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter21.setTagPattern("19000000000000000000029D");// Tags which starts with passed pattern
//        filter21.setTagPatternBitCount(96);
//        filter21.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter21.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter21.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter21.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter21.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter21.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[21] = filter21;
//
//        PreFilters filters22 = new PreFilters();
//        PreFilters.PreFilter filter22 = filters22.new PreFilter();
//        filter22.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter22.setTagPattern("00112233445566778899AABB");// Tags which starts with passed pattern
//        filter22.setTagPatternBitCount(96);
//        filter22.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter22.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter22.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter22.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter22.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter22.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[22] = filter22;
//
//        PreFilters filters23 = new PreFilters();
//        PreFilters.PreFilter filter23 = filters23.new PreFilter();
//        filter23.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter23.setTagPattern("00000000000000000000027B");// Tags which starts with passed pattern
//        filter23.setTagPatternBitCount(96);
//        filter23.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter23.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter23.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter23.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter23.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter23.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[23] = filter23;
//
//        PreFilters filters24 = new PreFilters();
//        PreFilters.PreFilter filter24 = filters24.new PreFilter();
//        filter24.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter24.setTagPattern("000000000000000000000283");// Tags which starts with passed pattern
//        filter24.setTagPatternBitCount(96);
//        filter24.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter24.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter24.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter24.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter24.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter24.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[24] = filter24;
//
//        PreFilters filters25 = new PreFilters();
//        PreFilters.PreFilter filter25 = filters25.new PreFilter();
//        filter25.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter25.setTagPattern("19850000000000000000072900000000");// Tags which starts with passed pattern
//        filter25.setTagPatternBitCount(96);
//        filter25.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter25.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter25.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter25.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter25.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter25.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[25] = filter25;
//
//        PreFilters filters26 = new PreFilters();
//        PreFilters.PreFilter filter26 = filters26.new PreFilter();
//        filter26.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter26.setTagPattern("3080689400005001B043B0B1");// Tags which starts with passed pattern
//        filter26.setTagPatternBitCount(96);
//        filter26.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter26.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter26.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter26.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter26.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter26.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[26] = filter26;
//
//        PreFilters filters27 = new PreFilters();
//        PreFilters.PreFilter filter27 = filters27.new PreFilter();
//        filter27.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter27.setTagPattern("04000000000000000000029E");// Tags which starts with passed pattern
//        filter27.setTagPatternBitCount(96);
//        filter27.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter27.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter27.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter27.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter27.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter27.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[27] = filter27;
//
//        PreFilters filters28 = new PreFilters();
//        PreFilters.PreFilter filter28 = filters28.new PreFilter();
//        filter28.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter28.setTagPattern("03000000000000000000029C");// Tags which starts with passed pattern
//        filter28.setTagPatternBitCount(96);
//        filter28.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter28.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter28.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter28.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter28.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter28.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[28] = filter28;
//
//        PreFilters filters29 = new PreFilters();
//        PreFilters.PreFilter filter29 = filters29.new PreFilter();
//        filter29.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter29.setTagPattern("000000000000000000000284");// Tags which starts with passed pattern
//        filter29.setTagPatternBitCount(96);
//        filter29.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter29.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter29.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter29.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter29.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter29.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[29] = filter29;
//
//        PreFilters filters30 = new PreFilters();
//        PreFilters.PreFilter filter30 = filters30.new PreFilter();
//        filter30.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter30.setTagPattern("00000000000000000000027E");// Tags which starts with passed pattern
//        filter30.setTagPatternBitCount(96);
//        filter30.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter30.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter30.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter30.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter30.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter30.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[30] = filter30;
//
//        PreFilters filters31 = new PreFilters();
//        PreFilters.PreFilter filter31 = filters31.new PreFilter();
//        filter31.setAntennaID((short) 1);// Set this filter for Antenna ID 1
//        filter31.setTagPattern("000000000000000000000281");// Tags which starts with passed pattern
//        filter31.setTagPatternBitCount(96);
//        filter31.setBitOffset(32); // skip PC bits (always it should be in bit length)
//        filter31.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
//        filter31.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE); // use state aware singulation
//        filter31.StateAwareAction.setTarget(TARGET.TARGET_SL); // inventoried flag of session S1 of matching tags to B
//        filter31.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL);
//        filter31.setTruncateAction(TRUNCATE_ACTION.TRUNCATE_ACTION_DO_NOT_TRUNCATE);
//        preFilterArray[31] = filter31;



        // not to select tags that match the criteria


        try {
            Log.d("setSingulationControl", "SingulationControl...");

            Antennas.SingulationControl singulationControl = new Antennas.SingulationControl();
            //singulationControl // mRfidReader.Config.Antennas.getSingulationControl(1);

            singulationControl.setSession(SESSION.SESSION_S2);
            singulationControl.setTagPopulation((short) 32);
            singulationControl.Action.setSLFlag(SL_FLAG.SL_FLAG_ASSERTED);
            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_AB_FLIP);
            // mRfidReader.Config.Antennas.setSingulationControl(1, singulationControl);
            reader.Actions.PreFilters.deleteAll();
            reader.Actions.PreFilters.add(preFilterArray, null);
            reader.Config.setUniqueTagReport(true);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

    }

    public void clearTags() {

    }

    // Enumerates SDK based on host device
    private class CreateInstanceTask extends AsyncTask<Void, Void, Void> {
        private InvalidUsageException invalidUsageException = null;
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "CreateInstanceTask");
            try {
                readers = new Readers((Context) context, ENUM_TRANSPORT.SERVICE_USB);
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in SERVICE_USB Transport trying with BLUETOOTH transport");
                    readers.setTransport(ENUM_TRANSPORT.BLUETOOTH);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in BLUETOOTH Transport trying with SERVICE_SERIAL transport");
                    readers.setTransport(ENUM_TRANSPORT.SERVICE_SERIAL);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in SERVICE_SERIAL Transport trying with RE_SERIAL transport");
                    readers.setTransport(ENUM_TRANSPORT.RE_SERIAL);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
            } catch (InvalidUsageException e) {
                invalidUsageException = e;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (invalidUsageException != null) {
                context.sendToast("Failed to get Available Readers\n"+invalidUsageException.getInfo());
                readers = null;
            } else if (availableRFIDReaderList.isEmpty()) {
                context.sendToast("No Available Readers to proceed");
                readers = null;
            } else {
                connectReader();
            }
        }
    }

    private synchronized void connectReader(){
        if(!isReaderConnected()){
            new ConnectionTask().execute();
        }
    }

    private class ConnectionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "ConnectionTask");
            GetAvailableReader();
            if (reader != null)
                return connect();
            return "Failed to find or connect reader";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            textView.setText(result);
        }
    }

    private synchronized void GetAvailableReader() {
        Log.d(TAG, "GetAvailableReader");
        if (readers != null) {
            readers.attach(this);
            try {
                ArrayList<ReaderDevice> availableReaders = readers.GetAvailableRFIDReaderList();
                if (availableReaders != null && !availableReaders.isEmpty()) {
                    availableRFIDReaderList = availableReaders;
                    // if single reader is available then connect it
                    Log.e(TAG,"Available readers to connect = "+availableRFIDReaderList.size());
                    if (availableRFIDReaderList.size() == 1) {
                        readerDevice = availableRFIDReaderList.get(0);
                        reader = readerDevice.getRFIDReader();
                    } else {
                        // search reader specified by name
                        for (ReaderDevice device : availableRFIDReaderList) {
                            Log.d(TAG,"device: "+device.getName());
                            if (device.getName().startsWith(readerName)) {

                                readerDevice = device;
                                reader = readerDevice.getRFIDReader();

                            }
                        }
                    }
                }
            }catch (InvalidUsageException ie){
                ie.printStackTrace();
            }

        }
    }

    // handler for receiving reader appearance events
    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
        context.sendToast("RFIDReaderAppeared");
        connectReader();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
        context.sendToast("RFIDReaderDisappeared");
        if (readerDevice.getName().equals(reader.getHostName()))
            disconnect();
    }


    private synchronized String connect() {
        if (reader != null) {
            Log.d(TAG, "connect " + reader.getHostName());
            try {
                if (!reader.isConnected()) {
                    // Establish connection to the RFID Reader
                    reader.connect();
                    ConfigureReader();

                    //Call this function if the readerdevice supports scanner to setup scanner SDK
                    //setupScannerSDK();
                    if(reader.isConnected()){
                        return "Connected: " + reader.getHostName();
                    }

                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                String des = e.getResults().toString();
                return "Connection failed" + e.getVendorMessage() + " " + des;
            }
        }
        return "";
    }

    private void ConfigureReader() {
        Log.d(TAG, "ConfigureReader " + reader.getHostName());
        IRFIDLogger.getLogger("SDKSAmpleApp").EnableDebugLogs(true);
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
//                int[] powerLevels = reader.ReaderCapabilities.getTransmitPowerLevelValues();
//                for (int power : powerLevels) {
//                    Log.d(TAG, "Available power level: " + power);
//                }
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                //
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupScannerSDK(){
        if (sdkHandler == null)
        {
            sdkHandler = new SDKHandler((Context) context);
            //For cdc device
            DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);

            //For bluetooth device
           DCSSDKDefs.DCSSDK_RESULT btResult = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
            DCSSDKDefs.DCSSDK_RESULT btNormalResult = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);

            Log.d(TAG,btNormalResult+ " results "+ btResult);
            sdkHandler.dcssdkSetDelegate(this);

            int notifications_mask = 0;
            // We would like to subscribe to all scanner available/not-available events
            notifications_mask |= DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value;

            // We would like to subscribe to all scanner connection events
            notifications_mask |= DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;


            // We would like to subscribe to all barcode events
            // subscribe to events set in notification mask
            sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
        }
        if (sdkHandler != null)
        {
            ArrayList<DCSScannerInfo> availableScanners = new ArrayList<>();
            availableScanners  = (ArrayList<DCSScannerInfo>) sdkHandler.dcssdkGetAvailableScannersList();

            scannerList.clear();
            if (availableScanners != null)
            {
                for (DCSScannerInfo scanner : availableScanners)
                {

                    scannerList.add(scanner);
                }
            }
            else
                Log.d(TAG,"Available scanners null");

        }
        if (reader != null )
        {
            for (DCSScannerInfo device : scannerList)
            {
                if (device.getScannerName().contains(reader.getHostName()))
                {
                    try
                    {
                        sdkHandler.dcssdkEstablishCommunicationSession(device.getScannerID());
                        scannerID= device.getScannerID();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "Disconnect");
        try {
            if (reader != null) {
                if (eventHandler != null)
                    reader.Events.removeEventsListener(eventHandler);
                if (sdkHandler != null) {
                    sdkHandler.dcssdkTerminateCommunicationSession(scannerID);
                    scannerList = null;
                }
                reader.disconnect();
                //context.sendToast("Disconnecting reader");
                //reader = null;
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void dispose() {
        disconnect();
        try {
            if (reader != null) {
                //Toast.makeText(getApplicationContext(), "Disconnecting reader", Toast.LENGTH_LONG).show();
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void performInventory() {
        try {
            reader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    synchronized void stopInventory() {
        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }
    public void scanCode(){
        String in_xml = "<inArgs><scannerID>" + scannerID+ "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    private class MyAsyncTask extends AsyncTask<String, Integer, Boolean> {
        int scannerId;
        StringBuilder outXML;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        ///private CustomProgressDialog progressDialog;

        public MyAsyncTask(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, StringBuilder outXML) {
            this.scannerId = scannerId;
            this.opcode = opcode;
            this.outXML = outXML;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected Boolean doInBackground(String... strings) {
            return executeCommand(opcode, strings[0], outXML, scannerId);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
        }
    }
    public boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
        if (sdkHandler != null)
        {
            if(outXML == null){
                outXML = new StringBuilder();
            }
            DCSSDKDefs.DCSSDK_RESULT result=sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode,inXML,outXML,scannerID);
            Log.d(TAG, "execute command returned " + result.toString() );
            if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
                return true;
            else if(result==DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
                return false;
        }
        return false;
    }
    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                List<TagData> filteredTags = new ArrayList<>();

                for (TagData tag : myTags) {
                    String tagIdHex = tag.getTagID();
                    String tagIdDecoded = hexToAscii(tagIdHex);

                    if (tagIdDecoded == null) continue;

                    Log.d(TAG, "Tag ID (HEX): " + tagIdHex + " | Dekodowany: " + tagIdDecoded);
                    tag.setTagID(tagIdDecoded);
                    filteredTags.add(tag);
                }

                if (!filteredTags.isEmpty()) {
                    new AsyncDataUpdate().execute(filteredTags.toArray(new TagData[0]));
                }
            }
        }

        private String hexToAscii(String hexStr) {
            StringBuilder output = new StringBuilder();

            for (int i = 0; i < hexStr.length(); i += 2) {
                String str = hexStr.substring(i, Math.min(i + 2, hexStr.length()));
                try {
                    int decimal = Integer.parseInt(str, 16);
                    output.append((char) decimal);
                } catch (NumberFormatException e) {
                    Log.e("RFID", "Błąd konwersji HEX na ASCII: " + str);
                }
            }
            return output.toString();
        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            Log.d(TAG,"HANDHELD_TRIGGER_PRESSED");
                            context.handleTriggerPress(true);
                            return null;
                        }
                    }.execute();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            context.handleTriggerPress(false);
                            Log.d(TAG,"HANDHELD_TRIGGER_RELEASED");
                            return null;
                        }
                    }.execute();
                }
            }
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {

                        disconnect();
                        return null;
                    }
                }.execute();
            }

        }
    }

    private class AsyncDataUpdate extends AsyncTask<TagData[], Void, Void> {
        @Override
        protected Void doInBackground(TagData[]... params) {
            context.handleTagdata(params[0]);

            return null;
        }
    }

    interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);

        void handleTriggerPress(boolean pressed);

        void barcodeData(String val);
        void sendToast(String val);
        //void handleStatusEvents(Events.StatusEventData eventData);
    }

}
