package com.ekrtf.capitv

/**
 * Remote key names used in capitv.json, mapped to android.view.KeyEvent codes.
 * Kept as plain ints so the mapping is unit-testable on the JVM.
 */
object KeyNames {
    private val codes: Map<String, Int> = buildMap {
        // KEYCODE_0..KEYCODE_9 = 7..16
        for (d in 0..9) put("$d", 7 + d)
        // KEYCODE_NUMPAD_0..9 = 144..153
        for (d in 0..9) put("NUMPAD_$d", 144 + d)
        // Colour keys found on most TV remotes (and passed through HDMI-CEC by many TVs).
        put("PROG_RED", 183)
        put("PROG_GREEN", 184)
        put("PROG_YELLOW", 185)
        put("PROG_BLUE", 186)
        // KEYCODE_BUTTON_1..16 = 188..203. Several Google TV remotes send these for
        // app buttons (e.g. YouTube / Netflix on the Google TV Streamer remote).
        for (n in 1..16) put("BUTTON_$n", 187 + n)
        // KEYCODE_F1..F12 = 131..142
        for (n in 1..12) put("F$n", 130 + n)
        // KEYCODE_MACRO_1..4 = 313..316 (Android 14+; the Streamer's customizable star button).
        for (n in 1..4) put("MACRO_$n", 312 + n)
        put("SETTINGS", 176)
        put("GUIDE", 172)
        put("TV", 170)
        put("ALL_APPS", 284)
    }

    fun codeOf(name: String): Int? = codes[name.trim().uppercase()]

    val all: Map<String, Int> get() = codes
}
