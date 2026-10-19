package com.example.tehranmetropoll

import android.app.Application
import com.example.tehranmetropoll.data.trip.TripNotifications
import com.example.tehranmetropoll.di.AppContainer
import org.osmdroid.config.Configuration

class MetroApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        TripNotifications.ensureChannels(this)
        Configuration.getInstance().apply {
            userAgentValue = BuildConfig.APPLICATION_ID
            // Tiles live in app storage (not the evictable cache) so areas seen once stay
            // available offline.
            osmdroidBasePath = filesDir.resolve("osmdroid")
            osmdroidTileCache = filesDir.resolve("osmdroid/tiles")
        }
    }
}
