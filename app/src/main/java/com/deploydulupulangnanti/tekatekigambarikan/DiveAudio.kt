package com.deploydulupulangnanti.tekatekigambarikan

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

internal class DiveAudio(private val context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(::focusChanged)
        .build()

    private var foreground = false
    private var hasFocus = false
    private var musicEnabled = false
    private var voiceEnabled = true
    private var music: MediaPlayer? = null
    private var voice: MediaPlayer? = null

    fun setForeground(active: Boolean) {
        foreground = active
        if (!active) {
            stopVoice()
            music?.pause()
            abandonFocus()
        }
        updateMusic()
    }

    fun setOptions(music: Boolean, voice: Boolean) {
        musicEnabled = music
        voiceEnabled = voice
        if (!voice) stopVoice()
        updateMusic()
    }

    fun narrate(id: String, answer: Boolean = false) {
        if (!foreground || !voiceEnabled) return
        stopVoice()
        if (!requestFocus()) return
        val kind = if (answer) "fact" else "clue"
        voice = player("audio/${id}_${kind}.ogg")
        if (voice == null) {
            if (!musicEnabled) abandonFocus()
            return
        }
        voice?.setOnCompletionListener {
            stopVoice()
        }
        music?.setVolume(0.10f, 0.10f)
        voice?.start()
    }

    fun stopVoice() {
        voice?.release()
        voice = null
        music?.setVolume(0.26f, 0.26f)
        if (!musicEnabled) abandonFocus()
    }

    fun close() {
        setForeground(false)
        music?.release()
        music = null
    }

    private fun updateMusic() {
        if (foreground && musicEnabled && requestFocus()) {
            if (music == null) {
                music = player("audio/tidal_notebook.ogg")?.apply {
                    isLooping = true
                    setVolume(0.26f, 0.26f)
                }
            }
            music?.start()
        } else {
            music?.pause()
            if (voice == null && !musicEnabled) abandonFocus()
        }
    }

    private fun requestFocus(): Boolean {
        if (!hasFocus) hasFocus = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        manager.abandonAudioFocusRequest(focus)
        hasFocus = false
    }

    private fun focusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                updateMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                music?.pause()
                stopVoice()
                abandonFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasFocus = false
                music?.pause()
                stopVoice()
            }
        }
    }

    private fun player(path: String): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            context.assets.openFd(path).use { asset ->
                player.setAudioAttributes(attributes)
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                player.prepare()
            }
            player
        } catch (_: java.io.IOException) {
            player.release()
            null
        }
    }
}
