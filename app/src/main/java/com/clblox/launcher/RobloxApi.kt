package com.clblox.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class GameInfo(
    val name: String,
    val playing: Long,
    val visits: Long,
    val iconUrl: String?
)

/** 1200 -> 1.2K, 3400000 -> 3.4M */
fun compact(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}

/**
 * Reads PUBLIC, read-only Roblox web endpoints (no login needed):
 * place -> universe id, game details, and the game icon.
 */
object GameInfoRepo {
    private const val TTL_MS = 60_000L
    private val cache = ConcurrentHashMap<Long, Pair<GameInfo, Long>>()

    suspend fun get(placeId: Long): GameInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cache[placeId]?.let { (info, at) ->
            if (now - at < TTL_MS) return@withContext info
        }
        val info = load(placeId)
        if (info != null) cache[placeId] = info to now
        info ?: cache[placeId]?.first
    }

    private fun load(placeId: Long): GameInfo? = try {
        val universeJson = httpGet("https://apis.roblox.com/universes/v1/places/$placeId/universe")
        val universeId = universeJson?.let { JSONObject(it).optLong("universeId", 0L) } ?: 0L
        if (universeId == 0L) {
            null
        } else {
            val gameJson = httpGet("https://games.roblox.com/v1/games?universeIds=$universeId")
            val data = gameJson?.let { JSONObject(it).optJSONArray("data")?.optJSONObject(0) }
            val iconJson = httpGet(
                "https://thumbnails.roblox.com/v1/games/icons?universeIds=$universeId" +
                    "&returnPolicy=PlaceHolder&size=256x256&format=Png&isCircular=false"
            )
            val icon = iconJson
                ?.let { JSONObject(it).optJSONArray("data")?.optJSONObject(0)?.optString("imageUrl") }
                ?.takeIf { it.isNotBlank() }
            if (data == null) null
            else GameInfo(
                name = data.optString("name"),
                playing = data.optLong("playing"),
                visits = data.optLong("visits"),
                iconUrl = icon
            )
        }
    } catch (_: Exception) {
        null
    }

    private fun httpGet(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 8_000
        c.setRequestProperty("Accept", "application/json")
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: Exception) {
        null
    }
}
