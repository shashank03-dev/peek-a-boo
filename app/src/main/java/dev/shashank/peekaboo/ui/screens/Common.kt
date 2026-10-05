package dev.shashank.peekaboo.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.components.SwipeToDelete
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.MonoValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val timeFmt = SimpleDateFormat("h:mm", Locale.getDefault())
val ampmFmt = SimpleDateFormat("a", Locale.getDefault())
private val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())

fun relative(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

fun peekTitle(event: PeekEvent) = if (event.maxPeepers == 1) "Someone peeked" else "${event.maxPeepers} people peeked"

/** Snapshot of the peeker, or a neutral tile with a red eye when there is no photo. */
@Composable
fun PeekThumb(event: PeekEvent, size: Dp = 48.dp, corner: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Ink.Raised),
        contentAlignment = Alignment.Center,
    ) {
        val path = event.snapshotPath
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = "Peeker snapshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Rounded.RemoveRedEye, null, tint = Ink.Alert, modifier = Modifier.size(size * 0.42f))
        }
    }
}

@Composable
private fun PeoplePill(count: Int) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Ink.AlertSoft)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Person, null, tint = Ink.Alert, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("$count", style = MonoValue, color = Ink.Alert)
    }
}

/** Compact row used on the Watch tab. */
@Composable
fun PeekRow(event: PeekEvent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .panel(RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeekThumb(event)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(peekTitle(event), style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Spacer(Modifier.height(4.dp))
            Text(
                "${relative(event.startedAt)} · ${Reports.formatDuration(event.durationMs)}",
                style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
            )
        }
        PeoplePill(event.maxPeepers)
    }
}

/** One stop on the Activity timeline: time on the left, a node on the rail, the peek on the right. */
@Composable
fun TimelineItem(
    event: PeekEvent,
    first: Boolean,
    last: Boolean,
    showDay: Boolean,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(52.dp).padding(top = 16.dp), horizontalAlignment = Alignment.End) {
            if (showDay) Text(dayFmt.format(Date(event.startedAt)).uppercase(), style = Eyebrow, color = Ink.TextMuted)
            Text(timeFmt.format(Date(event.startedAt)), style = MonoValue, color = Ink.Text)
            Text(ampmFmt.format(Date(event.startedAt)).uppercase(), style = Eyebrow, color = Ink.TextFaint)
        }
        Canvas(Modifier.width(32.dp).fillMaxHeight()) {
            val x = size.width / 2f
            val nodeY = 28.dp.toPx()
            if (!first) drawLine(Ink.LineStrong, Offset(x, 0f), Offset(x, nodeY), 1.dp.toPx())
            if (!last) drawLine(Ink.LineStrong, Offset(x, nodeY), Offset(x, size.height), 1.dp.toPx())
            drawCircle(Ink.Bg, 6.dp.toPx(), Offset(x, nodeY))
            drawCircle(Ink.Alert, 4.dp.toPx(), Offset(x, nodeY))
        }
        SwipeToDelete(onDelete = onDelete, modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .bouncyClick(onClick = onClick)
                .panel(RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeekThumb(event, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(peekTitle(event), style = MaterialTheme.typography.titleMedium, color = Ink.Text, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text("Watched for ${Reports.formatDuration(event.durationMs)}", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
            }
            PeoplePill(event.maxPeepers)
        }
        }
    }
}
