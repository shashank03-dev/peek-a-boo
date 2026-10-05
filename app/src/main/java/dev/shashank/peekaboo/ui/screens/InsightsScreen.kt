package dev.shashank.peekaboo.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.BarChart
import dev.shashank.peekaboo.ui.components.IconBadge
import dev.shashank.peekaboo.ui.components.Panel
import dev.shashank.peekaboo.ui.components.RoundIconButton
import dev.shashank.peekaboo.ui.components.ScreenHeader
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.SectionLabel
import dev.shashank.peekaboo.ui.components.SegmentedControl
import dev.shashank.peekaboo.ui.components.Tag
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.InterDisplay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val report by vm.today.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val weekTotal by vm.weekTotal.collectAsStateWithLifecycle()
    val todayEvents by vm.todayEvents.collectAsStateWithLifecycle()
    val weekEvents by vm.weekEvents.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val guardOn = settings?.guardEnabled == true
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) vm.setGuard(true) }
    var range by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<PeekEvent?>(null) }
    val list = if (range == 0) todayEvents else weekEvents.take(50)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Activity", "Who's been looking") {
                RoundIconButton(Icons.Rounded.IosShare, "Share report") { ctx.startActivity(vm.shareReport()) }
            }
        }
        item { SegmentedControl(listOf("Today", "This week"), range, { range = it }) }

        item {
            AnimatedContent(range, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(120)) }, label = "hero") { r ->
                val big = if (r == 0) report.peeks else weekTotal
                val animated by animateIntAsState(big, tween(700), label = "big")
                Panel(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Text(if (r == 0) "Peeks today" else "Peeks in the last 7 days", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$animated",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (big > 0) Ink.Alert else Ink.Text,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (r == 0) "by ${report.people} ${if (report.people == 1) "person" else "people"}"
                            else "about ${"%.1f".format(weekTotal / 7f)} a day",
                            style = MaterialTheme.typography.titleMedium, color = Ink.TextMuted,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (r == 0) {
                        BarChart(report.hourly.toList(), listOf("12AM", "6AM", "12PM", "6PM"), report.peakHour, Ink.Alert)
                    } else {
                        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                        BarChart(week.map { it.count }, week.map { dayFmt.format(Date(it.dayStart)).take(2).uppercase() }, 6, Ink.Alert)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Watched", Reports.formatDuration(report.totalPeekMs), Modifier.weight(1f))
                MiniStat("Longest", Reports.formatDuration(report.longestPeekMs), Modifier.weight(1f))
                MiniStat("Busiest", report.peakHour?.let { Reports.formatHour(it) } ?: "—", Modifier.weight(1f))
            }
        }

        item { InsightCard(report.peeks, report.peakHour, report.people, week.maxOfOrNull { it.count } ?: 0) }

        item { SectionLabel(if (range == 0) "Timeline" else "Timeline · this week", Modifier.padding(top = 16.dp)) }
        if (list.isEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconBadge(Icons.Rounded.NightsStay, Ink.TextMuted, size = 48.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(if (guardOn) "Nothing to report" else "The guard is off", style = MaterialTheme.typography.titleLarge, color = Ink.Text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (guardOn) "You're covered. Each peek will show up here with the time, how long they looked and a snapshot."
                            else "Turn it on and every peek will land here with the time, how long they looked and a snapshot.",
                            style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, textAlign = TextAlign.Center,
                        )
                        if (!guardOn) {
                            Spacer(Modifier.height(24.dp))
                            PrimaryButton("Start guard") {
                                if (Permissions.camera(ctx)) vm.setGuard(true) else cameraLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(list, key = { _, e -> e.id }) { i, e ->
                TimelineItem(e, first = i == 0, last = i == list.lastIndex, showDay = range == 1) { selected = e }
            }
        }
    }

    selected?.let { event ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheet,
            containerColor = Ink.Surface,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            dragHandle = {
                Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(Ink.LineStrong))
            },
        ) {
            PeekDetail(event) {
                vm.deleteEvent(event)
                selected = null
            }
        }
    }
}

@Composable
internal fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.panel(RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = Ink.Text, maxLines = 1)
    }
}

@Composable
internal fun InsightCard(peeks: Int, peakHour: Int?, people: Int, bestDay: Int) {
    val text = when {
        peeks == 0 -> "Your screen stayed private today. Keep the guard on in crowded places like metros and cafés."
        peakHour != null && peeks >= 3 -> "Most peeks happen around ${Reports.formatHour(peakHour)}. That's a good time to dim your screen or angle it away."
        people > 1 -> "$people different people looked at your screen today. Repeat peekers are grouped by face, so each person counts once."
        else -> "One curious onlooker today. A quick tilt of the screen is usually enough."
    }
    Panel(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
        Row {
            IconBadge(Icons.Rounded.Lightbulb, Ink.Text, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                if (bestDay > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Busiest day this week: $bestDay peeks", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                }
            }
        }
    }
}

@Composable
internal fun PeekDetail(event: PeekEvent, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("EEEE, h:mm:ss a", Locale.getDefault())
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val path = event.snapshotPath
        if (path != null && File(path).exists()) {
            Box {
                AsyncImage(
                    model = File(path),
                    contentDescription = "Peeker",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)),
                )
                Tag("Caught", Ink.Alert, Ink.OnAlert, Modifier.align(Alignment.TopStart).padding(12.dp))
            }
        } else {
            PeekThumb(event, size = 112.dp, corner = 28.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(peekTitle(event), style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
        Spacer(Modifier.height(4.dp))
        Text(fmt.format(Date(event.startedAt)), style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("Duration", Reports.formatDuration(event.durationMs), Modifier.weight(1f))
            MiniStat("People", "${event.maxPeepers}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Delete this peek", color = Ink.Alert, icon = Icons.Rounded.DeleteOutline, onClick = onDelete)
    }
}
