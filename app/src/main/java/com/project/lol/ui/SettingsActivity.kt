package com.project.lol.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.project.lol.R
import com.project.lol.ui.theme.SpotifyTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.webkit.WebViewCompat
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.update.UpdateChecker

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)

        setContent {
            var materialYou by remember { mutableStateOf(prefs.getBoolean("MaterialYou", false)) }
            var amoledTheme by remember { mutableStateOf(prefs.getBoolean("AmoledTheme", false)) }
            SpotifyTheme(useDynamicColor = materialYou, amoled = amoledTheme) {
                SettingsScreen(
                    prefs = prefs,
                    materialYou = materialYou,
                    onMaterialYouChange = { materialYou = it },
                    amoledThemeState = amoledTheme,
                    onAmoledThemeChange = { amoledTheme = it },
                    onBack = { finish() },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() }
                )
            }
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
        Toast.makeText(this, "All data cleared, please login again", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    materialYou: Boolean,
    onMaterialYouChange: (Boolean) -> Unit,
    amoledThemeState: Boolean,
    onAmoledThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit
) {
    var autoplayMode by remember { mutableStateOf(prefs.getString("APlayMode", "disabled") ?: "disabled") }
    var takeControl by remember { mutableStateOf(prefs.getBoolean("TakeControl", true)) }
    var andAuto by remember { mutableStateOf(prefs.getBoolean("AndAuto", true)) }
    var closeNowPlay by remember { mutableStateOf(prefs.getBoolean("CloseNowPlay", true)) }
    var guiMode by remember { mutableStateOf(prefs.getString("GuiMode", "csshack") ?: "csshack") }
    var customCss by remember { mutableStateOf(prefs.getString("CustomCss", "") ?: "") }
    var amoledTheme by remember { mutableStateOf(amoledThemeState) }
    var swipeStop by remember { mutableStateOf(prefs.getBoolean("SwipeStop", true)) }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAutoPlayDialog by remember { mutableStateOf(false) }
    var showGuiModeDialog by remember { mutableStateOf(false) }
    var showCustomCssDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingSectionCard(
                title = "PLAYER",
                icon = Icons.Default.PlayCircle
            ) {
                val autoplayLabel = when (autoplayMode) {
                    "disabled" -> "Disabled"
                    "onetime" -> "One time at start"
                    "permanent" -> "Permanent"
                    else -> "One time at start"
                }
                SettingTile(
                    title = "AutoPlay Mode",
                    subtitle = autoplayLabel,
                    icon = Icons.Default.PlayCircle,
                    onClick = { showAutoPlayDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Take Player Control",
                    subtitle = "Auto-accept 'Take Control' prompt",
                    icon = Icons.Default.TouchApp,
                    checked = takeControl,
                    onCheckedChange = {
                        takeControl = it
                        prefs.edit().putBoolean("TakeControl", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Android Auto Controls",
                    subtitle = "Media metadata for notifications",
                    icon = Icons.Default.DirectionsCar,
                    checked = andAuto,
                    onCheckedChange = {
                        andAuto = it
                        prefs.edit().putBoolean("AndAuto", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Always Close Now Playing",
                    subtitle = "Auto-close the Now Playing panel",
                    icon = Icons.Default.CloseFullscreen,
                    checked = closeNowPlay,
                    onCheckedChange = {
                        closeNowPlay = it
                        prefs.edit().putBoolean("CloseNowPlay", it).apply()
                    }
                )
            }

            SettingSectionCard(
                title = "APPEARANCE",
                icon = Icons.Default.Palette
            ) {
                val guiLabel = when (guiMode) {
                    "csshack" -> "Mobile CSS + JS"
                    "bigwindow" -> "Wide Window"
                    "none" -> "None"
                    else -> "Mobile CSS + JS"
                }
                SettingTile(
                    title = "GUI Hack Mode",
                    subtitle = guiLabel,
                    icon = Icons.Default.Palette,
                    onClick = { showGuiModeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Custom CSS",
                    subtitle = if (customCss.isBlank()) "None configured" else customCss,
                    icon = Icons.Default.Code,
                    onClick = { showCustomCssDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Material You Theme",
                    subtitle = "Use Android system dynamic colors",
                    icon = Icons.Default.ColorLens,
                    checked = materialYou,
                    onCheckedChange = { enabled ->
                        onMaterialYouChange(enabled)
                        prefs.edit().putBoolean("MaterialYou", enabled).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "AMOLED Theme",
                    subtitle = "Pure black background (saves battery)",
                    icon = Icons.Default.DarkMode,
                    checked = amoledTheme,
                    onCheckedChange = { enabled ->
                        amoledTheme = enabled
                        onAmoledThemeChange(enabled)
                        prefs.edit().putBoolean("AmoledTheme", enabled).apply()
                    }
                )
            }

            SettingSectionCard(
                title = "SECURITY & NETWORK",
                icon = Icons.Default.Shield
            ) {
                val context = LocalContext.current
                SettingTile(
                    title = "CA Certificate",
                    subtitle = "Re-export proxy certificate to Downloads",
                    icon = Icons.Default.Shield,
                    onClick = {
                        val path = LocalProxyManager.exportCACert(context)
                        Toast.makeText(context, "Exported to $path", Toast.LENGTH_LONG).show()
                    }
                )
            }

            SettingSectionCard(
                title = "SYSTEM",
                icon = Icons.Default.PowerSettingsNew
            ) {
                SettingSwitchTile(
                    title = "Swipe to Stop Service",
                    subtitle = "Kill background service from recents",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = swipeStop,
                    onCheckedChange = {
                        swipeStop = it
                        prefs.edit().putBoolean("SwipeStop", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Empty Cache",
                    subtitle = "Useful if player navigation is slow",
                    icon = Icons.Default.CleaningServices,
                    onClick = { showClearCacheDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Empty Cache & Login Data",
                    subtitle = "Clear everything and log out",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            val updateContext = LocalContext.current
            val uc = remember { UpdateChecker(updateContext) }
            var hasUpdate by remember { mutableStateOf(uc.hasUpdateAvailable()) }
            SettingSectionCard(
                title = "UPDATES",
                icon = Icons.Default.SystemUpdate
            ) {
                SettingTile(
                    title = "Check for Updates",
                    subtitle = "Manually check for new releases",
                    icon = Icons.Default.SystemUpdate,
                    showBadge = hasUpdate,
                    onClick = {
                        uc.clearUpdateAvailable()
                        hasUpdate = false
                    }
                )
            }

            val context = LocalContext.current
            val pkg = remember { WebViewCompat.getCurrentWebViewPackage(context) }
            val packageInfo = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }.getOrNull()
            }
            val appVersionName = packageInfo?.versionName ?: "1.0.0"

            SettingSectionCard(
                title = "ABOUT",
                icon = Icons.Default.Info
            ) {
                SettingTile(
                    title = "GitHub Repository",
                    subtitle = "github.com/lyssadev/Spotilol",
                    painter = painterResource(id = R.drawable.ic_github),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lyssadev/Spotilol"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Spotilol Version",
                    subtitle = "v$appVersionName",
                    icon = Icons.Default.Smartphone,
                    onClick = {}
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "WebView Engine",
                    subtitle = pkg?.versionName ?: "System WebView",
                    icon = Icons.Default.Language,
                    onClick = {}
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Developed by lyssadev & reversed Spotifuck app by Deviato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAutoPlayDialog) {
        SingleChoiceDialog(
            title = "AutoPlay Mode",
            options = listOf(
                "disabled" to "Disabled",
                "onetime" to "One time at start",
                "permanent" to "Permanent"
            ),
            selected = autoplayMode,
            onSelect = { value ->
                autoplayMode = value
                prefs.edit().putString("APlayMode", value).apply()
            },
            onDismiss = { showAutoPlayDialog = false }
        )
    }

    if (showGuiModeDialog) {
        SingleChoiceDialog(
            title = "GUI Hack Mode",
            options = listOf(
                "csshack" to "Mobile CSS + JS",
                "bigwindow" to "Wide Window",
                "none" to "None"
            ),
            selected = guiMode,
            onSelect = { value ->
                guiMode = value
                prefs.edit().putString("GuiMode", value).apply()
            },
            onDismiss = { showGuiModeDialog = false }
        )
    }

    if (showCustomCssDialog) {
        CustomCssDialog(
            initialCss = customCss,
            onSave = { css ->
                customCss = css
                prefs.edit().putString("CustomCss", css).apply()
                showCustomCssDialog = false
            },
            onDismiss = { showCustomCssDialog = false }
        )
    }

    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Empty Cache",
            message = "This will clear the WebView cache. Continue?",
            confirmText = "Clear Cache",
            onConfirm = {
                showClearCacheDialog = false
                onClearCache()
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showClearDataDialog) {
        ConfirmationDialog(
            title = "Empty Cache & Login Data",
            message = "All cookies and login data will be deleted. On restart you will need to log in again. Continue?",
            confirmText = "Clear All Data",
            isDestructive = true,
            onConfirm = {
                showClearDataDialog = false
                onClearData()
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
fun SettingSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingTile(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    showBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Red, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingSwitchTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    val isSelected = selected == value
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun CustomCssDialog(
    initialCss: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempCss by remember { mutableStateOf(initialCss) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Custom CSS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Injected custom CSS rules into Spotify webview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = tempCss,
                    onValueChange = { tempCss = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = {
                        Text(
                            "/* Write your CSS overrides here (use !important if needed) */\naside[data-testid=now-playing-bar] { display: none !important; }",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(tempCss) }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE) }
    SpotifyTheme {
        SettingsScreen(
            prefs = prefs,
            materialYou = false,
            onMaterialYouChange = {},
            amoledThemeState = false,
            onAmoledThemeChange = {},
            onBack = {},
            onClearCache = {},
            onClearData = {}
        )
    }
}
