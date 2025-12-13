package com.tvbox.ribbonoverlay

import android.accessibilityservice.AccessibilityServiceInfo // <-- ADD THIS IMPORT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val OVERLAY_PERMISSION_REQUEST_CODE = 100
    private val ACCESSIBILITY_PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request Overlay Permission
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        } else {
            // Check and request Accessibility Service Permission
            if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            } else {
                startOverlayService()
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) // Fixes Unresolved reference
        for (service in enabledServices) {
            if (service.id.contains(packageName)) {
                return true
            }
        }
        return false
    }

    private fun requestAccessibilityPermission() {
        // Directs user to the Accessibility settings page
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startOverlayService() {
        if (hasOverlayPermission() && isAccessibilityServiceEnabled()) {
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (hasOverlayPermission()) {
                if (!isAccessibilityServiceEnabled()) {
                    requestAccessibilityPermission()
                } else {
                    startOverlayService()
                }
            } else {
                // Handle permission denied
            }
        }
    }

    // You might want to override onResume to check accessibility status after user returns from settings
    override fun onResume() {
        super.onResume()
        if (hasOverlayPermission() && isAccessibilityServiceEnabled()) {
            startOverlayService()
        }
    }
}
