package dev.shashank.peekaboo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.components.BarChart
import dev.shashank.peekaboo.ui.components.Card
import dev.shashank.peekaboo.ui.components.LargeTitle
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.SegmentedControl
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.theme.Ios
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
    var range by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<PeekEvent?>(null) }
    val weekEventsAll = remember(week) { week }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LargeTitle("Report", subtitle = "Who's been looking") {
                Box(
                    Modifier
                        .padding(bottom = 4.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ios.Card)
                        .bouncyClick { ctx.startActivity(vm.shareReport()) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.IosShare, "Share report", tint = Ios.Blue, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { SegmentedControl(listOf("Today", "This Week"), range, { range = it }) }

        item {
            AnimatedContent(range, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "hero") { r ->
                Card(Modifier.fillMaxWidth()) {
                    val big = if (r == 0) report.peeks else weekTotal
                    val animated by animateIntAsState(big, tween(700), label = "big")
                    Text(if (r == 0) "PEEKS TODAY" else "PEEKS THIS WEEK", style = MaterialTheme.typography.labelSmall, color = Ios.Pink)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$animated", style = MaterialTheme.typography.displayLarge, color = Ios.Label)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (r == 0) "by ${report.people} ${if (report.people == 1) "person" else "people"}"
                            else "avg ${"%.1f".format(weekTotal / 7f)} / day",
                            style = MaterialTheme.typography.titleMedium, color = Ios.Secondary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (r == 0) {
                        val labels = List(24) { h -> if (h % 6 == 0) Reports.formatHour(h).replace(" ", "").lowercase() else "" }
                        BarChart(
                            values = report.hourly.toList(),
                            labels = labels,
                            highlight = report.peakHour,
                            color = Ios.Pink,
                        )
                    } else {
                        val dayFmt = SimpleDateFormat("EEEEE", Locale.getDefault())
                        BarChart(
                            values = week.map { it.count },
                            labels = week.map { dayFmt.format(Date(it.dayStart)) },
                            highlight = 6,
                            color = Ios.Orange,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Watched for", Reports.formatDuration(report.totalPeekMs), Ios.Teal, Modifier.weight(1f))
                MiniStat("Longest", Reports.formatDuration(report.longestPeekMs), Ios.Indigo, Modifier.weight(1f))
                MiniStat("Busiest", report.peakHour?.let { Reports.formatHour(it) } ?: "—", Ios.Orange, Modifier.weight(1f))
            }
        }

        item { InsightCard(report.peeks, report.peakHour, report.people, weekEventsAll.maxByOrNull { it.count }?.count ?: 0) }

        item {
            Text(
                if (range == 0) "Today's timeline" else "This week",
                style = MaterialTheme.typography.titleLarge, color = Ios.Label,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
        val list = if (range == 0) todayEvents else null
        if (list != null && list.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("🕶️", style = MaterialTheme.typography.displayMedium)
                    Text("Nothing to report", style = MaterialTheme.typography.titleMedium, color = Ios.Label)
                    Text("Every peek will show up here with the time, how long they looked and a snapshot.", style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
                }
            }
        }
        if (list != null && list.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 6.dp)) {
                    list.forEach { e -> PeekListItem(e) { selected = e } }
                }
            }
        }
        if (range == 1) {
            item { WeekTimeline(vm) { selected = it } }
        }
    }

    selected?.let { event ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheet,
            containerColor = Ios.Card,
        ) {
            PeekDetail(event) {
                vm.deleteEvent(event)
                selected = null
            }
        }
    }
}

@Composable
private fun WeekTimeline(vm: MainViewModel, onSelect: (PeekEvent) -> Unit) {
    // Week events are derived from the same stream as today's; reuse the VM's flows.
    val events by produceWeekEvents(vm)
    if (events.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Text("A quiet week. No peeks recorded.", style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary)
        }
    } else {
        Card(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 6.dp)) {
            events.take(50).forEach { e -> PeekListItem(e, showDay = true) { onSelect(e) } }
        }
    }
}

@Composable
private fun produceWeekEvents(vm: MainViewModel): State<List<PeekEvent>> =
    vm.weekEvents.collectAsStateWithLifecycle()

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier, padding = PaddingValues(14.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ios.Label, maxLines = 1)
    }
}

@Composable
private fun InsightCard(peeks: Int, peakHour: Int?, people: Int, bestDay: Int) {
    val text = when {
        peeks == 0 -> "Your screen stayed private today. Keep the guard on in crowded places like metros and cafés."
        peakHour != null && peeks >= 3 -> "Most peeks happen around ${Reports.formatHour(peakHour)}. That's a good time to lower your brightness or use a privacy screen."
        people > 1 -> "$people different people looked at your screen today. Peek-a-Boo groups repeat peekers so each person is counted once."
        else -> "One curious onlooker today. A quick tilt of the screen is usually enough."
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ios.Yellow.copy(alpha = 0.10f))
            .padding(16.dp),
    ) {
        Icon(Icons.Rounded.Lightbulb, null, tint = Ios.Yellow, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Insight", style = MaterialTheme.typography.titleMedium, color = Ios.Yellow)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Ios.Label)
            if (bestDay > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Busiest day this week: $bestDay peeks", style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
            }
        }
    }
}

@Composable
private fun PeekDetail(event: PeekEvent, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("EEEE, h:mm:ss a", Locale.getDefault())
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val path = event.snapshotPath
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = "Peeker",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            PeekThumb(event, size = 120.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (event.maxPeepers == 1) "Someone peeked" else "${event.maxPeepers} people peeked",
            style = MaterialTheme.typography.headlineMedium, color = Ios.Label, fontWeight = FontWeight.Bold,
        )
        Text(fmt.format(Date(event.startedAt)), style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailPill("Duration", Reports.formatDuration(event.durationMs), Modifier.weight(1f))
            DetailPill("People", "${event.maxPeepers}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Delete this peek", color = Ios.Red, onClick = onDelete)
    }
}

@Composable
private fun DetailPill(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Ios.CardElevated).padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ios.Secondary)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ios.Label)
    }
}
