package com.deploydulupulangnanti.tekatekigambarikan

import android.content.Context
import org.json.JSONArray

internal data class Fish(
    val id: String,
    val name: String,
    val question: String,
    val fact: String,
    val choices: List<String>,
    val habitat: String,
)

internal data class Expedition(val title: String, val subtitle: String, val fish: List<Fish>)

internal fun loadExpeditions(context: Context): List<Expedition> {
    val content = context.assets.open("riddles.json").bufferedReader().use { it.readText() }
    val array = JSONArray(content)
    val fish = List(array.length()) { index ->
        val item = array.getJSONObject(index)
        val choices = item.getJSONArray("choices")
        Fish(
            id = item.getString("id"),
            name = item.getString("name"),
            question = item.getString("question"),
            fact = item.getString("fact"),
            choices = List(choices.length()) { choices.getString(it) },
            habitat = item.getString("habitat"),
        )
    }
    val routes = listOf(
        "First Dive" to "Get your bearings beneath the waves.",
        "Reef Detectives" to "Look closer at fins, spines and beaks.",
        "Out Past the Reef" to "Meet the drifters and giants.",
        "Odd Fish Out" to "Follow the ocean’s strangest clues.",
    )
    return routes.zip(fish.chunked(4)) { (title, subtitle), specimens ->
        Expedition(title, subtitle, specimens)
    }
}
