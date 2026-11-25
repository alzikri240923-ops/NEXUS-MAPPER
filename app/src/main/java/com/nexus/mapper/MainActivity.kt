package com.nexus.mapper

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(12, 16, 28))
        }

        val title = TextView(this).apply {
            text = "NEXUS MAPPER"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "KEYMAPPING CONTROL CENTER"
            textSize = 12f
            setTextColor(Color.rgb(0, 220, 180))
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "\nSYSTEM INITIALIZED\n\nUSB INPUT READY\n\nPROFILE: DEFAULT"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)

        setContentView(root)
    }
}
