package com.tvbox.ribbonoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val launchButton: Button = findViewById(R.id.launch_ribbon_button)
        val settingsButton: Button = findViewById(R.id.settings_button)
        
        checkOverlayPermission()
        
        launchButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                Toast.makeText(this, "Please grant 'Display over other apps' permission.", Toast.LENGTH_LONG).show()
                checkOverlayPermission()
            }
        }
        
        // FIX: Ensuring the Settings Intent is properly flagged for non-standard environments
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
            }
            startActivity(intent)
        }
    }
    
    // ... (rest of the file is the same) ...
}
