package com.project.lol.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.project.lol.ui.theme.SpotifyTheme
import com.project.lol.update.UpdateChecker
import com.project.lol.webview.SpotifyWebChromeClient
import com.project.lol.webview.SpotifyWebViewClient
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var serviceStarted = false

    private val serviceEnabledState = mutableStateOf(true)
    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)

    private val showSleepTimerDialog = mutableStateOf(false)
    private val sleepTimerSelectedMinutes = mutableIntStateOf(0)
    private var sleepTimer: CountDownTimer? = null
    private val sleepTimerRemainingMs = mutableLongStateOf(0L)
    private val sleepTimerActive = mutableStateOf(false)

    private val loadingProgress = mutableIntStateOf(100)
    private val updateAvailable = mutableStateOf(false)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val btPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val sleepTimerOptions = listOf(
        0 to "Off",
        5 to "5 min",
        10 to "10 min",
        15 to "15 min",
        30 to "30 min",
        60 to "60 min"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestNotificationPermission()
        requestBluetoothPermission()
        val uc = UpdateChecker(this)
        updateAvailable.value = uc.hasUpdateAvailable()
        uc.autoCheck()

        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("LoggedIn", false)

        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)

        setContent {
            val serviceEnabled = serviceEnabledState.value
            val materialYou = materialYouState.value
            val amoled = amoledState.value
            val showDialog = showSleepTimerDialog.value
            val timerActive = sleepTimerActive.value
            val loadProgress = loadingProgress.intValue

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
                                    Box {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_settings),
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (updateAvailable.value) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Red, CircleShape)
                                            )
                                        }
                                    }
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
                                    sleepTimerSelectedMinutes.intValue = 0
                                }
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
                                            }
                                        )

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
                                    color = Color(0xFF22DD66),
                                    trackColor = Color.Transparent,
                                )
                            }

                            if (showDialog) {
                                SleepTimerDialog(
                                    timerActive = timerActive,
                                    timerRemainingMs = sleepTimerRemainingMs.longValue,
                                    selectedMinutes = sleepTimerSelectedMinutes.intValue,
                                    onSelectMinute = { sleepTimerSelectedMinutes.intValue = it },
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
                    }
                }
            }
        }
    }

    private fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L
        sleepTimerActive.value = true
        sleepTimerRemainingMs.longValue = totalMs

        webView?.evaluateJavascript(
            "if(window.timerBtn) timerBtn.style.color='#2d6';",
            null
        )

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerRemainingMs.longValue = millisUntilFinished
            }

            override fun onFinish() {
                sleepTimerActive.value = false
                sleepTimerRemainingMs.longValue = 0L
                webView?.evaluateJavascript(
                    "if(window.timerBtn) timerBtn.style.color='';",
                    null
                )
                webView?.evaluateJavascript("actPlayPause(false)", null)
            }
        }.start()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        sleepTimerActive.value = false
        sleepTimerRemainingMs.longValue = 0L
        webView?.evaluateJavascript(
            "if(window.timerBtn) timerBtn.style.color='';",
            null
        )
    }

    @Composable
    private fun SleepTimerDialog(
        timerActive: Boolean,
        timerRemainingMs: Long,
        selectedMinutes: Int,
        onSelectMinute: (Int) -> Unit,
        onSetTimer: (Int) -> Unit,
        onCancelTimer: () -> Unit,
        onDismiss: () -> Unit
    ) {
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
                    Column(Modifier.selectableGroup()) {
                        sleepTimerOptions.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = selectedMinutes == minutes,
                                        onClick = { onSelectMinute(minutes) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMinutes == minutes,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
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
                            onClick = { onSetTimer(selectedMinutes) },
                            enabled = selectedMinutes > 0
                        ) {
                            Text(
                                if (selectedMinutes > 0) "Set Timer" else "Off"
                            )
                        }
                    }
                },
                dismissButton = {}
            )
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
            // Must detach from its parent before destroy()
            // Otherwise the WebView and its renderer will be left in a bad state;
            (it.parent as? ViewGroup)?.removeView(it)
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

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        webView?.evaluateJavascript("""
            try {
                document.querySelectorAll('video').forEach(function(v) {
                    if(v.muted || v.hasAttribute('loop') || v.style.objectFit === 'cover') {
                        v.pause();
                        v.removeAttribute('src');
                        v.load();
                    }
                });
            } catch(e) {}
        """.trimIndent(), null)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)

        val customCss = prefs.getString("CustomCss", "") ?: ""
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)

        webView?.let { view ->
            val js = buildString {
                append("window.closeNpPref=$closeNowPlay;\n")
                append(SpotifyWebViewClient.buildAmoledJs(amoledEnabled))
                append("\n")
                append(SpotifyWebViewClient.buildCustomCssJs(customCss))
            }
            view.evaluateJavascript(js, null)

            view.evaluateJavascript(SpotifyWebViewClient.LOGOUT_CHECK_JS) { result ->
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
