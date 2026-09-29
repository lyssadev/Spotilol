package com.project.lol.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.project.lol.BuildConfig
import com.project.lol.R
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.ui.theme.SpotifyTheme
import com.project.lol.util.BuildInfo
import compose.icons.TablerIcons
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Bluetooth
import compose.icons.tablericons.Language
import compose.icons.tablericons.ShieldLock
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MonochromeAccent = Color(0xFFE0E0E0)

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        super.onCreate(savedInstanceState)

        requestedOrientation = if (
            getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
                .getBoolean("LandscapeMode", false)
        ) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        FirebaseCrashlytics.getInstance()
        // Analytics/Performance are not needed for first frame; init off the main thread.
        lifecycleScope.launch(Dispatchers.Default) {
            FirebasePerformance.getInstance()
            FirebaseAnalytics.getInstance(this@SplashActivity)
                .logEvent(FirebaseAnalytics.Event.APP_OPEN, Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, "Spotilol")
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SplashActivity")
                })
        }

        setContent {
            val prefs = remember { getSharedPreferences("spotilol_prefs", MODE_PRIVATE) }
            var intro by remember { mutableStateOf(true) }
            var onboarding by remember { mutableStateOf(false) }
            var onboardingStep by remember { mutableIntStateOf(0) }
            var selectedMode by remember { mutableStateOf("normal") }
            var certInstalled by remember { mutableStateOf(false) }
            var checkDone by remember { mutableStateOf(false) }
            var checking by remember { mutableStateOf(false) }
            var checkTrigger by remember { mutableIntStateOf(0) }
            var exiting by remember { mutableStateOf(false) }
            var contentAlpha by remember { mutableFloatStateOf(1f) }
            val onboardingAppear = remember { Animatable(0f) }
            var onboardingLeaving by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { onboardingStep = 1 }

            LaunchedEffect(onboarding) {
                if (onboarding) {
                    onboardingLeaving = false
                    onboardingAppear.snapTo(0f)
                    onboardingAppear.animateTo(1f, tween(340, easing = LinearOutSlowInEasing))
                }
            }

            LaunchedEffect(Unit) {
                if (prefs.getBoolean("OfflineMode", false)) {
                    startActivity(Intent(this@SplashActivity, OfflineActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    return@LaunchedEffect
                }
                intro = false
                if (prefs.getBoolean("OnboardingDone", false)) {
                    checking = true
                    checkTrigger++
                } else {
                    onboarding = true
                }
            }

            LaunchedEffect(checkTrigger) {
                if (checkTrigger == 0) return@LaunchedEffect
                withContext(Dispatchers.IO) {
                    if (prefs.getString("ConnectionMode", "normal") == "proxy") {
                        LocalProxyManager.init(this@SplashActivity)
                        LocalProxyManager.start()
                        awaitProxyBound()
                        certInstalled = LocalProxyManager.isCAInstalled()
                    } else {
                        LocalProxyManager.stop()
                        certInstalled = true
                    }
                    checkDone = true
                    checking = false
                }
            }

            LaunchedEffect(certInstalled, checkDone) {
                if (checkDone && certInstalled && !exiting) {
                    exiting = true
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = tween(150, easing = LinearEasing)
                    ) { value, _ -> contentAlpha = value }
                    val linkIntent = intent?.takeIf { it.action == Intent.ACTION_VIEW && it.data != null }
                    startActivity(
                        if (linkIntent != null) {
                            Intent(linkIntent).setClass(this@SplashActivity, MainActivity::class.java)
                        } else {
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }
                    )
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            }

            SpotifyTheme {
                Box(modifier = Modifier.graphicsLayer { alpha = contentAlpha }) {
                    when {
                        intro || checking -> LoadingScreen()
                        onboarding -> OnboardingScreen(
                            modifier = Modifier.graphicsLayer {
                                alpha = onboardingAppear.value
                                translationY = (1f - onboardingAppear.value) * 28.dp.toPx()
                            },
                            step = onboardingStep,
                            mode = selectedMode,
                            onAccept = {
                                val required = requiredPermissions()
                                if (required.isEmpty()) {
                                    onboardingStep = 1
                                } else {
                                    permissionLauncher.launch(required.toTypedArray())
                                }
                            },
                            onMode = { selectedMode = it },
                            onGo = {
                                if (!onboardingLeaving) {
                                    onboardingLeaving = true
                                    prefs.edit()
                                        .putBoolean("OnboardingDone", true)
                                        .putString("ConnectionMode", selectedMode)
                                        .apply()
                                    scope.launch {
                                        onboardingAppear.animateTo(0f, tween(200, easing = LinearEasing))
                                        onboarding = false
                                        checking = true
                                        checkTrigger++
                                    }
                                }
                            }
                        )
                        !certInstalled -> {
                            var certAlpha by remember { mutableStateOf(0f) }
                            LaunchedEffect(Unit) {
                                animate(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = tween(1300, easing = LinearEasing)
                                ) { value, _ -> certAlpha = value }
                            }
                            CACertScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .graphicsLayer { alpha = certAlpha },
                                onSwitchNormal = {
                                    getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("ConnectionMode", "normal")
                                        .putBoolean("ServiceOn", false)
                                        .apply()
                                    LocalProxyManager.stop()
                                    recreate()
                                },
                                onCheck = {
                                    checking = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            if (!LocalProxyManager.isRunning) {
                                                LocalProxyManager.start()
                                                awaitProxyBound()
                                            }
                                            certInstalled = LocalProxyManager.isCAInstalled()
                                        }
                                        checking = false
                                    }
                                },
                                onExport = {
                                    scope.launch {
                                        val path = withContext(Dispatchers.IO) {
                                            LocalProxyManager.exportCACert(this@SplashActivity)
                                        }
                                        Toast.makeText(
                                            this@SplashActivity,
                                            this@SplashActivity.getString(R.string.splash_exported_to, path),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /** start() binds asynchronously; wait for the socket instead of a fixed sleep. */
    private suspend fun awaitProxyBound(timeoutMs: Long = 2000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!LocalProxyManager.isRunning && System.currentTimeMillis() < deadline) delay(25)
    }

    private fun requiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        return permissions
    }
}

@Composable
private fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val titleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(R.string.splash_title),
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = titleAlpha },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Text(
            text = stringResource(R.string.splash_version_label, BuildConfig.VERSION_NAME, BuildInfo.id),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 28.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun CACertScreen(
    modifier: Modifier = Modifier,
    onSwitchNormal: () -> Unit,
    onCheck: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.splash_cert_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.splash_cert_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.06f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Step(1, stringResource(R.string.splash_cert_step_1))
                Spacer(Modifier.height(16.dp))
                Step(2, stringResource(R.string.splash_cert_step_2))
                Spacer(Modifier.height(16.dp))
                Step(3, stringResource(R.string.splash_cert_step_3))
                Spacer(Modifier.height(16.dp))
                Step(4, stringResource(R.string.splash_cert_step_4))
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onExport,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.splash_cert_export_button), color = Color.White.copy(alpha = 0.7f))
                }
            }

            Surface(
                onClick = onCheck,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.splash_cert_check_button), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            onClick = onSwitchNormal,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.06f)
        ) {
            Box(
                modifier = Modifier.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.splash_cert_switch_normal), color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun OnboardingScreen(
    modifier: Modifier = Modifier,
    step: Int,
    mode: String,
    onAccept: () -> Unit,
    onMode: (String) -> Unit,
    onGo: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (
                    fadeIn(tween(280, easing = LinearOutSlowInEasing)) +
                        slideInVertically(tween(280, easing = LinearOutSlowInEasing)) { it / 12 }
                    ) togetherWith (
                    fadeOut(tween(160, easing = LinearEasing)) +
                        slideOutVertically(tween(160, easing = LinearEasing)) { -it / 12 }
                    )
            },
            label = "onboardingStep"
        ) { current ->
            OnboardingPhase(
                step = current,
                mode = mode,
                onAccept = onAccept,
                onMode = onMode,
                onGo = onGo
            )
        }
    }
}

