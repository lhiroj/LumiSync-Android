
package com.lumisync

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(12, 16, 29)
        val accent = Color.rgb(100, 120, 255)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "LUMISYNC"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "SMART LIGHTING CONTROL"
            textSize = 12f
            setTextColor(accent)
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
        }

        val status = TextView(this).apply {
            text = "SİSTEM DURUMU\n\nUygulama başlatıldı\n\nGovee bağlantısı henüz eklenmedi."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 32, 20, 32)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)

        setContentView(layout)
    }
}
