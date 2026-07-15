package com.project.lol.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.project.lol.R
import com.project.lol.bridge.SpotifyBridge
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.service.MediaNotificationService
import com.project.lol.webview.SpotifyWebChromeClient
import com.project.lol.webview.SpotifyWebViewClient
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

private val MonochromeHeader = Color(0xFF000000)
private val MonochromeText = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var serviceStarted = false

    private val serviceEnabledState = mutableStateOf(true)
    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("LoggedIn", false)

        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)

        setContent {
            val serviceEnabled = serviceEnabledState.value
            val materialYou = materialYouState.value
            val amoled = amoledState.value

            SpotifyTheme(useDynamicColor = materialYou, amoled = amoled) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Spotilol",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_settings),
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = { newValue ->
                                        serviceEnabledState.value = newValue
                                        prefs.edit()
                                            .putBoolean("ServiceOn", newValue)
                                            .apply()
                                        if (!newValue) {
                                            stopService(Intent(this@MainActivity, MediaNotificationService::class.java))
                                            serviceStarted = false
                                            destroyWebView()
                                        }
                                    },
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
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    if (serviceEnabled) {
                        val bridge = remember {
                            SpotifyBridge(WeakReference(this@MainActivity))
                        }

                        BackHandler(enabled = webView?.canGoBack() == true) {
                            webView?.goBack()
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
                                        databaseEnabled = true
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
                                    webChromeClient = SpotifyWebChromeClient()

                                    webViewClient = SpotifyWebViewClient(
                                        onLoginRequired = {
                                            loadUrl("https://accounts.spotify.com/login")
                                        }
                                    )

                                    if (LocalProxyManager.isRunning) {
                                        val executor = Executors.newSingleThreadExecutor()
                                        val proxyConfig = ProxyConfig.Builder()
                                            .addProxyRule("localhost:${LocalProxyManager.port}")
                                            .build()
                                        ProxyController.getInstance().setProxyOverride(
                                            proxyConfig,
                                            executor,
                                            { }
                                        )
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
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        )

                        LaunchedEffect(webView) {
                            webView?.let { startMediaService() }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }
        }
    }

    private fun destroyWebView() {
        webView?.let {
            it.stopLoading()
            it.removeJavascriptInterface("AndBridge")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)) {
                try {
                    WebViewCompat.getWebViewRenderProcess(it)?.terminate()
                } catch (_: Exception) {}
            }
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
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

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)

        val customCss = prefs.getString("CustomCss", "") ?: ""
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)

        webView?.let { view ->
            val js = SpotifyWebViewClient.buildAmoledJs(amoledEnabled) + "\n" +
                    SpotifyWebViewClient.buildCustomCssJs(customCss)
            view.evaluateJavascript(js, null)
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
        webView?.let {
            it.stopLoading()
            it.clearHistory()
            it.clearCache(true)
            it.clearFormData()
            it.removeJavascriptInterface("AndBridge")
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
        serviceStarted = false
        super.onDestroy()
    }
}

@Composable
fun SpotifyTheme(
    useDynamicColor: Boolean = false,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        }
        else -> androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFE0E0E0),
            onPrimary = Color(0xFF121212),
            primaryContainer = Color(0xFF2E2E2E),
            onPrimaryContainer = Color(0xFFF5F5F5),
            inversePrimary = Color(0xFF121212),
            secondary = Color(0xFFCCCCCC),
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF262626),
            onSecondaryContainer = Color(0xFFE0E0E0),
            tertiary = Color(0xFFB0B0B0),
            onTertiary = Color(0xFF181818),
            tertiaryContainer = Color(0xFF202020),
            onTertiaryContainer = Color(0xFFD6D6D6),
            outline = Color(0xFF767676),
            outlineVariant = Color(0xFF444444)
        )
    }

    val colorScheme = if (amoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0F0F0F),
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerLowest = Color.Black,
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
