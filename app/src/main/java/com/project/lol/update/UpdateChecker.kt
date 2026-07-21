package com.project.lol.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        private const val API_URL = "https://api.github.com/repos/lyssadev/Spotilol/releases/latest"
        private const val PREFS_NAME = "spotilol_prefs"
        private const val KEY_HAS_UPDATE = "hasUpdateAvailable"
        private const val KEY_LAST_CHECK = "LastUpdateCheck"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    fun hasUpdateAvailable(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_UPDATE, false)
    }

    fun clearUpdateAvailable() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAS_UPDATE, false).apply()
    }

    fun autoCheck() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return

        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        fetchUpdateAvailable()
    }

    private fun fetchUpdateAvailable() {
        Thread {
            try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val tagName = json.getString("tag_name").removePrefix("v")
                    val currentVersion = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName ?: ""

                    if (isNewer(tagName, currentVersion)) {
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_HAS_UPDATE, true).apply()
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".")
        val currentParts = current.split(".")
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
