package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.BroadcastEpisode
import com.example.data.model.RadioJsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

data class EpisodeEntity(
    val id: String,
    val channelId: String,
    val channelNameAr: String,
    val channelFrequency: String,
    val programTitleAr: String,
    val broadcastStyleId: String,
    val generatedAtMillis: Long,
    val payloadJson: String
)

interface EpisodeDao {
    fun observeRecentEpisodes(): Flow<List<EpisodeEntity>>
    suspend fun insertEpisode(entity: EpisodeEntity)
    suspend fun deleteEpisode(episodeId: String)
    suspend fun pruneOldEpisodes()
}

private const val DB_NAME = "atheer_radio_cache.db"
private const val DB_VERSION = 1
private const val TABLE_EPISODES = "broadcast_episodes"

class RadioDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val daoInstance: EpisodeDao = SqliteEpisodeDao(this)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$TABLE_EPISODES` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `channelId` TEXT NOT NULL,
                `channelNameAr` TEXT NOT NULL,
                `channelFrequency` TEXT NOT NULL,
                `programTitleAr` TEXT NOT NULL,
                `broadcastStyleId` TEXT NOT NULL,
                `generatedAtMillis` INTEGER NOT NULL,
                `payloadJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun episodeDao(): EpisodeDao = daoInstance

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        fun getInstance(context: Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RadioDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private class SqliteEpisodeDao(
    private val dbHelper: RadioDatabase
) : EpisodeDao {

    private val episodesState = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    override fun observeRecentEpisodes(): Flow<List<EpisodeEntity>> =
        episodesState.onStart {
            val loaded = withContext(Dispatchers.IO) { queryRecentEpisodesLocked() }
            episodesState.value = loaded
        }

    override suspend fun insertEpisode(entity: EpisodeEntity) {
        withContext(Dispatchers.IO) {
            synchronized(dbHelper) {
                val values = ContentValues().apply {
                    put("id", entity.id)
                    put("channelId", entity.channelId)
                    put("channelNameAr", entity.channelNameAr)
                    put("channelFrequency", entity.channelFrequency)
                    put("programTitleAr", entity.programTitleAr)
                    put("broadcastStyleId", entity.broadcastStyleId)
                    put("generatedAtMillis", entity.generatedAtMillis)
                    put("payloadJson", entity.payloadJson)
                }
                dbHelper.writableDatabase.insertWithOnConflict(
                    TABLE_EPISODES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                episodesState.value = queryRecentEpisodesLocked()
            }
        }
    }

    override suspend fun deleteEpisode(episodeId: String) {
        withContext(Dispatchers.IO) {
            synchronized(dbHelper) {
                dbHelper.writableDatabase.delete(
                    TABLE_EPISODES,
                    "id = ?",
                    arrayOf(episodeId)
                )
                episodesState.value = queryRecentEpisodesLocked()
            }
        }
    }

    override suspend fun pruneOldEpisodes() {
        withContext(Dispatchers.IO) {
            synchronized(dbHelper) {
                dbHelper.writableDatabase.execSQL(
                    """
                    DELETE FROM $TABLE_EPISODES
                    WHERE id NOT IN (
                        SELECT id FROM $TABLE_EPISODES
                        ORDER BY generatedAtMillis DESC
                        LIMIT 25
                    )
                    """.trimIndent()
                )
                episodesState.value = queryRecentEpisodesLocked()
            }
        }
    }

    private fun queryRecentEpisodesLocked(): List<EpisodeEntity> {
        val result = mutableListOf<EpisodeEntity>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, channelId, channelNameAr, channelFrequency, programTitleAr, broadcastStyleId, generatedAtMillis, payloadJson FROM $TABLE_EPISODES ORDER BY generatedAtMillis DESC LIMIT 25",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    EpisodeEntity(
                        id = cursor.getString(0),
                        channelId = cursor.getString(1),
                        channelNameAr = cursor.getString(2),
                        channelFrequency = cursor.getString(3),
                        programTitleAr = cursor.getString(4),
                        broadcastStyleId = cursor.getString(5),
                        generatedAtMillis = cursor.getLong(6),
                        payloadJson = cursor.getString(7)
                    )
                )
            }
        }
        return result
    }
}

class EpisodeArchiveRepository(private val dao: EpisodeDao) {
    fun observeEpisodes(): Flow<List<BroadcastEpisode>> =
        dao.observeRecentEpisodes().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    RadioJsonSerializer.deserializeEpisodePayload(
                        id = entity.id,
                        channelId = entity.channelId,
                        channelNameAr = entity.channelNameAr,
                        channelFrequency = entity.channelFrequency,
                        programTitleAr = entity.programTitleAr,
                        broadcastStyleId = entity.broadcastStyleId,
                        generatedAtMillis = entity.generatedAtMillis,
                        payloadJson = entity.payloadJson
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }

    suspend fun saveEpisode(episode: BroadcastEpisode) {
        val entity = EpisodeEntity(
            id = episode.id,
            channelId = episode.channelId,
            channelNameAr = episode.channelNameAr,
            channelFrequency = episode.channelFrequency,
            programTitleAr = episode.programTitleAr,
            broadcastStyleId = episode.broadcastStyle.id,
            generatedAtMillis = episode.generatedAtMillis,
            payloadJson = RadioJsonSerializer.serializeEpisodePayload(episode)
        )
        dao.insertEpisode(entity)
        dao.pruneOldEpisodes()
    }

    suspend fun deleteEpisode(episodeId: String) {
        dao.deleteEpisode(episodeId)
    }
}
