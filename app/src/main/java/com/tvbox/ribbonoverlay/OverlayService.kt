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
import android.view.*
import android.widget.*
import android.graphics.Color
import android.view.accessibility.AccessibilityNodeInfo

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    
    private lateinit var clockHubContainer: LinearLayout 
    private lateinit var ribbon1: LinearLayout
    private lateinit var ribbon2: LinearLayout
    private lateinit var ribbon3: LinearLayout
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var transparencySeekBar: SeekBar
    
    private val handler = Handler(Looper.getMainLooper())
    
    private val expandedWidth = WindowManager.LayoutParams.WRAP_CONTENT
    private val audioStream = AudioManager.STREAM_MUSIC
    
    // NO COLLAPSE LOGIC - The ribbons are permanent.

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
    
    private fun simulateKey(keyCode: Int) {
        val intent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(intent)
        Toast.makeText(this, "Attempting to send key event: $keyCode", Toast.LENGTH_SHORT).show()
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


        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.ribbon_overlay_layout, null)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Window Flags for permanent visibility (FLAG_NOT_FOCUSABLE)
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

        // 1. Initialize Views 
        clockHubContainer = overlayView.findViewById(R.id.clock_hub_container)
        val analogClock = overlayView.findViewById<AnalogClock>(R.id.analog_clock_widget)
        
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
        val viewsToColor = listOf<View>(homeButton, recentButton, volumeToggleButton, closeButton)
        for (view in viewsToColor) {
            if (view is TextView) {
                view.setTextColor(buttonColor)
            } else if (view is Button) {
                view.setTextColor(buttonColor)
            }
        }

        // 2. Set up Functionality (Buttons/SeekBars)
        
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
            override fun onStopTrackingTouch(seekBar: SeekBar?) {} 
        })
        
        // --- BUTTON ACTIONS IMPLEMENTATION ---
        
        // HOME Button Action 
        homeButton.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
        
        // RECENT Button Action
        recentButton.setOnClickListener {
            val recentsIntent = Intent("com.android.systemui.recent.action.TOGGLE_RECENTS").apply {
                 flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            try {
                startActivity(recentsIntent)
            } catch (e: Exception) {
                simulateKey(KeyEvent.KEYCODE_APP_SWITCH) 
            }
        }
        
        // VOLUME Button Action (Toggle SeekBar)
        volumeToggleButton.setOnClickListener {
            if (volumeSeekBar.visibility == View.VISIBLE) {
                volumeSeekBar.visibility = View.GONE
            } else {
                volumeSeekBar.progress = audioManager.getStreamVolume(audioStream)
                volumeSeekBar.visibility = View.VISIBLE
            }
            transparencySeekBar.visibility = View.GONE 
        }
        
        // Transparency Seekbar Toggle (using Long Press on HOME button)
        homeButton.setOnLongClickListener {
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
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        // 4. MOUSE SCROLL VOLUME CONTROL FIX (Corrected Direction)
        overlayView.setOnGenericMotionListener { _, event ->
            if (event.action == MotionEvent.ACTION_SCROLL) {
                val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                
                // Scroll Down (Positive scroll) -> Decrease volume (-1)
                // Scroll Up (Negative scroll) -> Increase volume (+1)
                if (scroll > 0) {
                    changeVolume(-1) 
                } else if (scroll < 0) {
                    changeVolume(1)  
                }
                return@setOnGenericMotionListener true 
            }
            false
        }
        
        // 5. Drag and Drop Control 
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_SCROLL) return false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
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
        
        // 6. Close Button Listener
        closeButton.setOnClickListener { stopSelf() }

        // 7. Start Overlay
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
