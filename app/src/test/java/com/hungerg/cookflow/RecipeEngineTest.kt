package com.hungerg.cookflow

import com.hungerg.cookflow.data.ChineseRecipeCatalog
import com.hungerg.cookflow.data.Ingredient
import com.hungerg.cookflow.data.LocalRecipes
import com.hungerg.cookflow.data.Recipe
import com.hungerg.cookflow.data.RecipeEngine
import com.google.gson.Gson
import java.io.File
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
    fun chineseSearchFindsBraisedPorkRibs() {
        val results = ChineseRecipeCatalog.search(LocalRecipes.all, "红烧排骨")
        assertEquals("红烧排骨", results.first().title)
        assertEquals(7, results.first().steps.size)
        assertTrue(results.first().steps.all { step -> !step.text.contains(Regex("[A-Za-z]")) })
        assertEquals(2100, results.first().steps[4].timerSeconds)
    }

    @Test
    fun chineseCatalogParsesIngredientsStepsAndTimers() {
        val recipes = ChineseRecipeCatalog.parse(
            """{"source":{"name":"测试中文菜谱"},"recipes":[{"id":"test-1","title":"测试菜","ingredients":[{"name":"盐","measure":"适量"}],"steps":["翻炒 2 分钟后出锅。"],"isLocal":false}]}"""
        )
        val recipe = recipes.single()
        assertEquals("测试菜", recipe.title)
        assertTrue(recipe.ingredients.single().isSeasoning)
        assertEquals(120, recipe.steps.single().timerSeconds)
        assertTrue(recipe.isLocal)
    }

    @Test
    fun bundledChineseCatalogUsesChineseStepsOnly() {
        val recipes = ChineseRecipeCatalog.parse(File("src/main/assets/chinese_recipes.json").readText())
        assertTrue(recipes.size >= 150)
        assertTrue(recipes.all { recipe -> recipe.steps.isNotEmpty() })
        assertTrue(recipes.all { recipe -> recipe.steps.all { step -> !step.text.contains(Regex("[A-Za-z]")) } })
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
    fun chineseSearchRanksExactTitleBeforeIngredientMatches() {
        val recipes = listOf(
            Recipe(id = "1", title = "番茄炒蛋", ingredients = listOf(Ingredient("红烧排骨"))),
            Recipe(id = "2", title = "红烧排骨")
        )
        assertEquals("2", ChineseRecipeCatalog.search(recipes, "红烧排骨").first().id)
    }
}
