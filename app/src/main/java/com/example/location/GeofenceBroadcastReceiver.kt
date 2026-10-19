package com.example.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = com.google.android.gms.location.GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition !in listOf(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER, com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)) return
        val habitIds = event.triggeringGeofences?.mapNotNull {
            it.requestId.removePrefix("habit_").toIntOrNull()
        } ?: return
        val isEntering = event.geofenceTransition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
        
        val prefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        val activeGeofences = prefs.getStringSet("active_geofences", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        var changed = false
        habitIds.forEach { id ->
            val idStr = id.toString()
            if (isEntering) {
                if (activeGeofences.add(idStr)) changed = true
            } else {
                if (activeGeofences.remove(idStr)) changed = true
            }
        }
        
        if (changed) {
            prefs.edit().putStringSet("active_geofences", activeGeofences).apply()
            
            // Refresh widgets
            val mgr = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, com.example.widget.HabitWidgetProvider::class.java))
            mgr.notifyAppWidgetViewDataChanged(ids, com.example.R.id.widget_stack)
            mgr.notifyAppWidgetViewDataChanged(ids, com.example.R.id.widget_flipper)
            context.sendBroadcast(Intent(context, com.example.widget.HabitWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    }
}
