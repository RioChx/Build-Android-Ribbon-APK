package com.tvbox.ribbonoverlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityServiceInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No visual layout needed, this activity just handles permissions
        
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            startServices()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startServices()
            } else {
                Toast.makeText(this, "Overlay permission denied. Cannot show ribbon.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startServices() {
        // Start the main ribbon service
        startForegroundService(Intent(this, OverlayService::class.java))

        // Check if Accessibility Service is enabled (User must do this manually on TV box)
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        // This checks if our accessibility service is enabled by comparing service names.
        val isServiceEnabled = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.name == SystemAccessibilityService::class.java.name }

        if (!isServiceEnabled) {
             Toast.makeText(this, "Please enable the 'Ribbon System Actions' service in Accessibility Settings.", Toast.LENGTH_LONG).show()
             // Guide the user to enable the Accessibility Service
             startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        
        // Close the permission activity immediately
        finish() 
    }
}
