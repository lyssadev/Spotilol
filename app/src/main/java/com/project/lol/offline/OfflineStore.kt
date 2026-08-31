package com.project.lol.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class OfflineSong(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val coverFile: File? = null,
)

object OfflineStore {
    private const val TAG = "Spl-DL"
    private const val FOLDER = "Spotilol"

    private val TrackIdRegex = Regex("\\[([^\\]]+)\\]\\.[^.]+$")
    private val FileNameRegex = Regex("^(.*) - (.*) \\[([^\\]]+)\\]\\.[^.]+$")

    private fun isUnknown(value: String?): Boolean =
        value.isNullOrBlank() || value.equals("<unknown>", ignoreCase = true)

    private fun metaFile(context: Context) = File(context.filesDir, "offline_meta.json")

    private fun coverDir(context: Context) = File(context.filesDir, "covers").apply { mkdirs() }

    fun coverFile(context: Context, trackId: String): File? =
        File(coverDir(context), "$trackId.jpg").takeIf { it.exists() && it.length() > 0 }

    fun saveMetadata(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        album: String,
        coverUrl: String?,
    ) {
        runCatching {
            val file = metaFile(context)
            val root = if (file.exists()) {
                runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            root.put(
                trackId,
                JSONObject().apply {
                    put("title", title)
                    put("artist", artist)
                    put("album", album)
                    put("coverUrl", coverUrl ?: "")
                }
            )
            file.writeText(root.toString())
        }.onFailure { Log.w(TAG, "saveMetadata: failed to write manifest: ${it.message}") }

        if (coverUrl.isNullOrBlank()) return
        if (coverFile(context, trackId) != null) return
        runCatching {
            val conn = URL(coverUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input ->
                File(coverDir(context), "$trackId.jpg").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.onFailure { Log.w(TAG, "saveMetadata: cover fetch failed: ${it.message}") }
    }

    fun removeMetadata(context: Context, trackId: String) {
        runCatching {
            val file = metaFile(context)
            if (!file.exists()) return
            val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return
            root.remove(trackId)
            file.writeText(root.toString())
        }
        runCatching { File(coverDir(context), "$trackId.jpg").delete() }
    }

    fun loadSongs(context: Context): List<OfflineSong> {
        val songs = mutableListOf<OfflineSong>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_PENDING,
            )
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Audio.Media.IS_PENDING}=0",
                arrayOf("%Music/$FOLDER%"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    runCatching {
                        if (c.getInt(4) != 0) return@runCatching
                        val displayName = c.getString(3) ?: return@runCatching
                        val nameMatch = FileNameRegex.find(displayName)
                        val rawTitle = c.getString(1) ?: ""
                        val rawArtist = c.getString(2) ?: ""
                        val title = if (isUnknown(rawTitle)) {
                            nameMatch?.groupValues?.get(2)?.trim()?.ifBlank { rawTitle } ?: rawTitle
                        } else {
                            rawTitle
                        }
                        val artist = if (isUnknown(rawArtist)) {
                            nameMatch?.groupValues?.get(1)?.trim()?.ifBlank { rawArtist } ?: rawArtist
                        } else {
                            rawArtist
                        }
                        val trackId = TrackIdRegex.find(displayName)?.groupValues?.get(1)
                            ?.takeIf { it.isNotBlank() }
                            ?: "ms${c.getLong(0)}"
                        val uri = ContentUris.withAppendedId(collection, c.getLong(0))
                        songs.add(
                            OfflineSong(
                                id = trackId,
                                title = title,
                                artist = artist,
                                uri = uri,
                                coverFile = coverFile(context, trackId),
                            )
                        )
                    }
                }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                FOLDER,
            )
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { f ->
                if (!f.isFile) return@forEach
                val match = FileNameRegex.find(f.name)
                val artist = match?.groupValues?.get(1)?.trim().orEmpty()
                val title = match?.groupValues?.get(2)?.trim() ?: f.nameWithoutExtension
                val trackId = match?.groupValues?.get(3) ?: f.nameWithoutExtension
                songs.add(
                    OfflineSong(
                        id = trackId,
                        title = title,
                        artist = artist,
                        uri = Uri.fromFile(f),
                        coverFile = coverFile(context, trackId),
                    )
                )
            }
        }
        return songs
    }

    fun deleteSong(context: Context, song: OfflineSong): Boolean {
        val ok = runCatching {
            if (song.uri.scheme == "content") {
                context.contentResolver.delete(song.uri, null, null) > 0
            } else {
                File(song.uri.path ?: return@runCatching false).delete()
            }
        }.getOrElse {
            Log.w(TAG, "deleteSong: failed to delete ${song.uri}: ${it.message}")
            false
        }
        removeMetadata(context, song.id)
        return ok
    }

    /**
     * TRUE if a track with this Spotify ID is already saved in Music/Spotilol.
     * Used to de-duplicate album/playlist batch downloads. MediaStore is the
     * source of truth on Q+; the folder listing on older devices.
     */
    fun isTrackSaved(context: Context, trackId: String): Boolean {
        if (trackId.isBlank()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val escaped = trackId
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    arrayOf(MediaStore.Audio.Media._ID),
                    "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? AND " +
                            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? ESCAPE '\\'",
                    arrayOf("%Music/$FOLDER%", "%[$escaped]%"),
                    null,
                )?.use { it.count > 0 } ?: false
            }.getOrDefault(false)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                FOLDER,
            )
            val marker = "[$trackId]."
            runCatching {
                dir.listFiles()?.any { it.isFile && it.name.contains(marker) } == true
            }.getOrDefault(false)
        }
    }
}
