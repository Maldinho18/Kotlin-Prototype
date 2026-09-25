package com.sidequests.app.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): LocationData? {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(
                        location?.let {
                            LocationData(
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        }
                    )
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
