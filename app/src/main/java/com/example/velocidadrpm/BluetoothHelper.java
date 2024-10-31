package com.example.velocidadrpm;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothHelper {

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public BluetoothHelper(BluetoothDevice device, Context context) throws IOException {
        // Verificar si los permisos están concedidos
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Permiso de Bluetooth no concedido.");
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);

        try {
            bluetoothSocket.connect();
        } catch (IOException e) {
            bluetoothSocket.close();
            throw new IOException("No se pudo conectar al socket Bluetooth", e);
        }

        outputStream = bluetoothSocket.getOutputStream();
        inputStream = bluetoothSocket.getInputStream();
    }

    public String sendCommand(String command) throws IOException {
        outputStream.write((command + "\r").getBytes());
        byte[] buffer = new byte[1024];
        int bytes = inputStream.read(buffer);
        return new String(buffer, 0, bytes);
    }

    public int readSpeed() throws IOException {
        String response = sendCommand("010D"); // Comando OBD-II para velocidad
        return Integer.parseInt(response.substring(4, 6), 16);
    }

    public int readRPM() throws IOException {
        String response = sendCommand("010C"); // Comando OBD-II para RPM
        return (Integer.parseInt(response.substring(4, 8), 16)) / 4;
    }

    public void close() throws IOException {
        if (bluetoothSocket != null) bluetoothSocket.close();
    }
}
