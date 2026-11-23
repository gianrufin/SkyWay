package com.example.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.model.LatLon
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(context: Context): LatLon? {
        val fused = withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            cont.resume(LatLon(loc.latitude, loc.longitude))
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last ->
                                    cont.resume(last?.let { LatLon(it.latitude, it.longitude) })
                                }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
        }
        if (fused != null) return fused

        // Fallback to Android LocationManager lastKnownLocation if Play Services is unavailable
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            providers.firstNotNullOfOrNull { provider ->
                lm?.getLastKnownLocation(provider)?.let { LatLon(it.latitude, it.longitude) }
            }
        }.getOrNull()
    }
}
