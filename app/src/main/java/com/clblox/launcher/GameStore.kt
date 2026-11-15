package com.clblox.launcher

import android.content.Context

/** Stores the favorite games list in SharedPreferences (one "id|name" per line). */
class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences("clblox", Context.MODE_PRIVATE)

    fun load(): List<Game> =
        prefs.getString(KEY, "").orEmpty()
            .lines()
            .mapNotNull { line ->
                val parts = line.split("|", limit = 2)
                val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                Game(id, parts.getOrNull(1).orEmpty().ifBlank { "Game $id" })
            }

    fun save(games: List<Game>) {
        val text = games.joinToString("\n") { "${it.placeId}|${it.name.replace("\n", " ")}" }
        prefs.edit().putString(KEY, text).apply()
    }

    private companion object {
        const val KEY = "games"
    }
}
