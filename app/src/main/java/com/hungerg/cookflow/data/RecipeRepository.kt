package com.hungerg.cookflow.data

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 中文菜谱仓库：内置目录即时可用，联网刷新失败时保留本地数据。
class RecipeRepository(private val context: Context? = null) {

    @Volatile
    private var cachedRecipes: List<Recipe>? = null

    suspend fun discover(): SearchOutcome = withContext(Dispatchers.IO) {
        SearchOutcome(loadRecipes())
    }

    suspend fun refreshCatalog(): SearchOutcome = withContext(Dispatchers.IO) {
        val refreshed = runCatching { downloadCatalog() }.getOrNull()
        SearchOutcome((LocalRecipes.all + (refreshed ?: loadRecipes())).distinctBy { it.id })
    }

    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        SearchOutcome(ChineseRecipeCatalog.search(loadRecipes(), query))
    }

    suspend fun detail(id: String, fallback: Recipe? = null): Recipe = withContext(Dispatchers.IO) {
        loadRecipes().firstOrNull { it.id == id } ?: fallback ?: throw IOException("没有找到这道菜谱")
    }

    private fun loadRecipes(): List<Recipe> {
        cachedRecipes?.let { return it }
        val catalog = context?.let { runCatching { ChineseRecipeCatalog.load(it) }.getOrNull() }.orEmpty()
        return (LocalRecipes.all + catalog).distinctBy { it.id }.also { cachedRecipes = it }
    }

    private fun downloadCatalog(): List<Recipe>? {
        if (context == null) return null
        val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CookFlow-Android/1.1.1")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val recipes = ChineseRecipeCatalog.parse(body).takeIf { it.isNotEmpty() } ?: return null
            cachedRecipes = (LocalRecipes.all + recipes).distinctBy { it.id }
            return recipes
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CATALOG_URL =
            "https://raw.githubusercontent.com/joah9806/cookflow-android/main/app/src/main/assets/chinese_recipes.json"
    }
}
