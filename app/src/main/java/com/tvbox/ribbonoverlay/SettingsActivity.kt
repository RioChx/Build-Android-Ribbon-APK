package com.tvbox.ribbonoverlay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Button to launch the Color Picker Activity
        val ribbonColorButton: Button = findViewById(R.id.customize_ribbon_color_button)
        ribbonColorButton.setOnClickListener {
            val intent = Intent(this, ColorPickerActivity::class.java)
            startActivity(intent)
        }
        
        // Save Button - used to reload the service after color selection
        val saveButton: Button = findViewById(R.id.save_settings_button) 
        saveButton.setOnClickListener {
            reloadOverlayService()
        }
    }

    private fun reloadOverlayService() {
        // Stop the existing service
        stopService(Intent(this, OverlayService::class.java))
        
        // Wait a moment and then start the new service to ensure new colors are loaded
        Handler(Looper.getMainLooper()).postDelayed({
            if (android.provider.Settings.canDrawOverlays(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(this, OverlayService::class.java))
                } else {
                    startService(Intent(this, OverlayService::class.java))
                }
                Toast.makeText(this, "Settings Saved and Ribbon Reloaded", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission lost. Relaunch app.", Toast.LENGTH_LONG).show()
            }
        }, 500)
    }
}