@Composable
private fun OnboardingPhase(
    step: Int,
    mode: String,
    onAccept: () -> Unit,
    onMode: (String) -> Unit,
    onGo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.splash_onboarding_step, step + 1, 2),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(
                if (step == 0) R.string.splash_onboarding_welcome else R.string.splash_onboarding_mode_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(
                if (step == 0) R.string.splash_onboarding_permissions_subtitle
                else R.string.splash_onboarding_mode_subtitle
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f),
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(22.dp))

        if (step == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OnboardingItem(
                    icon = TablerIcons.Bell,
                    title = stringResource(R.string.splash_onboarding_notifications_title),
                    description = stringResource(R.string.splash_onboarding_notifications_desc)
                )
                Spacer(Modifier.height(10.dp))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OnboardingItem(
                    icon = TablerIcons.Bluetooth,
                    title = stringResource(R.string.splash_onboarding_bluetooth_title),
                    description = stringResource(R.string.splash_onboarding_bluetooth_desc)
                )
            }
            Spacer(Modifier.height(26.dp))
            OnboardingAction(
                label = stringResource(R.string.splash_onboarding_accept),
                onClick = onAccept
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingMode(
                    modifier = Modifier.weight(1f),
                    icon = TablerIcons.Language,
                    title = stringResource(R.string.splash_onboarding_mode_normal_title),
                    description = stringResource(R.string.splash_onboarding_mode_normal_desc),
                    selected = mode == "normal",
                    onClick = { onMode("normal") }
                )
                OnboardingMode(
                    modifier = Modifier.weight(1f),
                    icon = TablerIcons.ShieldLock,
                    title = stringResource(R.string.splash_onboarding_mode_cert_title),
                    description = stringResource(R.string.splash_onboarding_mode_cert_desc),
                    selected = mode == "proxy",
                    onClick = { onMode("proxy") }
                )
            }
            Spacer(Modifier.height(26.dp))
            OnboardingAction(
                label = stringResource(R.string.splash_onboarding_go),
                onClick = onGo
            )
        }
    }
}


@Composable
private fun OnboardingItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun OnboardingMode(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun OnboardingAction(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

