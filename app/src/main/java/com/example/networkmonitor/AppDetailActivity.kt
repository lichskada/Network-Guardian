package com.example.networkmonitor

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class AppDetailActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var uid: Int = -1
    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var isUpdating = false

    private lateinit var graphView: GraphView
    private lateinit var txtDownloadSpeed: TextView
    private lateinit var txtUploadSpeed: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: "Unknown"
        val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
        uid = intent.getIntExtra("UID", -1)
        title = "$appName Diagnostics"

        graphView = findViewById(R.id.graphView)
        txtDownloadSpeed = findViewById(R.id.txtDownloadSpeed)
        txtUploadSpeed = findViewById(R.id.txtUploadSpeed)

        val txtDetails = findViewById<TextView>(R.id.txtDetails)
        val detailsText = StringBuilder()
        detailsText.append("App Package: $packageName\n")
        detailsText.append("System UID: $uid\n\n")
        detailsText.append("--- Engine Telemetry Status ---\n")
        detailsText.append("VpnService State: ${if (LocalVpnService.isServiceActive) "ACTIVE" else "INACTIVE"}\n")
        detailsText.append("Routing Bypass Mode: allowBypass() enabled\n\n")
        detailsText.append("--- Historical Monthly Network Usage ---\n\n")
        detailsText.append(getMonthlyUsageHistory(uid))

        txtDetails.text = detailsText.toString()

        executor.execute {
            val (rx, tx) = getUidBytes(uid)
            lastRxBytes = rx
            lastTxBytes = tx
            runOnUiThread { startGraphUpdates() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isUpdating = false
        handler.removeCallbacksAndMessages(null)
        executor.shutdown()
    }

    private fun startGraphUpdates() {
        isUpdating = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isUpdating) return
                executor.execute {
                    val (currentRx, currentTx) = getUidBytes(uid)

                    val rxDelta = if (lastRxBytes >= 0 && currentRx >= lastRxBytes) currentRx - lastRxBytes else 0L
                    val txDelta = if (lastTxBytes >= 0 && currentTx >= lastTxBytes) currentTx - lastTxBytes else 0L

                    lastRxBytes = currentRx
                    lastTxBytes = currentTx

                    val rxKb = rxDelta / 1024f
                    val txKb = txDelta / 1024f

                    runOnUiThread {
                        txtDownloadSpeed.text = String.format("Download: %.2f KB/s", rxKb)
                        txtUploadSpeed.text = String.format("Upload: %.2f KB/s", txKb)
                        graphView.addSample(rxKb, txKb)
                    }
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun getUidBytes(targetUid: Int): Pair<Long, Long> {
        val trafficRx = TrafficStats.getUidRxBytes(targetUid)
        val trafficTx = TrafficStats.getUidTxBytes(targetUid)

        if (trafficRx != TrafficStats.UNSUPPORTED.toLong() && trafficRx >= 0) {
            return Pair(trafficRx, trafficTx)
        }

        // Real-time fallback: Use global live VPN delta counters if service is running
        if (LocalVpnService.isServiceActive) {
            return Pair(LocalVpnService.liveRxBytes.get(), LocalVpnService.liveTxBytes.get())
        }

        val netStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val endTime = System.currentTimeMillis()
        var totalRx = 0L; var totalTx = 0L
        try {
            val mobileBucket = netStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null, 0L, endTime, targetUid)
            val (mRx, mTx) = sumBucketBytes(mobileBucket)
            val wifiBucket = netStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, 0L, endTime, targetUid)
            val (wRx, wTx) = sumBucketBytes(wifiBucket)
            totalRx = mRx + wRx
            totalTx = mTx + wTx
        } catch (_: Exception) { }
        return Pair(totalRx, totalTx)
    }

    private fun sumBucketBytes(stats: NetworkStats): Pair<Long, Long> {
        var rx = 0L; var tx = 0L
        val bucket = NetworkStats.Bucket()
        while (stats.hasNextBucket()) { stats.getNextBucket(bucket); rx += bucket.rxBytes; tx += bucket.txBytes }
        stats.close()
        return Pair(rx, tx)
    }

    private fun getMonthlyUsageHistory(uid: Int): String {
        val netStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val sb = StringBuilder()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        for (i in 0..5) {
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            val startTime = calendar.timeInMillis
            var mobileBytes = 0L; var wifiBytes = 0L
            try {
                val mobileBucket = netStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime, uid)
                val (mRx, mTx) = sumBucketBytes(mobileBucket); mobileBytes = mRx + mTx
                val wifiBucket = netStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, startTime, endTime, uid)
                val (wRx, wTx) = sumBucketBytes(wifiBucket); wifiBytes = wRx + wTx
            } catch (_: Exception) { }
            val monthLabel = dateFormat.format(endTime)
            sb.append("[$monthLabel] -> Mobile: ${formatBytes(mobileBytes)} | Wi-Fi: ${formatBytes(wifiBytes)}\n")
        }
        return sb.toString()
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }
}
