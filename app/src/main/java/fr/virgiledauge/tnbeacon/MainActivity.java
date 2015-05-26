package fr.virgiledauge.tnbeacon;

import android.app.Activity;
/*
Imports pour le Bluetooth LE
 */
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
/*
Imports divers
 */
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
/*
Imports pour l'interface
 */
import android.util.Log;
import android.util.Pair;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
/*
Lib pour l'affichage d'une grande BitMap
 */
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
/*
Imports utilitaires
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static fr.virgiledauge.tnbeacon.GattUuids.*;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    /*
    Définition des TAG pour le LogCat android
     */
    private static final String TAG = "BluetoothGattActivity";
    /*
    Définition des constantes de temps pour le scan
     */
    private static final int SCAN_TIME = 5000; //En ms
    private static final int SCAN_PERIOD= 30000;
    /*
    Sert à filtrer les devices (on ne prends en compte que ceux dont le nom contient DEVICE_NAME
     */
    private  static final boolean PERSISTENT_MODE = true;
    private static final CharSequence DEVICE_NAME = "TNBeacon";

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<DevicePerso> mDevices;
    private BluetoothGatt mConnectedGatt;

    private TextView mTemperature, mBeaconName;
    private SubsamplingScaleImageView image;

    private Timer timer;
    private TimerTask timerTask;

    private ProgressDialog mProgress;
    /*
    Enum des services disponibles
     */
    private enum Services{IRT("IRT"), ACC("ACC"), HUM("HUM"), MAG("MAG"), OPT("OPT"), GYR("GYR"), BAR("BAR");
        private final String symbol;
        private Services(String symbol){
            this.symbol = symbol;
        }
        public String getSymbol(){
            return  this.symbol;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);

        /*
         * Binding de l'interface graphique
         */
        mBeaconName = (TextView) findViewById(R.id.BeaconName_textView);
        mTemperature = (TextView) findViewById(R.id.TempVal_textView);

        //beaconList = new TNBeaconList(this,"BeaconStorage.json");
        ArrayList<TNBeaconData> list = JSONParserPerso.getTNBeaconList(this,"BeaconStorage.json");


        image = (SubsamplingScaleImageView) findViewById(R.id.MapImageView);
        image.setDoubleTapZoomDpi(400);
        image.setImage(ImageSource.resource(R.drawable.map0));

        /*
        Récupération du Bluetooth Adapter commun à tout le système android
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        /*
        Initialisation de la liste de Devices
         */
        mDevices = new ArrayList<>();
        /*
         Une boite de dialog de progression permettra d'afficher l'état de la connexion avec les devices.
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        On vérifie ici que le bluetooth est activé sur le terminal Android
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            /*
            S'il ne l'est pas, on emet une requete pour l'activer à l'utilisateur
             */
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
        On vérifie que la fonctionnalité LE du bluetooth est supportée
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        startTimer();
        clearDisplayValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(this);
        stoptimertask();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        stoptimertask();
    }

    private void clearDisplayValues() {
        //mTemperature.setText("---");
    }
    /*
    Gestion Du Timer de scan
     */
    private void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 100ms the TimerTask will run every SCAN_PERIOD ms
        timer.schedule(timerTask, 100, SCAN_PERIOD); //
    }
    private void stoptimertask() {
         //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                mHandler.post(new Runnable() {
                    public void run() {
                        //show the toast
                        mDevices.clear();
                        startScan();
                        Toast toast = Toast.makeText(getApplicationContext(),"Scan Started for: "+SCAN_TIME+"ms" , Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }
        };
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);
        mHandler.postDelayed(mStopRunnable, SCAN_TIME);
    }
    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        updateData();
    }
    private void updateData(){
        BluetoothDevice device;
        //On trie la liste de devices
        Collections.sort(mDevices);
        if (mDevices.size() != 0){
            device = mDevices.get(0).getDevice();
            mBeaconName.setText(device.getName());
            mConnectedGatt = device.connectGatt(this, false,mGattCallback);
        }else{
            Log.d(TAG,"No Device Detected");
            Toast toast = Toast.makeText(this, "No Device Detected",Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    /* BluetoothAdapter.LeScanCallback */

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New TNBeacon: " + device.getName() + " @ " + rssi);
        /*
         * We are looking for SensorTag devices only, so validate the name
         * that each device reports before adding it to our collection
         */
        if (device.getName().contains(DEVICE_NAME)) {
            mDevices.add(new DevicePerso(device, rssi));
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        private void enableSensor(BluetoothGatt gatt, Services services){
            BluetoothGattCharacteristic characteristic;
            UUID serviceID = UUID_IRT_SERV, confID = UUID_IRT_CONF;
            String SensorName = "";
            switch (services){
                case IRT :
                    serviceID = UUID_IRT_SERV;
                    confID = UUID_IRT_CONF;
                    SensorName = "IRT";
                    break;
                case ACC :
                    serviceID = UUID_ACC_SERV;
                    confID = UUID_ACC_CONF;
                    SensorName = "ACC";
                    break;
                case HUM :
                    serviceID = UUID_HUM_SERV;
                    confID = UUID_HUM_CONF;
                    SensorName = "HUM";
                    break;
                case MAG :
                    serviceID = UUID_MAG_SERV;
                    confID = UUID_MAG_CONF;
                    SensorName = "MAG";
                    break;
                case OPT :
                    serviceID = UUID_OPT_SERV;
                    confID = UUID_OPT_CONF;
                    SensorName = "OPT";
                    break;
                case GYR :
                    serviceID = UUID_GYR_SERV;
                    confID = UUID_GYR_CONF;
                    SensorName = "GYR";
                    break;
                case BAR :
                    serviceID = UUID_BAR_SERV;
                    confID = UUID_BAR_CONF;
                    SensorName = "BAR";
                    break;
            }
            Log.d(TAG, "Enabling "+SensorName+" Sensor");
            characteristic = gatt.getService(serviceID).getCharacteristic(confID);
            characteristic.setValue(new byte[]{0x01});
            mHandler.sendEmptyMessage(MSG_DISMISS);
            gatt.writeCharacteristic(characteristic);
        }

        private void readSensor(BluetoothGatt gatt, Services services){
            BluetoothGattCharacteristic characteristic;
            Pair<UUID,UUID> pair =  setService(services);
            Log.d(TAG, "Reading " + services.getSymbol() + " sensor");
            characteristic = gatt.getService(pair.first)
                    .getCharacteristic(pair.second);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gatt.readCharacteristic(characteristic);
        }
        private Pair<UUID,UUID> setService(Services services){
            UUID serviceID = UUID_IRT_SERV, dataID = UUID_IRT_DATA;
            switch (services){
                case IRT :
                    serviceID = UUID_IRT_SERV;
                    dataID = UUID_IRT_DATA;
                    break;
                case ACC :
                    serviceID = UUID_ACC_SERV;
                    dataID = UUID_ACC_DATA;
                    break;
                case HUM :
                    serviceID = UUID_HUM_SERV;
                    dataID = UUID_HUM_DATA;
                    break;
                case MAG :
                    serviceID = UUID_MAG_SERV;
                    dataID = UUID_MAG_DATA;
                    break;
                case OPT :
                    serviceID = UUID_OPT_SERV;
                    dataID = UUID_OPT_DATA;
                    break;
                case GYR :
                    serviceID = UUID_GYR_SERV;
                    dataID = UUID_GYR_DATA;
                    break;
                case BAR :
                    serviceID = UUID_BAR_SERV;
                    dataID = UUID_BAR_DATA;
                    break;
            }
            return Pair.create(serviceID,dataID);
        }
        private void setNotifySensor(BluetoothGatt gatt, Services services) {
            BluetoothGattCharacteristic characteristic;
            UUID serviceID, dataID;
            String SensorName = "";
            //On récupère les ID correspondant au service et à la donnée désirés
            Pair<UUID,UUID> pair = setService(services);
            Log.d(TAG, "Set notify "+services.getSymbol());
            characteristic = gatt.getService(pair.first).getCharacteristic(pair.second);
            //activation des notifications sur le socket local
            gatt.setCharacteristicNotification(characteristic, true);
            //activation des notifications sur le SensorTag
            BluetoothGattDescriptor desc = characteristic.getDescriptor(UUID_CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                Une fois le service connecté, On cherche les services disponibles
                 */
                mHandler.sendEmptyMessage(MSG_DISMISS);
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 Si l'on se déconnecte, on envoie un message au handler
                 */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 Si une étape échoue, On se déconnecte
                 */
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: " + status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            enableSensor(gatt, Services.IRT);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(UUID_IRT_DATA.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_TEMPERATURE, characteristic));
                Log.d(TAG, "READ: " + characteristic.getUuid());
            }
            if(PERSISTENT_MODE){
                setNotifySensor(gatt,Services.IRT);
            }else{
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            Log.d(TAG, "WRITE: "+characteristic.getUuid());
            readSensor(gatt, Services.IRT);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(UUID_IRT_DATA.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_TEMPERATURE, characteristic));
            }
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    /*
     * We have a Handler to process event results on the main thread
     */
    private static final int MSG_TEMPERATURE = 101;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_TEMPERATURE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining temperature value");
                        return;
                    }
                    updateTemperaturesValues(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    clearDisplayValues();
                    break;
            }
        }
    };

    /* Methods to extract sensor data and update the UI */
    private void updateTemperaturesValues(BluetoothGattCharacteristic characteristic){
        double temperature = SensorTagData.extractAmbientTemperature(characteristic);
        mTemperature.setText(String.format("%.1f\u00B0C", temperature));
    }
}
