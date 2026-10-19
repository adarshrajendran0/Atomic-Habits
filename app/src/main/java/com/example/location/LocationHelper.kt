package com.example.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun getCurrentWifiSsid(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info?.ssid?.trim('"')
            return ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } catch (e: Exception) {
            return null
        }
    }

    @SuppressLint("MissingPermission")
    fun observeWifiNetwork(context: Context): Flow<String?> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(getCurrentWifiSsid(context)) }
            override fun onLost(network: Network) { trySend(null) }
        }
        
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            trySend(getCurrentWifiSsid(context))
        } catch (e: Exception) {
            // Ignored
        }
        
        awaitClose { 
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch(e: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun registerGeofence(context: Context, habitId: Int, lat: Double, lng: Double, radiusMeters: Float) {
        val geofence = Geofence.Builder()
            .setRequestId("habit_$habitId")
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = PendingIntent.getBroadcast(
            context, habitId, Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        LocationServices.getGeofencingClient(context).addGeofences(request, pendingIntent)
    }
}
