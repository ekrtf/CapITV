package com.ekrtf.capitv

/** Pure launch-resolution logic, separated from Android so it can be unit-tested. */
class LaunchResolver(
    private val config: AppConfig,
    private val isInstalled: (String) -> Boolean,
) {
    private val bindings = config.keyBindings()

    /** First installed candidate package for the tile, or null if none is installed. */
    fun resolve(tile: Tile): String? = config.candidatesFor(tile).firstOrNull(isInstalled)

    fun tile(id: String): Tile? = config.tiles.firstOrNull { it.id == id }

    /** Action bound to a raw Android key code, or null if the key is not a shortcut. */
    fun actionForKey(keyCode: Int): KeyAction? = bindings[keyCode]

    /** Package to suggest in the Play Store when nothing is installed (first non-sideload package). */
    fun storePackageFor(tile: Tile): String =
        tile.packages.firstOrNull { it !in config.smarttubePackages } ?: tile.packages.first()
}
