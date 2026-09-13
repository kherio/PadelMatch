package com.kherio.padelmatch.data

import android.content.Context
import androidx.room.Room

class TournamentRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "padelmatch.db"
    ).build()

    private val dao = db.tournamentDao()

    suspend fun getAll(): List<Tournament> = dao.getAll().map { TournamentMapper.toDomain(it) }

    suspend fun getById(id: String): Tournament? = dao.getById(id)?.let { TournamentMapper.toDomain(it) }

    suspend fun save(tournament: Tournament) = dao.upsert(TournamentMapper.toEntity(tournament))

    suspend fun delete(id: String) = dao.delete(id)
}
