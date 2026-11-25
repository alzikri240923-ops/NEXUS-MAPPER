
package com.nexus.mapper

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity(),
    InputManager.InputDeviceListener {

    private lateinit var inputManager: InputManager
    private lateinit var deviceList: TextView
    private lateinit var eventStatus: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputManager = getSystemService(
            Context.INPUT_SERVICE
        ) as InputManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF0C101C.toInt())
        }

        val title = TextView(this).apply {
            text = "NEXUS MAPPER"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "USB INPUT DETECTION"
            textSize = 14f
            setTextColor(0xFF00DCB4.toInt())
            gravity = Gravity.CENTER
        }

        deviceList = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 35, 0, 25)
        }

        eventStatus = TextView(this).apply {
            text = "INPUT: WAITING"
            textSize = 14f
            setTextColor(0xFF00DCB4.toInt())
            gravity = Gravity.CENTER
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(deviceList)
        root.addView(eventStatus)

        setContentView(root)

        inputManager.registerInputDeviceListener(
            this, handler
        )

        refreshDevices()
    }

    private fun refreshDevices() {
        val devices = inputManager.inputDeviceIds
        val result = StringBuilder()

        result.append("\nCONNECTED DEVICES\n\n")

        var count = 0

        for (id in devices) {
            val device = inputManager.getInputDevice(id)
                ?: continue

            if (!device.isExternal) continue

            val mouse =
                device.supportsSource(InputDevice.SOURCE_MOUSE)

            val keyboard =
                device.supportsSource(InputDevice.SOURCE_KEYBOARD)

            if (mouse || keyboard) {
                count++

                result.append(device.name)
                result.append("\n")

                if (mouse) result.append("TYPE: MOUSE\n")
                if (keyboard) result.append("TYPE: KEYBOARD\n")

                result.append("\n")
            }
        }

        if (count == 0) {
            result.append("NO EXTERNAL INPUT DEVICE\n")
            result.append("\nConnect a mouse or keyboard.")
        }

        deviceList.text = result.toString()
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshDevices()
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_MOUSE ==
            InputDevice.SOURCE_MOUSE) {

            eventStatus.text = "MOUSE INPUT DETECTED"
            return true
        }

        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent
    ): Boolean {
        eventStatus.text = "KEYBOARD INPUT: $keyCode"
        return true
    }

    override fun onDestroy() {
        inputManager.unregisterInputDeviceListener(this)
        super.onDestroy()
    }
}
