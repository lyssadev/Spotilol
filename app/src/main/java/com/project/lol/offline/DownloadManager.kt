package com.project.lol.offline

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.project.lol.innertube.YouTube
import com.project.lol.innertube.models.SongItem
import com.project.lol.yt.AudioQuality
import com.project.lol.yt.CandidateScorer
import com.project.lol.yt.CandidateScorer.isAcceptableMatch
import com.project.lol.yt.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

private data class TrackMeta(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String?,
)

private data class DownloadedTrack(
    val success: Boolean,
    val title: String,
    val artist: String,
    val album: String,
)

private sealed class TrackResult {
    data class Saved(val title: String, val artist: String, val album: String) : TrackResult()
    data class Failed(val title: String, val artist: String, val album: String) : TrackResult()
    object Aborted : TrackResult()
}

private data class ResolvedStream(
    val url: String,
    val chosen: SongItem,
)

private sealed class DownloadJob {
    data class Single(val track: TrackMeta) : DownloadJob()
    data class Collection(
        val name: String,
        val cover: String?,
        val tracks: List<TrackMeta>,
    ) : DownloadJob()
}

object DownloadManager {
    private const val TAG = "Spl-DL"
    private const val BATCH_INTER_TRACK_DELAY_MS = 300L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var onStatus: ((String) -> Unit)? = null

    @Volatile
    var onProgress: ((Int, String) -> Unit)? = null

    @Volatile
    private var activeTrackId: String? = null

    @Volatile
    private var batchActive = false

    private val jobs = Channel<DownloadJob>(Channel.UNLIMITED)

    @Volatile
    private var consumerStarted = false
    private val consumerLock = Any()

    @Volatile
    var onProgress2: ((Int, String) -> Unit)? = null

    @Volatile
    var lastPct: Int = 0

    @Volatile
    var lastLabel: String = ""

    private val pendingJobs = java.util.concurrent.atomic.AtomicInteger(0)

    fun isWorkPending(): Boolean = pendingJobs.get() > 0

    private fun progress(pct: Int, label: String) {
        lastPct = pct
        lastLabel = label
        onProgress?.invoke(pct, label)
        onProgress2?.invoke(pct, label)
    }

    private enum class ControlSignal { SKIP, CANCEL }

    @Volatile
    private var signal: ControlSignal? = null

    fun skipCurrent() {
        signal = ControlSignal.SKIP
        Log.i(TAG, "skipCurrent: skip requested (active=${activeTrackId ?: "none"})")
    }

    fun cancelAll() {
        signal = ControlSignal.CANCEL
        var dropped = 0
        while (jobs.tryReceive().isSuccess) {
            pendingJobs.decrementAndGet()
            dropped++
        }
        Log.i(TAG, "cancelAll: cancel requested, dropped $dropped queued job(s)")
    }

    private fun consumeSignal(): ControlSignal? {
        val s = signal
        signal = null
        return s
    }

    fun isDownloading(): Boolean = activeTrackId != null

    fun currentTrackId(): String? = activeTrackId

    fun isBatchActive(): Boolean = batchActive

    fun downloadCurrentTrack(
        context: Context,
        payload: String,
    ) {
        val appContext = context.applicationContext
        val parsed = runCatching { JSONObject(payload) }.getOrNull() ?: run {
            onStatus?.invoke("Invalid download request")
            return
        }
        val trackId = parsed.optString("trackId").trim()
        if (trackId.isBlank()) {
            Log.e(TAG, "downloadCurrentTrack: empty trackId in payload")
            onStatus?.invoke("Could not identify the current track")
            return
        }
        if (trackId == activeTrackId) {
            Log.w(TAG, "downloadCurrentTrack: $trackId is already in progress")
            onStatus?.invoke("This track is already downloading")
            return
        }
        val track = TrackMeta(
            trackId = trackId,
            title = parsed.optString("title"),
            artist = parsed.optString("artist"),
            album = parsed.optString("album"),
            cover = parsed.optString("cover").ifBlank { null },
        )
        Log.d(TAG, "downloadCurrentTrack: queued id=$trackId title=${track.title} artist=${track.artist}")
        onProgress?.invoke(0, "Resolving audio...")
        enqueue(appContext, DownloadJob.Single(track))
    }

