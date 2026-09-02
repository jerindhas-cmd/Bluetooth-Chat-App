package com.example.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val appName = "BluetoothChat"
    private val myUUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val messageBox = findViewById<EditText>(R.id.messageBox)
        val sendBtn = findViewById<Button>(R.id.sendBtn)
        val chatView = findViewById<TextView>(R.id.chatView)

        // 🔹 Get Bluetooth Adapter
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        // 🔐 Permission check (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    1
                )
                return
            }
        }

        // 🔥 Start connection
        startServer(chatView)
        startClient(chatView)

        // 📩 Send message
        sendBtn.setOnClickListener {
            val message = messageBox.text.toString()

            try {
                outputStream?.write(message.toByteArray())
                chatView.append("\nYou: $message")
                messageBox.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // 🟢 SERVER
    @SuppressLint("MissingPermission")
    private fun startServer(chatView: TextView) {
        Thread {
            try {
                val serverSocket = bluetoothAdapter
                    ?.listenUsingRfcommWithServiceRecord(appName, myUUID)

                socket = serverSocket?.accept()
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected (Server)", Toast.LENGTH_SHORT).show()
                }

                startReading(chatView)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 🔵 CLIENT
    @SuppressLint("MissingPermission")
    private fun startClient(chatView: TextView) {
        Thread {
            try {
                val device = bluetoothAdapter?.bondedDevices?.firstOrNull() ?: return@Thread

                val tempSocket = device.createRfcommSocketToServiceRecord(myUUID)
                tempSocket.connect()

                socket = tempSocket
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected (Client)", Toast.LENGTH_SHORT).show()
                }

                startReading(chatView)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 📥 RECEIVE MESSAGES
    private fun startReading(chatView: TextView) {
        Thread {
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1

                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)

                        runOnUiThread {
                            chatView.append("\nFriend: $message")
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }
}