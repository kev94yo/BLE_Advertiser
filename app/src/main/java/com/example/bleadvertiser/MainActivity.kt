package com.example.bleadvertiser

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.bleadvertiser.databinding.ActivityMainBinding

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
const val BT_ADVERTISING_FAILED_EXTRA_CODE = "bt_adv_failure_code"
const val INVALID_CODE = -1
const val ADVERTISING_TIMED_OUT = 6
val ScanFilterService_UUID: ParcelUuid = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

class MainActivity : AppCompatActivity() {
    // Bind this code to xml file
    private lateinit var binding: ActivityMainBinding
    private lateinit var btAdvertisingFailureReceiver: BroadcastReceiver

    // Declare BluetoothAdapter
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Declare BLE advertiser
    private val bleAdvertiser by lazy {
        bluetoothAdapter.bluetoothLeAdvertiser
    }

    // Define advertise settings
    private val advertiseSettings: AdvertiseSettings =
        AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTimeout(0)
            .build()

    // Define advertise data
    private val advertiseData: AdvertiseData =
        AdvertiseData.Builder()
            .addServiceUuid(ScanFilterService_UUID)
            .setIncludeDeviceName(true)
            .build()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d("AdvertiseCallback", "Advertising failed")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d("AdvertiseCallback", "Advertising succeeded")
        }
    }

    /**
     * Activity overrides
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This BroadcastReceiver object will alert UI when error is received from AdvertiseCallback
        btAdvertisingFailureReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                // The codes are defined in Constants.kt
                val errorCode = intent?.getIntExtra(BT_ADVERTISING_FAILED_EXTRA_CODE, INVALID_CODE)
                binding.advertiseSwitch.isChecked = false

                // Define error message to alert in UI based on type.
                var errMsg = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> getString(R.string.already_started)
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> getString(R.string.data_too_large)
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> getString(R.string.not_supported)
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> getString(R.string.inter_err)
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers."
                    ADVERTISING_TIMED_OUT -> "Timed out"
                    else -> "Error unknown."
                }
                errMsg = "Start advertising failed: $errMsg"
                // Show error message.
                Toast.makeText(applicationContext, errMsg, Toast.LENGTH_LONG).show()
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.advertiseSwitch.setOnClickListener{
            val view = it as SwitchCompat
            if (view.isChecked) startAdvertising() else stopAdvertising()
            Log.d("Switch", "onCreate: switch clicked")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    // If Bluetooth is not enabled, request user to enable
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    /**
     * Advertising functions
     */
    private fun startAdvertising() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    private fun stopAdvertising() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        bleAdvertiser.stopAdvertising(advertiseCallback)
        Log.d("StopAdvertising", "Advertising stopped")
    }

    /**
     * Permissions launcher
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(applicationContext, "Permission Granted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Permission Needed", Toast.LENGTH_LONG).show()
            }
        }
}