    fun downloadCollection(
        context: Context,
        payload: String,
    ) {
        val appContext = context.applicationContext
        val parsed = runCatching { JSONObject(payload) }.getOrNull() ?: run {
            onStatus?.invoke("Invalid download request")
            return
        }
        val tracksJson = parsed.optJSONArray("tracks") ?: run {
            onStatus?.invoke("No tracks found")
            return
        }
        val seen = HashSet<String>()
        val tracks = ArrayList<TrackMeta>(tracksJson.length())
        for (i in 0 until tracksJson.length()) {
            val o = tracksJson.optJSONObject(i) ?: continue
            val id = o.optString("trackId").trim()
            if (id.isBlank() || !seen.add(id)) continue
            tracks.add(
                TrackMeta(
                    trackId = id,
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    cover = o.optString("cover").ifBlank { null },
                )
            )
        }
        if (tracks.isEmpty()) {
            onStatus?.invoke("No downloadable tracks found")
            return
        }
        val name = parsed.optString("name").ifBlank { "Collection" }
        val collectionCover = parsed.optString("cover").ifBlank { null }
        val batch = if (collectionCover != null) {
            tracks.map { if (it.cover == null) it.copy(cover = collectionCover) else it }
        } else tracks
        Log.i(TAG, "downloadCollection: '$name' type=${parsed.optString("type")} tracks=${batch.size}")
        onProgress?.invoke(0, "Queued ${batch.size} tracks — $name")
        enqueue(appContext, DownloadJob.Collection(name, collectionCover, batch))
    }

    private fun enqueue(appContext: Context, job: DownloadJob) {
        pendingJobs.incrementAndGet()
        jobs.trySend(job)
        synchronized(consumerLock) {
            if (!consumerStarted) {
                consumerStarted = true
                scope.launch { consumeLoop(appContext) }
            }
        }
    }

    private suspend fun consumeLoop(appContext: Context) {
        for (job in jobs) {
            pendingJobs.decrementAndGet()
            signal = null
            runCatching {
                when (job) {
                    is DownloadJob.Single -> runSingle(appContext, job.track)
                    is DownloadJob.Collection -> runCollection(appContext, job)
                }
            }.onFailure { Log.e(TAG, "consumeLoop: job failed: ${it.message}", it) }
        }
    }

    private suspend fun runSingle(appContext: Context, track: TrackMeta) {
        if (OfflineStore.isTrackSaved(appContext, track.trackId)) {
            onProgress?.invoke(100, "Already saved")
            withContext(Dispatchers.Main) { onStatus?.invoke("Already saved to Music/Spotilol") }
            return
        }
        activeTrackId = track.trackId
        try {
            val result = runCatching {
                downloadToFile(appContext, track) { pct, label -> onProgress?.invoke(pct, label) }
            }.onFailure { Log.e(TAG, "runSingle: exception: ${it.message}", it) }
                .getOrElse {
                    if (signal != null) TrackResult.Aborted
                    else TrackResult.Failed(track.title, track.artist, track.album)
                }
            Log.i(TAG, "runSingle: finished id=${track.trackId} result=${result::class.simpleName}")
            when (result) {
                is TrackResult.Saved -> {
                    OfflineStore.saveMetadata(
                        appContext, track.trackId, result.title, result.artist, result.album, track.cover,
                    )
                    onProgress?.invoke(100, "Saved to Music/Spotilol")
                    withContext(Dispatchers.Main) { onStatus?.invoke("Saved to Music/Spotilol") }
                }
                is TrackResult.Failed -> {
                    val msg = "Download failed: ${lastDownloadError ?: "unknown error"}"
                    onProgress?.invoke(-1, msg)
                    withContext(Dispatchers.Main) { onStatus?.invoke(msg) }
                }
                // Single track: skip and cancel both simply stop the download.
                TrackResult.Aborted -> {
                    consumeSignal()
                    onProgress?.invoke(-1, "Cancelled")
                    withContext(Dispatchers.Main) { onStatus?.invoke("Download cancelled") }
                }
            }
        } finally {
            activeTrackId = null
        }
    }


