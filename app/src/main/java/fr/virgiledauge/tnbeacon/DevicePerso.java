package fr.virgiledauge.tnbeacon;

import android.bluetooth.BluetoothDevice;

/**
 * Created by virgile on 25/05/15.
 */
public class DevicePerso implements Comparable{
    private BluetoothDevice device;
    private int rssi;
    public DevicePerso(BluetoothDevice device, int rssi){
        this.device = device;
        this.rssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public int getRssi() {
        return rssi;
    }

    @Override
    public int compareTo(Object another) {
        DevicePerso anotherDevice = (DevicePerso) another;
        if(this.rssi > anotherDevice.getRssi()){
            return -1;
        }else if(this.rssi == anotherDevice.getRssi()){
            return 0;
        }else{
            return 1;
        }
    }
}
