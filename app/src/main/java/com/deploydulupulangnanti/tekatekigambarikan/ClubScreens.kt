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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val destinations = listOf(
    Triple(Page.Dive, "Dive", Glyph.Dive),
    Triple(Page.Guide, "Field guide", Glyph.Book),
    Triple(Page.Settings, "Settings", Glyph.Settings),
)

@Composable
internal fun ClubScreen(page: Page, navigate: (Page) -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Rule()
        NavigationBar(containerColor = InkDeep, tonalElevation = 0.dp, windowInsets = androidx.compose.foundation.layout.WindowInsets(0)) {
            destinations.forEach { (destination, title, glyph) ->
                NavigationBarItem(
                    selected = page == destination,
                    onClick = { navigate(destination) },
                    icon = { FieldGlyph(glyph, if (page == destination) Ink else Mist, Modifier.size(23.dp)) },
                    label = { Text(title) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Gold,
                        selectedTextColor = Paper,
                        unselectedTextColor = Mist,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun DiveHome(
    expeditions: List<Expedition>,
    found: Set<String>,
    current: Int,
    begin: (Int) -> Unit,
) {
    val total = expeditions.sumOf { it.fish.size }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1170.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val wide = maxWidth >= 690.dp
            val horizontal = if (wide) 32.dp else 20.dp
            Column(
                Modifier.fillMaxWidth().padding(horizontal = horizontal, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FieldGlyph(Glyph.Dive, Gold, Modifier.size(24.dp))
                    Label("THE OCEAN FIELD CLUB", modifier = Modifier.weight(1f))
                    Stamp("${found.size} / $total")
                }
                Rule()
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        HomeCover(expeditions.first().fish.first(), current, found.size == total, Modifier.weight(1.16f), begin)
                        Routes(expeditions, found, current, Modifier.weight(1f), begin)
                    }
                } else {
                    HomeCover(expeditions.first().fish.first(), current, found.size == total, Modifier.fillMaxWidth(), begin)
                    Routes(expeditions, found, current, Modifier.fillMaxWidth(), begin)
                }
            }
        }
    }
}

@Composable
private fun HomeCover(fish: Fish, current: Int, completed: Boolean, modifier: Modifier, begin: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().clip(shape).background(InkDeep).border(1.dp, InkLine, shape)) {
            CurrentLines(Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 17.dp, bottom = 12.dp)) {
                Label("ILLUSTRATED OCEAN RIDDLES", Gold)
                Spacer(Modifier.height(8.dp))
                Headline("Fish Riddle\nClub", 43)
                FishArt(
                    fish, null,
                    Modifier.align(Alignment.End).fillMaxWidth(0.75f).height(153.dp),
                )
                Rule()
                Text(
                    "Read the clue. Name the fish.",
                    Modifier.padding(top = 9.dp),
                    color = Mist, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }
        }
        PrimaryAction(
            if (completed) "Dive again" else if (current == 0) "Start diving" else "Continue diving",
            Modifier.fillMaxWidth(),
        ) { begin(current) }
    }
}

@Composable
private fun Routes(expeditions: List<Expedition>, found: Set<String>, current: Int, modifier: Modifier, begin: (Int) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("THE EXPEDITIONS")
        expeditions.forEachIndexed { index, expedition ->
            val complete = expedition.fish.count { it.id in found }
            val selected = index == current && complete < expedition.fish.size
            val shape = RoundedCornerShape(9.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (selected) InkLift else Ink)
                    .border(1.dp, if (selected) InkLine else InkLift, shape)
                    .clickable(role = Role.Button) { begin(index) }
                    .heightIn(min = 59.dp).padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Label((index + 1).toString().padStart(2, '0'), if (selected) Gold else SeaGlass)
                Column(Modifier.weight(1f)) {
                    Text(expedition.title, color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(expedition.subtitle, color = Mist, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Text("$complete/${expedition.fish.size}", color = SeaGlass, fontSize = 12.sp)
                FieldGlyph(if (complete == expedition.fish.size) Glyph.Check else Glyph.Forward, Gold, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun Guide(expeditions: List<Expedition>, found: Set<String>, show: (Fish) -> Unit) {
    val fish = expeditions.flatMap { it.fish }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1080.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val columns = if (maxWidth >= 760.dp) 4 else if (maxWidth >= 540.dp) 3 else 2
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("THE SPECIMEN CABINET")
                Headline("Field guide", 44)
                Text("Solve riddles to fill your guide.", color = Mist, fontSize = 15.sp)
                Stamp("${found.size} OF ${fish.size} DISCOVERED")
                fish.withIndex().chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (index, specimen) ->
                            val unlocked = specimen.id in found
                            val shape = RoundedCornerShape(12.dp)
                            Column(
                                Modifier.weight(1f).clip(shape).background(InkDeep).border(1.dp, InkLine, shape)
                                    .clickable(enabled = unlocked, role = Role.Button) { show(specimen) }
                                    .padding(11.dp),
                            ) {
                                Label("NO. " + (index + 1).toString().padStart(2, '0'), if (unlocked) Gold else Mist)
                                Box(Modifier.fillMaxWidth().aspectRatio(1.46f), contentAlignment = Alignment.Center) {
                                    if (unlocked) FishArt(specimen, null, Modifier.fillMaxSize())
                                    else Headline("?", 42, InkLine)
                                }
                                Text(
                                    if (unlocked) specimen.name else "Undiscovered",
                                    color = if (unlocked) Paper else Mist, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Settings(music: Boolean, voice: Boolean, setMusic: (Boolean) -> Unit, setVoice: (Boolean) -> Unit, openPrivacy: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 660.dp).fillMaxWidth().fillMaxHeight()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Label("THE CLUBHOUSE")
            Headline("Settings", 44)
            Rule()
            Toggle("Narration", "Hear each clue and fish fact in English.", voice, setVoice)
            Toggle("Music", "Play the original Tidal Notebook soundtrack.", music, setMusic)
            Rule()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = openPrivacy)
                    .heightIn(min = 52.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Privacy policy", color = Paper, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                FieldGlyph(Glyph.Forward, Gold, Modifier.size(20.dp))
            }
            Text("PT Deploy Dulu Pulang Nanti  •  1.0", color = Mist, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Toggle(title: String, summary: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(InkLift)
            .toggleable(value = checked, role = Role.Switch, onValueChange = change)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Paper, fontWeight = FontWeight.SemiBold)
            Text(summary, color = Mist, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Switch(
            checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = InkDeep),
        )
    }
}
