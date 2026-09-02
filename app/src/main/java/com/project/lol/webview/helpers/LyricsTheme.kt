package com.project.lol.webview.helpers

import org.json.JSONObject

/**
 * Spotilol - Lyrics Style Engine.
 *
 * Injects a dedicated <style id="spotilol-lyrics-style"> element. It does
 * NOT touch 'spotilol-custom-css' (owned by the CustomCss setting), so the
 * two features can coexist without clobbering each other.
 *
 * Style "default" removes the element entirely - Spotify renders untouched.
 * Every other style shares the background-seam fix (the old one-screen blue
 * layer hidden, full-height lyric chain painted with
 * --lyrics-color-background) so the album color flows to the bottom in all
 * of them.
 */

object LyricsTheme {

    /** (prefValue, settingsLabel) - feeds SingleChoiceDialog directly. */
    val STYLE_OPTIONS: List<Pair<String, String>> = listOf(
        "default" to "Spotify Default",
        "fullscreen" to "Fullscreen (Album Colors)",
        "compact" to "Compact",
        "karaoke" to "Karaoke",
        "bold" to "Bold Large"
    )

    const val DEFAULT_STYLE = "fullscreen"
    private const val STYLE_ID = "spotilol-lyrics-style"

    fun buildLyricsStyleJs(style: String?): String {
        val css = cssFor(style)
        return if (css.isEmpty()) {
            """
            (function(){
                var st = document.getElementById('$STYLE_ID');
                if (st) st.remove();
            })();
            """.trimIndent()
        } else {
            val jsonCss = JSONObject.quote(css)
            """
            (function(){
                var st = document.getElementById('$STYLE_ID');
                if (!st) {
                    st = document.createElement('style');
                    st.id = '$STYLE_ID';
                }
                st.textContent = $jsonCss;
                var target = document.head || document.documentElement;
                if (target && !st.parentNode) target.appendChild(st);
            })();
            """.trimIndent()
        }
    }

    private fun cssFor(style: String?): String = when (style) {
        "compact" -> SHARED_FIX + COMPACT_CSS
        "karaoke" -> SHARED_FIX + KARAOKE_CSS
        "bold" -> SHARED_FIX + BOLD_CSS
        "fullscreen" -> SHARED_FIX + FULLSCREEN_CSS
        else -> "" // "default" and anything unknown -> remove style
    }

    // ---------------------------------------------------------------
    // SHARED: background-seam fix + viewport + credit + reduced motion
    // (your v5 sections A, B, J, M)
    // ---------------------------------------------------------------
    private val SHARED_FIX = """
/* --- Spotilol Lyrics Engine: shared fixes --- */

/* Old ~1-screen background layer -> hidden */
.nqmjceMqTFCSMXlnquLP { display: none !important; }

/* Paint the full-height lyric container chain with the dynamic album color */
.bqldaBkacR41KxR2Z0jY,
.NAOY0Orzgl4rd4__VtAw,
.l2GQ00sPnkqe8YLHcfzL,
.l2GQ00sPnkqe8YLHcfzL > div {
  background-color: var(--lyrics-color-background, #121212) !important;
  background-image: none !important;
  transition: background-color .6s ease !important;
}

/* Outer containers transparent */
.WiwnWsPYbL585uUaVMp3,
.main-view-container__scroll-node-child,
.TheioDphh_FsNXvBiDj4 {
  background-color: transparent !important;
}

/* Viewport scroll */
[data-overlayscrollbars-viewport] {
  overscroll-behavior-y: contain !important;
  -webkit-overflow-scrolling: touch;
}

/* Musixmatch credit */
.upDNlpL8xEkxmKWWw9IF {
  margin: 16px 5% 0 !important;
  text-align: center !important;
  opacity: .45 !important;
}
.upDNlpL8xEkxmKWWw9IF .e-10860-text { font-size: 11px !important; }

/* Reduce motion */
@media (prefers-reduced-motion: reduce) {
  [data-testid="lyrics-line"] ._3s1DGSMxRHUVPuxgkoss { transition: none !important; }
}
""".trimIndent()

