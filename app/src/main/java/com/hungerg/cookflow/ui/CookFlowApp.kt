package com.hungerg.cookflow.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.hungerg.cookflow.RecipeViewModel
import com.hungerg.cookflow.data.Recipe
import kotlin.math.roundToInt

// 应用页面导航：保存当前页面与菜谱快照，协调详情和做饭流程。
private enum class Screen { Home, Saved, Detail, Cooking }

@Composable
fun CookFlowApp(viewModel: RecipeViewModel = viewModel()) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val motionScale = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val transitionOffset = with(density) { 24.dp.roundToPx() }
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
        screen = Screen.Home
    }

    LaunchedEffect(screen, activeRecipe) {
        if (screen in listOf(Screen.Detail, Screen.Cooking) && activeRecipe == null) {
            goHome()
        }
    }

    BackHandler(enabled = screen == Screen.Saved || screen == Screen.Detail) {
        when (screen) {
            Screen.Home, Screen.Cooking -> Unit
            Screen.Saved -> screen = Screen.Home
            Screen.Detail -> {
                viewModel.clearDetail()
                screen = Screen.Home
            }
        }
    }

    AnimatedContent(
        targetState = screen,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (motionScale == 0f) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                val enter = slideInHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) { direction * transitionOffset } + fadeIn(tween(180))
                val exit = slideOutHorizontally(
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ) { -direction * transitionOffset } + fadeOut(tween(120))
                enter togetherWith exit using SizeTransform(clip = false)
            }
        },
        label = "screen transition"
    ) { currentScreen ->
        when (currentScreen) {
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
                        screen = Screen.Home
                    },
                    onToggleSaved = { viewModel.toggleSaved(recipe) },
                    onStartCooking = { screen = Screen.Cooking }
                )
            }
            Screen.Cooking -> activeRecipe?.let { recipe ->
                CookingScreen(
                    recipe = recipe,
                    isActive = currentScreen == screen,
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
}
