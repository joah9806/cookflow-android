package com.hungerg.cookflow.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SeasoningOrderStore(context: Context) {
    private val preferences = context.getSharedPreferences("cookflow_seasoning_order", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(recipeId: String): List<String> = runCatching {
        val json = preferences.getString(recipeId, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(json, type).orEmpty()
    }.getOrDefault(emptyList())

    fun save(recipeId: String, order: List<String>) {
        preferences.edit().putString(recipeId, gson.toJson(order)).apply()
    }
}
