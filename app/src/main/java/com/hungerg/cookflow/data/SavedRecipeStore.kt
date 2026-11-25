package com.hungerg.cookflow.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SavedRecipeStore(context: Context) {
    private val preferences = context.getSharedPreferences("cookflow_saved", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): List<Recipe> = runCatching {
        val json = preferences.getString(KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Recipe>>() {}.type
        gson.fromJson<List<Recipe>>(json, type).orEmpty()
    }.getOrDefault(emptyList())

    fun save(recipes: List<Recipe>) {
        preferences.edit().putString(KEY, gson.toJson(recipes)).apply()
    }

    private companion object {
        const val KEY = "saved_recipes"
    }
}
