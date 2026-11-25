
package com.nexus.mapper

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

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

    private val background = Color.rgb(8, 12, 17)
    private val panel = Color.rgb(18, 25, 32)
    private val panelLight = Color.rgb(25, 34, 42)
    private val accent = Color.rgb(0, 240, 170)
    private val white = Color.WHITE
    private val muted = Color.rgb(155, 170, 180)

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
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(this@MainActivity.background)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val brand = TextView(this).apply {
            text = "NEXUS"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(accent)
        }

        val title = TextView(this).apply {
            text = "MAPPER"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
        }

        val subtitle = TextView(this).apply {
            text = "GAMING CONTROL CONFIGURATION"
            textSize = 11f
            letterSpacing = 1.4f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(5), 0, dp(20))
        }

        val statusCard = makeCard()

        val statusTitle = makeText(
            "SYSTEM STATUS",
            11f,
            accent,
            true
        )

        statusText = makeText(
            "● SYSTEM READY",
            15f,
            white,
            true
        )

        statusText.setPadding(0, dp(8), 0, 0)

        statusCard.addView(statusTitle)
        statusCard.addView(statusText)

        val deviceCard = makeCard()

        deviceCard.addView(
            makeText("CONNECTED DEVICES", 13f, accent, true)
        )

        deviceText = makeText(
            "Checking devices...",
            13f,
            muted,
            false
        )

        deviceText.setPadding(0, dp(10), 0, 0)
        deviceCard.addView(deviceText)

        editor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(brand)
        root.addView(title)
        root.addView(subtitle)
        root.addView(statusCard, matchWidth())
        addGap(root, 12)
        root.addView(deviceCard, matchWidth())
        addGap(root, 12)
        root.addView(editor, matchWidth())

        setContentView(scroll)

        buildEditor()
        refreshDevices()

        inputManager.registerInputDeviceListener(this, null)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(panel, 16)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Color.rgb(39, 55, 63))
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun addGap(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun makeButton(
        value: String,
        primary: Boolean = false,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (primary) this@MainActivity.background else accent)

            this.background = rounded(
                if (primary) accent else panelLight,
                12
            )

            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { onClick() }
        }
    }

    private fun addHeading(value: String) {
        val heading = makeText(
            value,
            16f,
            white,
            true
        )

        heading.setPadding(0, dp(18), 0, dp(12))
        editor.addView(heading)
    }

    private fun buildEditor() {
        editor.removeAllViews()

        addHeading("01  /  CONTROL CONFIGURATION")

        for (action in actions) {

            val card = makeCard()

            val name = makeText(
                action.replace("_", " "),
                15f,
                white,
                true
            )

            val key = prefs.getInt("mapping_$action", 0)

            val keyLabel = makeText(
                if (key == 0) {
                    "ASSIGNED KEY: NOT SET"
                } else {
                    "ASSIGNED KEY: ${keyName(key)}"
                },
                12f,
                accent,
                false
            )

            keyLabel.setPadding(0, dp(7), 0, dp(10))

            val setButton = makeButton("SET KEY") {
                waitingAction = action
                statusText.text =
                    "● PRESS A KEY FOR ${action.replace("_", " ")}"
            }

            card.addView(name)
            card.addView(keyLabel)
            card.addView(setButton, matchWidth())

            editor.addView(card, matchWidth())
            addGap(editor, 9)
        }

        buildSensitivityPanel()
    }

    private fun buildSensitivityPanel() {

        addHeading("02  /  RECOIL SETTINGS")

        val card = makeCard()

        card.addView(
            makeText(
                "LOCAL SENSITIVITY PRESETS",
                12f,
                accent,
                true
            )
        )

        addGap(card, 16)

        verticalLabel = makeText(
            "Vertical Sensitivity: $verticalValue",
            14f,
            white,
            true
        )

        card.addView(verticalLabel)

        val verticalSeek = SeekBar(this).apply {
            max = 99
            progress = (verticalValue - 1).coerceIn(0, 99)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        verticalValue = progress + 1
                        verticalLabel.text =
                            "Vertical Sensitivity: $verticalValue"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }

        card.addView(verticalSeek)

        addGap(card, 16)

        horizontalLabel = makeText(
            "Horizontal Sensitivity: $horizontalValue",
            14f,
            white,
            true
        )

        card.addView(horizontalLabel)

        val horizontalSeek = SeekBar(this).apply {
            max = 99
            progress = (horizontalValue - 1).coerceIn(0, 99)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        horizontalValue = progress + 1
                        horizontalLabel.text =
                            "Horizontal Sensitivity: $horizontalValue"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }

        card.addView(horizontalSeek)

        addGap(card, 18)

        profileLabel = makeText(
            "ACTIVE PROFILE: $profile",
            14f,
            accent,
            true
        )

        card.addView(profileLabel)

        val profileRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val profiles = listOf("DEFAULT", "PRECISE", "SMOOTH")

        for (item in profiles) {

            val button = makeButton(item) {

                profile = item

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

                profileLabel.text = "ACTIVE PROFILE: $profile"
                statusText.text = "● PROFILE SELECTED: $profile"
            }

            val params = LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )

            params.setMargins(dp(3), dp(8), dp(3), dp(8))

            profileRow.addView(button, params)
        }

        card.addView(profileRow)

        addGap(card, 10)

        val saveButton = makeButton(
            "SAVE CONFIGURATION",
            true
        ) {

            prefs.edit()
                .putInt("vertical_sensitivity", verticalValue)
                .putInt("horizontal_sensitivity", horizontalValue)
                .putString("recoil_profile", profile)
                .apply()

            statusText.text = "● CONFIGURATION SAVED"
        }

        card.addView(saveButton, matchWidth())

        addGap(card, 8)

        val resetButton = makeButton("RESET TO DEFAULT") {

            profile = "DEFAULT"

            verticalSeek.progress = 49
            horizontalSeek.progress = 49

            profileLabel.text = "ACTIVE PROFILE: DEFAULT"

            prefs.edit()
                .putInt("vertical_sensitivity", 50)
                .putInt("horizontal_sensitivity", 50)
                .putString("recoil_profile", "DEFAULT")
                .apply()

            statusText.text = "● SETTINGS RESET"
        }

        card.addView(resetButton, matchWidth())

        addGap(card, 12)

        card.addView(
            makeText(
                "These are local presets. They do not automatically control game recoil or change game settings.",
                11f,
                muted,
                false
            )
        )

        editor.addView(card, matchWidth())

        addGap(editor, 20)

        val footer = makeText(
            "NEXUS MAPPER  •  VERSION 1.0",
            11f,
            muted,
            true
        )

        footer.gravity = Gravity.CENTER
        footer.setPadding(0, dp(12), 0, dp(20))

        editor.addView(footer)
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

        val result = StringBuilder()
        var count = 0

        for (id in inputManager.inputDeviceIds) {

            val device = inputManager.getInputDevice(id) ?: continue

            if (!device.isExternal) continue

            val mouse = device.supportsSource(InputDevice.SOURCE_MOUSE)
            val keyboard = device.supportsSource(InputDevice.SOURCE_KEYBOARD)

            if (mouse || keyboard) {
                count++
                result.append("● ")
                    .append(device.name)
                    .append("\n")
            }
        }

        if (count == 0) {
            result.append("No external device detected")
        } else {
            result.append("\n$count external device(s) detected")
        }

        deviceText.text = result.toString()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (event.device?.isExternal != true) {
            return super.dispatchKeyEvent(event)
        }

        if (
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {

            val keyCode = event.keyCode
            val action = waitingAction

            if (action != null) {

                prefs.edit()
                    .putInt("mapping_$action", keyCode)
                    .apply()

                waitingAction = null

                statusText.text = "● SAVED: ${keyName(keyCode)}"

                buildEditor()

                return true
            }

            statusText.text = "● KEYBOARD INPUT: ${keyName(keyCode)}"
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {

        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            statusText.text = "● MOUSE INPUT DETECTED"
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
