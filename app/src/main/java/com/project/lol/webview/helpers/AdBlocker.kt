package com.project.lol.webview.helpers

private val ANALYTICS_DOMAINS = listOf(
    "doubleclick.net",
    "googlesyndication.com",
    "fastly-insights.com",
    "sentry.io",
    "t.6sc.co",
    "tracker.samplicio.us",
    "adsrvr.org",
    "aet.spotify.com",
    "retargeting-pixels",
    "spotify.com/gabo-receiver-service/public/v3/events"
    // workbox-window REMOVED: Spotify now loads it as a lazy webpack chunk
    // (chunk 6201) during web-player init. Blocking it caused ChunkLoadError
    // -> React error boundary -> "Something wrong with page". The service
    // worker itself is already neutralized by WorkerNeutralize injecting
    // before Spotify's code runs, so this block was redundant anyway.
)

private val AD_AUDIO_DOMAINS = listOf(
    "akamaized.net/audio/",
    "scdn.co/audio/",
    "scdn.co/mp3-ad/",
    "spotifycdn.com/audio/",
    "amillionads.com",
    "2mdn.net",
    "adxcel.com",
    "adstudio-assets.scdn.co"
)

private val POWER_HOG_HOSTS = listOf(
    "canvasset.scdn.co",
    "video-ak.spotify.com",
    "video-provider.net"
)

private val VIDEO_EXTENSIONS = listOf(".mp4", ".m4s", ".m3u8", ".webm")

private val AD_CDN_PATTERNS = listOf(
    "scdn.co/mp3-ad/",
    "mp3ad.scdn.co",
    "amillionads.com",
    "2mdn.net",
    "adxcel.com",
    "adstudio-assets.scdn.co",
    "audio-ads.spotify.com",
    "ads-akp.spotify.com",
    "ads-fa.spotify.com",
    "adeventtracker.spotify.com",
    "pixel-static.spotify.com",
    "pixel.spotify.com",
    "adstudio.spotify.com",
    "ads.spotify.com"
)

fun isAnalyticsDomain(url: String): Boolean =
    ANALYTICS_DOMAINS.any { url.contains(it) }

@Suppress("unused")
fun isAdAudioUrl(url: String): Boolean =
    AD_AUDIO_DOMAINS.any { url.contains(it) }

fun isPowerHogUrl(url: String): Boolean {
    if (POWER_HOG_HOSTS.any { url.contains(it) }) return true
    if (url.contains("/audio/")) return false
    val path = url.substringBefore('?').substringBefore('#')
    return VIDEO_EXTENSIONS.any { path.endsWith(it, ignoreCase = true) }
}

fun matchAdCdn(url: String): String? =
    AD_CDN_PATTERNS.firstOrNull { url.contains(it) }