    private suspend fun runCollection(appContext: Context, job: DownloadJob.Collection) {
        val total = job.tracks.size
        var saved = 0
        var failed = 0
        var skipped = 0
        var cancelled = false
        batchActive = true
        Log.i(TAG, "runCollection: start '${job.name}' total=$total")

        try {
            for ((index, track) in job.tracks.withIndex()) {
                val n = index + 1
                val shortTitle = track.title.ifBlank { track.trackId }
                val completed = saved + failed + skipped

                fun overallPct(trackPct: Int): Int {
                    val frac = if (trackPct in 0..100) trackPct / 100.0 else 0.0
                    return ((completed + frac) * 100.0 / total).toInt().coerceIn(0, 100)
                }

                fun report(pct: Int, text: String) {
                    onProgress?.invoke(overallPct(pct), "$n/$total · $shortTitle — $text")
                }

                // Skip/cancel tapped between tracks (e.g. during the
                // inter-track delay): consume it before starting this one.
                when (consumeSignal()) {
                    ControlSignal.CANCEL -> {
                        cancelled = true
                        break
                    }
                    ControlSignal.SKIP -> {
                        skipped++
                        Log.i(TAG, "runCollection: user skipped ${track.trackId}")
                        report(100, "Skipped")
                        continue
                    }
                    null -> {}
                }

                if (OfflineStore.isTrackSaved(appContext, track.trackId)) {
                    skipped++
                    Log.d(TAG, "runCollection: ${track.trackId} already saved, skipping")
                    report(100, "Already saved")
                    continue
                }

                activeTrackId = track.trackId
                // Report at track start so the batch flags reach the UI
                // before the (non-interruptible) resolver runs.
                report(0, "Resolving...")
                try {
                    val result = runCatching {
                        downloadToFile(appContext, track) { pct, text -> report(pct, text) }
                    }.onFailure { Log.e(TAG, "runCollection: ${track.trackId} exception: ${it.message}", it) }
                        .getOrElse {
                            if (signal != null) TrackResult.Aborted
                            else TrackResult.Failed(track.title, track.artist, track.album)
                        }

                    when (result) {
                        is TrackResult.Saved -> {
                            saved++
                            OfflineStore.saveMetadata(
                                appContext, track.trackId, result.title, result.artist, result.album, track.cover,
                            )
                            report(100, "Saved")
                        }
                        is TrackResult.Failed -> {
                            failed++
                            Log.w(TAG, "runCollection: ${track.trackId} failed: $lastDownloadError")
                            report(0, "Failed — skipping")
                        }
                        TrackResult.Aborted -> when (consumeSignal()) {
                            ControlSignal.CANCEL -> cancelled = true
                            // SKIP (or the tail of a cancel): drop this track, batch lives on.
                            else -> {
                                skipped++
                                report(100, "Skipped")
                            }
                        }
                    }
                } finally {
                    activeTrackId = null
                }

                if (cancelled) break
                if (index < job.tracks.lastIndex) {
                    delay(BATCH_INTER_TRACK_DELAY_MS.milliseconds)
                }
            }
        } finally {
            batchActive = false
        }

        val processed = saved + failed + skipped
        val summary = buildString {
            append(saved)
            append(if (saved == 1) " track saved" else " tracks saved")
            if (skipped > 0) append(", $skipped skipped")
            if (failed > 0) append(", $failed failed")
            if (cancelled) append(" — cancelled at $processed/$total")
        }
        Log.i(TAG, "runCollection: done '${job.name}' saved=$saved failed=$failed skipped=$skipped cancelled=$cancelled")
        withContext(Dispatchers.Main) { onStatus?.invoke(summary) }
        when {
            cancelled -> onProgress?.invoke(-1, summary)
            saved > 0 -> onProgress?.invoke(100, "$summary — Music/Spotilol")
            failed > 0 -> onProgress?.invoke(-1, summary)
        }
    }

