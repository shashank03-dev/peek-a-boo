package dev.shashank.peekaboo.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.shashank.peekaboo.app
import dev.shashank.peekaboo.data.DailyCount
import dev.shashank.peekaboo.data.DayReport
import dev.shashank.peekaboo.data.GuardSettings
import dev.shashank.peekaboo.data.OwnerProfile
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.data.Sensitivity
import dev.shashank.peekaboo.data.ShieldMode
import dev.shashank.peekaboo.data.ShieldStyle
import dev.shashank.peekaboo.billing.ProPlan
import dev.shashank.peekaboo.billing.ProStore
import dev.shashank.peekaboo.billing.StoreStatus
import android.app.Activity
import androidx.core.content.FileProvider
import dev.shashank.peekaboo.service.GuardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full-screen pages that slide up over the tabs. */
enum class Route { Pro, Apps }

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    private val dao = app.db.dao()

    val settings: StateFlow<GuardSettings?> =
        app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val owner: StateFlow<OwnerProfile?> = app.owner.profile

    val isPro: StateFlow<Boolean> = app.pro.isPro
    val plans: StateFlow<List<ProPlan>> = app.pro.plans
    val storeStatus: StateFlow<StoreStatus> = app.pro.status
    val purchasing: StateFlow<Boolean> = app.pro.busy

    val route = MutableStateFlow<Route?>(null)
    fun open(r: Route) { route.value = r }
    fun close() { route.value = null }

    /** Runs [block] when Pro is unlocked, otherwise shows the paywall. */
    fun withPro(block: () -> Unit) = if (isPro.value) block() else open(Route.Pro)

    fun buy(activity: Activity, plan: ProPlan) = app.pro.buy(activity, plan)
    fun restorePurchases() = app.pro.refresh()
    fun manageSubscriptionUrl(): String = ProStore.manageUrl(getApplication<Application>().packageName)

    /** Ticks every minute so "today" rolls over and relative times stay fresh. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    /** Peeks deleted in the UI but still undoable: hidden everywhere until the undo window closes. */
    private val hidden = MutableStateFlow<Set<Long>>(emptySet())
    private val _undo = MutableStateFlow<PeekEvent?>(null)
    val undo: StateFlow<PeekEvent?> = _undo.asStateFlow()
    private var commitJob: Job? = null

    val weekEvents: StateFlow<List<PeekEvent>> = clock
        .map { Reports.startOfDay(it, 6) }
        .flatMapLatest { dao.eventsSince(it) }
        .combine(hidden) { list, h -> if (h.isEmpty()) list else list.filter { it.id !in h } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEvents: StateFlow<List<PeekEvent>> = weekEvents
        .map { list -> val start = Reports.startOfDay(); list.filter { it.startedAt >= start } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val today: StateFlow<DayReport> = combine(todayEvents, owner) { events, o -> events to o }
        .mapLatest { (events, o) ->
            val faces = withContext(Dispatchers.IO) { dao.facesSince(Reports.startOfDay()) }
            Reports.day(events, faces, o?.threshold)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Reports.day(emptyList(), emptyList(), null))

    val week: StateFlow<List<DailyCount>> = weekEvents
        .map { Reports.week(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Reports.week(emptyList()))

    val weekTotal: StateFlow<Int> = weekEvents.map { list -> list.count { !it.isStranger } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val previewNotch = MutableStateFlow(false)

    fun setGuard(enabled: Boolean) = viewModelScope.launch {
        app.settings.setGuardEnabled(enabled)
        val ctx = getApplication<Application>()
        if (enabled) GuardService.start(ctx) else GuardService.stop(ctx)
    }

    fun finishOnboarding() = viewModelScope.launch { app.settings.setOnboardingDone(true) }
    fun setShowNotch(v: Boolean) = viewModelScope.launch { app.settings.setShowNotch(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { app.settings.setHaptics(v) }
    fun setSnapshots(v: Boolean) = viewModelScope.launch { app.settings.setSnapshots(v) }
    fun setStrict(v: Boolean) = viewModelScope.launch { app.settings.setStrictMode(v) }
    fun setStartOnBoot(v: Boolean) = viewModelScope.launch { app.settings.setStartOnBoot(v) }
    fun setSensitivity(v: Sensitivity) = viewModelScope.launch { app.settings.setSensitivity(v) }
    fun setNotchOffset(v: Int) = viewModelScope.launch { app.settings.setNotchOffset(v) }
    fun setShieldMode(v: ShieldMode) = viewModelScope.launch { app.settings.setShieldMode(v) }
    fun setShieldStyle(v: ShieldStyle) = viewModelScope.launch { app.settings.setShieldStyle(v) }
    fun setShieldStrength(v: Int) = viewModelScope.launch { app.settings.setShieldStrength(v) }
    fun setBlackout(v: Boolean) = viewModelScope.launch { app.settings.setBlackoutOnPeek(v) }
    fun setProtectedOnly(v: Boolean) = viewModelScope.launch { app.settings.setProtectedOnly(v) }
    fun setProtectedApp(pkg: String, on: Boolean) = viewModelScope.launch { app.settings.setProtectedApp(pkg, on) }
    fun setStrangerAlert(v: Boolean) = viewModelScope.launch { app.settings.setStrangerAlert(v) }

    fun saveOwner(profile: OwnerProfile) = viewModelScope.launch(Dispatchers.IO) { app.owner.save(profile) }
    fun clearOwner() = viewModelScope.launch(Dispatchers.IO) { app.owner.clear() }

    /** Hides the peek right away and offers an undo; it's only really deleted when the toast goes away. */
    fun deleteEvent(event: PeekEvent) {
        commitPending()
        hidden.update { it + event.id }
        _undo.value = event
        commitJob = viewModelScope.launch {
            delay(4500)
            commitPending()
        }
    }

    fun undoDelete() {
        commitJob?.cancel()
        val e = _undo.value ?: return
        _undo.value = null
        hidden.update { it - e.id }
    }

    fun dismissUndo() {
        commitJob?.cancel()
        commitPending()
    }

    private fun commitPending(scope: CoroutineScope = viewModelScope) {
        val e = _undo.value ?: return
        _undo.value = null
        scope.launch(Dispatchers.IO) {
            e.snapshotPath?.let { File(it).delete() }
            dao.delete(e.id)
            hidden.update { it - e.id }
        }
    }

    override fun onCleared() {
        // The undo window is still open: finish the delete outside the dying view model scope.
        commitPending(CoroutineScope(Dispatchers.IO))
        super.onCleared()
    }

    fun clearHistory() = viewModelScope.launch(Dispatchers.IO) {
        commitJob?.cancel()
        _undo.value = null
        hidden.value = emptySet()
        dao.allSnapshots().forEach { File(it).delete() }
        dao.clear()
    }

    /** Pro: the whole history as a spreadsheet-friendly CSV, handed to the share sheet. */
    suspend fun exportCsv(): Intent = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val csv = buildString {
            appendLine("type,started,ended,duration_seconds,people,has_photo")
            dao.all().forEach { e ->
                val type = if (e.isStranger) "someone_else_used_phone" else "peek"
                appendLine("$type,${iso.format(Date(e.startedAt))},${iso.format(Date(e.endedAt))},${e.durationMs / 1000},${e.maxPeepers},${e.snapshotPath != null}")
            }
        }
        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "peekaboo-history.csv").apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            "Export peek history",
        )
    }

    fun shareReport(): Intent {
        val r = today.value
        val fmt = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        val time = SimpleDateFormat("h:mm a", Locale.getDefault())
        val lines = buildString {
            appendLine("👀 Peek-a-Boo report — ${fmt.format(Date())}")
            appendLine()
            appendLine("• Peeks caught: ${r.peeks}")
            appendLine("• People who peeked: ${r.people}")
            appendLine("• Total time watched: ${Reports.formatDuration(r.totalPeekMs)}")
            appendLine("• Longest peek: ${Reports.formatDuration(r.longestPeekMs)}")
            r.peakHour?.let { appendLine("• Busiest hour: ${Reports.formatHour(it)}") }
            appendLine("• This week: ${weekTotal.value} peeks")
            if (todayEvents.value.isNotEmpty()) {
                appendLine()
                appendLine("Timeline")
                todayEvents.value.sortedBy { it.startedAt }.forEach {
                    val what = if (it.isStranger) "someone else used my phone" else
                        "${it.maxPeepers} ${if (it.maxPeepers == 1) "person" else "people"}, ${Reports.formatDuration(it.durationMs)}"
                    appendLine("  ${time.format(Date(it.startedAt))} — $what")
                }
            }
        }
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lines),
            "Share today's report",
        )
    }
}
