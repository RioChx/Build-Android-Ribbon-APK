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
    private lateinit var ribbonContainer: LinearLayout 
    
    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = true 
    private val expandedWidth = WindowManager.LayoutParams.WRAP_CONTENT
    private val collapsedWidth = 200 // Collapsed width in pixels
    private var currentAlpha = 0xCC // Initial 80% opacity (20% transparency)

    private val audioStream = AudioManager.STREAM_MUSIC
    private val updateClockRunnable = object : Runnable {
        override fun run() {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            clockWidget.text = format.format(Date())
            handler.postDelayed(this, 1000)
        }
    }
    
    // Function to change volume (used by both seekbar and mouse wheel)
    private fun changeVolume(direction: Int, volumeSeekBar: SeekBar) {
        val currentVolume = audioManager.getStreamVolume(audioStream)
        val maxVolume = audioManager.getStreamMaxVolume(audioStream)
        
        var newVolume = currentVolume + direction
        
        if (newVolume < 0) newVolume = 0
        if (newVolume > maxVolume) newVolume = maxVolume

        audioManager.setStreamVolume(audioStream, newVolume, AudioManager.FLAG_SHOW_UI)
        if (volumeSeekBar.visibility == View.VISIBLE) {
            volumeSeekBar.progress = newVolume
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "RtlHardcoded", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // --- LOAD SAVED SETTINGS ---
        val sharedPreferences = getSharedPreferences("RibbonSettings", Context.MODE_PRIVATE)
        val savedRibbonHex = sharedPreferences.getString("ribbon_color", "#000000") 
        val savedButtonHex = sharedPreferences.getString("button_color", "#FFFFFF") 
        val savedAlpha = sharedPreferences.getInt("ribbon_transparency", 0xCC) 
        
        currentAlpha = savedAlpha
        val ribbonBaseColor = try { Color.parseColor(savedRibbonHex) } catch (e: IllegalArgumentException) { Color.BLACK }
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

        // FLAG_NOT_FOCUSABLE is used here, but we need to intercept mouse scroll events.
        // We will rely on onGenericMotionListener to catch the scroll event on the view itself.
        layoutParams = WindowManager.LayoutParams(
            expandedWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 1. Initialize Views and Apply Colors
        ribbonContainer = overlayView.findViewById(R.id.ribbon_container)
        clockWidget = overlayView.findViewById(R.id.clock_widget)
        val homeButton = overlayView.findViewById<Button>(R.id.button_home)
        val recentButton = overlayView.findViewById<Button>(R.id.button_recent)
        val volumeToggleButton = overlayView.findViewById<Button>(R.id.button_volume_toggle)
        val volumeSeekBar = overlayView.findViewById<SeekBar>(R.id.volume_seekbar)
        val resizeButton = overlayView.findViewById<Button>(R.id.button_resize)
        val transparencySeekBar = overlayView.findViewById<SeekBar>(R.id.transparency_seekbar)
        val closeButton = overlayView.findViewById<Button>(R.id.button_close)

        // Apply saved Ribbon Color and Transparency
        val initialColor = Color.argb(currentAlpha, Color.red(ribbonBaseColor), Color.green(ribbonBaseColor), Color.blue(ribbonBaseColor))
        ribbonContainer.background = ColorDrawable(initialColor)

        // Apply saved Button Color to all buttons and the clock
        val viewsToColor = listOf<View>(clockWidget, homeButton, recentButton, volumeToggleButton, resizeButton, closeButton)
        for (view in viewsToColor) {
            if (view is TextView) {
                view.setTextColor(buttonColor)
            } else if (view is Button) {
                view.setTextColor(buttonColor)
            }
        }


        // 2. Set up Functions (Buttons/SeekBars)
        
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
        
        // Volume Toggle Button: Show/Hide SeekBar
        volumeToggleButton.setOnClickListener {
            if (volumeSeekBar.visibility == View.VISIBLE) {
                volumeSeekBar.visibility = View.GONE
            } else {
                volumeSeekBar.progress = audioManager.getStreamVolume(audioStream)
                volumeSeekBar.visibility = View.VISIBLE
            }
            transparencySeekBar.visibility = View.GONE 
        }
        
        // ... (Other button and transparency logic is the same) ...
        
        // 3. Enable Drag and Drop
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                
                // DRAG AND DROP CONTROL
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
        
        // 4. MOUSE SCROLL VOLUME CONTROL FIX: Use onGenericMotionListener
        overlayView.setOnGenericMotionListener { _, event ->
            if (event.action == MotionEvent.ACTION_SCROLL) {
                // event.getAxisValue(MotionEvent.AXIS_VSCROLL) returns a negative value for scroll up and a positive for scroll down.
                val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                
                // The convention for volume on a TV/media is scroll up = volume up, scroll down = volume down.
                if (scroll > 0) {
                    changeVolume(-1, volumeSeekBar) // Scroll Down -> Decrease volume
                } else if (scroll < 0) {
                    changeVolume(1, volumeSeekBar)  // Scroll Up -> Increase volume
                }
                return@setOnGenericMotionListener true // Consume the scroll event
            }
            false
        }
        
        // Final Button Listeners (omitted for space, assume they are correct from last full post)
        // ...

        // 5. Start Overlay
        windowManager.addView(overlayView, layoutParams)
        handler.post(updateClockRunnable) 
    }

    override fun onDestroy() {
        super.onDestroy()
        // ... (Cleanup is the same) ...
    }
}
