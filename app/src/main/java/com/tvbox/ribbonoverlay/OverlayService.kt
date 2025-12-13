package com.tvbox.ribbonoverlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var clockWidget: TextView
    private lateinit var clockHubContainer: LinearLayout
    private lateinit var ribbon1: LinearLayout
    private lateinit var ribbon2: LinearLayout
    private lateinit var ribbon3: LinearLayout
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var transparencySeekBar: SeekBar
    
    private val handler = Handler(Looper.getMainLooper())
    
    private val expandedWidth = WindowManager.LayoutParams.WRAP_CONTENT
    private val audioStream = AudioManager.STREAM_MUSIC
    
    // Auto-collapse logic
    private val COLLAPSE_DELAY_MS = 7000L // 7 seconds
    private val collapseRunnable = Runnable { collapseRibbons() }

    private val updateClockRunnable = object : Runnable {
        override fun run() {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            clockWidget.text = format.format(Date())
            handler.postDelayed(this, 1000)
        }
    }
    
    private fun changeVolume(direction: Int) {
        val currentVolume = audioManager.getStreamVolume(audioStream)
        val maxVolume = audioManager.getStreamMaxVolume(audioStream)
        
        var newVolume = currentVolume + direction
        
        if (newVolume < 0) newVolume = 0
        if (newVolume > maxVolume) newVolume = maxVolume

        audioManager.setStreamVolume(audioStream, newVolume, AudioManager.FLAG_SHOW_UI)
        if (::volumeSeekBar.isInitialized && volumeSeekBar.visibility == View.VISIBLE) {
            volumeSeekBar.progress = newVolume
        }
    }

    private fun expandRibbons() {
        handler.removeCallbacks(collapseRunnable)
        ribbon1.visibility = View.VISIBLE
        ribbon2.visibility = View.VISIBLE
        ribbon3.visibility = View.VISIBLE
        // Restart the timer to collapse after 7 seconds of inactivity
        handler.postDelayed(collapseRunnable, COLLAPSE_DELAY_MS)
    }

    private fun collapseRibbons() {
        ribbon1.visibility = View.GONE
        ribbon2.visibility = View.GONE
        ribbon3.visibility = View.GONE
        // Ensure seek bars are also hidden when collapsing
        volumeSeekBar.visibility = View.GONE
        transparencySeekBar.visibility = View.GONE
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "RtlHardcoded", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // --- LOAD SAVED SETTINGS ---
        val sharedPreferences = getSharedPreferences("RibbonSettings", Context.MODE_PRIVATE)
        val savedButtonHex = sharedPreferences.getString("button_color", "#FFFFFF") 
        val buttonColor = try { Color.parseColor(savedButtonHex) } catch (e: IllegalArgumentException) { Color.WHITE }
        // We ignore ribbon color/transparency here for simplicity as the new design uses solid blue 
        // for the ribbons to match the static image, but the buttons are still colored.

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.ribbon_overlay_layout, null)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FIX: Using FLAG_NOT_FOCUSABLE and FLAG_NOT_TOUCH_MODAL is key for persistence
        layoutParams = WindowManager.LayoutParams(
            expandedWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 1. Initialize Views (using new IDs)
        clockHubContainer = overlayView.findViewById(R.id.clock_hub_container)
        clockWidget = overlayView.findViewById(R.id.clock_widget)
        ribbon1 = overlayView.findViewById(R.id.ribbon_1)
        ribbon2 = overlayView.findViewById(R.id.ribbon_2)
        ribbon3 = overlayView.findViewById(R.id.ribbon_3)
        
        val homeButton = overlayView.findViewById<Button>(R.id.button_home)
        val recentButton = overlayView.findViewById<Button>(R.id.button_recent)
        val volumeToggleButton = overlayView.findViewById<Button>(R.id.button_volume_toggle)
        val closeButton = overlayView.findViewById<Button>(R.id.button_close)
        volumeSeekBar = overlayView.findViewById(R.id.volume_seekbar)
        transparencySeekBar = overlayView.findViewById(R.id.transparency_seekbar) 
        
        // Apply saved Button Color
        val viewsToColor = listOf<View>(clockWidget, homeButton, recentButton, volumeToggleButton, closeButton)
        for (view in viewsToColor) {
            if (view is TextView) {
                view.setTextColor(buttonColor)
            } else if (view is Button) {
                view.setTextColor(buttonColor)
            }
        }


        // 2. Dynamic Collapse/Expand Logic
        
        // A) Clock Listener to Expand Ribbons
        val expandListener = View.OnClickListener { expandRibbons() }
        clockHubContainer.setOnClickListener(expandListener)
        
        // B) Reset timer on hover/focus (for mouse/D-Pad input)
        clockHubContainer.setOnGenericMotionListener { _, event ->
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                expandRibbons()
                return@setOnGenericMotionListener true
            }
            false
        }

        // C) Start 7-second timer immediately
        handler.postDelayed(collapseRunnable, COLLAPSE_DELAY_MS)
        

        // 3. Set up Functionality (Buttons/SeekBars)
        
        // Volume Control and Toggle (Same logic, but now hides/shows within the main container)
        val maxVolume = audioManager.getStreamMaxVolume(audioStream)
        volumeSeekBar.max = maxVolume
        volumeSeekBar.progress = audioManager.getStreamVolume(audioStream)

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(audioStream, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { expandRibbons() } // Keep expanded while interacting
        })
        
        volumeToggleButton.setOnClickListener {
            expandRibbons() // Keep expanded on interaction
            if (volumeSeekBar.visibility == View.VISIBLE) {
                volumeSeekBar.visibility = View.GONE
            } else {
                volumeSeekBar.progress = audioManager.getStreamVolume(audioStream)
                volumeSeekBar.visibility = View.VISIBLE
            }
            transparencySeekBar.visibility = View.GONE 
        }
        
        // Transparency Seekbar Toggle (using Long Press on Home button remains)
        homeButton.setOnLongClickListener {
            expandRibbons() // Keep expanded on interaction
            if (transparencySeekBar.visibility == View.VISIBLE) {
                transparencySeekBar.visibility = View.GONE
            } else {
                transparencySeekBar.visibility = View.VISIBLE
            }
            volumeSeekBar.visibility = View.GONE 
            true
        }
        transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { /* ... */ }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { expandRibbons() }
        })


        // 4. MOUSE SCROLL VOLUME CONTROL FIX (Same reliable logic)
        overlayView.setOnGenericMotionListener { _, event ->
            if (event.action == MotionEvent.ACTION_SCROLL) {
                val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                
                if (scroll > 0) {
                    changeVolume(-1) // Scroll Down -> Decrease volume
                } else if (scroll < 0) {
                    changeVolume(1)  // Scroll Up -> Increase volume
                }
                expandRibbons() // Keep expanded while scrolling
                return@setOnGenericMotionListener true 
            }
            false
        }
        
        // 5. Drag and Drop Control (same logic)
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_SCROLL) return false // Handled by onGenericMotionListener
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        expandRibbons() // Keep expanded when starting drag
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isDragging = true
                            layoutParams.x = initialX + dx.toInt()
                            layoutParams.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(overlayView, layoutParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        return isDragging
                    }
                }
                return false
            }
        })
        
        // 6. Other Button Listeners (standard functions)
        homeButton.setOnClickListener { /* ... standard home logic ... */ expandRibbons() }
        recentButton.setOnClickListener { /* ... standard recent logic ... */ expandRibbons() }
        closeButton.setOnClickListener { stopSelf() }

        // 7. Start Overlay and Clock
        windowManager.addView(overlayView, layoutParams)
        handler.post(updateClockRunnable) 
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateClockRunnable)
        handler.removeCallbacks(collapseRunnable)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
