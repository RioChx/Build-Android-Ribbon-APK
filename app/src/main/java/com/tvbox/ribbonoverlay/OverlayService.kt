// ... (Top imports are the same)
// ... (Class definition and top variables are the same)

    @SuppressLint("InflateParams", "RtlHardcoded", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // --- NEW: Load Saved Settings ---
        val sharedPreferences = getSharedPreferences("RibbonSettings", Context.MODE_PRIVATE)
        val savedRibbonHex = sharedPreferences.getString("ribbon_color", "#000000") // Default Black
        val savedButtonHex = sharedPreferences.getString("button_color", "#FFFFFF") // Default White
        val savedAlpha = sharedPreferences.getInt("ribbon_transparency", 0xCC) // Default 80% opacity
        
        // Use the saved alpha and color for initial launch
        currentAlpha = savedAlpha
        val ribbonBaseColor = try { Color.parseColor(savedRibbonHex) } catch (e: IllegalArgumentException) { Color.BLACK }
        val buttonColor = try { Color.parseColor(savedButtonHex) } catch (e: IllegalArgumentException) { Color.WHITE }

        // --- (Rest of WindowManager setup is the same) ---
        // ...
        
        // 1. Initialize Views
        // ... (All view initializations are the same) ...

        // Set the initial color and transparency from settings
        val initialColor = Color.argb(currentAlpha, Color.red(ribbonBaseColor), Color.green(ribbonBaseColor), Color.blue(ribbonBaseColor))
        ribbonContainer.background = ColorDrawable(initialColor)

        // Apply Button Color to all buttons and the clock
        val viewsToColor = listOf<View>(clockWidget, homeButton, recentButton, volumeToggleButton, resizeButton, closeButton)
        for (view in viewsToColor) {
            if (view is TextView) {
                view.setTextColor(buttonColor)
            } else if (view is Button) {
                view.setTextColor(buttonColor)
                // Note: Changing the button background requires more complex styling,
                // so we only change the text color for simplicity here.
            }
        }


        // 2. Set up Functions (Transparency logic is now simplified as the settings activity saves the final values)
        // ... (All button and seekbar logic remains the same) ...

        // 2.5 Transparency/Color Control (Updated to use loaded color)
        transparencySeekBar.max = 255 
        transparencySeekBar.progress = currentAlpha // Start the seekbar at the saved value

        transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentAlpha = progress
                    // Apply new alpha (transparency) to the saved ribbon base color
                    val newColor = Color.argb(currentAlpha, Color.red(ribbonBaseColor), Color.green(ribbonBaseColor), Color.blue(ribbonBaseColor))
                    ribbonContainer.background = ColorDrawable(newColor)
                }
            }
            // ... (onStartTrackingTouch and onStopTrackingTouch are empty) ...
        })
        
        // ... (All other function implementations are the same) ...

        // 3. Enable Drag and Drop
        // ... (Drag and drop logic is the same) ...

        // 4. Start Overlay and Services
        windowManager.addView(overlayView, layoutParams)
        handler.post(updateClockRunnable) 
    }

// ... (Rest of the class methods are the same)
