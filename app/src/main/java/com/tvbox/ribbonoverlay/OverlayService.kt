// ... (imports remain the same)

class OverlayService : Service() {

    // ... (existing variable declarations)
    
    // NEW: Digital clock reference
    private lateinit var digitalClock: TextView
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            digitalClock.text = currentTime
            handler.postDelayed(this, 1000) // Update every second
        }
    }
    
    // ... (rest of variable declarations and functions remain the same)
    
    @SuppressLint("InflateParams", "RtlHardcoded", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // ... (existing code to load settings and initialize WindowManager)

        // 1. Initialize Views 
        clockHubContainer = overlayView.findViewById(R.id.clock_hub_container)
        
        // FIXED: Reference the new digital clock TextView
        digitalClock = overlayView.findViewById(R.id.digital_clock_widget)
        
        // ... (existing ribbon/button initialization)

        // Apply saved Button Color 
        // ... (existing code to apply color)

        // 2. Set up Functionality (Buttons/SeekBars)
        // ... (existing code for volume/seekbars)

        // 6. Clock Initialization (Start time update)
        handler.post(updateTimeRunnable) 

        // 7. Close Button Listener
        closeButton.setOnClickListener { stopSelf() }

        // 8. Start Overlay
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the time update thread when service is destroyed
        handler.removeCallbacks(updateTimeRunnable) 
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
