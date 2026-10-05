package dev.shashank.peekaboo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dev.shashank.peekaboo.service.GuardService
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.PeekRoot
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.theme.PeekTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { PeekTheme { PeekRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        app.pro.refresh()
        // Whenever the app is opened, make sure an enabled guard is actually running:
        // this is the moment Android allows a camera service to (re)start.
        lifecycleScope.launch {
            val s = app.settings.current()
            if (s.guardEnabled && !GuardState.running.value && Permissions.camera(this@MainActivity)) {
                GuardService.start(this@MainActivity)
            }
        }
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_RESUME_GUARD) {
            lifecycleScope.launch {
                app.settings.setGuardEnabled(true)
                if (Permissions.camera(this@MainActivity)) GuardService.start(this@MainActivity)
            }
        }
    }

    companion object {
        const val ACTION_RESUME_GUARD = "dev.shashank.peekaboo.RESUME_GUARD"
    }
}
