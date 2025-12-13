package com.tvbox.ribbonoverlay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat

class SystemAccessibilityService : AccessibilityService() {

    private val recentsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Check the action sent by the OverlayService
            if (intent?.action == "com.tvbox.ribbonoverlay.OPEN_RECENTS") {
                // Perform the system action to open the recent apps screen
                val success = performGlobalAction(GLOBAL_ACTION_RECENTS)
                if (!success) {
                    Toast.makeText(context, "Failed to open Recents.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Register the BroadcastReceiver to listen for the instruction from the OverlayService
        val filter = IntentFilter("com.tvbox.ribbonoverlay.OPEN_RECENTS")
        // Use RECEIVER_EXPORTED for broad compatibility
        ContextCompat.registerReceiver(this, recentsReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This is required but not used for this ribbon functionality
    }

    override fun onInterrupt() {
        // Must be implemented
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(recentsReceiver)
    }
}
