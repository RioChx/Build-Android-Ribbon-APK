package com.tvbox.ribbonoverlay

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ColorPickerActivity : AppCompatActivity() {

    // Define fixed colors for easy selection
    private val colorMap = mapOf(
        "Red" to "#F44336",
        "Green" to "#4CAF50",
        "Blue" to "#2196F3",
        "Yellow" to "#FFEB3B",
        "Black" to "#000000",
        "White" to "#FFFFFF",
        "Purple" to "#9C27B0",
        "Orange" to "#FF9800"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker) 

        val colorGrid: GridView = findViewById(R.id.color_grid_view)
        val colorNames = colorMap.keys.toList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, colorNames)
        colorGrid.adapter = adapter

        colorGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val colorName = colorNames[position]
            val colorHex = colorMap[colorName] ?: "#FFFFFF" // Default to white if error

            // Save the chosen color (applies to button text color)
            val sharedPreferences = getSharedPreferences("RibbonSettings", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                // We'll save the chosen color as the button text color
                putString("button_color", colorHex)
                apply()
            }

            Toast.makeText(this, "Selected $colorName. Click 'Save Settings' to apply.", Toast.LENGTH_LONG).show()
            finish() // Close the picker and return to SettingsActivity
        }
    }
}