    // ---------------------------------------------------------------
    // FULLSCREEN - your v5 body, unchanged
    // ---------------------------------------------------------------
    private val FULLSCREEN_CSS = """
/* === FULLSCREEN: mobile fullscreen, album colors, glowing active line === */
.l2GQ00sPnkqe8YLHcfzL {
  width: 100% !important;
  max-width: 100% !important;
  margin: 0 !important;
  padding: 8px 5% 140px 5% !important;
  box-sizing: border-box !important;
}
.bqldaBkacR41KxR2Z0jY {
  --lyrics-color-active:   #ffffff !important;
  --lyrics-color-inactive: rgba(255,255,255,.55) !important;
  --lyrics-color-passed:   rgba(255,255,255,.28) !important;
}
[data-testid="lyrics-line"] {
  margin: 0 !important;
  padding: 1px 0 !important;
  line-height: 1.45 !important;
  font-size: clamp(1.25rem, 2.4vw, 2.25rem) !important;
  overflow-wrap: anywhere !important;
  user-select: text !important;
}
[data-testid="lyrics-line"] ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1em !important;
  font-weight: 700 !important;
  line-height: 1.45 !important;
  color: rgba(255,255,255,.55) !important;
  text-shadow: none !important;
  transition: font-size .3s cubic-bezier(.3,.7,.3,1),
              color .4s ease, text-shadow .4s ease;
}
[data-testid="lyrics-line"].loNizikBbaCKyI9Gv8xg ._3s1DGSMxRHUVPuxgkoss {
  color: rgba(255,255,255,.28) !important;
}
[data-testid="lyrics-line"].dPaa_Hg0z0Ql_UBrV9uZ ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1.35em !important;
  font-weight: 800 !important;
  line-height: 1.32 !important;
  color: #fff !important;
  text-shadow: 0 0 18px rgba(255,255,255,.28), 0 2px 6px rgba(0,0,0,.5) !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
  height: 20px !important; min-height: 20px !important; max-height: 20px !important;
  margin: 0 !important; padding: 0 !important; overflow: hidden !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt ._3s1DGSMxRHUVPuxgkoss {
  line-height: 0 !important;
}
@media (max-width: 768px) {
  .NAOY0Orzgl4rd4__VtAw { min-height: 100dvh !important; }
  .l2GQ00sPnkqe8YLHcfzL {
    padding:
      calc(8px + env(safe-area-inset-top))
      max(5%, env(safe-area-inset-right))
      calc(120px + env(safe-area-inset-bottom))
      max(5%, env(safe-area-inset-left)) !important;
  }
  [data-testid="lyrics-line"] { font-size: clamp(1.15rem, 5.5vw, 1.6rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 18px !important; min-height: 18px !important; max-height: 18px !important;
  }
}
@media (max-height: 480px) and (orientation: landscape) {
  [data-testid="lyrics-line"] { font-size: clamp(1rem, 4.5vh, 1.3rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 12px !important; min-height: 12px !important; max-height: 12px !important;
  }
  .l2GQ00sPnkqe8YLHcfzL { padding-bottom: 80px !important; }
}
""".trimIndent()

