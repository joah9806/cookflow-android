package com.hungerg.cookflow

import com.hungerg.cookflow.data.Ingredient
import com.hungerg.cookflow.data.LocalRecipes
import com.hungerg.cookflow.data.Recipe
import com.hungerg.cookflow.data.RecipeEngine
import com.hungerg.cookflow.data.RecipeRepository
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEngineTest {

    @Test
    fun splitsNumberedInstructionsAndSentences() {
        val steps = RecipeEngine.splitInstructions(
            "1. Heat the oil in a pan. Add the onion.\n2. Cook for 2 minutes. Add salt and serve. Enjoy!"
        )
        assertEquals(4, steps.size)
        assertEquals("Heat the oil in a pan.", steps[0].text)
        assertEquals("Cook for 2 minutes.", steps[2].text)
        assertEquals(120, steps[2].timerSeconds)
        assertFalse(steps.any { it.text == "Enjoy!" })
    }

    @Test
    fun preservesParagraphBreaksWithoutSplittingDecimals() {
        val steps = RecipeEngine.splitInstructions(
            "Add 1.2 kg chicken and cook for 5 minutes.\r\n\r\nThen add the sauce."
        )
        assertEquals(2, steps.size)
        assertTrue(steps[0].text.contains("1.2 kg"))
        assertEquals(300, steps[0].timerSeconds)
    }

    @Test
    fun parsesEnglishAndChineseTimers() {
        assertEquals(900, RecipeEngine.parseTimerSeconds("Cook covered for 15 minutes."))
        assertEquals(120, RecipeEngine.parseTimerSeconds("Sauté for couple of minutes."))
        assertEquals(90, RecipeEngine.parseTimerSeconds("翻炒 1 分 30 秒后出锅。"))
        assertEquals(120, RecipeEngine.parseTimerSeconds("中火翻炒2分钟。"))
        assertEquals(0, RecipeEngine.parseTimerSeconds("Add salt to taste."))
    }

    @Test
    fun identifiesSeasoningsAndSuggestedStepsInOrder() {
        val seasonings = LocalRecipes.tomatoEgg.seasonings
        assertEquals(listOf("盐", "食用油", "白糖", "盐", "葱花"), seasonings.map { it.ingredientName })
        assertEquals(listOf(1, 2, 4, 4, 6), seasonings.map { it.suggestedStep })
        assertEquals(5, seasonings.map { it.id }.distinct().size)
    }

    @Test
    fun recognizesEnglishSeasoningMentions() {
        val recipe = Recipe(
            id = "test",
            title = "Test",
            ingredients = listOf(
                Ingredient("Vegetable oil", "1 tbsp", true),
                Ingredient("Salt", "To taste", true),
                Ingredient("Chicken", "500 g", false)
            ),
            steps = RecipeEngine.splitInstructions("Heat the oil. Add the chicken and season with salt.")
        )
        val queue = recipe.seasonings
        assertEquals(2, queue.size)
        assertEquals(1, queue.first { it.ingredientName == "Vegetable oil" }.suggestedStep)
        assertEquals(2, queue.first { it.ingredientName == "Salt" }.suggestedStep)
        assertFalse(queue.any { it.ingredientName == "Chicken" })
    }

    @Test
    fun localRecipesExposeDetectedTimers() {
        assertEquals(120, LocalRecipes.tomatoEgg.steps[2].timerSeconds)
        assertEquals(60, LocalRecipes.tomatoEgg.steps[4].timerSeconds)
        assertEquals(300, LocalRecipes.garlicBroccoli.steps[0].timerSeconds)
        assertEquals(60, LocalRecipes.garlicBroccoli.steps[1].timerSeconds)
    }

    @Test
    fun nullMealSearchResultIsEmptyRatherThanAnError() {
        val repository = RecipeRepository()
        assertTrue(repository.parseMeals("""{"meals":null}""").isEmpty())

        val recipes = repository.parseMeals(
            """{"meals":[{"idMeal":"1","strMeal":"Test meal","strInstructions":"Heat the oil. Add salt.","strIngredient1":"Vegetable oil","strMeasure1":"1 tbsp","strIngredient2":"Salt","strMeasure2":"To taste"}]}"""
        )
        assertEquals(1, recipes.size)
        assertEquals("Test meal", recipes.first().title)
        assertEquals(2, recipes.first().steps.size)
        assertEquals(listOf(1, 2), recipes.first().seasonings.map { it.suggestedStep })
    }

    @Test
    fun recipeCanBeSerializedForScreenRestoration() {
        val gson = Gson()
        val restored = gson.fromJson(gson.toJson(LocalRecipes.tomatoEgg), Recipe::class.java)
        assertEquals(LocalRecipes.tomatoEgg.title, restored.title)
        assertEquals(LocalRecipes.tomatoEgg.steps, restored.steps)
        assertEquals(LocalRecipes.tomatoEgg.seasonings.map { it.id }, restored.seasonings.map { it.id })
    }

    @Test
    fun mapsCommonChineseSearchTerms() {
        assertEquals("chicken", RecipeEngine.searchQueryFor("鸡肉"))
        assertEquals("beef", RecipeEngine.searchQueryFor("牛肉"))
        assertEquals("pasta", RecipeEngine.searchQueryFor("意大利面"))
        assertEquals("Chicken Handi", RecipeEngine.searchQueryFor("Chicken Handi"))
    }
}
