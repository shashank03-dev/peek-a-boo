package dev.shashank.peekaboo.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.glass
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.MonoValue
import dev.shashank.peekaboo.ui.theme.Night
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

/** Snapshot of the peeker, or a hot gradient tile with an eye when there is no photo. */
@Composable
fun PeekThumb(event: PeekEvent, size: Dp = 52.dp, corner: Dp = 16.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(Night.Hot.copy(alpha = 0.45f), Night.Ember.copy(alpha = 0.25f))))
            .border(1.dp, Night.Hot.copy(alpha = 0.35f), RoundedCornerShape(corner)),
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
            Icon(Icons.Rounded.RemoveRedEye, null, tint = Night.Hot, modifier = Modifier.size(size * 0.42f))
        }
    }
}

@Composable
private fun PeoplePill(count: Int) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Night.Hot.copy(alpha = 0.14f))
            .border(1.dp, Night.Hot.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Person, null, tint = Night.Hot, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text("$count", style = MonoValue, color = Night.Hot)
    }
}

/** Compact glass row used on the Watch tab. */
@Composable
fun PeekRow(event: PeekEvent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .glass(RoundedCornerShape(22.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeekThumb(event)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(peekTitle(event), style = MaterialTheme.typography.titleMedium, color = Night.Text)
            Spacer(Modifier.height(3.dp))
            Text(
                "${relative(event.startedAt).uppercase()} · ${Reports.formatDuration(event.durationMs).uppercase()}",
                style = Eyebrow, color = Night.TextDim,
            )
        }
        PeoplePill(event.maxPeepers)
        Spacer(Modifier.width(6.dp))
    }
}

/** One stop on the Activity timeline: time on the left, a glowing node on the rail, the peek card on the right. */
@Composable
fun TimelineItem(event: PeekEvent, first: Boolean, last: Boolean, showDay: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(54.dp).padding(top = 18.dp), horizontalAlignment = Alignment.End) {
            if (showDay) Text(dayFmt.format(Date(event.startedAt)).uppercase(), style = Eyebrow, color = Night.Hot)
            Text(timeFmt.format(Date(event.startedAt)), style = MonoValue, color = Night.Text)
            Text(ampmFmt.format(Date(event.startedAt)).uppercase(), style = Eyebrow, color = Night.TextFaint)
        }
        Canvas(Modifier.width(34.dp).fillMaxHeight()) {
            val x = size.width / 2f
            val nodeY = 28.dp.toPx()
            val rail = Brush.verticalGradient(listOf(Night.Hot.copy(alpha = 0.5f), Night.Violet.copy(alpha = 0.25f)))
            if (!first) drawLine(rail, Offset(x, 0f), Offset(x, nodeY), 1.5.dp.toPx())
            if (!last) drawLine(rail, Offset(x, nodeY), Offset(x, size.height), 1.5.dp.toPx())
            drawCircle(Night.Hot.copy(alpha = 0.25f), 9.dp.toPx(), Offset(x, nodeY))
            drawCircle(Night.Hot, 4.dp.toPx(), Offset(x, nodeY))
            drawCircle(Color.White, 1.5.dp.toPx(), Offset(x, nodeY))
        }
        Row(
            Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .bouncyClick(onClick = onClick)
                .glass(RoundedCornerShape(22.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeekThumb(event, size = 46.dp, corner = 14.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(peekTitle(event), style = MaterialTheme.typography.titleMedium, color = Night.Text, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text("WATCHED ${Reports.formatDuration(event.durationMs).uppercase()}", style = Eyebrow, color = Night.TextDim)
            }
            PeoplePill(event.maxPeepers)
        }
    }
}
