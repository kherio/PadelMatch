package com.kherio.padelmatch.data

import androidx.room.*
import com.google.gson.Gson

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val isFinished: Boolean,
    val json: String // Tournament completo serializado
)

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    suspend fun getAll(): List<TournamentEntity>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [TournamentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
}

/** Convierte entre Tournament (modelo de dominio) y TournamentEntity (fila de BD). */
object TournamentMapper {
    private val gson = Gson()

    fun toEntity(t: Tournament): TournamentEntity = TournamentEntity(
        id = t.id,
        name = t.name,
        createdAt = t.createdAt,
        isFinished = t.isFinished,
        json = gson.toJson(t)
    )

    fun toDomain(e: TournamentEntity): Tournament = gson.fromJson(e.json, Tournament::class.java)
}
