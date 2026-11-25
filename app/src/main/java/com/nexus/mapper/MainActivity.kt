
package com.nexus.mapper

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.hardware.input.InputManager
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity(),
    InputManager.InputDeviceListener {

    private lateinit var inputManager: InputManager
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var editor: LinearLayout

    private var waitingAction: String? = null

    private val actions = listOf(
        "MOVE_FORWARD",
        "MOVE_BACKWARD",
        "MOVE_LEFT",
        "MOVE_RIGHT",
        "JUMP",
        "ACTION",
        "RELOAD",
        "INTERACT"
    )

    private val prefs by lazy {
        getSharedPreferences("nexus_mapping", Context.MODE_PRIVATE)
    }

    private val bg = Color.rgb(12, 16, 28)
    private val accent = Color.rgb(0, 220, 180)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputManager = getSystemService(
            Context.INPUT_SERVICE
        ) as InputManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "NEXUS MAPPER"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "KEYMAPPING EDITOR"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(accent)
        }

        deviceText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 15)
        }

        statusText = TextView(this).apply {
            text = "STATUS: READY"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setPadding(0, 10, 0, 20)
        }

        val heading = TextView(this).apply {
            text = "CONTROL CONFIGURATION"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 15, 0, 15)
        }

        editor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(editor)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(deviceText)
        root.addView(statusText)
        root.addView(heading)
        root.addView(scroll)

        setContentView(root)

        buildEditor()
        refreshDevices()

        inputManager.registerInputDeviceListener(this, null)
    }

    private fun buildEditor() {
        editor.removeAllViews()

        for (action in actions) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.rgb(24, 31, 46))
            }

            val name = TextView(this).apply {
                text = action.replace("_", " ")
                textSize = 15f
                setTextColor(Color.WHITE)
            }

            val key = prefs.getInt("mapping_$action", 0)

            val keyLabel = TextView(this).apply {
                text = if (key == 0) {
                    "Assigned key: NOT SET"
                } else {
                    "Assigned key: ${KeyEvent.keyCodeToString(key)}"
                }
                textSize = 13f
                setTextColor(accent)
                setPadding(0, 6, 0, 8)
            }

            val setButton = Button(this).apply {
                text = "SET KEY"
                setOnClickListener {
                    waitingAction = action
                    statusText.text =
                        "PRESS A KEY FOR ${action.replace("_", " ")}"
                }
            }

            row.addView(name)
            row.addView(keyLabel)
            row.addView(setButton)

            editor.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            )
        }
    }

    private fun refreshDevices() {
        val result = StringBuilder("USB INPUT DEVICES\n\n")
        var count = 0

        for (id in inputManager.inputDeviceIds) {
            val device = inputManager.getInputDevice(id)
                ?: continue

            if (!device.isExternal) continue

            val mouse = device.supportsSource(
                InputDevice.SOURCE_MOUSE
            )

            val keyboard = device.supportsSource(
                InputDevice.SOURCE_KEYBOARD
            )

            if (mouse || keyboard) {
                count++
                result.append(device.name).append("\n")
            }
        }

        if (count == 0) {
            result.append("No external device detected")
        }

        deviceText.text = result.toString()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent
    ): Boolean {

        val action = waitingAction

        if (action != null) {
            prefs.edit()
                .putInt("mapping_$action", keyCode)
                .apply()

            waitingAction = null

            statusText.text =
                "SAVED: ${KeyEvent.keyCodeToString(keyCode)}"

            buildEditor()
            return true
        }

        statusText.text =
            "KEYBOARD INPUT: ${KeyEvent.keyCodeToString(keyCode)}"

        return true
    }

    override fun onGenericMotionEvent(
        event: MotionEvent
    ): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            statusText.text = "MOUSE INPUT DETECTED"
            return true
        }

        return super.onGenericMotionEvent(event)
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

    override fun onDestroy() {
        inputManager.unregisterInputDeviceListener(this)
        super.onDestroy()
    }
}
