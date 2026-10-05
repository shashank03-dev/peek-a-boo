package dev.shashank.peekaboo.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.theme.Ios
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())

fun relative(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

@Composable
fun PeekThumb(event: PeekEvent, size: androidx.compose.ui.unit.Dp = 52.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Ios.Pink.copy(alpha = 0.35f), Ios.Orange.copy(alpha = 0.25f)))),
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
            Icon(Icons.Rounded.Visibility, null, tint = Ios.Pink, modifier = Modifier.size(size * 0.45f))
        }
    }
}

@Composable
fun PeekListItem(event: PeekEvent, showDay: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeekThumb(event)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            val who = if (event.maxPeepers == 1) "Someone peeked" else "${event.maxPeepers} people peeked"
            Text(who, style = MaterialTheme.typography.titleMedium, color = Ios.Label)
            Spacer(Modifier.height(2.dp))
            val day = if (showDay) "${dayFmt.format(Date(event.startedAt))} · " else ""
            Text(
                "$day${timeFmt.format(Date(event.startedAt))} · ${Reports.formatDuration(event.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = Ios.Secondary,
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Ios.Pink.copy(alpha = 0.15f))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Person, null, tint = Ios.Pink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text("${event.maxPeepers}", style = MaterialTheme.typography.labelMedium, color = Ios.Pink)
        }
    }
}
