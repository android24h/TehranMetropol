package ir.halvaei.metropol

import android.app.Application
import ir.halvaei.metropol.data.trip.TripNotifications
import ir.halvaei.metropol.di.AppContainer
import org.osmdroid.config.Configuration

class MetroApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applyDefaultLanguage()
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
