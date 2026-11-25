
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
import android.widget.SeekBar

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private lateinit var inputManager: InputManager
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var editor: LinearLayout
    private lateinit var verticalLabel: TextView
    private lateinit var horizontalLabel: TextView
    private lateinit var profileLabel: TextView

    private var waitingAction: String? = null
    private var verticalValue = 50
    private var horizontalValue = 50
    private var profile = "DEFAULT"

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

        verticalValue = prefs.getInt("vertical_sensitivity", 50)
        horizontalValue = prefs.getInt("horizontal_sensitivity", 50)
        profile = prefs.getString("recoil_profile", "DEFAULT") ?: "DEFAULT"

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
            text = "KEYMAPPING & SENSITIVITY EDITOR"
            textSize = 13f
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
        root.addView(scroll)

        setContentView(root)

        buildEditor()
        refreshDevices()
        inputManager.registerInputDeviceListener(this, null)
    }

    private fun addHeading(text: String) {
        editor.addView(TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 12)
        })
    }

    private fun buildEditor() {
        editor.removeAllViews()

        addHeading("CONTROL CONFIGURATION")

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
                    "Assigned key: ${keyName(key)}"
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

            editor.addView(row)
            addSpace(12)
        }

        buildSensitivityPanel()
    }

    private fun addSpace(height: Int) {
        editor.addView(TextView(this), LinearLayout.LayoutParams(
            1, height
        ))
    }

    private fun buildSensitivityPanel() {
        addHeading("RECOIL SETTINGS")

        val info = TextView(this).apply {
            text = "LOCAL SENSITIVITY PRESET"
            textSize = 12f
            setTextColor(accent)
        }
        editor.addView(info)

        verticalLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        editor.addView(verticalLabel)

        val verticalSeek = SeekBar(this).apply {
            max = 99
            progress = verticalValue - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    verticalValue = progress + 1
                    verticalLabel.text =
                        "Vertical Sensitivity: $verticalValue"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        editor.addView(verticalSeek)

        horizontalLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 12, 0, 0)
        }
        editor.addView(horizontalLabel)

        val horizontalSeek = SeekBar(this).apply {
            max = 99
            progress = horizontalValue - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    horizontalValue = progress + 1
                    horizontalLabel.text =
                        "Horizontal Sensitivity: $horizontalValue"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        editor.addView(horizontalSeek)

        profileLabel = TextView(this).apply {
            text = "Recoil Profile: $profile"
            textSize = 15f
            setTextColor(accent)
            setPadding(0, 18, 0, 8)
        }
        editor.addView(profileLabel)

        val profiles = listOf("DEFAULT", "PRECISE", "SMOOTH")
        for (item in profiles) {
            val button = Button(this).apply {
                text = item
                setOnClickListener {
                    profile = item
                    profileLabel.text = "Recoil Profile: $profile"
                    when (item) {
                        "DEFAULT" -> {
                            verticalSeek.progress = 49
                            horizontalSeek.progress = 49
                        }
                        "PRECISE" -> {
                            verticalSeek.progress = 39
                            horizontalSeek.progress = 39
                        }
                        "SMOOTH" -> {
                            verticalSeek.progress = 59
                            horizontalSeek.progress = 59
                        }
                    }
                }
            }
            editor.addView(button)
        }

        val saveButton = Button(this).apply {
            text = "SAVE CONFIGURATION"
            setOnClickListener {
                prefs.edit()
                    .putInt("vertical_sensitivity", verticalValue)
                    .putInt("horizontal_sensitivity", horizontalValue)
                    .putString("recoil_profile", profile)
                    .apply()

                statusText.text = "SENSITIVITY CONFIGURATION SAVED"
            }
        }
        editor.addView(saveButton)

        val resetButton = Button(this).apply {
            text = "RESET TO DEFAULT"
            setOnClickListener {
                profile = "DEFAULT"
                verticalSeek.progress = 49
                horizontalSeek.progress = 49
                profileLabel.text = "Recoil Profile: DEFAULT"

                prefs.edit()
                    .putInt("vertical_sensitivity", 50)
                    .putInt("horizontal_sensitivity", 50)
                    .putString("recoil_profile", "DEFAULT")
                    .apply()

                statusText.text = "SETTINGS RESET"
            }
        }
        editor.addView(resetButton)

        val note = TextView(this).apply {
            text = "Note: These are local presets. They do not automatically control game recoil or change game settings."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 14, 0, 20)
        }
        editor.addView(note)

        verticalLabel.text = "Vertical Sensitivity: $verticalValue"
        horizontalLabel.text = "Horizontal Sensitivity: $horizontalValue"
    }

    private fun keyName(keyCode: Int): String {
        return if (keyCode == KeyEvent.KEYCODE_SPACE) {
            "SPACE"
        } else {
            KeyEvent.keyCodeToString(keyCode)
                .removePrefix("KEYCODE_")
        }
    }

    private fun refreshDevices() {
        val result = StringBuilder("USB INPUT DEVICES\n\n")
        var count = 0

        for (id in inputManager.inputDeviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            if (!device.isExternal) continue

            val mouse = device.supportsSource(InputDevice.SOURCE_MOUSE)
            val keyboard = device.supportsSource(InputDevice.SOURCE_KEYBOARD)

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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.device?.isExternal != true) {
            return super.dispatchKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0) {

            val keyCode = event.keyCode
            val action = waitingAction

            if (action != null) {
                prefs.edit()
                    .putInt("mapping_$action", keyCode)
                    .apply()

                waitingAction = null
                statusText.text = "SAVED: ${keyName(keyCode)}"
                buildEditor()
                return true
            }

            statusText.text = "KEYBOARD INPUT: ${keyName(keyCode)}"
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
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
