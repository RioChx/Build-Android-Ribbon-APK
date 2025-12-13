package com.tvbox.ribbonoverlay

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var ribbonView: View
    private lateinit var params: WindowManager.LayoutParams
    
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val HIDE_DELAY: Long = 5000 // 5 seconds

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable: Runnable = object : Runnable {
        override fun run() {
            // Update the clock every second in 12-hour format (hh:mm a)
            val clockText = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            ribbonView.findViewById<TextView>(R.id.digital_clock)?.text = clockText
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        ribbonView = LayoutInflater.from(this).inflate(R.layout.ribbon_layout, null)
        
        // Setup WindowManager LayoutParams for a system overlay window
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0 // Initial position
            y = 0 
        }

        // Setup Drag and Drop
        ribbonView.setOnTouchListener(createTouchListener())

        setupButtons()
        setupVolumeControl()
        
        // Add the view initially
        try {
            windowManager.addView(ribbonView, params)
        } catch (e: Exception) {
            stopSelf()
            return
        }
        clockHandler.post(clockRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "RibbonServiceChannel"
        // Setup Foreground Notification to keep the service running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ribbon Overlay"
            val notificationChannel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ribbon Active")
            .setContentText("System overlay is running.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .build()
        
        startForeground(1, notification)
        
        resetHideTimer() 
        return START_STICKY
    }
    
    private fun setupButtons() {
        // HOME Button
        ribbonView.findViewById<Button>(R.id.btn_home)?.setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
            resetHideTimer()
        }

        // RECENT Button: Sends a broadcast to the SystemAccessibilityService
        ribbonView.findViewById<Button>(R.id.btn_recent)?.setOnClickListener {
            val intent = Intent("com.tvbox.ribbonoverlay.OPEN_RECENTS")
            sendBroadcast(intent)
            resetHideTimer()
        }
    }
    
    private fun setupVolumeControl() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volumeSeekBar = ribbonView.findViewById<SeekBar>(R.id.volume_bar)
        
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        volumeSeekBar?.max = 100 
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val scaledCurrentVolume = (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
        volumeSeekBar?.progress = scaledCurrentVolume

        volumeSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newVolume = (progress.toFloat() / 100.0 * maxVolume).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    resetHideTimer()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun createTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            resetHideTimer() 
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(ribbonView, params)
                    return@OnTouchListener true
                }
                else -> return@OnTouchListener false
            }
        }
    }
    
    private fun resetHideTimer() {
        autoHideHandler.removeCallbacksAndMessages(null)
        
        // Post new 5-second timer to auto-hide the ribbon
        autoHideHandler.postDelayed({
            if (ribbonView.isAttachedToWindow) {
                windowManager.removeView(ribbonView)
            }
            stopSelf() // Stop the service when the ribbon is hidden
        }, HIDE_DELAY)

        // Ensure the ribbon is visible if it was previously hidden
        if (!ribbonView.isAttachedToWindow) {
             try {
                windowManager.addView(ribbonView, params)
             } catch (e: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources when service stops
        if (ribbonView.isAttachedToWindow) {
            windowManager.removeView(ribbonView)
        }
        autoHideHandler.removeCallbacksAndMessages(null)
        clockHandler.removeCallbacksAndMessages(null)
    }
}