    private suspend fun downloadToFile(
        context: Context,
        track: TrackMeta,
        progress: (Int, String) -> Unit,
    ): TrackResult {
        val trackId = track.trackId
        val title = track.title
        val artist = track.artist
        val album = track.album

        if (signal != null) return TrackResult.Aborted

        val resolved = resolveStream(context, trackId, title, artist, album) ?: run {
            Log.w(TAG, "downloadToFile: no stream source for $trackId")
            lastDownloadError = "Download source not available yet"
            return TrackResult.Failed(title, artist, album)
        }
        // Resolution takes a few seconds and is not interruptible - catch
        // aborts requested during it here instead of downloading anyway.
        if (signal != null) return TrackResult.Aborted

        val effectiveTitle = title.ifBlank { resolved.chosen.title }
        val effectiveArtist = artist.ifBlank {
            resolved.chosen.artists.firstOrNull()?.name.orEmpty()
        }
        val effectiveAlbum = album.ifBlank { resolved.chosen.album?.name.orEmpty() }

        val dir = java.io.File(context.filesDir, "downloads").apply { mkdirs() }
        val tmpFile = java.io.File(dir, "$trackId.part")
        progress(1, "Downloading audio...")
        val downloaded = httpDownloadRanged(
            resolved.url,
            tmpFile,
            { pct -> progress(pct, "Downloading audio...") },
            { signal != null },
        )
        if (!downloaded) {
            Log.e(TAG, "downloadToFile: audio download failed: $lastDownloadError")
            runCatching { tmpFile.delete() }
            if (signal != null) return TrackResult.Aborted
            return TrackResult.Failed(effectiveTitle, effectiveArtist, effectiveAlbum)
        }
        Log.d(TAG, "downloadToFile: audio downloaded size=${tmpFile.length()}")
        progress(99, "Saving...")

        val uri = saveToPublicMusic(context, trackId, effectiveTitle, effectiveArtist, tmpFile, "m4a", "audio/mp4")
        tmpFile.delete()
        if (uri != null) {
            Log.d(TAG, "downloadToFile: saved to Music/Spotilol uri=$uri")
            return TrackResult.Saved(effectiveTitle, effectiveArtist, effectiveAlbum)
        }
        Log.w(TAG, "downloadToFile: MediaStore save failed")
        lastDownloadError = "Couldn't save file"
        return TrackResult.Failed(effectiveTitle, effectiveArtist, effectiveAlbum)
    }

