package com.deploydulupulangnanti.tekatekigambarikan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class Fish(
    val id: String,
    val name: String,
    val question: String,
    val fact: String,
    val choices: List<String>,
    val correctChoice: Int,
    val habitat: String,
)

internal data class Expedition(val title: String, val subtitle: String, val fish: List<Fish>)

internal fun loadExpeditions(context: Context): List<Expedition> {
    fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    val source = JSONArray(asset("riddles.json"))
    val tag = context.getString(R.string.language_tag)
    val translation = if (tag == "en") null else JSONObject(asset("translations/$tag.json"))
    val specimens = translation?.getJSONObject("fish")
    val names = mutableMapOf<String, String>()
    translation?.getJSONObject("otherNames")?.let { other ->
        other.keys().forEach { name -> names[name] = other.getString(name) }
    }
    val entries = List(source.length()) { source.getJSONObject(it) }
    entries.forEach { item ->
        val name = item.getString("name")
        names[name] = specimens?.getJSONObject(item.getString("id"))?.getString("name") ?: name
    }

    val fish = entries.map { item ->
        val id = item.getString("id")
        val englishName = item.getString("name")
        val localized = specimens?.getJSONObject(id) ?: item
        val rawChoices = item.getJSONArray("choices")
        val choices = List(rawChoices.length()) { rawChoices.getString(it) }
        Fish(
            id = id,
            name = names.getValue(englishName),
            question = localized.getString("question"),
            fact = localized.getString("fact"),
            choices = choices.map { names[it] ?: it },
            correctChoice = choices.indexOf(englishName),
            habitat = localized.getString("habitat"),
        )
    }
    val routes = listOf(
        R.string.route_first to R.string.route_first_sub,
        R.string.route_reef to R.string.route_reef_sub,
        R.string.route_open to R.string.route_open_sub,
        R.string.route_odd to R.string.route_odd_sub,
    )
    return routes.zip(fish.chunked(4)) { (title, subtitle), routeFish ->
        Expedition(context.getString(title), context.getString(subtitle), routeFish)
    }
}
