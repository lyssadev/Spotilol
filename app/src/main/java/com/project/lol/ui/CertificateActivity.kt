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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lol.proxy.LocalProxyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MonochromeAccent = Color(0xFFE0E0E0)
private val MonochromeText = Color(0xFFFFFFFF)

class CertificateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var certInstalled by remember { mutableStateOf(false) }
            var checkDone by remember { mutableStateOf(false) }
            var checking by remember { mutableStateOf(true) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    LocalProxyManager.init(this@CertificateActivity)
                    LocalProxyManager.start()
                    delay(800)
                    certInstalled = LocalProxyManager.isCAInstalled()
                    checkDone = true
                    checking = false
                }
            }

            LaunchedEffect(certInstalled, checkDone) {
                if (checkDone && certInstalled) {
                    startActivity(Intent(this@CertificateActivity, MainActivity::class.java))
                    finish()
                }
            }

            SpotifyTheme {
                when {
                    checking -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MonochromeAccent)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Checking certificate...",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    !certInstalled -> {
                        CACertDialog(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            onInstall = {
                                LocalProxyManager.installCACert(this@CertificateActivity)
                            },
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
                                        LocalProxyManager.exportCACert(this@CertificateActivity)
                                    }
                                    Toast.makeText(
                                        this@CertificateActivity,
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

@Composable
fun CACertDialog(
    modifier: Modifier = Modifier,
    onInstall: () -> Unit,
    onCheck: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Certificate Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MonochromeAccent,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Spotilol uses a local certificate to bypass Spotify's WebView restrictions. Install it once to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Step(1, "Tap \"Export .pem\" below to save the certificate to Downloads")
                Spacer(Modifier.height(12.dp))
                Step(2, "Go to Settings > Security > Encryption & credentials > Install a certificate > CA certificate")
                Spacer(Modifier.height(12.dp))
                Step(3, "Select the file Spotilol_CA.pem from your Downloads folder")
                Spacer(Modifier.height(12.dp))
                Step(4, "When prompted, choose \"Install anyway\"")
                Spacer(Modifier.height(12.dp))
                Step(5, "Come back here and tap \"Check\" below")
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onExport,
                modifier = Modifier.weight(1f)
            ) {
                Text("Export .pem", color = MonochromeAccent)
            }

            TextButton(
                onClick = onCheck,
                modifier = Modifier.weight(1f)
            ) {
                Text("Check", color = Color(0xFF1DB954))
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MonochromeAccent,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 18.sp
        )
    }
}
