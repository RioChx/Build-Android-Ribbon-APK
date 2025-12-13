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
        
        // 1. Check for Overlay Permission on start
        checkOverlayPermission()
        
        // 2. Button Listener to start the service
        launchButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                Toast.makeText(this, "Please grant 'Display over other apps' permission.", Toast.LENGTH_LONG).show()
                checkOverlayPermission() // Re-request permission
            }
        }
        
        // 3. Button Listener to open Settings (FIXED to prevent crash)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
            }
            startActivity(intent)
        }
    }

    // --- REQUIRED HELPER FUNCTIONS (MISSING IN YOUR LAST BUILD) ---
    
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    private fun startOverlayService() {
        if (Settings.canDrawOverlays(this)) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, OverlayService::class.java))
            } else {
                startService(Intent(this, OverlayService::class.java))
            }
            Toast.makeText(this, "Ribbon Overlay Launched!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Overlay permission required to start the service.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // Check if permission was just granted
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            }
        }
    }
}
