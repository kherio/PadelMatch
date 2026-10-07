package com.kherio.padelmatch.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Lista de jugadores guardados (solo nombres), para no teclearlos en cada torneo.
 * Se guarda en SharedPreferences como un array JSON; así no hace falta tocar el
 * esquema de Room ni escribir migraciones para quien ya tiene la app instalada.
 */
class PlayerRosterRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("padelmatch_roster", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    fun getAll(): List<String> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            gson.fromJson<List<String>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Añade un nombre (sin duplicados, sin distinguir mayúsculas) y devuelve la lista actualizada. */
    fun add(name: String): List<String> {
        val clean = name.trim()
        val current = getAll()
        if (clean.isEmpty() || current.any { it.equals(clean, ignoreCase = true) }) return current
        return save((current + clean).sortedBy { it.lowercase() })
    }

    /** Quita un nombre de la lista guardada y devuelve la lista actualizada. */
    fun remove(name: String): List<String> =
        save(getAll().filterNot { it.equals(name, ignoreCase = true) })

    private fun save(list: List<String>): List<String> {
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
        return list
    }

    private companion object {
        const val KEY = "names"
    }
}
