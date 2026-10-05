package dev.shashank.peekaboo.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.service.ForegroundApp
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.PeekSwitch
import dev.shashank.peekaboo.ui.components.RoundIconButton
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppRow(val pkg: String, val label: String, val icon: ImageBitmap)

/** Pro: pick the apps the Privacy Shield should protect. */
@Composable
fun AppsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    var usageAccess by remember { mutableStateOf(ForegroundApp.granted(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { usageAccess = ForegroundApp.granted(ctx) }
    val iconPx = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.roundToPx() }
    val apps by produceState<List<AppRow>?>(null) {
        value = withContext(Dispatchers.IO) {
            ForegroundApp.launchable(ctx).map { AppRow(it.packageName, it.label, it.icon.toBitmap(iconPx, iconPx).asImageBitmap()) }
        }
    }
    // Chosen apps float to the top, but only when the screen opens so rows don't jump while toggling.
    val initial = remember { s.protectedApps }
    val sorted = remember(apps) { apps?.sortedByDescending { it.pkg in initial } }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", vm::close)
            }
            Spacer(Modifier.height(16.dp))
            Text("PRIVACY SHIELD", style = Eyebrow, color = Ink.TextFaint)
            Spacer(Modifier.height(8.dp))
            Text("Protected apps", style = MaterialTheme.typography.displaySmall, color = Ink.Text)
            Section(footer = "When on, the shield only switches on while one of the apps below is open. Peeks are still counted everywhere.") {
                ListRow("Only in protected apps", icon = Icons.Rounded.AppShortcut, showDivider = !usageAccess || !s.protectedOnly, onClick = { vm.setProtectedOnly(!s.protectedOnly) }, chevron = false, trailing = {
                    PeekSwitch(s.protectedOnly, vm::setProtectedOnly)
                })
                if (!usageAccess) {
                    PermissionRow("Usage access", "Needed to see which app is open", false, Icons.Rounded.QueryStats, last = true) {
                        runCatching { ctx.startActivity(ForegroundApp.settingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            }
            Text("APPS", style = Eyebrow, color = Ink.TextFaint, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp))
        }
        val list = sorted
        if (list == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
        } else if (list.isEmpty()) {
            item {
                Text(
                    "No apps to show.",
                    style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        } else {
            items(list, key = { it.pkg }) { app ->
                val on = app.pkg in s.protectedApps
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .panel(RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(app.icon, null, Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, style = MaterialTheme.typography.titleMedium, color = Ink.Text, maxLines = 1, modifier = Modifier.weight(1f))
                    PeekSwitch(on, { vm.setProtectedApp(app.pkg, it) })
                }
            }
        }
    }
}
