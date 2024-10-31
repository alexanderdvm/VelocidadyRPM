package com.example.velocidadrpm;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView speedTextView, rpmTextView, textViewData;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> deviceListAdapter;
    private Handler handler;
    private int speed = 0;
    private int rpm = 0;
    private boolean isBluetoothConnected = false;

    private static final String[] IP_ADDRESSES = {"18.117.243.59"};
    private static final int[] PORTS = {6100};
    private boolean isSending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        initializeBluetoothAdapter();

        if (bluetoothAdapter == null) {
            initializeWithoutBluetooth();
        } else {
            checkAndRequestPermissions();
            registerReceivers();
        }

        handler = new Handler(Looper.getMainLooper());
        startDataUpdate(); // Forzar inicio de actualización de datos
    }

    private void initializeUI() {
        speedTextView = findViewById(R.id.speedTextView);
        rpmTextView = findViewById(R.id.rpmTextView);
        textViewData = findViewById(R.id.textViewData);

        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);

        ListView listView = findViewById(R.id.deviceListView);
        listView.setAdapter(deviceListAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = deviceList.get(position);
            String macAddress = selectedDevice.split("\n")[1];

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startBluetoothService(macAddress, selectedDevice);
            } else {
                Toast.makeText(this, "Permiso de conexión Bluetooth no concedido", Toast.LENGTH_SHORT).show();
                isBluetoothConnected = false;
            }
        });
    }

    private void initializeBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    private void startBluetoothService(String macAddress, String selectedDevice) {
        Intent serviceIntent = new Intent(MainActivity.this, BluetoothService.class);
        serviceIntent.putExtra("device_mac", macAddress);
        try {
            startService(serviceIntent);
            Toast.makeText(this, "Conectando con " + selectedDevice, Toast.LENGTH_SHORT).show();
            isBluetoothConnected = true;
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar el servicio Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeWithoutBluetooth() {
        speed = 0;
        rpm = 0;
        updateUI();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            startBluetoothScan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                startBluetoothScan();
            } else {
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_SHORT).show();
                initializeWithoutBluetooth();
            }
        }
    }

    private void startBluetoothScan() {
        if (bluetoothAdapter.isEnabled()) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.startDiscovery();
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(bluetoothReceiver, filter);
                } else {
                    Toast.makeText(this, "Permiso para escanear Bluetooth no concedido", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Excepción de Seguridad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Por favor, habilita el Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerReceivers() {
        try {
            IntentFilter filter = new IntentFilter("BluetoothData");
            registerReceiver(broadcastReceiver, filter);
        } catch (Exception e) {
            Toast.makeText(this, "Error al registrar receptores", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            String deviceName = device.getName();
                            String deviceAddress = device.getAddress();
                            deviceList.add(deviceName + "\n" + deviceAddress);
                            deviceListAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, "Permiso para acceder al nombre del dispositivo no concedido", Toast.LENGTH_SHORT).show();
                        }
                    } catch (SecurityException e) {
                        Toast.makeText(context, "Excepción de Seguridad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                speed = intent.getIntExtra("speed", 0);
                rpm = intent.getIntExtra("rpm", 0);
                isBluetoothConnected = true;
                updateUI();
            } catch (Exception e) {
                Toast.makeText(context, "Error al recibir datos de Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final Runnable updateDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSending) {
                if (!isBluetoothConnected) {
                    speed = 0;
                    rpm = 0;
                }
                updateUI();

                String message = speed + "," + rpm;
                for (int i = 0; i < IP_ADDRESSES.length; i++) {
                    sendMessageOverUDP(IP_ADDRESSES[i], PORTS[i], message);
                }
                handler.postDelayed(this, 10000);
            }
        }
    };

    private void sendMessageOverUDP(String ip, int port, String message) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress ipAddress = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), ipAddress, port);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
            }
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al enviar el mensaje por UDP", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void updateUI() {
        speedTextView.setText("Velocidad: " + speed + " km/h");
        rpmTextView.setText("RPM: " + rpm);
        textViewData.setText(isBluetoothConnected ? "Datos de Velocidad y RPM" : "Bluetooth no conectado. Mostrando datos en 0.");
    }

    private void startDataUpdate() {
        handler.post(updateDataRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothReceiver);
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Receptor ya desregistrado, ignorar
        }
        handler.removeCallbacks(updateDataRunnable);
    }
}
