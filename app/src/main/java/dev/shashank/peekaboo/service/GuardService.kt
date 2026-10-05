package dev.shashank.peekaboo.service

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.shashank.peekaboo.MainActivity
import dev.shashank.peekaboo.PeekApp
import dev.shashank.peekaboo.R
import dev.shashank.peekaboo.app
import dev.shashank.peekaboo.data.GuardSettings
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.PeekFace
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.detect.FaceSignature
import dev.shashank.peekaboo.detect.FrameResult
import dev.shashank.peekaboo.detect.PeekAnalyzer
import dev.shashank.peekaboo.detect.PeekSessionTracker
import dev.shashank.peekaboo.detect.toUprightBitmap
import dev.shashank.peekaboo.detect.cameraProvider
import dev.shashank.peekaboo.overlay.NotchOverlay
import dev.shashank.peekaboo.overlay.NotchPill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service that keeps the front camera watching while the phone is unlocked and in use,
 * shows the notch overlay when someone else is looking, and records each peek.
 */
class GuardService : LifecycleService() {

    private val analyzer by lazy { PeekAnalyzer() }
    private val tracker = PeekSessionTracker(dwellMs = 800)
    private val overlay by lazy { NotchOverlay(this) }
    private lateinit var executor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    private val screenUsable = MutableStateFlow(false)
    @Volatile private var settings = GuardSettings()
    @Volatile private var lastProcessed = 0L
    @Volatile private var lastFaceAt = 0L
    @Volatile private var currentEventId: Long? = null
    private var todayCount = 0

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshScreenState()
    }

    override fun onCreate() {
        super.onCreate()
        executor = Executors.newSingleThreadExecutor()
        if (!startInForeground()) {
            // Android refused a camera foreground service (e.g. sticky restart from the background).
            // The app restarts the guard the next time it's opened.
            stopSelf()
            return
        }
        GuardState.running.value = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshScreenState()

        lifecycleScope.launch {
            app.settings.settings.collect {
                settings = it
                tracker.dwellMs = it.sensitivity.dwellMs
            }
        }
        lifecycleScope.launch {
            combine(GuardState.peekActive, GuardState.peepersNow, app.settings.settings) { a, n, _ -> a to n }
                .distinctUntilChanged()
                .collect { updateOverlay() }
        }
        lifecycleScope.launch {
            combine(screenUsable, GuardState.uiCameraLeases) { usable, leases -> usable && leases == 0 }
                .distinctUntilChanged()
                .collect { shouldScan -> if (shouldScan) bindCamera() else unbindCamera() }
        }
        lifecycleScope.launch {
            // Re-query whenever scanning flips (e.g. each unlock) so the count rolls over at midnight.
            @OptIn(ExperimentalCoroutinesApi::class)
            GuardState.scanning
                .flatMapLatest { app.db.dao().countSince(Reports.startOfDay()) }
                .collect { n ->
                    todayCount = n
                    refreshNotification()
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            lifecycleScope.launch {
                app.settings.setGuardEnabled(false)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (!GuardState.running.value) {
            if (::executor.isInitialized) executor.shutdown()
            super.onDestroy()
            return
        }
        endOpenSession()
        unbindCamera()
        overlay.hide()
        runCatching { unregisterReceiver(screenReceiver) }
        executor.shutdown()
        analyzer.close()
        GuardState.running.value = false
        GuardState.scanning.value = false
        GuardState.peekActive.value = false
        GuardState.peepersNow.value = 0
        GuardState.facesInView.value = 0
        super.onDestroy()
    }

    // region screen state
    private fun refreshScreenState() {
        val pm = getSystemService(PowerManager::class.java)
        val km = getSystemService(KeyguardManager::class.java)
        val usable = pm.isInteractive && !km.isKeyguardLocked
        screenUsable.value = usable
        if (!usable) endOpenSession()

    }
    // endregion

    // region camera
    private suspend fun bindCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        val provider = cameraProvider ?: runCatching { cameraProvider() }
            .getOrNull()?.also { cameraProvider = it } ?: return
        val ia = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(android.util.Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    ).build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        ia.setAnalyzer(executor, ::onFrame)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, ia)
            analysis = ia
            GuardState.scanning.value = true
        } catch (t: Throwable) {
            Log.w(TAG, "Camera bind failed", t)
            GuardState.scanning.value = false
        }
    }

    private fun unbindCamera() {
        analysis?.clearAnalyzer()
        analysis = null
        runCatching { cameraProvider?.unbindAll() }
        GuardState.scanning.value = false
        GuardState.facesInView.value = 0
        GuardState.ownerInView.value = false
        GuardState.peepersNow.value = 0
    }

    private fun onFrame(image: ImageProxy) {
        image.use { proxy ->
            val now = System.currentTimeMillis()
            // ~5 fps while faces are around, ~2 fps when the room is empty: plenty for people and easy on battery.
            val interval = if (now - lastFaceAt < 10_000) 200 else 500
            if (now - lastProcessed < interval) return
            lastProcessed = now

            val upright = proxy.toUprightBitmap()
            val s = settings
            val result = runCatching {
                analyzer.analyze(upright, app.owner.profile.value, s.sensitivity, s.strictMode)
            }.getOrElse {
                Log.w(TAG, "analysis failed", it)
                return
            }
            if (result.faces.isNotEmpty()) lastFaceAt = now
            GuardState.facesInView.value = result.faces.size
            GuardState.ownerInView.value = result.ownerInView
            handle(tracker.onFrame(now, result.peepers.size), result)
        }
    }

    private fun handle(change: PeekSessionTracker.Change?, frame: FrameResult) {
        when (change) {
            is PeekSessionTracker.Change.Started -> {
                GuardState.peekActive.value = true
                GuardState.peepersNow.value = change.peepers
                if (settings.haptics) buzz()
                val sigs = frame.peepers.map { FaceSignature.toBytes(it.signature) }
                val snapshot = if (settings.snapshots) frame.peeperSnapshot() else null
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = app.db.dao()
                    val id = dao.insert(PeekEvent(startedAt = change.at, endedAt = change.at, maxPeepers = change.peepers))
                    currentEventId = id
                    dao.insertFaces(sigs.map { PeekFace(eventId = id, signature = it) })
                    snapshot?.let { bmp ->
                        val dir = File(filesDir, "snapshots").apply { mkdirs() }
                        val f = File(dir, "peek_$id.jpg")
                        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                        bmp.recycle()
                        dao.setSnapshot(id, f.absolutePath)
                    }
                }
            }
            is PeekSessionTracker.Change.Updated -> GuardState.peepersNow.value = change.peepers
            is PeekSessionTracker.Change.Ended -> finishSession(change)
            null -> Unit
        }
    }

    private fun endOpenSession() {
        tracker.flush()?.let(::finishSession)
    }

    private fun finishSession(ended: PeekSessionTracker.Change.Ended) {
        GuardState.peekActive.value = false
        GuardState.peepersNow.value = 0
        val id = currentEventId ?: return
        currentEventId = null
        lifecycleScope.launch(Dispatchers.IO) {
            app.db.dao().finish(id, ended.endedAt, ended.maxPeepers)
        }
    }

    private fun FrameResult.peeperSnapshot(): Bitmap? = runCatching {
        // Union of all peeper faces, padded so the snapshot shows who it was.
        val first = peepers.firstOrNull() ?: return null
        val r = android.graphics.Rect(first.box)
        peepers.forEach { r.union(it.box) }
        val pad = (maxOf(r.width(), r.height()) * 0.45f).toInt()
        r.inset(-pad, -pad)
        r.intersect(0, 0, bitmap.width, bitmap.height)
        val scale = (360f / maxOf(r.width(), r.height())).coerceAtMost(1f)
        // Mirror so the snapshot looks like a selfie, the way people expect to see themselves.
        val m = Matrix().apply { postScale(-scale, scale) }
        Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height(), m, true)
    }.getOrNull()

    // endregion

    // region overlay
    private val pillVisible = MutableStateFlow(false)
    private var hideJob: Job? = null

    /**
     * The overlay window is only attached while a peek is on screen: Android 12+ blocks touches
     * that pass through other apps' overlays, so a permanently attached window would eat taps.
     */
    private fun updateOverlay() {
        val wanted = settings.showNotch && GuardState.peekActive.value && GuardState.peepersNow.value > 0
        if (wanted && overlay.canShow()) {
            hideJob?.cancel()
            val attached = overlay.show {
                val peepers by GuardState.peepersNow.collectAsState()
                val visible by pillVisible.collectAsState()
                val density = resources.displayMetrics.density
                NotchPill(
                    peepers = peepers,
                    visible = visible,
                    topOffsetPx = overlay.cameraBottomPx() + (settings.notchOffsetDp * density).toInt(),
                )
            }
            if (attached) {
                // Start collapsed so the pill springs open once the window is on screen.
                pillVisible.value = false
                lifecycleScope.launch {
                    delay(60)
                    pillVisible.value = true
                }
            } else {
                pillVisible.value = true
            }
        } else if (pillVisible.value || hideJob == null) {
            pillVisible.value = false
            hideJob?.cancel()
            hideJob = lifecycleScope.launch {
                delay(450) // let the exit animation play
                overlay.hide()
            }
        }
    }

    private fun buzz() {
        val v = getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 70, 35), -1))
    }
    // endregion

    // region notification
    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, GuardService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val scanning = GuardState.scanning.value
        val title = if (scanning) "Guarding your screen" else "Guard ready · resumes when you unlock"
        val text = when (todayCount) {
            0 -> "No peekers today 🎉"
            1 -> "1 peek caught today"
            else -> "$todayCount peeks caught today"
        }
        return NotificationCompat.Builder(this, PeekApp.CHANNEL_GUARD)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, "Stop guard", stop)
            .build()
    }

    private fun startInForeground(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return false
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0
        return runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        }.onFailure { Log.w(TAG, "startForeground refused", it) }.isSuccess
    }

    private fun refreshNotification() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            getSystemService(android.app.NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }
    // endregion

    companion object {
        private const val TAG = "GuardService"
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "dev.shashank.peekaboo.STOP"

        /** Starts the guard. Must be called while the app is visible (camera is a while-in-use permission). */
        fun start(context: Context): Boolean = runCatching {
            ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java))
        }.isSuccess

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
