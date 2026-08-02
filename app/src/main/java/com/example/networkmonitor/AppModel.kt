package com.example.networkmonitor

import android.graphics.drawable.Drawable

data class AppModel(
    val appName: String,
    val packageName: String,
    val uid: Int,
    val icon: Drawable,
    var mobileUsageBytes: Long = 0,
    var wifiUsageBytes: Long = 0,
    var totalRecentBytes: Long = 0
)
