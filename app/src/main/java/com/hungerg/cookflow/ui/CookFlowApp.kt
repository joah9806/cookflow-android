package com.hungerg.cookflow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.hungerg.cookflow.RecipeViewModel
import com.hungerg.cookflow.data.Recipe

private enum class Screen { Home, Saved, Detail, Cooking }

@Composable
fun CookFlowApp(viewModel: RecipeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val gson = remember { Gson() }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var activeRecipeJson by rememberSaveable { mutableStateOf<String?>(null) }
    val activeRecipe = remember(activeRecipeJson, state.detail) {
        state.detail ?: activeRecipeJson?.let { json ->
            runCatching { gson.fromJson(json, Recipe::class.java) }.getOrNull()
        }
    }

    fun goHome() {
        activeRecipeJson = null
        screen = Screen.Home
    }

    LaunchedEffect(screen, activeRecipe) {
        if (screen in listOf(Screen.Detail, Screen.Cooking) && activeRecipe == null) {
            goHome()
        }
    }

    BackHandler(enabled = screen != Screen.Home) {
        when (screen) {
            Screen.Home -> Unit
            Screen.Saved -> screen = Screen.Home
            Screen.Detail -> {
                viewModel.clearDetail()
                activeRecipeJson = null
                screen = Screen.Home
            }
            Screen.Cooking -> goHome()
        }
    }

    when (screen) {
        Screen.Home -> SearchScreen(
            state = state,
            viewModel = viewModel,
            onRecipe = { recipe ->
                activeRecipeJson = gson.toJson(recipe)
                viewModel.openRecipe(recipe)
                screen = Screen.Detail
            },
            onSaved = { screen = Screen.Saved }
        )
        Screen.Saved -> SavedScreen(
            recipes = state.saved,
            onBack = { screen = Screen.Home },
            onRecipe = { recipe ->
                activeRecipeJson = gson.toJson(recipe)
                viewModel.openRecipe(recipe)
                screen = Screen.Detail
            }
        )
        Screen.Detail -> activeRecipe?.let { recipe ->
            RecipeDetailScreen(
                recipe = recipe,
                isSaved = viewModel.isSaved(recipe.id),
                loading = state.detailLoading,
                onBack = {
                    viewModel.clearDetail()
                    activeRecipeJson = null
                    screen = Screen.Home
                },
                onToggleSaved = { viewModel.toggleSaved(recipe) },
                onStartCooking = { screen = Screen.Cooking }
            )
        }
        Screen.Cooking -> activeRecipe?.let { recipe ->
            CookingScreen(
                recipe = recipe,
                onExit = {
                    viewModel.clearDetail()
                    goHome()
                },
                onViewRecipe = {
                    viewModel.openRecipe(recipe)
                    screen = Screen.Detail
                }
            )
        }
    }
}
