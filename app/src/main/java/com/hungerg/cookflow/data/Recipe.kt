package com.hungerg.cookflow.data

data class Ingredient(
    val name: String,
    val measure: String = "",
    val isSeasoning: Boolean = false
)

data class CookStep(
    val number: Int,
    val text: String,
    val timerSeconds: Int = 0
)

data class SeasoningItem(
    val ingredientName: String,
    val measure: String,
    val suggestedStep: Int,
    val occurrence: Int = 1,
    val added: Boolean = false
) {
    val id: String get() = "${ingredientName.lowercase()}#${suggestedStep}#$occurrence"
}

data class Recipe(
    val id: String,
    val title: String,
    val category: String = "",
    val area: String = "",
    val imageUrl: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<CookStep> = emptyList(),
    val sourceLabel: String = "TheMealDB",
    val sourceUrl: String = "",
    val isLocal: Boolean = false
) {
    val seasonings: List<SeasoningItem>
        get() = RecipeEngine.buildSeasoningQueue(this)
}

data class SearchOutcome(
    val recipes: List<Recipe>,
    val partialError: String? = null
)
