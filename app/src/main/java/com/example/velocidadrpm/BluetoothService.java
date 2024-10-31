package com.example.velocidadrpm;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";
    private static final String DEVICE_ADDRESS = "MAC_ADDRESS"; // Cambia esto por la dirección MAC del dispositivo
    private BluetoothHelper bluetoothHelper;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);

        new Thread(() -> {
            try {
                // Aquí se pasa el contexto de la clase del servicio
                bluetoothHelper = new BluetoothHelper(device, BluetoothService.this);

                while (true) {
                    int speed = bluetoothHelper.readSpeed();
                    int rpm = bluetoothHelper.readRPM();

                    // Enviar datos a MainActivity a través de broadcast
                    Intent broadcastIntent = new Intent("BluetoothData");
                    broadcastIntent.putExtra("speed", speed);
                    broadcastIntent.putExtra("rpm", rpm);
                    sendBroadcast(broadcastIntent);

                    Thread.sleep(2000); // Leer cada 2 segundos
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en BluetoothService", e);
            }
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothHelper != null) bluetoothHelper.close();
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar BluetoothHelper", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
