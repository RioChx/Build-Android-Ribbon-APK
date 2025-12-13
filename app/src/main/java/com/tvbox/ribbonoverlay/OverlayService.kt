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
    private val baseColor = Color.rgb(0, 0, 0) // Fixed base color (Black)

    private val updateClockRunnable = object : Runnable {
        override fun run() {
            // 12-hour format with Am/Pm
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            clockWidget.text = format.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "RtlHardcoded", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.ribbon_overlay_layout, null)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Use TYPE_APPLICATION_OVERLAY to ensure it displays on top of all apps/screens
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

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
        ribbonContainer = overlayView.findViewById(R.id.ribbon_container)
        clockWidget = overlayView.findViewById(R.id.clock_widget)
        val homeButton = overlayView.findViewById<Button>(R.id.button_home)
        val recentButton = overlayView.findViewById<Button>(R.id.button_recent)
        val volumeToggleButton = overlayView.findViewById<Button>(R.id.button_volume_toggle)
        val volumeSeekBar = overlayView.findViewById<SeekBar>(R.id.volume_seekbar)
        val resizeButton = overlayView.findViewById<Button>(R.id.button_resize)
        val transparencySeekBar = overlayView.findViewById<SeekBar>(R.id.transparency_seekbar)
        val closeButton = overlayView.findViewById<Button>(R.id.button_close)

        // --- Set Initial Transparency ---
        val initialColor = Color.argb(currentAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        ribbonContainer.background = ColorDrawable(initialColor)


        // 2. Set up Functions

        // 2.1 Home Button
        homeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // 2.2 Recent Button
        recentButton.setOnClickListener {
            try {
                val intent = Intent("com.android.systemui.recent.action.TOGGLE_RECENTS")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Recent Apps action failed. Opening App Settings.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
        
        // 2.3 Volume Control
        val audioStream = AudioManager.STREAM_MUSIC
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
        
        // 2.4 Resize Functionality
        resizeButton.setOnClickListener {
            if (isExpanded) {
                layoutParams.width = collapsedWidth
                resizeButton.text = "+" 
            } else {
                layoutParams.width = expandedWidth
                resizeButton.text = "-"
            }
            isExpanded = !isExpanded
            windowManager.updateViewLayout(overlayView, layoutParams)
        }
        
        // 2.5 Transparency/Color Control
        transparencySeekBar.max = 255 
        transparencySeekBar.progress = currentAlpha 

        transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentAlpha = progress
                    // Apply new alpha (transparency) to the base color (Black)
                    val newColor = Color.argb(currentAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                    ribbonContainer.background = ColorDrawable(newColor)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Toggle transparency seekbar via Long Press on Home button 
        homeButton.setOnLongClickListener {
            if (transparencySeekBar.visibility == View.VISIBLE) {
                transparencySeekBar.visibility = View.GONE
            } else {
                transparencySeekBar.visibility = View.VISIBLE
            }
            volumeSeekBar.visibility = View.GONE 
            true
        }
        
        // 2.6 Close Button 
        closeButton.setOnClickListener {
            stopSelf()
        }


        // 3. Enable Drag and Drop
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
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

        // 4. Start Overlay and Services
        windowManager.addView(overlayView, layoutParams)
        handler.post(updateClockRunnable) 
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateClockRunnable)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
