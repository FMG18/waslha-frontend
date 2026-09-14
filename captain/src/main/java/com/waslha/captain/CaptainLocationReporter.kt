package com.waslha.captain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CaptainLocationReporter(context: Context) {
    private val appContext = context.applicationContext
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var callback: LocationCallback? = null
    private var job: Job? = null
    private var lastSentAt = 0L

    @SuppressLint("MissingPermission")
    fun start(onLocation: (Coordinates) -> Unit) {
        if (!hasPermission()) return
        stop()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setWaitForAccurateLocation(false)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val point = Coordinates(location.latitude, location.longitude)
                onLocation(point)
                val now = System.currentTimeMillis()
                if (now - lastSentAt >= 3000L) {
                    lastSentAt = now
                    job?.cancel()
                    job = scope.launch(Dispatchers.IO) {
                        runCatching { CaptainApiProvider.api.updateLocation(DriverLocationRequest(point.lat, point.lng)) }
                    }
                }
            }
        }
        fused.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }

    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        job?.cancel()
        job = null
    }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
