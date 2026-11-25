package com.hungerg.cookflow.data

import android.content.Context
import com.google.gson.Gson
import java.util.Locale

internal data class ChineseRecipeDocument(
    val source: ChineseRecipeSource? = null,
    val recipes: List<ChineseRecipeEntry>? = emptyList()
)

internal data class ChineseRecipeEntry(
    val id: String,
    val title: String,
    val category: String? = "",
    val area: String? = "",
    val imageUrl: String? = "",
    val ingredients: List<Ingredient>? = emptyList(),
    val steps: List<String>? = emptyList(),
    val sourceLabel: String? = "中文菜谱库",
    val sourceUrl: String? = "",
    val isLocal: Boolean = false
)

internal data class ChineseRecipeSource(
    val name: String = "",
    val url: String = "",
    val license: String = "",
    val upstreamCommit: String = ""
)

// 中文菜谱目录：加载内置或联网更新的中文菜谱，并负责中文关键词检索。
object ChineseRecipeCatalog {
    const val ASSET_NAME = "chinese_recipes.json"

    private val gson = Gson()

    fun load(context: Context): List<Recipe> =
        context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
            parse(reader.readText())
        }

    internal fun parse(json: String): List<Recipe> {
        val document = gson.fromJson(json, ChineseRecipeDocument::class.java)
        return document.recipes.orEmpty().map { entry ->
            Recipe(
                id = entry.id,
                title = entry.title,
                category = entry.category.orEmpty(),
                area = entry.area.orEmpty(),
                imageUrl = entry.imageUrl.orEmpty(),
                ingredients = entry.ingredients.orEmpty().map { ingredient ->
                    ingredient.copy(isSeasoning = RecipeEngine.isSeasoning(ingredient.name))
                },
                steps = entry.steps.orEmpty().mapIndexed { index, text ->
                    CookStep(
                        number = index + 1,
                        text = text.trim(),
                        timerSeconds = RecipeEngine.parseTimerSeconds(text)
                    )
                },
                sourceLabel = entry.sourceLabel.orEmpty().ifBlank { "中文菜谱库" },
                sourceUrl = entry.sourceUrl.orEmpty(),
                isLocal = true
            )
        }.distinctBy { it.id }
    }

    fun search(recipes: List<Recipe>, query: String): List<Recipe> {
        val keyword = query.trim().lowercase(Locale.ROOT)
        if (keyword.isBlank()) return recipes
        return recipes.mapNotNull { recipe ->
            val title = recipe.title.lowercase(Locale.ROOT)
            val category = recipe.category.lowercase(Locale.ROOT)
            val ingredientMatch = recipe.ingredients.any { it.name.lowercase(Locale.ROOT).contains(keyword) }
            val score = when {
                title == keyword -> 0
                title.startsWith(keyword) -> 1
                title.contains(keyword) -> 2
                ingredientMatch -> 3
                category.contains(keyword) -> 4
                else -> null
            }
            score?.let { recipe to it }
        }.sortedWith(compareBy({ it.second }, { it.first.title.length }))
            .map { it.first }
    }
}
