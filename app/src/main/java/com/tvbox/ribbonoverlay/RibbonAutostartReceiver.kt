package com.tvbox.ribbonoverlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

class RibbonAutostartReceiver : BroadcastReceiver() {

    // IMPORTANT: Listening for a specific app's launch is often restricted. 
    // This receiver attempts to trigger on a common system event (like app switch 
    // or screen on, if registered in the manifest), but we target a placeholder
    // action that you MUST set up to be triggered by Chrome/system event if possible.
    
    // The most common reliable trigger is BOOT_COMPLETED, which we'll use in the manifest.

    override fun onReceive(context: Context, intent: Intent) {
        
        // Only proceed if the user has already granted the necessary overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            // Cannot start service without permission, user must launch MainActivity first.
            return
        }

        // Check if the service is already running (optional but good practice)
        // For simplicity, we just start it; the OS handles duplicates.

        // Start the service
        val overlayIntent = Intent(context, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(overlayIntent)
        } else {
            context.startService(overlayIntent)
        }

        // Optional feedback
        Toast.makeText(context, "Ribbon Auto-Launched!", Toast.LENGTH_SHORT).show()
    }
}
