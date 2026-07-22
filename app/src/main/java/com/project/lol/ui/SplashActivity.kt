package com.project.lol.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.ui.theme.SpotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MonochromeAccent = Color(0xFFE0E0E0)

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var certInstalled by remember { mutableStateOf(false) }
            var checkDone by remember { mutableStateOf(false) }
            var checking by remember { mutableStateOf(true) }
            var exiting by remember { mutableStateOf(false) }
            var contentAlpha by remember { mutableFloatStateOf(1f) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    LocalProxyManager.init(this@SplashActivity)
                    LocalProxyManager.start()
                    delay(800)
                    certInstalled = LocalProxyManager.isCAInstalled()
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
                        animationSpec = tween(500, easing = LinearEasing)
                    ) { value, _ -> contentAlpha = value }
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            }

            SpotifyTheme {
                Box(modifier = Modifier.graphicsLayer { alpha = contentAlpha }) {
                    when {
                        checking -> LoadingScreen()
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
                                onCheck = {
                                    checking = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            if (!LocalProxyManager.isRunning) {
                                                LocalProxyManager.start()
                                                delay(500)
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
                                            "Exported to: $path",
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
}

@Composable
private fun LoadingScreen() {
    val trackColor = Color.White.copy(alpha = 0.15f)
    val barColor = Color.White
    val totalDuration = 2000
    val holdDuration = 150
    val slideDuration = totalDuration - holdDuration * 2

    val infiniteTransition = rememberInfiniteTransition(label = "slide")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDuration
                0f at 0
                0f at holdDuration
                1f at holdDuration + slideDuration using LinearEasing
                1f at totalDuration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = size.width * 4f * progress - size.width
                        }
                        .background(barColor)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "v$versionName",
                color = Color.White.copy(alpha = 0.35f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CACertScreen(
    modifier: Modifier = Modifier,
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
            text = "Certificate Required",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Install the Spotilol CA certificate to continue.",
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
                Step(1, "Export the .pem certificate below")
                Spacer(Modifier.height(16.dp))
                Step(2, "Settings > Security > Install a certificate > CA certificate")
                Spacer(Modifier.height(16.dp))
                Step(3, "Select the exported file and tap \"Install anyway\"")
                Spacer(Modifier.height(16.dp))
                Step(4, "Return here and tap \"Check\"")
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
                    Text("Export", color = Color.White.copy(alpha = 0.7f))
                }
            }

            Surface(
                onClick = onCheck,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1DB954)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Check", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
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
