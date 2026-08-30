package com.project.lol.webview.helpers

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import java.util.Locale

object AccentTheme {

    const val DEFAULT_HEX = "#1DB954"

    /** How far the hover tint is pulled toward white (#1ED760 vs #1DB954). */
    private const val BRIGHTEN_FACTOR = 0.12f

    private const val PREFS_NAME = "spotilol_prefs"
    private const val KEY_MATERIAL_YOU = "MaterialYou"
    private const val KEY_PALETTE_SEED = "PaletteSeed"

    /** One accent in every form the app needs — parsed & formatted exactly once. */
    data class Accent(
        val hex: String,
        val brightHex: String,
        val rgb: String,
        val color: Color
    ) {
        companion object {
            fun fromArgb(argb: Int): Accent {
                val c = argb or 0xFF000000.toInt()   // accent is always opaque
                fun lift(v: Int) = v + ((255 - v) * BRIGHTEN_FACTOR).toInt()
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                return Accent(
                    hex = String.format(Locale.US, "#%06X", 0xFFFFFF and c),
                    brightHex = String.format(Locale.US, "#%02X%02X%02X", lift(r), lift(g), lift(b)),
                    rgb = "$r,$g,$b",
                    color = Color(c)
                )
            }
        }
    }

    // Memoization — one volatile reference makes the swap atomic.
    @Volatile private var cache: Pair<String, Accent>? = null

    // SharedPreferences instances are app-lifetime singletons; hold one reference.
    @Volatile private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences =
        cachedPrefs ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { cachedPrefs = it }

    /**
     * Resolves the accent (Material You > seed hex > default) and returns all
     * derived forms in one shot. Memoized until the underlying prefs change.
     */
    fun resolve(context: Context): Accent {
        val p = prefs(context)
        val materialYou = p.getBoolean(KEY_MATERIAL_YOU, false)
        val seed = p.getString(KEY_PALETTE_SEED, null)?.trim()
        val key = "$materialYou|$seed"

        cache?.let { (k, accent) -> if (k == key) return accent }

        val argb = when {
            materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                dynamicDarkColorScheme(context).primary.toArgb()
            seed != null && seed.startsWith("#") ->
                seed.toColorIntOrNull() ?: DEFAULT_HEX.toColorInt()
            else -> DEFAULT_HEX.toColorInt()
        }

        return Accent.fromArgb(argb).also { cache = key to it }
    }

    /** Drop the memoized accent (e.g. after a wallpaper swap on Material You). */
    @Suppress("unused")
    fun invalidate() { cache = null }

    fun resolveHex(context: Context): String = resolve(context).hex

    /** Compose-friendly variant for native chrome (loading bar, etc.). */
    fun resolveColor(context: Context): Color = resolve(context).color

    /**
     * Publishes the accent trio on :root. Inline style on <html> beats any
     * stylesheet, so the player recolors instantly - no reload, no flash.
     */
    fun buildAccentJs(context: Context): String = buildAccentJs(resolve(context))

    fun buildAccentJs(accent: Accent): String = """
        (function(){
            var r=document.documentElement;
            r.style.setProperty('--spl-accent','${accent.hex}');
            r.style.setProperty('--spl-accent-bright','${accent.brightHex}');
            r.style.setProperty('--spl-accent-rgb','${accent.rgb}');
        })();
    """.trimIndent()

    private fun String.toColorIntOrNull(): Int? = try {
        toColorInt()
    } catch (_: IllegalArgumentException) {
        null
    }
}