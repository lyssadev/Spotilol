package com.project.lol.webview.helpers

fun isAnalyticsDomain(url: String): Boolean {
    return url.contains("doubleclick.net") ||
            url.contains("googlesyndication.com") ||
            url.contains("fastly-insights.com") ||
            url.contains("sentry.io") ||
            url.contains("t.6sc.co") ||
            url.contains("tracker.samplicio.us") ||
            url.contains("adsrvr.org") ||
            url.contains("aet.spotify.com")
}

fun matchAdCdn(url: String): String? {
    if (url.contains("scdn.co/mp3-ad/")) return "scdn.co/mp3-ad/"
    if (url.contains("mp3ad.scdn.co")) return "mp3ad.scdn.co"
    if (url.contains("amillionads.com")) return "amillionads.com"
    if (url.contains("2mdn.net")) return "2mdn.net"
    if (url.contains("adxcel.com")) return "adxcel.com"
    if (url.contains("adstudio-assets.scdn.co")) return "adstudio-assets.scdn.co"
    if (url.contains("audio-ads.spotify.com")) return "audio-ads.spotify.com"
    if (url.contains("ads-akp.spotify.com")) return "ads-akp.spotify.com"
    if (url.contains("ads-fa.spotify.com")) return "ads-fa.spotify.com"
    if (url.contains("adeventtracker.spotify.com")) return "adeventtracker.spotify.com"
    if (url.contains("pixel-static.spotify.com")) return "pixel-static.spotify.com"
    if (url.contains("pixel.spotify.com")) return "pixel.spotify.com"
    if (url.contains("adstudio.spotify.com")) return "adstudio.spotify.com"
    if (url.contains("ads.spotify.com")) return "ads.spotify.com"
    return null
}
