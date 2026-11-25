
package com.nexus.mapper

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable
import rikka.shizuku.Shizuku

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private val background = Color.rgb(8, 11, 10)
    private val surface = Color.rgb(18, 24, 21)
    private val accent = Color.rgb(0, 255, 145)
    private val muted = Color.rgb(150, 165, 157)
    private val white = Color.WHITE

    private lateinit var inputManager: InputManager
    private lateinit var deviceText: TextView
    private lateinit var shizukuText: TextView
    private lateinit var mappingText: TextView

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

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            runOnUiThread { refreshShizukuStatus() }
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            runOnUiThread { refreshShizukuStatus() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputManager =
            getSystemService(Context.INPUT_SERVICE) as InputManager

        window.statusBarColor = background
        window.navigationBarColor = background

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(25), dp(20), dp(30))

        scroll.addView(root)

        val brand = makeText(
            "NEXUS / MAPPER",
            14f,
            accent,
            true
        )

        val title = makeText(
            "CONTROL CENTER",
            27f,
            white,
            true
        )

        val subtitle = makeText(
            "External device configuration platform",
            12f,
            muted,
            false
        )

        root.addView(brand)
        addGap(root, 12)
        root.addView(title)
        addGap(root, 5)
        root.addView(subtitle)
        addGap(root, 24)

        val statusCard = makeCard()

        statusCard.addView(
            makeText("SYSTEM STATUS", 13f, accent, true)
        )

        deviceText = makeText(
            "Checking external devices...",
            13f,
            white,
            false
        )

        statusCard.addView(deviceText)
        root.addView(statusCard)

        addGap(root, 14)

        val shizukuCard = makeCard()

        shizukuCard.addView(
            makeText("SHIZUKU SERVICE", 13f, accent, true)
        )

        shizukuText = makeText(
            "Checking Shizuku connection...",
            13f,
            muted,
            false
        )

        shizukuCard.addView(shizukuText)
        root.addView(shizukuCard)

        addGap(root, 14)

        val mappingCard = makeCard()

        mappingCard.addView(
            makeText("CONTROL CONFIGURATION", 13f, accent, true)
        )

        mappingText = makeText(
            "Local keyboard configuration",
            12f,
            muted,
            false
        )

        mappingCard.addView(mappingText)

        val editorButton = makeButton("OPEN CONFIGURATION")

        editorButton.setOnClickListener {
            showMappingEditor()
        }

        mappingCard.addView(editorButton)
        root.addView(mappingCard)

        addGap(root, 14)

        val sensitivityCard = makeCard()

        sensitivityCard.addView(
            makeText("SENSITIVITY PRESETS", 13f, accent, true)
        )

        addGap(sensitivityCard, 10)

        val profileSpinner = Spinner(this)

        val profiles = arrayOf(
            "DEFAULT",
            "PRECISE",
            "SMOOTH"
        )

        profileSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            profiles
        )

        sensitivityCard.addView(profileSpinner)

        val verticalValue = getPreferences(0)
            .getInt("vertical_sensitivity", 50)

        val horizontalValue = getPreferences(0)
            .getInt("horizontal_sensitivity", 50)

        sensitivityCard.addView(
            makeText("Vertical sensitivity", 12f, white, false)
        )

        val verticalSlider = SeekBar(this)
        verticalSlider.max = 100
        verticalSlider.progress = verticalValue
        sensitivityCard.addView(verticalSlider)

        sensitivityCard.addView(
            makeText("Horizontal sensitivity", 12f, white, false)
        )

        val horizontalSlider = SeekBar(this)
        horizontalSlider.max = 100
        horizontalSlider.progress = horizontalValue
        sensitivityCard.addView(horizontalSlider)

        val prefs = getPreferences(0)

        verticalSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    prefs.edit()
                        .putInt("vertical_sensitivity", progress)
                        .apply()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        horizontalSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    prefs.edit()
                        .putInt("horizontal_sensitivity", progress)
                        .apply()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        profileSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    prefs.edit()
                        .putString("recoil_profile", profiles[position])
                        .apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        root.addView(sensitivityCard)

        addGap(root, 20)

        val footer = makeText(
            "NEXUS MAPPER • VERSION 1.0",
            11f,
            muted,
            false
        )

        footer.gravity = Gravity.CENTER
        root.addView(footer)

        setContentView(scroll)

        refreshDevices()
        refreshShizukuStatus()

        inputManager.registerInputDeviceListener(this, null)

        Shizuku.addBinderReceivedListenerSticky(
            binderReceivedListener
        )

        Shizuku.addBinderDeadListener(
            binderDeadListener
        )
    }

    private fun showMappingEditor() {
        val dialog = android.app.Dialog(this)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(dp(20), dp(20), dp(20), dp(20))
        container.setBackgroundColor(surface)

        val title = makeText(
            "BUTTON CONFIGURATION",
            18f,
            accent,
            true
        )

        container.addView(title)
        addGap(container, 12)

        val prefs = getPreferences(0)

        for (action in actions) {
            val current = prefs.getString(
                "mapping_$action",
                "NOT SET"
            ) ?: "NOT SET"

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL

            val label = makeText(
                action.replace("_", " "),
                12f,
                white,
                true
            )

            row.addView(
                label,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )

            val value = makeText(
                current,
                12f,
                accent,
                true
            )

            row.addView(value)

            row.setOnClickListener {
                showKeyPicker(action, value)
            }

            container.addView(row)

            val divider = View(this)
            divider.setBackgroundColor(Color.DKGRAY)

            container.addView(
                divider,
                LinearLayout.LayoutParams(
                    -1,
                    dp(1)
                )
            )
        }

        val close = makeButton("CLOSE")
        close.setOnClickListener { dialog.dismiss() }

        addGap(container, 12)
        container.addView(close)

        dialog.setContentView(container)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            -2
        )
    }

    private fun showKeyPicker(
        action: String,
        value: TextView
    ) {
        val choices = arrayOf(
            "W", "A", "S", "D",
            "SPACE", "E", "R", "F",
            "Q", "SHIFT", "CTRL",
            "ALT", "1", "2", "3",
            "4", "5", "6", "7", "8", "9"
        )

        AlertDialog.Builder(this)
            .setTitle("Select key for $action")
            .setItems(choices) { _, which ->
                val selected = choices[which]

                getPreferences(0)
                    .edit()
                    .putString("mapping_$action", selected)
                    .apply()

                value.text = selected
                refreshMappingSummary()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun refreshMappingSummary() {
        val prefs = getPreferences(0)

        val configured = actions.count {
            prefs.getString("mapping_$it", "NOT SET") != "NOT SET"
        }

        mappingText.text =
            "$configured / ${actions.size} controls configured"
    }

    private fun refreshDevices() {
    val ids: IntArray = inputManager.getInputDeviceIds()

    val devices = ids.mapNotNull { deviceId: Int ->
        inputManager.getInputDevice(deviceId)
    }.filter { device ->
        !device.isVirtual
    }

    val deviceMask =
        InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE

    val external = devices.filter { device ->
        (device.sources and deviceMask) != 0
    }

    deviceText.text = if (external.isEmpty()) {
        "No external device detected"
    } else {
        external.joinToString("\n") { device ->
            "● ${device.name}"
        }
    }

    refreshMappingSummary()
}

    private fun refreshShizukuStatus() {
        val connected = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

        if (connected) {
            shizukuText.text = "● SHIZUKU SERVICE CONNECTED"
            shizukuText.setTextColor(accent)
        } else {
            shizukuText.text = "● SHIZUKU SERVICE NOT CONNECTED"
            shizukuText.setTextColor(Color.RED)
        }
    }

    private fun makeCard(): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(16), dp(16), dp(16), dp(16))

        val shape = GradientDrawable()
        shape.setColor(surface)
        shape.cornerRadius = dp(14).toFloat()
        shape.setStroke(dp(1), Color.rgb(35, 55, 44))

        card.background = shape

        return card
    }

    private fun makeText(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }

            setPadding(0, dp(5), 0, dp(5))
        }
    }

    private fun makeButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(this@MainActivity.background)
            textSize = 12f
            setBackgroundColor(accent)
        }
    }

    private fun addGap(
        parent: LinearLayout,
        value: Int
    ) {
        val gap = View(this)

        parent.addView(
            gap,
            LinearLayout.LayoutParams(
                1,
                dp(value)
            )
        )
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            -1,
            -2
        )
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

        Shizuku.removeBinderReceivedListener(
            binderReceivedListener
        )

        Shizuku.removeBinderDeadListener(
            binderDeadListener
        )

        super.onDestroy()
    }
}
