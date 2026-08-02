package com.example.networkmonitor

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var txtEngineStatus: TextView
    private lateinit var btnVpnToggle: Button

    private val appListFromApplication: ArrayList<AppModel>
        get() = (application as NetworkMonitorApplication).appList

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var isMonitoring = false
    private val VPN_REQUEST_CODE = 2002
    private val PERM_REQUEST_CODE = 3003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtEngineStatus = findViewById(R.id.txtEngineStatus)
        btnVpnToggle = findViewById(R.id.btnVpnToggle)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AppAdapter(appListFromApplication) { selectedApp ->
            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtra("PACKAGE_NAME", selectedApp.packageName)
                putExtra("APP_NAME", selectedApp.appName)
                putExtra("UID", selectedApp.uid)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        btnVpnToggle.setOnClickListener {
            checkPermissionsAndStartVpn()
        }

        checkAndRequestPermissions()

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access Permission Required", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (appListFromApplication.isEmpty()) {
            startRealtimeMonitoring()
        }
    }

    override fun onResume() {
        super.onResume()
        updateVpnUiState()
        if (hasUsageStatsPermission()) {
            adapter.updateList(appListFromApplication)
            if (appListFromApplication.isEmpty() && !isMonitoring) {
                startRealtimeMonitoring()
            }
        }
    }

    private fun updateVpnUiState() {
        if (LocalVpnService.isServiceActive) {
            txtEngineStatus.text = "MODE: Option A Local VPN (ACTIVE)"
            txtEngineStatus.setTextColor(0xFF00E676.toInt())
            btnVpnToggle.text = "VPN ACTIVE"
        } else {
            txtEngineStatus.text = "MODE: Option A Local VPN (INACTIVE)"
            txtEngineStatus.setTextColor(0xFFFFC107.toInt())
            btnVpnToggle.text = "START VPN"
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERM_REQUEST_CODE)
            }
        }
    }

    private fun checkPermissionsAndStartVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        } else {
            startVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, LocalVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateVpnUiState()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startRealtimeMonitoring() {
        isMonitoring = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                executor.execute {
                    val apps = fetchBatchNetworkUsage()
                    runOnUiThread {
                        appListFromApplication.clear()
                        appListFromApplication.addAll(apps)
                        adapter.updateList(apps)
                    }
                }
                handler.postDelayed(this, 3000)
            }
        })
    }

    private fun stopRealtimeMonitoring() {
        isMonitoring = false
    }

    private fun fetchBatchNetworkUsage(): List<AppModel> {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val netStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 60 * 24)

        val mobileUidMap = HashMap<Int, Long>()
        val wifiUidMap = HashMap<Int, Long>()

        try {
            val mobileStats = netStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val current = mobileUidMap.getOrDefault(bucket.uid, 0L)
                mobileUidMap[bucket.uid] = current + bucket.rxBytes + bucket.txBytes
            }
            mobileStats.close()
        } catch (_: Exception) {}

        try {
            val wifiStats = netStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val current = wifiUidMap.getOrDefault(bucket.uid, 0L)
                wifiUidMap[bucket.uid] = current + bucket.rxBytes + bucket.txBytes
            }
            wifiStats.close()
        } catch (_: Exception) {}

        val resultList = ArrayList<AppModel>()
        for (appInfo in packages) {
            val mobileBytes = mobileUidMap.getOrDefault(appInfo.uid, 0L)
            val wifiBytes = wifiUidMap.getOrDefault(appInfo.uid, 0L)
            val totalBytes = mobileBytes + wifiBytes

            if (totalBytes > 0) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                resultList.add(AppModel(appName, appInfo.packageName, appInfo.uid, icon, mobileBytes, wifiBytes, totalBytes))
            }
        }
        return resultList.sortedByDescending { it.totalRecentBytes }
    }
}
