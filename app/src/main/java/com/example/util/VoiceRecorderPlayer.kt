package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

class VoiceRecorderPlayer(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): Boolean {
        return try {
            stopRecording()
            val outFile = File(context.cacheDir, "vibe_voice_${System.currentTimeMillis()}.m4a")
            currentFile = outFile
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(16000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            startTimeMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            recorder = null
            false
        }
    }

    fun stopRecordingAndEncode(): Pair<String, Long>? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).coerceAtLeast(1L)
            val file = currentFile ?: return null
            if (!file.exists()) return null
            val bytes = file.readBytes()
            file.delete()
            if (bytes.size > 300_000) return null
            val encoded = "data:audio/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            encoded to durationSec
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    fun playEncodedAudio(dataUri: String, onComplete: () -> Unit) {
        stopPlayback()
        if (!dataUri.startsWith("data:audio")) {
            onComplete()
            return
        }
        try {
            val base64 = dataUri.substringAfter("base64,", "")
            if (base64.isBlank()) {
                onComplete()
                return
            }
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "vibe_play_${System.currentTimeMillis()}.m4a")
            tempFile.writeBytes(bytes)
            player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    tempFile.delete()
                    stopPlayback()
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    tempFile.delete()
                    stopPlayback()
                    onComplete()
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