    private suspend fun resolveStream(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        album: String,
    ): ResolvedStream? {
        val searchText = buildString {
            append(title)
            if (artist.isNotBlank()) append(" $artist")
        }
        Log.d(TAG, "resolveStream: id=$trackId searching '$searchText'")

        val searchResult = runCatching {
            YouTube.search(searchText, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        }.onFailure { Log.e(TAG, "resolveStream: search failed: ${it.message}", it) }
            .getOrNull()
        if (searchResult == null || searchResult.items.isEmpty()) {
            Log.w(TAG, "resolveStream: no results for '$searchText'")
            return null
        }

        val songItems = searchResult.items.filterIsInstance<SongItem>()
        if (songItems.isEmpty()) {
            Log.w(TAG, "resolveStream: no song items for '$searchText'")
            return null
        }

        val metadata = CandidateScorer.TrackMatchMetadata(
            title = title,
            artist = artist,
            album = album,
        )
        val scored = songItems.mapNotNull { song ->
            CandidateScorer.ytmusicTransferScore(song, metadata, expectedDurationMs = 0)
                .takeIf { it.isAcceptableMatch() }
        }.sortedByDescending { it.score }

        val chosen = scored.firstOrNull()?.item ?: run {
            Log.w(TAG, "resolveStream: no acceptable match for '$searchText'")
            return null
        }
        Log.d(TAG, "resolveStream: chosen '${chosen.title}' by ${chosen.artists.joinToString { it.name }} (videoId=${chosen.id})")

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val playback = runCatching {
            YTPlayerUtils.playerResponseForPlayback(
                videoId = chosen.id,
                playlistId = null,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = connectivityManager,
                skipValidation = true,
            ).getOrNull()
        }.onFailure { Log.e(TAG, "resolveStream: playback resolve failed: ${it.message}", it) }
            .getOrNull()

        val streamUrl = playback?.streamUrl
        if (streamUrl != null) {
            Log.d(TAG, "resolveStream: resolved url=${streamUrl.take(80)}")
        } else {
            Log.w(TAG, "resolveStream: no stream url for ${chosen.id}")
        }
        return streamUrl?.let { ResolvedStream(it, chosen) }
    }

    @Volatile
    private var lastDownloadError: String? = null

    private fun httpDownloadRanged(url: String, tmpFile: java.io.File, onProgress: ((Int) -> Unit)? = null, shouldAbort: (() -> Boolean)? = null): Boolean {
        val chunk = 8L * 1024 * 1024
        var total = -1L
        var position = 0L
        return try {
            java.io.BufferedOutputStream(tmpFile.outputStream()).use { output ->
                outer@ while (true) {
                    if (shouldAbort?.invoke() == true) {
                        Log.i(TAG, "httpDownloadRanged: aborted at $position bytes")
                        return false
                    }
                    val end = if (total > 0) minOf(position + chunk - 1, total - 1) else position + chunk - 1
                    var attempt = 0
                    var fullBody = false
                    while (true) {
                        if (shouldAbort?.invoke() == true) return false
                        attempt++
                        val conn = openDownloadConn(url)
                        conn.setRequestProperty("Range", "bytes=$position-$end")
                        try {
                            val code = conn.responseCode
                            if (code !in 200..299) {
                                Log.e(TAG, "httpDownloadRanged: HTTP $code at $position (attempt $attempt)")
                                lastDownloadError = "Stream returned HTTP $code"
                                return false
                            }
                            if (total < 0) {
                                total = conn.getHeaderField("Content-Range")
                                    ?.substringAfter('/')?.toLongOrNull()
                                    ?: conn.contentLengthLong
                                Log.d(TAG, "httpDownloadRanged: total=$total bytes")
                            }
                            fullBody = code == 200
                            conn.inputStream.use { input ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val r = input.read(buf)
                                    if (r < 0) break
                                    if (shouldAbort != null && shouldAbort()) {
                                        Log.i(TAG, "httpDownloadRanged: aborted mid-chunk at $position")
                                        return false
                                    }
                                    output.write(buf, 0, r)
                                    position += r
                                    if (total > 0) {
                                        val pct = ((position * 100) / total).toInt().coerceIn(0, 100)
                                        onProgress?.invoke(pct)
                                    }
                                }
                            }
                            if (total > 0) Log.d(TAG, "httpDownloadRanged: chunk done $position/$total")
                            break
                        } catch (e: Exception) {
                            if (shouldAbort?.invoke() == true) return false
                            Log.w(TAG, "httpDownloadRanged: chunk @$position failed attempt $attempt: ${e.message}")
                            if (attempt >= 4) {
                                lastDownloadError = e.message ?: "Connection reset"
                                return false
                            }
                        } finally {
                            conn.disconnect()
                        }
                    }
                    if (fullBody) { total = position; break@outer }
                    if (total in 1..position) break@outer
                    if (total < 0) break@outer
                }
            }
            val ok = total <= 0 || position >= total
            Log.d(TAG, "httpDownloadRanged: done ok=$ok position=$position total=$total")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "httpDownloadRanged: exception: ${e.message}", e)
            lastDownloadError = e.message ?: "Download error"
            false
        }
    }

    private fun openDownloadConn(url: String): java.net.HttpURLConnection =
        (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        }

    private fun saveToPublicMusic(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        tmpFile: java.io.File,
        ext: String,
        mime: String,
    ): String? {
        val folderName = "Spotilol"
        val fileName = "$artist - $title [$trackId]"
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .let { if (it.length > 200) it.take(200) else it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val displayName = "$fileName.$ext"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$folderName")
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            ) ?: run {
                Log.w(TAG, "saveToPublicMusic: MediaStore insert returned null")
                return null
            }
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tmpFile.inputStream().use { it.copyTo(out) }
                } ?: run {
                    Log.w(TAG, "saveToPublicMusic: openOutputStream returned null")
                    context.contentResolver.delete(uri, null, null)
                    return null
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "saveToPublicMusic: MediaStore write failed: ${e.message}")
                runCatching { context.contentResolver.delete(uri, null, null) }
                return null
            }
            return uri.toString()
        } else {
            val dir = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                folderName,
            ).apply { mkdirs() }
            val outFile = java.io.File(dir, "$fileName.$ext")
            if (!tmpFile.renameTo(outFile)) {
                Log.w(TAG, "saveToPublicMusic: rename to public dir failed (API < 29)")
                return null
            }
            return outFile.absolutePath
        }
    }
}