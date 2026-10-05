package dev.shashank.peekaboo.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.BuildConfig
import dev.shashank.peekaboo.billing.ProPlan
import dev.shashank.peekaboo.billing.StoreStatus
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.components.IconBadge
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.RoundIconButton
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.Tag
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.components.staggerIn
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.MonoValue

private data class Perk(val icon: ImageVector, val title: String, val body: String)

private val perks = listOf(
    Perk(Icons.Rounded.Gradient, "Privacy Shield", "The screen darkens and scrambles the instant someone looks over your shoulder. Up close you can still read it; from beside you they can't."),
    Perk(Icons.Rounded.AppShortcut, "Protected apps", "Keep the shield on just for banking, chats and photos, and nowhere else."),
    Perk(Icons.Rounded.PersonSearch, "Someone-else alert", "If someone other than you is using your unlocked phone, it's quietly logged with a photo."),
)

@Composable
fun ProScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val plans by vm.plans.collectAsStateWithLifecycle()
    val status by vm.storeStatus.collectAsStateWithLifecycle()
    val busy by vm.purchasing.collectAsStateWithLifecycle()
    var selected by remember(plans) { mutableStateOf(plans.firstOrNull { it.yearly } ?: plans.firstOrNull()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag("Pro", Ink.AccentSoft, Ink.Accent)
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.Close, "Close", vm::close)
        }
        Spacer(Modifier.height(16.dp))
        ShieldDemo(Modifier.fillMaxWidth().height(168.dp))
        Spacer(Modifier.height(24.dp))
        Text("YOUR SCREEN, YOUR EYES", style = Eyebrow, color = Ink.TextFaint)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isPro) "You're on Pro" else "Stop them reading, not just catch them",
            style = MaterialTheme.typography.displaySmall, color = Ink.Text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A privacy screen that switches itself on only when someone is actually looking. No screen protector, no new phone.",
            style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
        )

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().panel().padding(vertical = 4.dp)) {
            perks.forEachIndexed { i, p ->
                Row(Modifier.staggerIn(i).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    IconBadge(p.icon, Ink.Accent, background = Ink.AccentSoft)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                        Spacer(Modifier.height(2.dp))
                        Text(p.body, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        when {
            isPro -> {
                Text(
                    if (BuildConfig.PRO_UNLOCKED) "Every Pro feature is unlocked on this build." else "Thanks for supporting Peek-a-Boo. Every Pro feature is unlocked.",
                    style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
                )
                if (!BuildConfig.PRO_UNLOCKED) {
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton("Manage subscription") {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.manageSubscriptionUrl()))) }
                    }
                }
            }
            plans.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val monthly = plans.firstOrNull { !it.yearly }
                    plans.forEach { plan ->
                        PlanCard(plan, monthly, selected == plan) { selected = plan }
                    }
                }
                Spacer(Modifier.height(20.dp))
                val plan = selected
                PrimaryButton(
                    text = when {
                        busy -> "Opening Google Play…"
                        plan?.trialDays != null -> "Start ${plan.trialDays}-day free trial"
                        else -> "Continue"
                    },
                    enabled = plan != null && !busy,
                    spark = true,
                ) {
                    val activity = ctx.findActivity()
                    if (plan != null && activity != null) vm.buy(activity, plan)
                }
                Spacer(Modifier.height(12.dp))
                if (plan != null) {
                    val per = if (plan.yearly) "year" else "month"
                    Text(
                        (if (plan.trialDays != null) "Free for ${plan.trialDays} days, then ${plan.price}/$per. " else "${plan.price}/$per. ") +
                            "Renews automatically until you cancel. Cancel anytime in Google Play › Subscriptions.",
                        style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Restore purchase",
                    style = MaterialTheme.typography.labelMedium, color = Ink.TextMuted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape).bouncyClick(onClick = vm::restorePurchases).padding(12.dp),
                )
            }
            status == StoreStatus.Connecting -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            else -> Column(Modifier.fillMaxWidth().panel().padding(16.dp)) {
                Text("Pro isn't available on this install", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Subscriptions go through Google Play. Install Peek-a-Boo from the Play Store, signed in to your Google account, to upgrade.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: ProPlan, monthly: ProPlan?, selected: Boolean, onClick: () -> Unit) {
    val saving = if (plan.yearly && monthly != null && monthly.priceMicros > 0) {
        (100 - plan.priceMicros * 100 / (monthly.priceMicros * 12)).toInt().takeIf { it in 5..90 }
    } else null
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .panel(
                RoundedCornerShape(18.dp),
                color = if (selected) Ink.Raised else Ink.Surface,
                border = if (selected) Ink.Accent else Ink.Line,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Ink.Accent else Ink.LineStrong, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) Ink.Accent else Color.Transparent)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (plan.yearly) "Yearly" else "Monthly", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                if (saving != null) {
                    Spacer(Modifier.width(8.dp))
                    Tag("Save $saving%", Ink.AccentSoft, Ink.Accent)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (plan.trialDays != null) "${plan.trialDays} days free, then billed ${if (plan.yearly) "yearly" else "monthly"}"
                else "Billed ${if (plan.yearly) "yearly" else "monthly"}",
                style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(plan.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink.Text)
            Text(if (plan.yearly) "per year" else "per month", style = MonoValue, color = Ink.TextFaint)
        }
    }
}

/**
 * The pitch in one picture: the left half of a chat screen reads normally (what you see), the
 * right half sits under the louver shield (what the person beside you sees).
 */
@Composable
private fun ShieldDemo(modifier: Modifier) {
    Canvas(modifier.panel(RoundedCornerShape(24.dp)).padding(18.dp)) {
        val r = CornerRadius(6.dp.toPx())
        val lineH = 10.dp.toPx()
        val gap = 12.dp.toPx()
        var y = 4.dp.toPx()
        val widths = listOf(0.62f, 0.8f, 0.45f, 0.7f, 0.55f, 0.75f, 0.4f)
        widths.forEachIndexed { i, w ->
            val mine = i % 3 == 1
            val bw = size.width * w
            val x = if (mine) size.width - bw else 0f
            drawRoundRect(if (mine) Ink.Accent.copy(alpha = 0.8f) else Ink.TextMuted.copy(alpha = 0.55f), Offset(x, y), Size(bw, lineH), r)
            y += lineH + gap
            if (y > size.height) return@forEachIndexed
        }
        // Shield over the right half.
        val half = size.width / 2f
        val slat = 2.dp.toPx()
        val clear = 1.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.55f), Offset(half, -18.dp.toPx()), Size(half + 18.dp.toPx(), size.height + 36.dp.toPx()))
        var sy = -18.dp.toPx()
        while (sy < size.height + 18.dp.toPx()) {
            drawRect(Color.Black.copy(alpha = 0.78f), Offset(half, sy), Size(half + 18.dp.toPx(), slat))
            sy += slat + clear
        }
        drawLine(Ink.Accent, Offset(half, -18.dp.toPx()), Offset(half, size.height + 18.dp.toPx()), 1.5.dp.toPx())
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ProBadgeIcon(modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.WorkspacePremium, "Pro", tint = Ink.Accent, modifier = modifier.size(16.dp))
}
