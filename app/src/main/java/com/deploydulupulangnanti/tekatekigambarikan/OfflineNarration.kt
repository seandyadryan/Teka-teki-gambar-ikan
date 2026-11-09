package com.deploydulupulangnanti.tekatekigambarikan

import android.content.Context
import android.icu.util.ULocale
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

internal enum class NarrationStatus { Loading, Ready, Missing }

internal class OfflineNarration(
    context: Context,
    languageTag: String,
    audioAttributes: AudioAttributes,
    private val onFinished: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val locale = Locale.forLanguageTag(languageTag)
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext) { result ->
        handler.post {
            val speech = engine ?: return@post
            if (result == TextToSpeech.SUCCESS) {
                initialized = true
                speech.setAudioAttributes(audioAttributes)
                speech.setOnUtteranceProgressListener(listener)
                refresh()
            } else {
                status = NarrationStatus.Missing
            }
        }
    }
    private var sequence = 0L
    private var currentUtterance: String? = null
    private var initialized = false
    var status by mutableStateOf(NarrationStatus.Loading)
        private set

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) = Unit
        override fun onDone(utteranceId: String) = finish(utteranceId)
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String) = finish(utteranceId)
        override fun onError(utteranceId: String, errorCode: Int) = finish(utteranceId)
        override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
    }

    fun refresh() {
        val speech = engine ?: return
        if (!initialized) return
        val preferredScript = ULocale.addLikelySubtags(ULocale.forLocale(locale)).script
        val voice = speech.voices.orEmpty()
            .asSequence()
            .filter {
                !it.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() &&
                    it.locale.language == locale.language &&
                    ULocale.addLikelySubtags(ULocale.forLocale(it.locale)).script == preferredScript
            }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale == locale }
                .thenByDescending { it.locale.country == locale.country }
                .thenByDescending { it.quality })
            .firstOrNull { speech.setVoice(it) == TextToSpeech.SUCCESS }
        status = if (voice != null) NarrationStatus.Ready else NarrationStatus.Missing
    }

    fun speak(text: String): Boolean {
        if (status != NarrationStatus.Ready) return false
        val id = (++sequence).toString()
        currentUtterance = id
        val started = engine?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id) == TextToSpeech.SUCCESS
        if (!started) currentUtterance = null
        return started
    }

    fun stop() {
        if (currentUtterance == null) return
        currentUtterance = null
        engine?.stop()
    }

    fun close() {
        stop()
        engine?.shutdown()
        engine = null
    }

    private fun finish(utteranceId: String) {
        handler.post {
            if (currentUtterance == utteranceId) {
                currentUtterance = null
                onFinished()
            }
        }
    }
}
