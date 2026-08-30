package com.project.lol.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import com.project.lol.R
import com.project.lol.offline.OfflineSong
import com.project.lol.offline.OfflineStore
import com.project.lol.service.OfflineMediaService
import com.project.lol.ui.components.SettingsDrawer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    modifier: Modifier = Modifier,
    prefs: SharedPreferences,
    materialYou: Boolean,
    onMaterialYouChange: (Boolean) -> Unit,
    amoledTheme: Boolean,
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
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var settingsDrawerOpen by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }

    var songs by remember { mutableStateOf<List<OfflineSong>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var playerSong by remember { mutableStateOf<OfflineSong?>(null) }
    var pendingDelete by remember { mutableStateOf<OfflineSong?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var scrubMs by remember { mutableIntStateOf(-1) }

    val mediaPlayer = remember { MediaPlayer() }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: ""
    }

    fun syncService() {
        val song = playerSong ?: return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OfflineMediaService::class.java).apply {
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("duration", durationMs.toLong())
                    putExtra("playing", isPlaying)
                    putExtra("position", positionMs.toLong())
                    putExtra("coverPath", song.coverFile?.absolutePath)
                }
            )
        }
    }

    fun play(index: Int) {
        playAt(mediaPlayer, context, songs, index, { currentIndex = it }, { playerSong = it }, { isPlaying = it }, { durationMs = it }, { positionMs = it })
        syncService()
    }

    fun togglePlayPause() {
        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else if (durationMs > 0) {
                mediaPlayer.start()
                isPlaying = true
            }
        }
        syncService()
    }

    fun step(delta: Int) {
        if (songs.isEmpty()) return
        val next = ((currentIndex + delta) % songs.size + songs.size) % songs.size
        play(next)
    }

    fun seekTo(position: Long) {
        if (durationMs <= 0) return
        runCatching { mediaPlayer.seekTo(position.toInt()) }
        positionMs = position.toInt()
        OfflineMediaService.instance?.updatePosition(position)
    }

    fun stopAndClear() {
        runCatching {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.reset()
        }
        isPlaying = false
        currentIndex = -1
        positionMs = 0
        durationMs = 0
        runCatching { context.stopService(Intent(context, OfflineMediaService::class.java)) }
    }

    fun performDelete(song: OfflineSong) {
        val index = songs.indexOfFirst { it.id == song.id && it.uri == song.uri }
        if (index == -1) return
        if (index == currentIndex) {
            stopAndClear()
        } else if (index < currentIndex) {
            currentIndex -= 1
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) { OfflineStore.deleteSong(context, song) }
            songs = songs.filterNot { it.id == song.id && it.uri == song.uri }
            if (!ok) {
                Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = settingsDrawerOpen) {
        settingsDrawerOpen = false
    }

    DisposableEffect(Unit) {
        val ctrl = object : OfflineMediaService.OfflineController {
            override fun onPlayPause() = togglePlayPause()
            override fun onNext() = step(1)
            override fun onPrev() = step(-1)
            override fun onStop() {
                stopAndClear()
                runCatching { context.stopService(Intent(context, OfflineMediaService::class.java)) }
            }

            override fun onSeekTo(position: Long) = seekTo(position)
        }
        OfflineMediaService.controller = ctrl
        onDispose {
            if (OfflineMediaService.controller === ctrl) OfflineMediaService.controller = null
            runCatching { mediaPlayer.release() }
            runCatching { context.stopService(Intent(context, OfflineMediaService::class.java)) }
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setOnCompletionListener {
            val index = currentIndex
            if (index in 0 until songs.lastIndex) {
                play(index + 1)
            } else {
                isPlaying = false
                positionMs = 0
                OfflineMediaService.instance?.updatePlaying(false, 0)
            }
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) { OfflineStore.loadSongs(context) }
        loading = false
    }

    LaunchedEffect(isPlaying, currentIndex) {
        while (isPlaying) {
            runCatching { positionMs = mediaPlayer.currentPosition }
            OfflineMediaService.instance?.updatePosition(positionMs.toLong())
            delay(500.milliseconds)
        }
    }

    SettingsDrawer(
        visible = settingsDrawerOpen,
        onClose = { settingsDrawerOpen = false },
        prefs = prefs,
        materialYou = materialYou,
        onMaterialYouChange = onMaterialYouChange,
        amoledThemeState = amoledTheme,
        onAmoledThemeChange = onAmoledThemeChange,
        hideTopBar = hideTopBar,
        onHideTopBarChange = onHideTopBarChange,
        landscapeMode = landscapeMode,
        onLandscapeModeChange = onLandscapeModeChange,
        keepScreenOn = keepScreenOn,
        onKeepScreenOnChange = onKeepScreenOnChange,
        paletteSeed = paletteSeed,
        onPaletteSeedChange = onPaletteSeedChange,
        onConnectionModeChange = onConnectionModeChange,
        onOfflineModeChange = onOfflineModeChange,
        onSaveProfile = onSaveProfile,
        onLoadProfile = onLoadProfile,
        onDeleteProfile = onDeleteProfile,
        onClearCache = onClearCache,
        onClearData = onClearData,
        onDebugToggle = {},
        blockServiceWorker = prefs.getBoolean("BlockServiceWorker", true),
        onBlockServiceWorkerChange = { enabled ->
            prefs.edit().putBoolean("BlockServiceWorker", enabled).apply()
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                            IconButton(onClick = { settingsDrawerOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onExit) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Exit offline mode",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (loading) {
                            "Loading…"
                        } else if (songs.isEmpty()) {
                            "No downloads yet"
                        } else {
                            "${songs.size} song${if (songs.size == 1) "" else "s"} available offline"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = if (hideTopBar) 60.dp else 12.dp, bottom = 8.dp)
                    )

                    when {
                        loading -> Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        songs.isEmpty() -> Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Nothing here yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Download songs with the download button\nin the player, then come back",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(songs, key = { "${it.id}-${it.uri}" }) { song ->
                                val index = songs.indexOfFirst { it.id == song.id && it.uri == song.uri }
                                OfflineSongRow(
                                    song = song,
                                    isCurrent = index == currentIndex,
                                    onClick = {
                                        if (index == currentIndex) {
                                            if (durationMs > 0 || mediaPlayer.isPlaying) {
                                                togglePlayPause()
                                            } else {
                                                play(index)
                                            }
                                        } else {
                                            play(index)
                                        }
                                    },
                                    onDelete = { pendingDelete = song }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = currentIndex >= 0,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(220)) + fadeIn(tween(220)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(tween(180))
                ) {
                    val song = playerSong
                    if (song != null) {
                        NowPlayingBar(
                            song = song,
                            playing = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            scrubMs = scrubMs,
                            onScrub = { scrubMs = it },
                            onScrubFinished = {
                                if (scrubMs >= 0) seekTo(scrubMs.toLong())
                                scrubMs = -1
                            },
                            onTogglePlay = { togglePlayPause() },
                            onPrev = { step(-1) },
                            onNext = { step(1) }
                        )
                    }
                }

                if (hideTopBar) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(44.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .clickable { showQuickMenu = !showQuickMenu },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_playstore),
                                contentDescription = "Quick actions",
                                tint = Color.Unspecified,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (showQuickMenu) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                offset = IntOffset(0, with(LocalDensity.current) { 64.dp.toPx() }.toInt()),
                                onDismissRequest = { showQuickMenu = false }
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
                                                .clickable {
                                                    showQuickMenu = false
                                                    settingsDrawerOpen = true
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
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
                                                imageVector = Icons.Default.Menu,
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
                                                .clickable {
                                                    showQuickMenu = false
                                                    onExit()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                text = "Exit Offline Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    }

    val songToDelete = pendingDelete
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Delete Song?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove \"${songToDelete.title}\" by ${songToDelete.artist.ifBlank { "Unknown artist" }} from your downloads?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    performDelete(songToDelete)
                }) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
            }
        }
    }

private fun playAt(
    mediaPlayer: MediaPlayer,
    context: android.content.Context,
    songs: List<OfflineSong>,
    index: Int,
    setCurrentIndex: (Int) -> Unit,
    setPlayerSong: (OfflineSong) -> Unit,
    setPlaying: (Boolean) -> Unit,
    setDuration: (Int) -> Unit,
    setPosition: (Int) -> Unit,
) {
    val song = songs.getOrNull(index) ?: return
    runCatching {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(context, song.uri)
        mediaPlayer.prepare()
        mediaPlayer.start()
        setCurrentIndex(index)
        setPlayerSong(song)
        setPlaying(true)
        setDuration(mediaPlayer.duration)
        setPosition(0)
    }.onFailure {
        setPlaying(false)
    }
}

@Composable
private fun OfflineSongRow(
    song: OfflineSong,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        SongCover(song = song, size = 52.dp, corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SongCover(song: OfflineSong, size: Dp, corner: Dp) {
    val context = LocalContext.current
    var bitmap by remember(song.id, song.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song.id, song.uri) {
        bitmap = withContext(Dispatchers.IO) { decodeCover(context, song) }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

private fun decodeCover(context: android.content.Context, song: OfflineSong): Bitmap? {
    song.coverFile?.let { file ->
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / 256)
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()?.let { return it }
    }
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            retriever.embeddedPicture?.let { data ->
                BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()
}

@Composable
private fun NowPlayingBar(
    song: OfflineSong,
    playing: Boolean,
    positionMs: Int,
    durationMs: Int,
    scrubMs: Int,
    onScrub: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SongCover(song = song, size = 44.dp, corner = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist.ifBlank { "Unknown artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = (if (scrubMs >= 0) scrubMs else positionMs)
                    .toFloat()
                    .coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onScrub(it.toInt()) },
                onValueChangeFinished = onScrubFinished,
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
            Row {
                Text(
                    text = formatTime(if (scrubMs >= 0) scrubMs else positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
