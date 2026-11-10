package com.ekrtf.capitv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Which YouTube app the YouTube tile prefers. The other one is always the fallback. */
@Serializable
enum class YoutubeClient {
    @SerialName("official") OFFICIAL,
    @SerialName("smarttube") SMARTTUBE,
}

@Serializable
data class Tile(
    val id: String,
    val label: String,
    /** #RRGGBB accent used for the tile's focus ring and stripe. */
    val accent: String,
    /** Packages to try, in order. The first installed one is launched. */
    val packages: List<String>,
    /** Remote keys (see [KeyNames]) that launch this tile from the home screen. */
    val keys: List<String> = emptyList(),
)

@Serializable
data class AppConfig(
    val schemaVersion: Int,
    val youtubeClient: YoutubeClient = YoutubeClient.OFFICIAL,
    val smarttubePackages: List<String> = emptyList(),
    val tiles: List<Tile>,
    val settingsKeys: List<String> = emptyList(),
) {
    companion object {
        const val SUPPORTED_SCHEMA = 1
        const val YOUTUBE_TILE_ID = "youtube"
        private val json = Json { ignoreUnknownKeys = false }

        /** Parses and validates a config. Throws [IllegalArgumentException] on any problem. */
        fun parse(text: String): AppConfig {
            val config = try {
                json.decodeFromString(serializer(), text)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw IllegalArgumentException("Invalid CapITV config: ${e.message}", e)
            }
            config.validate()
            return config
        }
    }

    fun validate() {
        require(schemaVersion == SUPPORTED_SCHEMA) { "Unsupported schemaVersion $schemaVersion" }
        require(tiles.isNotEmpty()) { "At least one tile is required" }
        require(tiles.size <= 8) { "At most 8 tiles are supported" }
        require(tiles.map { it.id }.toSet().size == tiles.size) { "Tile ids must be unique" }
        tiles.forEach { tile ->
            require(tile.id.isNotBlank() && tile.label.isNotBlank()) { "Tile id and label must not be blank" }
            require(tile.packages.isNotEmpty()) { "Tile '${tile.id}' has no packages" }
            require(ACCENT.matches(tile.accent)) { "Tile '${tile.id}' accent must be #RRGGBB" }
        }
        // Throws on unknown names and on keys bound twice.
        keyBindings()
    }

    /** Ordered launch candidates for a tile, with the YouTube client preference applied. */
    fun candidatesFor(tile: Tile): List<String> {
        if (tile.id != YOUTUBE_TILE_ID) return tile.packages.distinct()
        return when (youtubeClient) {
            YoutubeClient.SMARTTUBE -> smarttubePackages + tile.packages
            YoutubeClient.OFFICIAL -> tile.packages + smarttubePackages
        }.distinct()
    }

    /** Key code to action. Throws if a key name is unknown or bound twice. */
    fun keyBindings(): Map<Int, KeyAction> {
        val map = LinkedHashMap<Int, KeyAction>()
        fun bind(name: String, action: KeyAction) {
            val code = KeyNames.codeOf(name)
                ?: throw IllegalArgumentException("Unknown key name '$name'")
            val previous = map.put(code, action)
            require(previous == null) { "Key '$name' is bound twice" }
        }
        tiles.forEach { tile -> tile.keys.forEach { bind(it, KeyAction.LaunchTile(tile.id)) } }
        settingsKeys.forEach { bind(it, KeyAction.OpenSettings) }
        return map
    }
}

sealed interface KeyAction {
    data class LaunchTile(val tileId: String) : KeyAction
    data object OpenSettings : KeyAction
}

private val ACCENT = Regex("^#[0-9A-Fa-f]{6}$")
