package dev.shashank.peekaboo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dev.shashank.peekaboo.billing.ProStore
import dev.shashank.peekaboo.data.OwnerFaceStore
import dev.shashank.peekaboo.data.PeekDatabase
import dev.shashank.peekaboo.data.SettingsRepository

class PeekApp : Application() {
    val db by lazy { PeekDatabase.create(this) }
    val settings by lazy { SettingsRepository(this) }
    val owner by lazy { OwnerFaceStore(this) }
    val pro by lazy { ProStore(this, settings) }

    override fun onCreate() {
        super.onCreate()
        pro.start()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_GUARD, "Guard status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while Peek-a-Boo is watching for shoulder surfers"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Resume reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Asks you to resume the guard after a restart"
            }
        )
    }

    companion object {
        const val CHANNEL_GUARD = "guard"
        const val CHANNEL_ALERTS = "alerts"
    }
}

val Context.app: PeekApp get() = applicationContext as PeekApp
