package com.example.heartrateblock

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.startActivityForResult
import java.util.UUID

class BluetoothService {

    @RequiresPermission(allOf= [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun scan(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val REQUEST_ENABLE_BT = 2
            val activity = context.getActivity()
            if (activity != null)
                startActivityForResult( activity, enableBtIntent, REQUEST_ENABLE_BT, null)
            else
                return
        }

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner
        var isScanning = false
        val handler = Handler(Looper.getMainLooper())

        val SCAN_PERIOD: Long = 10000


        fun scanLeDevice() {
            if (!isScanning) {
                handler.postDelayed({
                    isScanning = false
                    bleScanner?.stopScan(leScanCallback)
                }, SCAN_PERIOD)

                isScanning = true
                bleScanner?.startScan(leScanCallback)
            } else {
                isScanning = false
                bleScanner?.stopScan(leScanCallback)
            }
        }
        var bluetoothGatt: BluetoothGatt? = null
        fun connectToDevice(device: BluetoothDevice, context: Context) {
            // connectGatt handles connection states via a custom callback
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
        scanLeDevice()
    }
    private val leScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device: BluetoothDevice = result.device
            // Access device.name or device.address here to populate your UI
            println(device.name)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Successfully connected, now discover services
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection logic
                gatt.close()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services found! Now you can look for target Characteristics
                readCustomCharacteristic(gatt)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value // Raw byte array data received
                // Parse data contextually (e.g., String, Int, Hex)
            }
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readCustomCharacteristic(gatt: BluetoothGatt) {
        val SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val service = gatt.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic != null) {
            gatt.readCharacteristic(characteristic)
        }
    }
}