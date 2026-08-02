package com.example.networkmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LocalVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        const val CHANNEL_ID = "NetworkMonitorVpnChannel"
        const val NOTIFICATION_ID = 1001
        val liveRxBytes = AtomicLong(0L)
        val liveTxBytes = AtomicLong(0L)
        @Volatile var isServiceActive = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.get()) {
            startForegroundNotification()
            startVpnTunnel()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnTunnel()
    }

    private fun startForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Monitor Persistent Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Monitor Running")
            .setContentText("Persistent VPN Network Inspection Active")
            .setSmallIcon(R.drawable.nt_guardg)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .setSession("Network Monitor Engine")
                .allowBypass()

            vpnInterface = builder.establish()
            isRunning.set(true)
            isServiceActive = true

            executor.execute {
                var lastGlobalRx = TrafficStats.getTotalRxBytes()
                var lastGlobalTx = TrafficStats.getTotalTxBytes()

                while (isRunning.get()) {
                    try {
                        Thread.sleep(1000)
                        val currGlobalRx = TrafficStats.getTotalRxBytes()
                        val currGlobalTx = TrafficStats.getTotalTxBytes()

                        if (currGlobalRx > lastGlobalRx && lastGlobalRx >= 0) {
                            liveRxBytes.set(currGlobalRx - lastGlobalRx)
                        } else {
                            liveRxBytes.set(0L)
                        }

                        if (currGlobalTx > lastGlobalTx && lastGlobalTx >= 0) {
                            liveTxBytes.set(currGlobalTx - lastGlobalTx)
                        } else {
                            liveTxBytes.set(0L)
                        }

                        lastGlobalRx = currGlobalRx
                        lastGlobalTx = currGlobalTx
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopVpnTunnel() {
        isRunning.set(false)
        isServiceActive = false
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
