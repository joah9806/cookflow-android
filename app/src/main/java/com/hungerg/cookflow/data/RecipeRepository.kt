package com.hungerg.cookflow.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hungerg.cookflow.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository {

    suspend fun discover(): SearchOutcome = withContext(Dispatchers.IO) {
        val local = LocalRecipes.all
        try {
            SearchOutcome((local + searchRemote("chicken")).distinctBy { it.id })
        } catch (error: Exception) {
            SearchOutcome(local, readableError(error))
        }
    }

    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        val local = LocalRecipes.search(query)
        val translated = RecipeEngine.searchQueryFor(query)
        try {
            SearchOutcome((local + searchRemote(translated)).distinctBy { it.id })
        } catch (error: Exception) {
            SearchOutcome(local, readableError(error))
        }
    }

    suspend fun detail(id: String, fallback: Recipe? = null): Recipe = withContext(Dispatchers.IO) {
        LocalRecipes.all.firstOrNull { it.id == id } ?: lookupRemote(id) ?: fallback ?: throw IOException("没有找到这道菜谱")
    }

    private fun searchRemote(query: String): List<Recipe> {
        if (query.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        return requestMeals("https://www.themealdb.com/api/json/v1/${BuildConfig.MEAL_DB_API_KEY}/search.php?s=$encoded")
    }

    private fun lookupRemote(id: String): Recipe? {
        val encoded = URLEncoder.encode(id, "UTF-8")
        return requestMeals("https://www.themealdb.com/api/json/v1/${BuildConfig.MEAL_DB_API_KEY}/lookup.php?i=$encoded").firstOrNull()
    }

    private fun requestMeals(urlString: String): List<Recipe> {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CookFlow-Android/1.0")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("菜谱服务暂时不可用（HTTP $status）")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parseMeals(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseMeals(body: String): List<Recipe> {
        val root = JsonParser.parseString(body).asJsonObject
        val meals = root.get("meals")?.takeUnless { it.isJsonNull }?.asJsonArray ?: return emptyList()
        return meals.map { parseMeal(it.asJsonObject) }
    }

    private fun parseMeal(json: JsonObject): Recipe {
        fun text(key: String): String = json.get(key)?.takeUnless { it.isJsonNull }?.asString?.trim().orEmpty()
        val ingredients = (1..20).mapNotNull { index ->
            val name = text("strIngredient$index")
            if (name.isBlank()) null else Ingredient(
                name = name,
                measure = text("strMeasure$index"),
                isSeasoning = RecipeEngine.isSeasoning(name)
            )
        }
        val id = text("idMeal")
        return Recipe(
            id = id,
            title = text("strMeal"),
            category = text("strCategory"),
            area = text("strArea"),
            imageUrl = text("strMealThumb"),
            ingredients = ingredients,
            steps = RecipeEngine.splitInstructions(text("strInstructions")),
            sourceLabel = "TheMealDB",
            sourceUrl = "https://www.themealdb.com/meal/$id",
            isLocal = false
        )
    }

    private fun readableError(error: Exception): String = when (error) {
        is IOException -> "网络连接失败，请检查网络后重试"
        else -> "菜谱加载失败，请稍后重试"
    }
}
