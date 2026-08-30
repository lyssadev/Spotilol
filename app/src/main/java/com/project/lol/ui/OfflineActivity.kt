package com.project.lol.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.project.lol.profile.ProfileManager
import com.project.lol.ui.screens.OfflineScreen
import com.project.lol.ui.theme.SpotifyTheme

class OfflineActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)
    private val hideTopBarState = mutableStateOf(false)
    private val landscapeState = mutableStateOf(false)
    private val keepScreenOnState = mutableStateOf(false)
    private val paletteSeedState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)

        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        hideTopBarState.value = prefs.getBoolean("HideTopBar", false)
        landscapeState.value = prefs.getBoolean("LandscapeMode", false)
        keepScreenOnState.value = prefs.getBoolean("KeepScreenOn", false)
        paletteSeedState.value = prefs.getString("PaletteSeed", null)

        applyOrientation()
        applyKeepScreenOn()

        setContent {
            SpotifyTheme(
                useDynamicColor = materialYouState.value,
                amoled = amoledState.value,
                seedColor = paletteSeedState.value?.let { hex ->
                    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                }
            ) {
                OfflineScreen(
                    prefs = prefs,
                    materialYou = materialYouState.value,
                    onMaterialYouChange = { enabled ->
                        materialYouState.value = enabled
                        prefs.edit().putBoolean("MaterialYou", enabled).apply()
                    },
                    amoledTheme = amoledState.value,
                    onAmoledThemeChange = { enabled ->
                        amoledState.value = enabled
                        prefs.edit().putBoolean("AmoledTheme", enabled).apply()
                    },
                    hideTopBar = hideTopBarState.value,
                    onHideTopBarChange = { enabled ->
                        hideTopBarState.value = enabled
                        prefs.edit().putBoolean("HideTopBar", enabled).apply()
                    },
                    landscapeMode = landscapeState.value,
                    onLandscapeModeChange = { enabled ->
                        landscapeState.value = enabled
                        prefs.edit().putBoolean("LandscapeMode", enabled).apply()
                        applyOrientation()
                    },
                    keepScreenOn = keepScreenOnState.value,
                    onKeepScreenOnChange = { enabled ->
                        keepScreenOnState.value = enabled
                        prefs.edit().putBoolean("KeepScreenOn", enabled).apply()
                        applyKeepScreenOn()
                    },
                    paletteSeed = paletteSeedState.value,
                    onPaletteSeedChange = { hex ->
                        paletteSeedState.value = hex
                        if (hex.isNullOrBlank()) {
                            prefs.edit().remove("PaletteSeed").apply()
                        } else {
                            prefs.edit().putString("PaletteSeed", hex).apply()
                        }
                    },
                    onConnectionModeChange = { mode ->
                        prefs.edit().putString("ConnectionMode", mode).apply()
                        restartToSplash()
                    },
                    onOfflineModeChange = { enabled ->
                        prefs.edit().putBoolean("OfflineMode", enabled).apply()
                        restartToSplash()
                    },
                    onSaveProfile = { name, cookies ->
                        ProfileManager.saveProfile(this, name, cookies)
                        Toast.makeText(this, "Account saved", Toast.LENGTH_SHORT).show()
                    },
                    onLoadProfile = { cookies ->
                        ProfileManager.applyProfile(this, cookies) { success ->
                            if (success) {
                                Toast.makeText(this, "Profile loaded", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Profile could not be loaded", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteProfile = { name ->
                        ProfileManager.deleteProfile(this, name)
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show()
                    },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() },
                    onExit = { exitOfflineMode() }
                )
            }
        }
    }

    private fun applyOrientation() {
        requestedOrientation = if (landscapeState.value) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun applyKeepScreenOn() {
        if (keepScreenOnState.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun clearWebViewCache() {
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.destroy()
        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllData() {
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.clearFormData()
        wv.destroy()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        prefs.edit().putBoolean("LoggedIn", false).apply()
        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun restartToSplash() {
        startActivity(
            Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun exitOfflineMode() {
        prefs.edit().putBoolean("OfflineMode", false).apply()
        restartToSplash()
    }
}
