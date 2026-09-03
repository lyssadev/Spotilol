package com.project.lol.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.project.lol.R
import com.project.lol.bridge.SpotifyBridge
import com.project.lol.offline.DownloadManager
import com.project.lol.profile.ProfileManager
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.service.MediaNotificationService
import com.project.lol.ui.components.SettingsDrawer
import com.project.lol.ui.theme.SpotifyTheme
import com.project.lol.util.UpdateChecker
import com.project.lol.webview.SpotifyWebChromeClient
import com.project.lol.webview.SpotifyWebViewClient
import com.project.lol.webview.helpers.DevLogPrelude
import com.project.lol.webview.helpers.LyricsTheme
import com.project.lol.webview.helpers.buildAmoledJs
import com.project.lol.webview.helpers.buildCustomCssJs
import com.project.lol.webview.injections.LogoutCheck
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.min
import org.json.JSONObject
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.project.lol.ui.components.ErrorScreen
import com.project.lol.ui.components.mapWebViewError
import com.project.lol.webview.helpers.AccentTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var serviceStarted = false
    @Volatile private var pipCoverBitmap: Bitmap? = null
    @Volatile private var pipPlaying = false
    private var lastPipCoverUrl = ""
    private var pipOverlay: FrameLayout? = null
    private var pipCoverImg: ImageView? = null
    private var pipUsed = false
    private var pipVideoView: View? = null
    private var pipVideoCallback: android.webkit.WebChromeClient.CustomViewCallback? = null
    private var pipVideoAspect: Rational? = null
    private var pipVideoPending = false
    private val pipVideoTimeout = Handler(Looper.getMainLooper())

    private val serviceEnabledState = mutableStateOf(true)
    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)
    private val hideTopBarState = mutableStateOf(false)
    private val landscapeModeState = mutableStateOf(false)
    private val keepScreenOnState = mutableStateOf(false)
    private val paletteSeedState = mutableStateOf<String?>(null)

    private val showSleepTimerDialog = mutableStateOf(false)
    private val sleepTimerInputText = mutableStateOf("")
    private var sleepTimer: CountDownTimer? = null
    private val sleepTimerRemainingMs = mutableLongStateOf(0L)
    private val sleepTimerActive = mutableStateOf(false)

    private val loadingProgress = mutableIntStateOf(100)
    private val blockServiceWorkerState = mutableStateOf(true)
    private val webViewError = mutableStateOf<Pair<Int, String>?>(null)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val btPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> requestNotificationPermission() }

    private lateinit var prefs: SharedPreferences

    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Track screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        })

        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"

        // After an OOM kill, Android can resume directly at MainActivity
        if (useProxy && !LocalProxyManager.isRunning) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestBluetoothPermission()
        val uc = UpdateChecker(this)
        uc.autoCheck { url ->
            Toast.makeText(this, "A new update is available", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
            }, 2000)
        }

        val loggedIn = prefs.getBoolean("LoggedIn", false)

        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        hideTopBarState.value = prefs.getBoolean("HideTopBar", false)
        landscapeModeState.value = prefs.getBoolean("LandscapeMode", false)
        keepScreenOnState.value = prefs.getBoolean("KeepScreenOn", false)
        paletteSeedState.value = prefs.getString("PaletteSeed", null)
        blockServiceWorkerState.value = prefs.getBoolean("BlockServiceWorker", true)
        applyOrientation()
        applyKeepScreenOn()

        setContent {
            val serviceEnabled = serviceEnabledState.value
            val materialYou = materialYouState.value
            val amoled = amoledState.value
            val hideTopBar = hideTopBarState.value
            val landscapeMode = landscapeModeState.value
            val keepScreenOn = keepScreenOnState.value
            val paletteSeed = paletteSeedState.value
            val showDialog = showSleepTimerDialog.value
            val timerActive = sleepTimerActive.value
            val loadProgress = loadingProgress.intValue
            val blockServiceWorker = blockServiceWorkerState.value

            var settingsDrawerOpen by remember { mutableStateOf(false) }
            var showMiniMenu by remember { mutableStateOf(false) }
            val versionName = remember {
                runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
                    .getOrNull() ?: ""
            }
            val seedColor = paletteSeed?.let { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
            }
            val accentColor = remember(materialYou, paletteSeed) {
                AccentTheme.resolveColor(this@MainActivity)
            }

            BackHandler(enabled = settingsDrawerOpen || webView?.canGoBack() == true) {
                if (settingsDrawerOpen) {
                    settingsDrawerOpen = false
                } else {
                    webView?.goBack()
                }
            }

            SpotifyTheme(useDynamicColor = materialYou, amoled = amoled, seedColor = seedColor) {
                SettingsDrawer(
                    visible = settingsDrawerOpen,
                    onClose = { settingsDrawerOpen = false },
                    prefs = prefs,
                    materialYou = materialYou,
                    onMaterialYouChange = { enabled ->
                        materialYouState.value = enabled
                        prefs.edit().putBoolean("MaterialYou", enabled).apply()
                    },
                    amoledThemeState = amoled,
                    onAmoledThemeChange = { enabled ->
                        amoledState.value = enabled
                        prefs.edit().putBoolean("AmoledTheme", enabled).apply()
                    },
                    hideTopBar = hideTopBar,
                    onHideTopBarChange = { enabled ->
                        hideTopBarState.value = enabled
                        prefs.edit().putBoolean("HideTopBar", enabled).apply()
                    },
                    landscapeMode = landscapeMode,
                    onLandscapeModeChange = { enabled ->
                        landscapeModeState.value = enabled
                        prefs.edit().putBoolean("LandscapeMode", enabled).apply()
                        applyOrientation()
                    },
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnChange = { enabled ->
                        keepScreenOnState.value = enabled
                        prefs.edit().putBoolean("KeepScreenOn", enabled).apply()
                        applyKeepScreenOn()
                    },
                    paletteSeed = paletteSeed,
                    onPaletteSeedChange = { hex ->
                        paletteSeedState.value = hex
                        if (hex.isNullOrBlank()) {
                            prefs.edit().remove("PaletteSeed").apply()
                        } else {
                            prefs.edit().putString("PaletteSeed", hex).apply()
                        }
                    },
                    onConnectionModeChange = { switchConnectionMode(it) },
                    onOfflineModeChange = { switchOfflineMode(it) },
                    onSaveProfile = { name, cookies -> saveProfile(name, cookies) },
                    onLoadProfile = { cookies -> loadProfile(cookies) },
                    onDeleteProfile = { name -> deleteProfile(name) },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() },
                    onDebugToggle = { enabled ->
                        webView?.evaluateJavascript(
                            if (enabled) DevLogPrelude.js()
                            else "window.dbg=null;window.dbgw=null;window.dbge=null;window.DevLog=null;",
                            null
                        )
                    },
                    blockServiceWorker = blockServiceWorker,
                    onBlockServiceWorkerChange = { enabled ->
                        blockServiceWorkerState.value = enabled
                        prefs.edit { putBoolean("BlockServiceWorker", enabled) }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            if (!hideTopBar) {
                                CenterAlignedTopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Spotilol",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "v$versionName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        analytics.logEvent("open_settings", Bundle().apply {
                                            putString(FirebaseAnalytics.Param.SCREEN_NAME, "SettingsDrawer")
                                        })
                                        settingsDrawerOpen = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                actions = {
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = { newValue -> setServiceEnabled(newValue) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                            },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            }
                    }
) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (serviceEnabled) {
                            val bridge = remember {
                                SpotifyBridge(WeakReference(this@MainActivity))
                            }

                            bridge.onTimerDialogRequest = {
                                showSleepTimerDialog.value = true
                                if (!timerActive) {
                                    sleepTimerInputText.value = ""
                                }
                            }

                            bridge.onEnterPipRequest = {
                                enterPipMode()
                            }

                            bridge.onEnterPipVideoRequest = { w, h ->
                                enterPipVideoMode(w, h)
                            }

                            bridge.onMediaStatus = { json ->
                                handleMediaStatus(json)
                            }

                            bridge.onDownloadTrack = { payload ->
                                Log.d("Spl-DL", "bridge.onDownloadTrack: payload=$payload")
                                wireDownloadCallbacks()
                                startDownloadService()
                                DownloadManager.downloadCurrentTrack(this@MainActivity, payload)
                            }

                            bridge.onDownloadCollection = { payload ->
                                Log.d("Spl-DL", "bridge.onDownloadCollection: payload=${payload.take(300)}")
                                wireDownloadCallbacks()
                                startDownloadService()
                                DownloadManager.downloadCollection(this@MainActivity, payload)
                            }



                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        webView = this

                                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                        settings.apply {
                                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            allowFileAccess = false
                                            allowContentAccess = false
                                            mediaPlaybackRequiresUserGesture = false
                                            setSupportMultipleWindows(true)
                                            javaScriptCanOpenWindowsAutomatically = true
                                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                            setGeolocationEnabled(false)
                                            @Suppress("DEPRECATION")
                                            saveFormData = false
                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        }

                                        setInitialScale(100)
                                        setBackgroundColor(0xFF000000.toInt())

                                        if (WebViewFeature.isFeatureSupported(WebViewFeature.BACK_FORWARD_CACHE)) {
                                            WebSettingsCompat.setBackForwardCacheEnabled(settings, true)
                                        }

                                        addJavascriptInterface(bridge, "AndBridge")
                                        webChromeClient = SpotifyWebChromeClient(
                                            onProgressChanged = { progress ->
                                                loadingProgress.intValue = progress
                                            },
                                            onShowCustomView = { view, callback ->
                                                handleCustomViewShown(view, callback)
                                            },
                                            onHideCustomView = {
                                                handleCustomViewHidden()
                                            }
                                        )

                                        webViewClient = SpotifyWebViewClient(
                                            onLoginRequired = {
                                                loadUrl("https://accounts.spotify.com/login")
                                            },
                                            onRenderProcessGone = {
                                                runOnUiThread {
                                                    webViewError.value = null
                                                    destroyWebView()
                                                }
                                            },
                                            onWebViewError = { code, desc ->
                                                webViewError.value = code to desc
                                            }
                                        )

                                        val executor = Executors.newSingleThreadExecutor()
                                        if (useProxy && LocalProxyManager.isRunning) {
                                            val proxyConfig = ProxyConfig.Builder()
                                                .addProxyRule("localhost:${LocalProxyManager.port}")
                                                .build()
                                            ProxyController.getInstance().setProxyOverride(
                                                proxyConfig,
                                                executor,
                                                { }
                                            )
                                        } else {
                                            ProxyController.getInstance().clearProxyOverride(executor, { })
                                        }

                                        if (loggedIn) {
                                            loadUrl("https://open.spotify.com/")
                                        } else {
                                            loadUrl("https://accounts.spotify.com/login")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )

                            LaunchedEffect(webView) {
                                webView?.let { startMediaService() }
                            }

                            val progressAlpha by animateFloatAsState(
                                targetValue = if (loadProgress < 100) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, delayMillis = 200),
                                label = "progressAlpha"
                            )
                            if (progressAlpha > 0.001f) {
                                LinearProgressIndicator(
                                    progress = { loadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .alpha(progressAlpha),
                                    color = accentColor,   // was Color(0xFF22DD66)
                                    trackColor = Color.Transparent,
                                )
                            }

                            webViewError.value?.let { (code, desc) ->
                                com.project.lol.ui.components.ErrorScreen(
                                    errorType = com.project.lol.ui.components.mapWebViewError(code),
                                    errorCode = code,
                                    errorDescription = desc,
                                    onRetry = {
                                        webViewError.value = null
                                        webView?.reload()
                                    }
                                )
                            }

                            if (showDialog) {
                                SleepTimerDialog(
                                    timerActive = timerActive,
                                    timerRemainingMs = sleepTimerRemainingMs.longValue,
                                    inputText = sleepTimerInputText.value,
                                    onInputChange = { sleepTimerInputText.value = it },
                                    onSetTimer = { minutes ->
                                        showSleepTimerDialog.value = false
                                        if (minutes > 0) {
                                            startSleepTimer(minutes)
                                        } else {
                                            cancelSleepTimer()
                                        }
                                    },
                                    onCancelTimer = {
                                        showSleepTimerDialog.value = false
                                        cancelSleepTimer()
                                    },
                                    onDismiss = {
                                        showSleepTimerDialog.value = false
                                    }
                                )
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    LayoutInflater.from(context)
                                        .inflate(R.layout.service_disabled, null).apply {
                                            val tvVersion = findViewById<TextView>(R.id.tvWebViewVersion)
                                            val pkg = WebViewCompat.getCurrentWebViewPackage(context)
                                            tvVersion.text = "Webview: ${pkg?.versionName ?: "N/A"}"
                                        }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (hideTopBar) {
                            QuickAccessOverlay(
                                showMenu = showMiniMenu,
                                onToggleMenu = { showMiniMenu = !showMiniMenu },
                                onOpenSettings = {
                                    showMiniMenu = false
                                    settingsDrawerOpen = true
                                },
                                serviceEnabled = serviceEnabled,
                                onServiceToggle = { newValue -> setServiceEnabled(newValue) },
                                onDismissMenu = { showMiniMenu = false }
                            )
                        }
                    }
                }
            }
            }

        }
    }

    private fun setServiceEnabled(newValue: Boolean) {
        serviceEnabledState.value = newValue
        prefs.edit().putBoolean("ServiceOn", newValue).apply()
        if (!newValue) {
            analytics.logEvent("service_toggle", Bundle().apply {
                putString("enabled", "off")
            })
            stopService(Intent(this, MediaNotificationService::class.java))
            serviceStarted = false
            destroyWebView()
        } else {
            analytics.logEvent("service_toggle", Bundle().apply {
                putString("enabled", "on")
            })
        }
    }

    private fun switchConnectionMode(mode: String) {
        prefs.edit().putString("ConnectionMode", mode).apply()
        prefs.edit().putBoolean("ServiceOn", false).apply()
        stopService(Intent(this, MediaNotificationService::class.java))
        LocalProxyManager.stop()
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun switchOfflineMode(enabled: Boolean) {
        prefs.edit().putBoolean("OfflineMode", enabled).apply()
        prefs.edit().putBoolean("ServiceOn", false).apply()
        stopService(Intent(this, MediaNotificationService::class.java))
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun saveProfile(name: String, cookies: String) {
        ProfileManager.saveProfile(this, name, cookies)
        Toast.makeText(this, "Account saved", Toast.LENGTH_SHORT).show()
    }

    private fun loadProfile(cookies: String) {
        if (!ProfileManager.applyProfile(this, cookies)) {
            Toast.makeText(this, "Profile could not be loaded", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun deleteProfile(name: String) {
        ProfileManager.deleteProfile(this, name)
        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "All data cleared, please login again", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L
        sleepTimerActive.value = true
        sleepTimerRemainingMs.longValue = totalMs

        analytics.logEvent("sleep_timer_start", Bundle().apply {
            putString("minutes", minutes.toString())
        })

        webView?.evaluateJavascript("""
            if(window.timerBtn) timerBtn.style.color='var(--spl-accent,#2d6)';
            var t=document.getElementById('spl-timer');
            if(t) t.classList.add('spl-active');
        """.trimIndent(), null)

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerRemainingMs.longValue = millisUntilFinished
            }

            override fun onFinish() {
                sleepTimerActive.value = false
                sleepTimerRemainingMs.longValue = 0L
                webView?.evaluateJavascript("""
                    if(window.timerBtn) timerBtn.style.color='';
                    var t=document.getElementById('spl-timer');
                    if(t) t.classList.remove('spl-active');
                """.trimIndent(), null)
                webView?.evaluateJavascript("actPlayPause(false)", null)
            }
        }.start()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        sleepTimerActive.value = false
        sleepTimerRemainingMs.longValue = 0L
        webView?.evaluateJavascript("""
            if(window.timerBtn) timerBtn.style.color='';
            var t=document.getElementById('spl-timer');
            if(t) t.classList.remove('spl-active');
        """.trimIndent(), null)
    }

    @Composable
    private fun SleepTimerDialog(
        timerActive: Boolean,
        timerRemainingMs: Long,
        inputText: String,
        onInputChange: (String) -> Unit,
        onSetTimer: (Int) -> Unit,
        onCancelTimer: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val minutes = inputText.toIntOrNull() ?: 0
        if (timerActive) {
            val remainingSecs = timerRemainingMs / 1000
            val mins = remainingSecs / 60
            val secs = remainingSecs % 60
            val timeStr = String.format("%d:%02d min remaining", mins, secs)

            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Text(
                        "Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⏰",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Timer active",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onCancelTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Timer")
                        }
                    }
                },
                dismissButton = {}
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Text(
                        "Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set minutes:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { new ->
                                if (new.length <= 5 && new.all { it.isDigit() }) {
                                    onInputChange(new)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("e.g. 25") },
                            trailingIcon = { Text("min", style = MaterialTheme.typography.bodyMedium) }
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { onSetTimer(minutes) },
                            enabled = minutes > 0
                        ) {
                            Text("Set Timer")
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }

    @Composable
    private fun QuickAccessOverlay(
        showMenu: Boolean,
        onToggleMenu: () -> Unit,
        onOpenSettings: () -> Unit,
        serviceEnabled: Boolean,
        onServiceToggle: (Boolean) -> Unit,
        onDismissMenu: () -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleMenu),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_playstore),
                    contentDescription = "Quick settings",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showMenu) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, with(LocalDensity.current) { 64.dp.toPx() }.toInt()),
                    onDismissRequest = onDismissMenu
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.width(220.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenSettings)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onServiceToggle(!serviceEnabled) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Service",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = onServiceToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enterPipMode() {
        pipUsed = true
        if (lastPipCoverUrl.isNotEmpty() && pipCoverBitmap == null) {
            fetchPipCover(lastPipCoverUrl)
        }
        showPipOverlay()
        val ok = enterPictureInPictureMode(buildPipParams())
        if (!ok) hidePipOverlay()
    }

    private fun enterPipVideoMode(w: Int, h: Int) {
        pipVideoAspect = if (w > 0 && h > 0) Rational(w, h) else Rational(9, 16)
        if (pipVideoView != null) {
            enterPipMode()
        } else {
            pipVideoPending = true
            pipVideoTimeout.removeCallbacksAndMessages(null)
            pipVideoTimeout.postDelayed({
                if (pipVideoView == null && pipVideoPending) {
                    pipVideoPending = false
                    enterPipMode()
                }
            }, 1200)
        }
    }

    private fun handleCustomViewShown(
        view: View?,
        callback: android.webkit.WebChromeClient.CustomViewCallback?
    ) {
        pipVideoView = view
        pipVideoCallback = callback
        showPipVideoOverlay()
        if (pipVideoPending) {
            pipVideoPending = false
            pipVideoTimeout.removeCallbacksAndMessages(null)
            enterPipMode()
        }
    }

    private fun handleCustomViewHidden() {
        pipVideoTimeout.removeCallbacksAndMessages(null)
        pipVideoView = null
        pipVideoCallback = null
        pipVideoPending = false
        hidePipOverlay()
        if (isInPictureInPictureMode) {
            pipVideoAspect = Rational(1, 1)
            if (pipCoverBitmap == null && lastPipCoverUrl.isNotEmpty()) {
                fetchPipCover(lastPipCoverUrl)
            }
            showPipOverlay()
            updatePipParams()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            if (pipVideoView == null) showPipOverlay()
            updatePipParams()
        } else {
            hidePipOverlay()
            pipVideoView = null
            pipVideoPending = false
            pipVideoAspect = null
            pipVideoCallback?.onCustomViewHidden()
            pipVideoCallback = null
        }
    }

    private fun buildPipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(pipVideoAspect ?: Rational(1, 1))
            .setActions(buildPipActions())
            .build()

    private fun buildPipActions(): List<RemoteAction> {
        val prev = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_skip_prev),
            "Previous", "Previous",
            pipActionIntent(MediaNotificationService.ACTION_PREV)
        )
        val playPause = RemoteAction(
            Icon.createWithResource(
                this,
                if (pipPlaying) R.drawable.ic_pause else R.drawable.ic_play
            ),
            if (pipPlaying) "Pause" else "Play",
            if (pipPlaying) "Pause" else "Play",
            pipActionIntent(MediaNotificationService.ACTION_PLAY_PAUSE)
        )
        val next = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_skip_next),
            "Next", "Next",
            pipActionIntent(MediaNotificationService.ACTION_NEXT)
        )
        return listOf(prev, playPause, next)
    }

    private fun pipActionIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    private fun showPipVideoOverlay() {
        if (pipOverlay != null) return
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }
        pipVideoView?.let {
            overlay.addView(
                it,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        pipOverlay = overlay
        content.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun showPipOverlay() {
        if (pipOverlay != null) return
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(pipCoverBitmap)
        }
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            addView(
                img,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        pipCoverImg = img
        pipOverlay = overlay
        content.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun hidePipOverlay() {
        pipOverlay?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        pipOverlay = null
        pipCoverImg = null
    }

    private fun handleMediaStatus(json: String) {
        try {
            val obj = JSONObject(json)
            pipPlaying = obj.optBoolean("playing", false)
            val coverUrl = obj.optString("cover", "")
            if (coverUrl.isNotEmpty() && coverUrl != "null" && coverUrl != lastPipCoverUrl) {
                lastPipCoverUrl = coverUrl
                if (pipUsed || isInPictureInPictureMode) {
                    fetchPipCover(coverUrl)
                } else {
                    pipCoverBitmap = null
                }
            }
            runOnUiThread { updatePipParams() }
        } catch (_: Exception) {}
    }

    private fun fetchPipCover(url: String) {
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val raw = BitmapFactory.decodeStream(conn.inputStream)
                if (raw != null) {
                    val target = 1024
                    val scale = min(target.toFloat() / raw.width, target.toFloat() / raw.height)
                    val w = (raw.width * scale).toInt().coerceAtLeast(1)
                    val h = (raw.height * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    if (scaled != raw) raw.recycle()
                    pipCoverBitmap = scaled
                    runOnUiThread {
                        pipCoverImg?.setImageBitmap(scaled)
                        updatePipParams()
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }.start()
    }

    private fun wireDownloadCallbacks() {
        DownloadManager.onStatus = { msg ->
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
        DownloadManager.onProgress = { pct, label ->
            runOnUiThread {
                val safe = label.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
                val batch = DownloadManager.isBatchActive()
                webView?.evaluateJavascript(
                    "window.__splDlBatch=$batch;window.splDownloadProgress($pct, '$safe')",
                    null
                )
            }
        }
    }

    private fun startDownloadService() {
        runCatching {
            ContextCompat.startForegroundService(
                this, Intent(this, com.project.lol.service.DownloadService::class.java)
            )
        }
    }

    private fun destroyWebView() {
        pipVideoTimeout.removeCallbacksAndMessages(null)
        pipVideoView = null
        pipVideoCallback = null
        pipVideoPending = false
        hidePipOverlay()
        webView?.let {
            it.stopLoading()
            it.removeJavascriptInterface("AndBridge")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)) {
                try {
                    WebViewCompat.getWebViewRenderProcess(it)?.terminate()
                } catch (_: Exception) {}
            }
            // Must detach from its parent before destroy()
            // Otherwise the WebView and its renderer will be left in a bad state;
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
    }

    private fun applyOrientation() {
        requestedOrientation = if (landscapeModeState.value) {
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

    private fun startMediaService() {
        if (MediaNotificationService.instance != null) {
            MediaNotificationService.webView = webView
            return
        }
        if (serviceStarted) return
        serviceStarted = true
        MediaNotificationService.webView = webView
        val intent = Intent(this, MediaNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                requestNotificationPermission()
            }
        } else {
            requestNotificationPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        webView?.evaluateJavascript("""
            try {
                window.__splBg = true;
                if(typeof pfint !== 'undefined' && pfint) { clearInterval(pfint); pfint = null; window.__splWasPfint = true; }
                if(typeof afint !== 'undefined' && afint) { clearInterval(afint); afint = null; window.__splWasAfint = true; }
                if(typeof cssint !== 'undefined' && cssint) { clearInterval(cssint); cssint = null; window.__splWasCssint = true; }
            } catch(e) {}
        """.trimIndent(), null)
    }

    override fun onResume() {
        super.onResume()

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        })

        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        hideTopBarState.value = prefs.getBoolean("HideTopBar", false)
        landscapeModeState.value = prefs.getBoolean("LandscapeMode", false)
        keepScreenOnState.value = prefs.getBoolean("KeepScreenOn", false)
        paletteSeedState.value = prefs.getString("PaletteSeed", null)
        blockServiceWorkerState.value = prefs.getBoolean("BlockServiceWorker", true)
        applyOrientation()
        applyKeepScreenOn()

        val customCss = prefs.getString("CustomCss", "") ?: ""
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)

        webView?.let { view ->
            view.evaluateJavascript("""
                try {
                    window.__splBg = false;
                    if(window.__splWasPfint) { window.__splWasPfint = false; firstFuck(); }
                    if(window.__splWasAfint) { window.__splWasAfint = false; addAutoFeatures(); }
                    if(window.__splWasCssint) { window.__splWasCssint = false; addCSSJSHack(); }
                    if(window.autoPlayMode === 'onetime') {
                        window.__splApDone = false;
                        window.__splApActive = false;
                        if(typeof splAutoPlay === 'function') splAutoPlay();
                    }
                } catch(e) {}
            """.trimIndent(), null)

            val js = buildString {
                append(buildAmoledJs(amoledEnabled))
                append(AccentTheme.buildAccentJs(this@MainActivity))
                append(buildCustomCssJs(customCss))
                append(LyricsTheme.buildLyricsStyleJs(prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE))
            }
            view.evaluateJavascript(js, null)

            view.evaluateJavascript(LogoutCheck.CONTENT) { result ->
                if (result == "\"out\"") {
                    prefs.edit().putBoolean("LoggedIn", false).apply()
                    view.loadUrl("https://accounts.spotify.com/login")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val loggedIn = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .getBoolean("LoggedIn", false)
        if (!loggedIn) {
            webView?.loadUrl("https://accounts.spotify.com/login")
        }
    }

    override fun onDestroy() {
        cancelSleepTimer()
        pipVideoTimeout.removeCallbacksAndMessages(null)
        pipVideoView = null
        pipVideoCallback = null
        pipVideoPending = false
        hidePipOverlay()
        webView?.let {
            it.stopLoading()
            it.clearHistory()
            it.clearCache(true)
            it.clearFormData()
            it.removeJavascriptInterface("AndBridge")
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
        serviceStarted = false
        super.onDestroy()
    }
}
