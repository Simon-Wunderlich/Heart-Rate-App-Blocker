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
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import java.util.UUID

class BluetoothService (private val caller : ActivityResultCaller,
    private val context: Context) {

    private val permissionLauncher: ActivityResultLauncher<String> =
        caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                scan()
            }
        }

    fun checkAndRequestPermission() : Boolean {
        val permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (!missingPermissions.isEmpty())
        {
            permissionLauncher.launch(missingPermissions.first())
            return false
        }
        return true
    }


    fun scan() {
        // Checks for permissions
        // If all permissions are granted, continue
        // If at least 1 permission has not been granted, the function exits and will be called again once permission is accepted
        if (!checkAndRequestPermission())
            return

        println("huh")
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        val REQUEST_ENABLE_BT = 2
        val activity = context.getActivity()
        if (activity != null)
            startActivityForResult( activity, enableBtIntent, REQUEST_ENABLE_BT, null)
        else
            return

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner
        var isScanning = false
        val handler = Handler(Looper.getMainLooper())

        val SCAN_PERIOD: Long = 10000

        fun scanLeDevice() {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkAndRequestPermission()
            }
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
            println(device.name)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCustomCharacteristic(gatt)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
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