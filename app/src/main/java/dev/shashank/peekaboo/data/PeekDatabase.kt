package dev.shashank.peekaboo.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One "peek": a continuous stretch of time where at least one stranger was looking at the screen. */
@Entity(tableName = "peek_events", indices = [Index("startedAt")])
data class PeekEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val maxPeepers: Int,
    val snapshotPath: String? = null,
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}

/** Face signature of a peeper captured at the start of a peek, used to estimate unique people. */
@Entity(
    tableName = "peek_faces",
    foreignKeys = [ForeignKey(
        entity = PeekEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("eventId")],
)
data class PeekFace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val signature: ByteArray,
)

@Dao
interface PeekDao {
    @Insert
    suspend fun insert(event: PeekEvent): Long

    @Insert
    suspend fun insertFaces(faces: List<PeekFace>)

    @Query("UPDATE peek_events SET endedAt = :endedAt, maxPeepers = :maxPeepers WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, maxPeepers: Int)

    @Query("UPDATE peek_events SET snapshotPath = :path WHERE id = :id")
    suspend fun setSnapshot(id: Long, path: String)

    @Query("SELECT * FROM peek_events WHERE startedAt >= :from ORDER BY startedAt DESC")
    fun eventsSince(from: Long): Flow<List<PeekEvent>>

    @Query("SELECT * FROM peek_events ORDER BY startedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<PeekEvent>>

    @Query("SELECT f.* FROM peek_faces f JOIN peek_events e ON e.id = f.eventId WHERE e.startedAt >= :from")
    suspend fun facesSince(from: Long): List<PeekFace>

    @Query("SELECT COUNT(*) FROM peek_events WHERE startedAt >= :from")
    fun countSince(from: Long): Flow<Int>

    @Query("SELECT snapshotPath FROM peek_events WHERE snapshotPath IS NOT NULL")
    suspend fun allSnapshots(): List<String>

    @Query("DELETE FROM peek_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM peek_events")
    suspend fun clear()
}

@Database(entities = [PeekEvent::class, PeekFace::class], version = 1, exportSchema = false)
abstract class PeekDatabase : RoomDatabase() {
    abstract fun dao(): PeekDao

    companion object {
        fun create(context: Context): PeekDatabase =
            Room.databaseBuilder(context, PeekDatabase::class.java, "peekaboo.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
