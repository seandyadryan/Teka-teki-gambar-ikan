package com.deploydulupulangnanti.tekatekigambarikan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
internal fun RiddleScreen(
    expedition: Expedition,
    expeditionIndex: Int,
    specimen: Int,
    picked: String?,
    voice: Boolean,
    exit: () -> Unit,
    listen: () -> Unit,
    answer: (String) -> Unit,
    next: () -> Unit,
) {
    val fish = expedition.fish[specimen]
    val solved = picked == fish.name
    val scroll = rememberScrollState()
    LaunchedEffect(fish.id) { scroll.scrollTo(0) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1140.dp).fillMaxWidth().verticalScroll(scroll)) {
            val wide = maxWidth >= 690.dp
            Column(
                Modifier.fillMaxWidth().padding(horizontal = if (wide) 30.dp else 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = exit) {
                        FieldGlyph(Glyph.Back, Gold, Modifier.size(19.dp))
                        Text("Dive", Modifier.padding(start = 6.dp))
                    }
                    Label(expedition.title.uppercase(java.util.Locale.ROOT), modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Stamp("${specimen + 1} / ${expedition.fish.size}")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    expedition.fish.indices.forEach { index ->
                        Spacer(
                            Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (index <= specimen) Gold else InkLine),
                        )
                    }
                }
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SpecimenPlate(fish, expeditionIndex, specimen, solved, 310, Modifier.weight(1.12f))
                        QuestionSheet(fish, specimen, expedition.fish.lastIndex, picked, voice, listen, answer, next, Modifier.weight(1f))
                    }
                } else {
                    SpecimenPlate(fish, expeditionIndex, specimen, solved, 178, Modifier.fillMaxWidth())
                    QuestionSheet(fish, specimen, expedition.fish.lastIndex, picked, voice, listen, answer, next, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SpecimenPlate(fish: Fish, expedition: Int, specimen: Int, solved: Boolean, height: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.height(height.dp).clip(shape).background(InkDeep).border(1.dp, InkLine, shape)) {
        CurrentLines(Modifier.matchParentSize())
        FishArt(
            fish,
            if (solved) "Illustration of ${fish.name}" else "Mystery fish illustration",
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 13.dp),
        )
        Label("PLATE ${expedition + 1}.${specimen + 1}", Gold, Modifier.align(Alignment.TopStart).padding(11.dp))
        Label(fish.habitat.uppercase(java.util.Locale.ROOT), SeaGlass, Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }
}

@Composable
private fun QuestionSheet(
    fish: Fish,
    specimen: Int,
    last: Int,
    picked: String?,
    voice: Boolean,
    listen: () -> Unit,
    answer: (String) -> Unit,
    next: () -> Unit,
    modifier: Modifier,
) {
    val solved = picked == fish.name
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(Paper).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label(
                if (solved) "SPECIMEN IDENTIFIED" else "THE CLUE",
                DarkMuted,
                Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (voice) ListenButton(if (solved) "Hear fact" else "Hear clue", listen)
        }
        Headline(if (solved) fish.name else fish.question, 29, InkDeep)
        Rule(color = PaperDim)
        if (solved) {
            Text(fish.fact, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
            PrimaryAction(if (specimen == last) "See dive summary" else "Next riddle", Modifier.fillMaxWidth(), next)
        } else {
            val answersPerRow = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2
            fish.choices.withIndex().chunked(answersPerRow).forEach { choices ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    choices.forEach { (index, choice) ->
                        AnswerChoice(
                            choice, ('A' + index).toString(), choice == picked,
                            Modifier.weight(1f),
                        ) { answer(choice) }
                    }
                }
            }
            Text(
                if (picked != null) "Try again. Look closely at the picture." else "Pick one answer to log your discovery.",
                color = if (picked != null) Rust else DarkMuted,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun AnswerChoice(name: String, letter: String, wrong: Boolean, modifier: Modifier, select: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier.clip(shape).background(if (wrong) PaperDim else Paper)
            .border(1.dp, if (wrong) Rust else PaperDim, shape)
            .clickable(role = Role.Button, onClick = select)
            .heightIn(min = 51.dp).padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Label(letter, if (wrong) Rust else DarkMuted)
        Text(name, color = InkDeep, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun ListenButton(text: String, listen: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(PaperDim).clickable(role = Role.Button, onClick = listen)
            .heightIn(min = 48.dp).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FieldGlyph(Glyph.Sound, Ink, Modifier.size(18.dp))
        Text(text, color = InkDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun Complete(expedition: Expedition, score: Int, nextRoute: Expedition?, home: () -> Unit, next: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = home) {
                FieldGlyph(Glyph.Back, Gold, Modifier.size(19.dp))
                Text("Dive", Modifier.padding(start = 6.dp))
            }
            Label("EXPEDITION COMPLETE", Gold)
            Headline(expedition.title, 44)
            Text("You logged all ${expedition.fish.size} fish on this route.", color = Mist, fontSize = 16.sp)
            Stamp("$score / ${expedition.fish.size} ON FIRST TRY")
            Rule()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                expedition.fish.forEach {
                    FishArt(it, it.name, Modifier.weight(1f).height(72.dp))
                }
            }
            PrimaryAction(nextRoute?.let { "Next: ${it.title}" } ?: "Open field guide", Modifier.fillMaxWidth(), next)
            TextButton(onClick = home) { Text("Back to expeditions") }
        }
    }
}

@Composable
internal fun SpecimenDialog(fish: Fish, voice: Boolean, listen: () -> Unit, close: () -> Unit) {
    Dialog(onDismissRequest = close) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Paper)
                .verticalScroll(rememberScrollState()).padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label(fish.habitat.uppercase(java.util.Locale.ROOT), DarkMuted)
            Headline(fish.name, 32, InkDeep)
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(9.dp)).background(InkDeep)) {
                CurrentLines(Modifier.matchParentSize())
                FishArt(fish, null, Modifier.fillMaxSize().padding(8.dp))
            }
            Text(fish.fact, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (voice) ListenButton("Hear fact", listen)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = close) { Text("Close", color = InkDeep) }
            }
        }
    }
}
