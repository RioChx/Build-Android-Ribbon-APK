package com.tvbox.ribbonoverlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var ribbonPreview: LinearLayout
    private lateinit var transparencySeekBar: SeekBar
    private lateinit var ribbonColorInput: EditText
    private lateinit var buttonColorInput: EditText
    private lateinit var applyButton: Button
    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize Shared Preferences
        sharedPreferences = getSharedPreferences("RibbonSettings", Context.MODE_PRIVATE)

        // Initialize Views
        ribbonPreview = findViewById(R.id.ribbon_preview)
        transparencySeekBar = findViewById(R.id.transparency_seekbar_settings)
        ribbonColorInput = findViewById(R.id.ribbon_color_input)
        buttonColorInput = findViewById(R.id.button_color_input)
        applyButton = findViewById(R.id.button_apply_settings)
        previewText = findViewById(R.id.preview_text)
        val testButton = findViewById<Button>(R.id.preview_button)

        // 1. Load current settings
        loadSettings()

        // 2. Set up listeners for real-time preview
        ribbonColorInput.setOnEditorActionListener { _, _, _ -> updatePreview(); false }
        buttonColorInput.setOnEditorActionListener { _, _, _ -> updatePreview(); false }
        testButton.setOnClickListener { updatePreview() } // Use a button to trigger update
        
        transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { updatePreview() }
        })

        // 3. Apply and Save Button
        applyButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings Applied and Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        val ribbonColor = sharedPreferences.getString("ribbon_color", "#000000") // Default Black
        val buttonColor = sharedPreferences.getString("button_color", "#112233") // Default dark blue
        val transparency = sharedPreferences.getInt("ribbon_transparency", 0xCC) // Default 80% opacity

        ribbonColorInput.setText(ribbonColor)
        buttonColorInput.setText(buttonColor)
        transparencySeekBar.progress = transparency

        // Initial preview update
        updatePreview(ribbonColor, buttonColor, transparency)
    }

    private fun updatePreview(
        ribbonHex: String? = ribbonColorInput.text.toString(),
        buttonHex: String? = buttonColorInput.text.toString(),
        alpha: Int = transparencySeekBar.progress
    ) {
        try {
            // Ribbon Color and Transparency
            val ribbonBaseColor = Color.parseColor(ribbonHex)
            val ribbonFinalColor = Color.argb(alpha, Color.red(ribbonBaseColor), Color.green(ribbonBaseColor), Color.blue(ribbonBaseColor))
            ribbonPreview.background = ColorDrawable(ribbonFinalColor)

            // Button Color
            val buttonColor = Color.parseColor(buttonHex)
            previewText.setTextColor(buttonColor) // Use text color as button color preview

            // Update transparency display text
            val transparencyPercent = ((255 - alpha) * 100) / 255
            previewText.text = "Preview: Ribbon Color: $ribbonHex, Button Color: $buttonHex\nTransparency: $transparencyPercent%"

        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid Hex Code", Toast.LENGTH_SHORT).show()
            ribbonPreview.background = ColorDrawable(Color.RED) // Show red error
        }
    }

    private fun saveSettings() {
        sharedPreferences.edit().apply {
            putString("ribbon_color", ribbonColorInput.text.toString())
            putString("button_color", buttonColorInput.text.toString())
            putInt("ribbon_transparency", transparencySeekBar.progress)
            apply()
        }
    }
}
