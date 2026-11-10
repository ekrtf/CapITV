package com.ekrtf.capitv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppConfigTest {

    private val bundled: AppConfig by lazy {
        AppConfig.parse(File("src/main/assets/${MainActivity.CONFIG_ASSET}").readText())
    }

    private fun config(
        youtubeClient: String = "official",
        extraTile: String = "",
        settingsKeys: String = "[\"0\"]",
        schema: Int = 1,
    ) = """
        {
          "schemaVersion": $schema,
          "youtubeClient": "$youtubeClient",
          "smarttubePackages": ["org.smarttube.stable"],
          "tiles": [
            {"id": "netflix", "label": "Netflix", "accent": "#E50914", "packages": ["com.netflix.ninja"], "keys": ["1", "PROG_RED"]},
            {"id": "youtube", "label": "YouTube", "accent": "#FF0033", "packages": ["com.google.android.youtube.tv"], "keys": ["2"]}
            $extraTile
          ],
          "settingsKeys": $settingsKeys
        }
    """.trimIndent()

    @Test
    fun bundledConfigHasTheFourAppsInMuscleMemoryOrder() {
        assertEquals(listOf("netflix", "youtube", "prime", "canal"), bundled.tiles.map { it.id })
        assertEquals(YoutubeClient.SMARTTUBE, bundled.youtubeClient)
    }

    @Test
    fun bundledConfigUsesVerifiedPackageNames() {
        val byId = bundled.tiles.associateBy { it.id }
        assertEquals("com.netflix.ninja", byId.getValue("netflix").packages.first())
        assertEquals("com.google.android.youtube.tv", byId.getValue("youtube").packages.first())
        assertEquals("com.amazon.amazonvideo.livingroom", byId.getValue("prime").packages.first())
        assertEquals("com.canal.android.canal", byId.getValue("canal").packages.first())
        assertEquals("org.smarttube.stable", bundled.smarttubePackages.first())
    }

    @Test
    fun bundledKeysMapDigitsAndColoursToTilesInOrder() {
        val resolver = LaunchResolver(bundled) { false }
        val expected = listOf("netflix", "youtube", "prime", "canal")
        expected.forEachIndexed { i, id ->
            assertEquals(KeyAction.LaunchTile(id), resolver.actionForKey(KeyNames.codeOf("${i + 1}")!!))
        }
        assertEquals(KeyAction.LaunchTile("netflix"), resolver.actionForKey(183)) // PROG_RED
        assertEquals(KeyAction.LaunchTile("canal"), resolver.actionForKey(186)) // PROG_BLUE
        assertEquals(KeyAction.OpenSettings, resolver.actionForKey(7)) // KEYCODE_0
        assertNull(resolver.actionForKey(23)) // DPAD_CENTER is not a shortcut
    }

    @Test
    fun smarttubeIsPreferredWhenChosenAndOfficialIsTheFallback() {
        val cfg = AppConfig.parse(config(youtubeClient = "smarttube"))
        val yt = cfg.tiles.first { it.id == "youtube" }
        assertEquals(listOf("org.smarttube.stable", "com.google.android.youtube.tv"), cfg.candidatesFor(yt))

        val both = LaunchResolver(cfg) { true }
        assertEquals("org.smarttube.stable", both.resolve(yt))

        val onlyOfficial = LaunchResolver(cfg) { it == "com.google.android.youtube.tv" }
        assertEquals("com.google.android.youtube.tv", onlyOfficial.resolve(yt))
    }

    @Test
    fun officialClientIsPreferredWhenChosen() {
        val cfg = AppConfig.parse(config(youtubeClient = "official"))
        val yt = cfg.tiles.first { it.id == "youtube" }
        assertEquals("com.google.android.youtube.tv", LaunchResolver(cfg) { true }.resolve(yt))
        assertEquals("org.smarttube.stable", LaunchResolver(cfg) { it == "org.smarttube.stable" }.resolve(yt))
    }

    @Test
    fun youtubePreferenceDoesNotLeakIntoOtherTiles() {
        val cfg = AppConfig.parse(config(youtubeClient = "smarttube"))
        val netflix = cfg.tiles.first { it.id == "netflix" }
        assertEquals(listOf("com.netflix.ninja"), cfg.candidatesFor(netflix))
    }

    @Test
    fun resolveReturnsNullWhenNothingIsInstalledAndSuggestsTheStorePackage() {
        val cfg = AppConfig.parse(config(youtubeClient = "smarttube"))
        val resolver = LaunchResolver(cfg) { false }
        val yt = cfg.tiles.first { it.id == "youtube" }
        assertNull(resolver.resolve(yt))
        // Never send the user to the Play Store for SmartTube (it is not on the store).
        assertEquals("com.google.android.youtube.tv", resolver.storePackageFor(yt))
    }

    @Test
    fun keyNamesAreCaseInsensitiveAndCoverRemoteButtons() {
        assertEquals(8, KeyNames.codeOf("1"))
        assertEquals(145, KeyNames.codeOf("numpad_1"))
        assertEquals(190, KeyNames.codeOf("BUTTON_3"))
        assertEquals(313, KeyNames.codeOf("MACRO_1"))
        assertNull(KeyNames.codeOf("NOT_A_KEY"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateKeyBindingIsRejected() {
        AppConfig.parse(config(settingsKeys = "[\"1\"]"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownKeyNameIsRejected() {
        AppConfig.parse(config(settingsKeys = "[\"BANANA\"]"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateTileIdIsRejected() {
        AppConfig.parse(
            config(extraTile = """, {"id": "netflix", "label": "Again", "accent": "#000000", "packages": ["x"]}"""),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun badAccentIsRejected() {
        AppConfig.parse(config(extraTile = """, {"id": "x", "label": "X", "accent": "red", "packages": ["x"]}"""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedSchemaIsRejected() {
        AppConfig.parse(config(schema = 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedJsonIsRejected() {
        AppConfig.parse("{ not json")
    }

    @Test
    fun everyBundledKeyNameResolves() {
        val names = bundled.tiles.flatMap { it.keys } + bundled.settingsKeys
        assertTrue(names.all { KeyNames.codeOf(it) != null })
    }
}