    // ---------------------------------------------------------------
    // COMPACT - dense, readable, no glow
    // ---------------------------------------------------------------
    private val COMPACT_CSS = """
/* === COMPACT: small, dense, zero glow === */
.l2GQ00sPnkqe8YLHcfzL {
  width: 100% !important;
  max-width: 100% !important;
  margin: 0 !important;
  padding: 8px 5% 120px 5% !important;
  box-sizing: border-box !important;
}
.bqldaBkacR41KxR2Z0jY {
  --lyrics-color-active:   #ffffff !important;
  --lyrics-color-inactive: rgba(255,255,255,.5) !important;
  --lyrics-color-passed:   rgba(255,255,255,.25) !important;
}
[data-testid="lyrics-line"] {
  margin: 0 !important;
  padding: 1px 0 !important;
  line-height: 1.35 !important;
  font-size: clamp(.95rem, 1.9vw, 1.35rem) !important;
  overflow-wrap: anywhere !important;
  user-select: text !important;
}
[data-testid="lyrics-line"] ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1em !important;
  font-weight: 600 !important;
  line-height: 1.35 !important;
  color: rgba(255,255,255,.5) !important;
  text-shadow: none !important;
  transition: font-size .2s ease, color .3s ease;
}
[data-testid="lyrics-line"].loNizikBbaCKyI9Gv8xg ._3s1DGSMxRHUVPuxgkoss {
  color: rgba(255,255,255,.25) !important;
}
[data-testid="lyrics-line"].dPaa_Hg0z0Ql_UBrV9uZ ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1.12em !important;
  font-weight: 800 !important;
  line-height: 1.3 !important;
  color: #fff !important;
  text-shadow: none !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
  height: 14px !important; min-height: 14px !important; max-height: 14px !important;
  margin: 0 !important; padding: 0 !important; overflow: hidden !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt ._3s1DGSMxRHUVPuxgkoss {
  line-height: 0 !important;
}
@media (max-width: 768px) {
  .NAOY0Orzgl4rd4__VtAw { min-height: 100dvh !important; }
  .l2GQ00sPnkqe8YLHcfzL {
    padding:
      calc(8px + env(safe-area-inset-top))
      max(5%, env(safe-area-inset-right))
      calc(110px + env(safe-area-inset-bottom))
      max(5%, env(safe-area-inset-left)) !important;
  }
  [data-testid="lyrics-line"] { font-size: clamp(.9rem, 4.2vw, 1.15rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 12px !important; min-height: 12px !important; max-height: 12px !important;
  }
}
@media (max-height: 480px) and (orientation: landscape) {
  [data-testid="lyrics-line"] { font-size: clamp(.85rem, 4vh, 1.05rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 10px !important; min-height: 10px !important; max-height: 10px !important;
  }
  .l2GQ00sPnkqe8YLHcfzL { padding-bottom: 70px !important; }
}
""".trimIndent()

    // ---------------------------------------------------------------
    // KARAOKE - huge glowing active line, everything else ghosted
    // ---------------------------------------------------------------
    private val KARAOKE_CSS = """
/* === KARAOKE: one giant line at a time === */
.l2GQ00sPnkqe8YLHcfzL {
  width: 100% !important;
  max-width: 100% !important;
  margin: 0 !important;
  padding: 8px 5% 150px 5% !important;
  box-sizing: border-box !important;
}
.bqldaBkacR41KxR2Z0jY {
  --lyrics-color-active:   #ffffff !important;
  --lyrics-color-inactive: rgba(255,255,255,.22) !important;
  --lyrics-color-passed:   rgba(255,255,255,.10) !important;
}
[data-testid="lyrics-line"] {
  margin: 0 !important;
  padding: 2px 0 !important;
  line-height: 1.45 !important;
  font-size: clamp(1.2rem, 2.6vw, 2rem) !important;
  overflow-wrap: anywhere !important;
  user-select: text !important;
}
[data-testid="lyrics-line"] ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1em !important;
  font-weight: 700 !important;
  line-height: 1.45 !important;
  color: rgba(255,255,255,.22) !important;
  text-shadow: none !important;
  transition: font-size .35s cubic-bezier(.3,.7,.3,1),
              color .4s ease, text-shadow .4s ease;
}
[data-testid="lyrics-line"].loNizikBbaCKyI9Gv8xg ._3s1DGSMxRHUVPuxgkoss {
  color: rgba(255,255,255,.10) !important;
}
[data-testid="lyrics-line"].dPaa_Hg0z0Ql_UBrV9uZ ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1.55em !important;
  font-weight: 900 !important;
  line-height: 1.28 !important;
  color: #fff !important;
  text-shadow: 0 0 30px rgba(255,255,255,.45), 0 2px 8px rgba(0,0,0,.55) !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
  height: 22px !important; min-height: 22px !important; max-height: 22px !important;
  margin: 0 !important; padding: 0 !important; overflow: hidden !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt ._3s1DGSMxRHUVPuxgkoss {
  line-height: 0 !important;
}
@media (max-width: 768px) {
  .NAOY0Orzgl4rd4__VtAw { min-height: 100dvh !important; }
  .l2GQ00sPnkqe8YLHcfzL {
    padding:
      calc(8px + env(safe-area-inset-top))
      max(5%, env(safe-area-inset-right))
      calc(130px + env(safe-area-inset-bottom))
      max(5%, env(safe-area-inset-left)) !important;
  }
  [data-testid="lyrics-line"] { font-size: clamp(1.1rem, 5vw, 1.6rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 18px !important; min-height: 18px !important; max-height: 18px !important;
  }
}
@media (max-height: 480px) and (orientation: landscape) {
  [data-testid="lyrics-line"] { font-size: clamp(1rem, 4.5vh, 1.35rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 12px !important; min-height: 12px !important; max-height: 12px !important;
  }
  .l2GQ00sPnkqe8YLHcfzL { padding-bottom: 90px !important; }
}
""".trimIndent()

