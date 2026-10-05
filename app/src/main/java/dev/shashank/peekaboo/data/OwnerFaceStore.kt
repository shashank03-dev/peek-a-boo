package dev.shashank.peekaboo.data

import android.content.Context
import dev.shashank.peekaboo.detect.FaceSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * The owner's enrolled face: a small gallery of signatures plus a match threshold that was
 * calibrated from the spread between the owner's own samples at enrollment time.
 */
data class OwnerProfile(
    val samples: List<FloatArray>,
    val threshold: Float,
    val enrolledAt: Long,
)

class OwnerFaceStore(context: Context) {
    private val file = File(context.filesDir, "owner_face.bin")
    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<OwnerProfile?> = _profile.asStateFlow()

    fun save(profile: OwnerProfile) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        DataOutputStream(tmp.outputStream().buffered()).use { out ->
            out.writeInt(VERSION)
            out.writeLong(profile.enrolledAt)
            out.writeFloat(profile.threshold)
            out.writeInt(profile.samples.size)
            for (s in profile.samples) {
                out.writeInt(s.size)
                for (v in s) out.writeFloat(v)
            }
        }
        tmp.renameTo(file)
        _profile.value = profile
    }

    fun clear() {
        file.delete()
        _profile.value = null
    }

    private fun load(): OwnerProfile? = runCatching {
        if (!file.exists()) return null
        DataInputStream(file.inputStream().buffered()).use { inp ->
            if (inp.readInt() != VERSION) return null
            val at = inp.readLong()
            val threshold = inp.readFloat()
            val n = inp.readInt()
            val samples = List(n) {
                val len = inp.readInt()
                FloatArray(len) { inp.readFloat() }
            }
            if (samples.any { it.size != FaceSignature.LENGTH }) return null
            OwnerProfile(samples, threshold, at)
        }
    }.getOrNull()

    private companion object {
        const val VERSION = 1
    }
}
