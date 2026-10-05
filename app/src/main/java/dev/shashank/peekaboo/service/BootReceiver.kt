package dev.shashank.peekaboo.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.shashank.peekaboo.MainActivity
import dev.shashank.peekaboo.PeekApp
import dev.shashank.peekaboo.R
import dev.shashank.peekaboo.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After a reboot or update, brings the guard back. Android doesn't let camera services start
 * from the background on newer versions, so when that's refused we post a one-tap resume prompt.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val s = context.app.settings.current()
                if (!s.guardEnabled || !s.startOnBoot) return@launch
                val started = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && GuardService.start(context)
                if (!started) remind(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun remind(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 7,
            Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_RESUME_GUARD)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, PeekApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle("Resume Peek Guard")
            .setContentText("Tap to keep watching for shoulder surfers")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(7, n)
    }
}