    // ---------------------------------------------------------------
    // BOLD - everything big & heavy, uniform size, brightness differs
    // ---------------------------------------------------------------
    private val BOLD_CSS = """
/* === BOLD: all lines large, active just brighter === */
.l2GQ00sPnkqe8YLHcfzL {
  width: 100% !important;
  max-width: 100% !important;
  margin: 0 !important;
  padding: 8px 5% 140px 5% !important;
  box-sizing: border-box !important;
}
.bqldaBkacR41KxR2Z0jY {
  --lyrics-color-active:   #ffffff !important;
  --lyrics-color-inactive: rgba(255,255,255,.72) !important;
  --lyrics-color-passed:   rgba(255,255,255,.42) !important;
}
[data-testid="lyrics-line"] {
  margin: 0 !important;
  padding: 1px 0 !important;
  line-height: 1.5 !important;
  font-size: clamp(1.3rem, 2.8vw, 2.4rem) !important;
  overflow-wrap: anywhere !important;
  user-select: text !important;
}
[data-testid="lyrics-line"] ._3s1DGSMxRHUVPuxgkoss {
  font-size: 1em !important;
  font-weight: 800 !important;
  line-height: 1.5 !important;
  color: rgba(255,255,255,.72) !important;
  text-shadow: none !important;
  transition: color .4s ease, text-shadow .4s ease;
}
[data-testid="lyrics-line"].loNizikBbaCKyI9Gv8xg ._3s1DGSMxRHUVPuxgkoss {
  color: rgba(255,255,255,.42) !important;
}
[data-testid="lyrics-line"].dPaa_Hg0z0Ql_UBrV9uZ ._3s1DGSMxRHUVPuxgkoss {
  color: #fff !important;
  text-shadow: 0 2px 10px rgba(0,0,0,.6) !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
  height: 20px !important; min-height: 20px !important; max-height: 20px !important;
  margin: 0 !important; padding: 0 !important; overflow: hidden !important;
}
[data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt ._3s1DGSMxRHUVPuxgkoss {
  line-height: 0 !important;
}
@media (max-width: 768px) {
  .NAOY0Orzgl4rd4__VtAw { min-height: 100dvh !important; }
  .l2GQ00sPnkqe8YLHcfzL {
    padding:
      calc(8px + env(safe-area-inset-top))
      max(5%, env(safe-area-inset-right))
      calc(120px + env(safe-area-inset-bottom))
      max(5%, env(safe-area-inset-left)) !important;
  }
  [data-testid="lyrics-line"] { font-size: clamp(1.2rem, 5.8vw, 1.75rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 16px !important; min-height: 16px !important; max-height: 16px !important;
  }
}
@media (max-height: 480px) and (orientation: landscape) {
  [data-testid="lyrics-line"] { font-size: clamp(1.05rem, 4.8vh, 1.4rem) !important; }
  [data-testid="lyrics-line"].WBNUk2iJWB8WkN8FaBOt {
    height: 12px !important; min-height: 12px !important; max-height: 12px !important;
  }
  .l2GQ00sPnkqe8YLHcfzL { padding-bottom: 80px !important; }
}
""".trimIndent()
}