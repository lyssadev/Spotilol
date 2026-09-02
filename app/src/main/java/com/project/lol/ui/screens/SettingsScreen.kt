package com.project.lol.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.webkit.WebViewCompat
import com.project.lol.R
import com.project.lol.profile.ProfileManager
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.ui.theme.SpotifyTheme
import com.project.lol.util.DebugLogStore
import com.project.lol.util.GitHubApi
import com.project.lol.util.GitHubRelease
import com.project.lol.util.MarkdownText
import com.project.lol.webview.helpers.LyricsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PalettePresets = listOf(
    "Default" to null,
    "Spotify Green" to "#1DB954",
    "Purple" to "#BB86FC",
    "Blue" to "#2196F3",
    "Red" to "#E53935",
    "Orange" to "#FB8C00",
    "Pink" to "#EC407A",
    "Teal" to "#26A69A",
    "Yellow" to "#FDD835",
    "Cyan" to "#00BCD4"
)

private fun parsePaletteColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}

private fun formatHex(color: Color): String {
    val r = (color.red * 255f).roundToInt()
    val g = (color.green * 255f).roundToInt()
    val b = (color.blue * 255f).roundToInt()
    return "#" + String.format(Locale.US, "%02X%02X%02X", r, g, b)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    prefs: SharedPreferences,
    materialYou: Boolean,
    onMaterialYouChange: (Boolean) -> Unit,
    amoledThemeState: Boolean,
    onAmoledThemeChange: (Boolean) -> Unit,
    hideTopBar: Boolean,
    onHideTopBarChange: (Boolean) -> Unit,
    landscapeMode: Boolean,
    onLandscapeModeChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    paletteSeed: String?,
    onPaletteSeedChange: (String?) -> Unit,
    onConnectionModeChange: (String) -> Unit,
    onOfflineModeChange: (Boolean) -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
    onDebugToggle: (Boolean) -> Unit = {}
) {
    var autoplayMode by remember { mutableStateOf(prefs.getString("APlayMode", "disabled") ?: "disabled") }
    var takeControl by remember { mutableStateOf(prefs.getBoolean("TakeControl", true)) }
    var andAuto by remember { mutableStateOf(prefs.getBoolean("AndAuto", true)) }
    var closeNowPlay by remember { mutableStateOf(prefs.getBoolean("CloseNowPlay", true)) }
    var guiMode by remember { mutableStateOf(prefs.getString("GuiMode", "csshack") ?: "csshack") }
    var customCss by remember { mutableStateOf(prefs.getString("CustomCss", "") ?: "") }
    var amoledTheme by remember { mutableStateOf(amoledThemeState) }
    var swipeStop by remember { mutableStateOf(prefs.getBoolean("SwipeStop", true)) }
    var btAutoPause by remember { mutableStateOf(prefs.getBoolean("BtAutoPause", false)) }
    var btAutoResume by remember { mutableStateOf(prefs.getBoolean("BtAutoResume", false)) }
    var playerMode by remember { mutableStateOf(prefs.getString("PlayerMode", "spotilol") ?: "spotilol") }
    var lyricsStyle by remember { mutableStateOf(prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE) }
    var connectionMode by remember { mutableStateOf(prefs.getString("ConnectionMode", "normal") ?: "normal") }
    var offlineMode by remember { mutableStateOf(prefs.getBoolean("OfflineMode", false)) }

    val context = LocalContext.current
    var profiles by remember { mutableStateOf(ProfileManager.getProfiles(context)) }

    var showConnectionModeDialog by remember { mutableStateOf(false) }
    var showLyricsStyleDialog by remember { mutableStateOf(false) }
    var showSaveAccountDialog by remember { mutableStateOf(false) }
    var pendingCookies by remember { mutableStateOf<String?>(null) }
    var accountNameInput by remember { mutableStateOf("") }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAutoPlayDialog by remember { mutableStateOf(false) }
    var showPlayerModeDialog by remember { mutableStateOf(false) }
    var showGuiModeDialog by remember { mutableStateOf(false) }
    var showCustomCssDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var dbgOverlay by remember { mutableStateOf(prefs.getBoolean("DebugOverlay", false)) }
    var showDevlogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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

            val accentLabel = when {
                materialYou -> "Dynamic (system colors)"
                paletteSeed.isNullOrBlank() -> "Default"
                else -> "Custom $paletteSeed"
            }
            SettingTile(
                title = "Accent Color",
                subtitle = accentLabel,
                icon = Icons.Default.ColorLens,
                onClick = { showPaletteDialog = true }
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingSwitchTile(
                title = "Hide Top Bar",
                subtitle = "Swipe down from the top to show it",
                icon = Icons.Default.VisibilityOff,
                checked = hideTopBar,
                onCheckedChange = { enabled ->
                    onHideTopBarChange(enabled)
                    prefs.edit().putBoolean("HideTopBar", enabled).apply()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingSwitchTile(
                title = "Landscape Mode",
                subtitle = "Allow rotating to landscape",
                icon = Icons.Default.ScreenRotation,
                checked = landscapeMode,
                onCheckedChange = { enabled ->
                    onLandscapeModeChange(enabled)
                    prefs.edit().putBoolean("LandscapeMode", enabled).apply()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingSwitchTile(
                title = "Keep Screen On",
                subtitle = "Prevent the screen from turning off",
                icon = Icons.Default.BrightnessHigh,
                checked = keepScreenOn,
                onCheckedChange = { enabled ->
                    onKeepScreenOnChange(enabled)
                    prefs.edit().putBoolean("KeepScreenOn", enabled).apply()
                }
            )
        }

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

            val playerModeLabel = when (playerMode) {
                "spotilol" -> "Spotilol Player"
                "original" -> "Spotify Original"
                else -> "Spotilol Player"
            }
            SettingTile(
                title = "Player Mode",
                subtitle = playerModeLabel,
                icon = Icons.Default.PlayCircle,
                onClick = { showPlayerModeDialog = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            val lyricsStyleLabel = LyricsTheme.STYLE_OPTIONS
                .firstOrNull { it.first == lyricsStyle }?.second ?: "Fullscreen (Album Colors)"
            SettingTile(
                title = "Lyrics Style",
                subtitle = lyricsStyleLabel,
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { showLyricsStyleDialog = true }
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
            title = "OFFLINE",
            icon = Icons.Default.CloudOff
        ) {
            SettingSwitchTile(
                title = "Offline Mode",
                subtitle = if (offlineMode) {
                    "On — playing downloaded songs only"
                } else {
                    "Play only downloaded songs — restarts the app"
                },
                icon = Icons.Default.CloudOff,
                checked = offlineMode,
                onCheckedChange = { enabled ->
                    offlineMode = enabled
                    onOfflineModeChange(enabled)
                }
            )
        }

        SettingSectionCard(
            title = "BLUETOOTH",
            icon = Icons.Default.Smartphone
        ) {
            SettingSwitchTile(
                title = "Pause on Disconnect",
                subtitle = "Pause when BT/headphones disconnect",
                icon = Icons.Default.Smartphone,
                checked = btAutoPause,
                onCheckedChange = {
                    btAutoPause = it
                    prefs.edit().putBoolean("BtAutoPause", it).apply()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingSwitchTile(
                title = "Resume on Connect",
                subtitle = "Resume when BT device connects",
                icon = Icons.Default.Smartphone,
                checked = btAutoResume,
                onCheckedChange = {
                    btAutoResume = it
                    prefs.edit().putBoolean("BtAutoResume", it).apply()
                }
            )
        }

        SettingSectionCard(
            title = "ACCOUNTS",
            icon = Icons.Default.PersonAdd
        ) {
            SettingTile(
                title = "Save Current Account",
                subtitle = "Store the current session as a profile",
                icon = Icons.Default.PersonAdd,
                onClick = {
                    val cookies = ProfileManager.captureSession(context)
                    if (cookies == null) {
                        Toast.makeText(context, "Log in to Spotify first", Toast.LENGTH_SHORT).show()
                    } else {
                        pendingCookies = cookies
                        accountNameInput = prefs.getString("CurrentAccountName", "") ?: ""
                        showSaveAccountDialog = true
                    }
                }
            )
            if (profiles.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                profiles.forEachIndexed { index, profile ->
                    ProfileRow(
                        name = profile.name,
                        subtitle = "Saved " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(Date(profile.savedAt)),
                        onLoad = { onLoadProfile(profile.cookies) },
                        onDelete = {
                            onDeleteProfile(profile.name)
                            profiles = ProfileManager.getProfiles(context)
                        }
                    )
                    if (index < profiles.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }
                }
            }
        }

        SettingSectionCard(
            title = "CONNECTION MODE",
            icon = Icons.Default.Shield
        ) {
            val modeLabel = if (connectionMode == "proxy") {
                "MITM Proxy (Certificate)"
            } else {
                "Normal (No Certificate)"
            }
            SettingTile(
                title = "MITM Proxy Mode",
                subtitle = "$modeLabel — restarts the app",
                icon = Icons.Default.Shield,
                onClick = { showConnectionModeDialog = true }
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

        if (connectionMode == "proxy") {
            SettingSectionCard(
                title = "SECURITY & NETWORK",
                icon = Icons.Default.Shield
            ) {
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
        }

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
                onClick = { showChangelogDialog = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingTile(
                title = "WebView Engine",
                subtitle = pkg?.versionName ?: "System WebView",
                icon = Icons.Default.Language,
                onClick = {}
            )
        }

        SettingSectionCard(
            title = "DEBUG",
            icon = Icons.Default.Science
        ) {
            SettingSwitchTile(
                title = "Collect Debug",
                subtitle = "Collect debug events thrown by JS",
                icon = Icons.Default.BugReport,
                checked = dbgOverlay,
                onCheckedChange = { enabled ->
                    dbgOverlay = enabled
                    prefs.edit().putBoolean("DebugOverlay", enabled).apply()
                    onDebugToggle(enabled)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingTile(
                title = "Open Devlog",
                subtitle = if (dbgOverlay) "Live - JS + native events" else "Enable debug first",
                icon = Icons.Default.Code,
                onClick = { showDevlogDialog = true },
                enabled = dbgOverlay
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showPaletteDialog) {
        PaletteDialog(
            currentSeed = paletteSeed,
            onSave = { hex ->
                onPaletteSeedChange(hex)
                showPaletteDialog = false
            },
            onDismiss = { showPaletteDialog = false }
        )
    }

    if (showChangelogDialog) {
        ChangelogDialog(onDismiss = { showChangelogDialog = false })
    }

    if (showSaveAccountDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAccountDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Save Current Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "The current Spotify session will be stored on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Account name") },
                        placeholder = { Text("e.g. Premium, Work") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cookies = pendingCookies
                        if (cookies != null && accountNameInput.isNotBlank()) {
                            onSaveProfile(accountNameInput, cookies)
                            profiles = ProfileManager.getProfiles(context)
                        }
                        accountNameInput = ""
                        showSaveAccountDialog = false
                    },
                    enabled = accountNameInput.isNotBlank()
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAccountDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showConnectionModeDialog) {
        SingleChoiceDialog(
            title = "MITM Proxy Mode",
            options = listOf(
                "normal" to "Normal (No Certificate)",
                "proxy" to "MITM Proxy (Certificate)"
            ),
            selected = connectionMode,
            onSelect = { value ->
                connectionMode = value
                onConnectionModeChange(value)
            },
            onDismiss = { showConnectionModeDialog = false }
        )
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

    if (showPlayerModeDialog) {
        SingleChoiceDialog(
            title = "Player Mode",
            options = listOf(
                "spotilol" to "Spotilol Player",
                "original" to "Spotify Original"
            ),
            selected = playerMode,
            onSelect = { value ->
                playerMode = value
                prefs.edit().putString("PlayerMode", value).apply()
            },
            onDismiss = { showPlayerModeDialog = false }
        )
    }

    if (showLyricsStyleDialog) {
        SingleChoiceDialog(
            title = "Lyrics Style",
            options = LyricsTheme.STYLE_OPTIONS,
            selected = lyricsStyle,
            onSelect = { value ->
                lyricsStyle = value
                prefs.edit().putString("LyricsStyle", value).apply()
            },
            onDismiss = { showLyricsStyleDialog = false }
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

    if (showDevlogDialog) {
        DevlogLiveDialog(onDismiss = { showDevlogDialog = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaletteDialog(
    currentSeed: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = parsePaletteColor(currentSeed) ?: Color(0xFFE0E0E0)
    var red by remember { mutableFloatStateOf(initial.red * 255f) }
    var green by remember { mutableFloatStateOf(initial.green * 255f) }
    var blue by remember { mutableFloatStateOf(initial.blue * 255f) }
    var useDefault by remember { mutableStateOf(currentSeed.isNullOrBlank()) }
    val preview = Color(red / 255f, green / 255f, blue / 255f)

    fun pick(hex: String?) {
        val c = parsePaletteColor(hex)
        if (c == null) {
            useDefault = true
            red = 224f
            green = 224f
            blue = 224f
        } else {
            useDefault = false
            red = c.red * 255f
            green = c.green * 255f
            blue = c.blue * 255f
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(preview)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Custom accent color",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (useDefault) "Default scheme" else formatHex(preview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PalettePresets.forEach { (label, hex) ->
                        val color = parsePaletteColor(hex) ?: Color(0xFFE0E0E0)
                        val isSelected = if (hex == null) {
                            currentSeed.isNullOrBlank()
                        } else {
                            hex == currentSeed
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { pick(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color.luminance() > 0.5f) {
                                            Color(0xFF1A1A1A)
                                        } else {
                                            Color.White
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider()

                ColorSlider("Red", red, { red = it; useDefault = false }, Color(0xFFF44336))
                ColorSlider("Green", green, { green = it; useDefault = false }, Color(0xFF4CAF50))
                ColorSlider("Blue", blue, { blue = it; useDefault = false }, Color(0xFF2196F3))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(if (useDefault) null else formatHex(preview))
            }) {
                Text("Apply", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var release by remember { mutableStateOf<GitHubRelease?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    fun fetch() {
        loading = true
        failed = false
        GitHubApi.fetchLatestRelease("lyssadev", "Spotilol") { r ->
            loading = false
            if (r == null || r.body.isBlank()) {
                failed = true
            } else {
                release = r
            }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    val publishedLabel = release?.publishedAt?.let { iso ->
        runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(iso)
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Changelog",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (release != null) {
                        Text(
                            text = listOfNotNull(
                                "v${release!!.tagName.removePrefix("v")}",
                                publishedLabel
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Could not load release notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { fetch() }) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                release != null -> {
                    val r = release!!
                    MarkdownText(
                        markdown = r.body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (configuration.screenHeightDp * 0.65f).dp)
                            .verticalScroll(rememberScrollState()),
                        onLinkClick = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
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
            border = BorderStroke(
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
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier else Modifier.alpha(0.38f))
            .clickable(enabled = enabled, onClick = onClick)
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
fun ProfileRow(
    name: String,
    subtitle: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
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
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
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
fun SettingsContentPreview() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE) }
    SpotifyTheme {
        SettingsContent(
            modifier = Modifier.fillMaxSize(),
            prefs = prefs,
            materialYou = false,
            onMaterialYouChange = {},
            amoledThemeState = false,
            onAmoledThemeChange = {},
            hideTopBar = false,
            onHideTopBarChange = {},
            landscapeMode = false,
            onLandscapeModeChange = {},
            keepScreenOn = false,
            onKeepScreenOnChange = {},
            paletteSeed = null,
            onPaletteSeedChange = {},
            onConnectionModeChange = {},
            onOfflineModeChange = {},
            onSaveProfile = { _, _ -> },
            onLoadProfile = {},
            onDeleteProfile = {},
            onClearCache = {},
            onClearData = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevlogLiveDialog(onDismiss: () -> Unit) {
    var lines by remember { mutableStateOf(DebugLogStore.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            lines = DebugLogStore.snapshot()
        }
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Devlog", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = if (lines.isEmpty()) "(empty - waiting for activity)"
                    else lines.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.height(320.dp).verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    DebugLogStore.clear()
                    lines = emptyList()
                }) { Text("Clear") }

                TextButton(onClick = {
                    val text = lines.joinToString("\n")
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("spotilol_devlog", text).toClipEntry()
                        )
                    }
                }) { Text("Copy") }

                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
