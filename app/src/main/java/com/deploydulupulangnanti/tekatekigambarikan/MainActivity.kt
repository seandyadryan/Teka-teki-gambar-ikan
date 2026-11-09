package com.deploydulupulangnanti.tekatekigambarikan

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri

internal const val PRIVACY_URL = "https://seandyadryan.github.io/Teka-teki-gambar-ikan/"

class MainActivity : AppCompatActivity() {
    private lateinit var audio: DiveAudio
    private var chosenLanguage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chosenLanguage = selectedLanguageTag()
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
        audio = DiveAudio(this, getString(R.string.language_tag))
        val expeditions = loadExpeditions(this)
        val preferences = getSharedPreferences("dive_log", MODE_PRIVATE)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Gold, onPrimary = Ink, surface = Ink, onSurface = Paper,
                    secondary = SeaGlass, onSecondary = Ink, background = Ink, onBackground = Paper,
                ),
            ) {
                Box(Modifier.fillMaxSize().background(Ink).safeDrawingPadding()) {
                    RiddleClub(
                        expeditions, preferences, audio,
                        selectedLanguage = chosenLanguage,
                        changeLanguage = { tag ->
                            selectLanguage(tag)
                            chosenLanguage = tag
                        },
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri())
                        try {
                            startActivity(intent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(this@MainActivity, PRIVACY_URL, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        chosenLanguage = selectedLanguageTag()
        audio.setForeground(true)
    }

    override fun onPause() {
        audio.setForeground(false)
        super.onPause()
    }

    override fun onDestroy() {
        audio.close()
        super.onDestroy()
    }
}

@Composable
private fun RiddleClub(
    expeditions: List<Expedition>,
    preferences: SharedPreferences,
    audio: DiveAudio,
    selectedLanguage: String,
    changeLanguage: (String) -> Unit,
    openPrivacy: () -> Unit,
) {
    var found by remember { mutableStateOf(preferences.getStringSet("found", emptySet()).orEmpty().toSet()) }
    var music by remember { mutableStateOf(preferences.getBoolean("music", false)) }
    var voice by remember { mutableStateOf(preferences.getBoolean("voice", true)) }
    var page by rememberSaveable { mutableStateOf(Page.Dive) }
    var route by rememberSaveable { mutableStateOf(0) }
    var specimen by rememberSaveable { mutableStateOf(0) }
    var picked by key("picked-choice-index") { rememberSaveable { mutableStateOf<Int?>(null) } }
    var wrongCount by rememberSaveable { mutableStateOf(0) }
    var resultCount by rememberSaveable { mutableStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }

    val canListen = voice && audio.narrationStatus == NarrationStatus.Ready

    androidx.compose.runtime.LaunchedEffect(music, voice) { audio.setOptions(music, voice) }

    fun go(to: Page) {
        audio.stopVoice()
        detail = null
        page = to
    }

    fun begin(index: Int) {
        route = index
        specimen = 0
        picked = null
        wrongCount = 0
        resultCount = 0
        go(Page.Riddle)
        audio.narrate(expeditions[index].fish.first())
    }

    BackHandler(page != Page.Dive || detail != null) {
        if (detail != null) {
            detail = null
            audio.stopVoice()
        } else {
            go(Page.Dive)
        }
    }

    val expedition = expeditions[route]
    val fish = expedition.fish[specimen]
    val current = expeditions.indexOfFirst { trip -> trip.fish.any { it.id !in found } }.coerceAtLeast(0)
    when (page) {
        Page.Dive -> ClubScreen(page, ::go) {
            DiveHome(expeditions, found, current, ::begin)
        }
        Page.Guide -> ClubScreen(page, ::go) {
            Guide(expeditions, found) { detail = it.id }
        }
        Page.Settings -> ClubScreen(page, ::go) {
            Settings(
                music, voice,
                selectedLanguage = selectedLanguage,
                effectiveLanguage = audio.languageTag,
                changeLanguage = changeLanguage,
                narrationStatus = audio.narrationStatus,
                setMusic = {
                    music = it
                    preferences.edit().putBoolean("music", it).apply()
                },
                setVoice = {
                    voice = it
                    preferences.edit().putBoolean("voice", it).apply()
                },
                openPrivacy = openPrivacy,
            )
        }
        Page.Riddle -> RiddleScreen(
            expedition = expedition,
            expeditionIndex = route,
            specimen = specimen,
            picked = picked,
            voice = canListen,
            exit = { go(Page.Dive) },
            listen = { audio.narrate(fish, picked == fish.correctChoice) },
            answer = { choice ->
                if (picked != fish.correctChoice && picked != choice) {
                    picked = choice
                    if (choice == fish.correctChoice) {
                        if (wrongCount == 0) {
                            resultCount++
                        }
                        found = found + fish.id
                        preferences.edit().putStringSet("found", found).apply()
                        audio.narrate(fish, answer = true)
                    } else {
                        wrongCount++
                    }
                }
            },
            next = {
                if (specimen == expedition.fish.lastIndex) {
                    go(Page.Complete)
                } else {
                    specimen++
                    picked = null
                    wrongCount = 0
                    audio.narrate(expedition.fish[specimen])
                }
            },
        )
        Page.Complete -> Complete(
            expedition, resultCount,
            nextRoute = expeditions.getOrNull(route + 1),
            home = { go(Page.Dive) },
            next = { if (route < expeditions.lastIndex) begin(route + 1) else go(Page.Guide) },
        )
    }

    val selected = expeditions.asSequence().flatMap { it.fish.asSequence() }.firstOrNull { it.id == detail }
    if (selected != null && selected.id in found) {
        SpecimenDialog(
            selected, canListen,
            listen = { audio.narrate(selected, answer = true) },
            close = {
                detail = null
                audio.stopVoice()
            },
        )
    }
}

internal enum class Page { Dive, Guide, Settings, Riddle, Complete }
