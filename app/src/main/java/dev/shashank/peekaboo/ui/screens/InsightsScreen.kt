package dev.shashank.peekaboo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.NightsStay
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.components.BarChart
import dev.shashank.peekaboo.ui.components.GhostButton
import dev.shashank.peekaboo.ui.components.GlassCard
import dev.shashank.peekaboo.ui.components.GlassIconButton
import dev.shashank.peekaboo.ui.components.GradientText
import dev.shashank.peekaboo.ui.components.IconBadge
import dev.shashank.peekaboo.ui.components.ScreenHeader
import dev.shashank.peekaboo.ui.components.SegmentedControl
import dev.shashank.peekaboo.ui.components.glass
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Mood
import dev.shashank.peekaboo.ui.theme.Night
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
                GlassIconButton(Icons.Rounded.IosShare, "Share report") { ctx.startActivity(vm.shareReport()) }
            }
        }
        item { SegmentedControl(listOf("Today", "This week"), range, { range = it }) }

        item {
            AnimatedContent(range, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) }, label = "hero") { r ->
                val big = if (r == 0) report.peeks else weekTotal
                val animated by animateIntAsState(big, tween(900), label = "big")
                GlassCard(Modifier.fillMaxWidth(), glow = Night.Hot, padding = PaddingValues(22.dp)) {
                    Text(if (r == 0) "PEEKS TODAY" else "PEEKS · LAST 7 DAYS", style = Eyebrow, color = Night.Hot)
                    Row(verticalAlignment = Alignment.Bottom) {
                        GradientText("$animated", MaterialTheme.typography.displayLarge, if (big > 0) Mood.Peek.brush else Brush.verticalGradient(listOf(Night.Text, Night.TextDim)))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (r == 0) "by ${report.people} ${if (report.people == 1) "person" else "people"}"
                            else "≈ ${"%.1f".format(weekTotal / 7f)} a day",
                            style = MaterialTheme.typography.titleMedium, color = Night.TextDim,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (r == 0) {
                        BarChart(report.hourly.toList(), listOf("12AM", "6AM", "12PM", "6PM"), report.peakHour, listOf(Night.Hot, Night.Ember))
                    } else {
                        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                        BarChart(
                            week.map { it.count },
                            week.map { dayFmt.format(Date(it.dayStart)).take(2).uppercase() },
                            6,
                            listOf(Night.Violet, Night.Indigo),
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Watched", Reports.formatDuration(report.totalPeekMs), Night.Teal, Modifier.weight(1f))
                MiniStat("Longest", Reports.formatDuration(report.longestPeekMs), Night.Violet, Modifier.weight(1f))
                MiniStat("Busiest", report.peakHour?.let { Reports.formatHour(it) } ?: "—", Night.Ember, Modifier.weight(1f))
            }
        }

        item { InsightCard(report.peeks, report.peakHour, report.people, week.maxOfOrNull { it.count } ?: 0) }

        item { SectionLabel(if (range == 0) "Timeline · today" else "Timeline · this week", Modifier.padding(top = 8.dp)) }
        if (list.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconBadge(Icons.Rounded.NightsStay, Night.Violet, size = 56.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Nothing to report", style = MaterialTheme.typography.titleLarge, color = Night.Text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Each peek lands here with the time, how long they looked and a snapshot.",
                            style = MaterialTheme.typography.bodyMedium, color = Night.TextDim, textAlign = TextAlign.Center,
                        )
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
            containerColor = Color(0xFF101018),
            scrimColor = Color.Black.copy(alpha = 0.6f),
            dragHandle = {
                Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(Night.StrokeBright))
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
internal fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.glass(RoundedCornerShape(22.dp)).padding(14.dp)) {
        Text(label.uppercase(), style = Eyebrow, color = color, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Night.Text, maxLines = 1)
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
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Night.Violet.copy(alpha = 0.18f), Night.Mint.copy(alpha = 0.06f))))
            .border(1.dp, Brush.linearGradient(listOf(Night.Violet.copy(alpha = 0.6f), Night.Mint.copy(alpha = 0.2f))), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Night.Violet, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("INSIGHT", style = Eyebrow, color = Night.Violet)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Night.Text)
            if (bestDay > 0) {
                Spacer(Modifier.height(6.dp))
                Text("BUSIEST DAY THIS WEEK · $bestDay PEEKS", style = Eyebrow, color = Night.TextDim)
            }
        }
    }
}

@Composable
internal fun PeekDetail(event: PeekEvent, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("EEEE · h:mm:ss a", Locale.getDefault())
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Night.Hot.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                )
                Text(
                    "CAUGHT",
                    style = Eyebrow,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Night.Hot)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        } else {
            PeekThumb(event, size = 120.dp, corner = 32.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text(peekTitle(event), style = MaterialTheme.typography.headlineMedium, color = Night.Text)
        Spacer(Modifier.height(4.dp))
        Text(fmt.format(Date(event.startedAt)).uppercase(), style = Eyebrow, color = Night.TextDim)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Duration", Reports.formatDuration(event.durationMs), Night.Teal, Modifier.weight(1f))
            MiniStat("People", "${event.maxPeepers}", Night.Hot, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        GhostButton("Delete this peek", color = Night.Hot, icon = Icons.Rounded.DeleteOutline, onClick = onDelete)
    }
}
