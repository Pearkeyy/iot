package com.example.motorcontroller

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var ipEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var speedLabel: TextView
    private lateinit var stopButton: Button
    private lateinit var startButton: Button
    private lateinit var fwdButton: Button
    private lateinit var revButton: Button

    private lateinit var speedButtons: List<Button>

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var isConnected = false

    private var currentSpeed = 0      // 0-100 value sent to ESP
    private var currentSpeedLevel = 0 // 0-5 display level
    private var currentDir = "fwd"

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipEditText = findViewById(R.id.ipEditText)
        portEditText = findViewById(R.id.portEditText)
        connectButton = findViewById(R.id.connectButton)
        speedLabel = findViewById(R.id.speedLabel)
        stopButton = findViewById(R.id.stopButton)
        startButton = findViewById(R.id.startButton)
        fwdButton = findViewById(R.id.fwdButton)
        revButton = findViewById(R.id.revButton)

        speedButtons = listOf(
            findViewById(R.id.speed0Button),
            findViewById(R.id.speed1Button),
            findViewById(R.id.speed2Button),
            findViewById(R.id.speed3Button),
            findViewById(R.id.speed4Button),
            findViewById(R.id.speed5Button)
        )

        updateDirectionButtons()
        updateSpeedButtons()

        connectButton.setOnClickListener { connectToESP() }

        // Speed buttons 0-5
        speedButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                setSpeedLevel(index)
            }
        }

        stopButton.setOnClickListener {
            setSpeedLevel(0)
        }

        startButton.setOnClickListener {
            setSpeedLevel(3) // Start at level 3 (60%)
        }

        fwdButton.setOnClickListener {
            currentDir = "fwd"
            updateDirectionButtons()
            sendImmediate()
        }

        revButton.setOnClickListener {
            currentDir = "rev"
            updateDirectionButtons()
            sendImmediate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
    }

    private fun setSpeedLevel(level: Int) {
        currentSpeedLevel = level
        currentSpeed = level * 20  // 0→0, 1→20, 2→40, 3→60, 4→80, 5→100
        updateSpeedButtons()
        sendImmediate()
    }

    private fun updateSpeedButtons() {
        speedLabel.text = "Speed: $currentSpeedLevel"
        speedButtons.forEachIndexed { index, button ->
            if (index == currentSpeedLevel) {
                button.setBackgroundColor(0xFF0077B6.toInt())
                button.setTextColor(0xFFFFFFFF.toInt())
            } else {
                button.setBackgroundColor(0xFFE0E0E0.toInt())
                button.setTextColor(0xFF000000.toInt())
            }
        }
    }

    private fun updateDirectionButtons() {
        if (currentDir == "fwd") {
            fwdButton.setBackgroundColor(0xFF4CAF50.toInt())
            fwdButton.setTextColor(0xFFFFFFFF.toInt())
            revButton.setBackgroundColor(0xFFE0E0E0.toInt())
            revButton.setTextColor(0xFF000000.toInt())
        } else {
            revButton.setBackgroundColor(0xFFF44336.toInt())
            revButton.setTextColor(0xFFFFFFFF.toInt())
            fwdButton.setBackgroundColor(0xFFE0E0E0.toInt())
            fwdButton.setTextColor(0xFF000000.toInt())
        }
    }

    private fun connectToESP() {
        val ip = ipEditText.text.toString().trim()
        val port = portEditText.text.toString().trim().toIntOrNull()

        if (ip.isEmpty() || port == null) {
            Toast.makeText(this, "Invalid IP or port", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                socket = Socket(ip, port)
                writer = PrintWriter(
                    BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())),
                    true
                )
                isConnected = true

                runOnUiThread {
                    Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                isConnected = false
                runOnUiThread {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendImmediate() {
        if (!isConnected) {
            runOnUiThread {
                Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val packet = "$currentSpeed,$currentDir"

        Thread {
            try {
                writer?.print(packet)
                writer?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
