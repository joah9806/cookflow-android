package com.hungerg.cookflow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hungerg.cookflow.data.Recipe
import com.hungerg.cookflow.data.RecipeRepository
import com.hungerg.cookflow.data.SavedRecipeStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 菜谱界面状态：协调目录加载、搜索、详情和收藏，不负责解析菜谱。
data class RecipeUiState(
    val query: String = "",
    val recipes: List<Recipe> = emptyList(),
    val saved: List<Recipe> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val detailLoading: Boolean = false,
    val detail: Recipe? = null,
    val error: String? = null,
    val searched: Boolean = false
)

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecipeRepository(application)
    private val store = SavedRecipeStore(application)
    private val _state = MutableStateFlow(
        RecipeUiState(saved = store.load())
    )
    val state: StateFlow<RecipeUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var detailJob: Job? = null

    init {
        refresh()
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun refresh() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = it.recipes.isEmpty(), refreshing = true, error = null) }
            val bundled = repository.discover()
            ensureActive()
            _state.update {
                it.copy(recipes = bundled.recipes, loading = false, searched = false)
            }
            val refreshed = repository.refreshCatalog()
            ensureActive()
            _state.update {
                it.copy(
                    recipes = refreshed.recipes,
                    refreshing = false,
                    error = refreshed.partialError
                )
            }
        }
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isBlank()) {
            refresh()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, refreshing = false, error = null, searched = true) }
            val result = repository.search(query)
            ensureActive()
            _state.update {
                it.copy(
                    recipes = result.recipes,
                    loading = false,
                    error = result.partialError
                )
            }
        }
    }

    fun openRecipe(recipe: Recipe) {
        detailJob?.cancel()
        if (recipe.isLocal) {
            _state.update { it.copy(detail = recipe, detailLoading = false, error = null) }
            return
        }
        detailJob = viewModelScope.launch {
            _state.update { it.copy(detailLoading = true, detail = recipe, error = null) }
            val result = runCatching { repository.detail(recipe.id, recipe) }
            ensureActive()
            _state.update { current ->
                if (current.detail?.id != recipe.id) {
                    current
                } else {
                    current.copy(
                        detail = result.getOrNull() ?: recipe,
                        detailLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun clearDetail() {
        detailJob?.cancel()
        detailJob = null
        _state.update { it.copy(detail = null, detailLoading = false) }
    }

    fun isSaved(id: String): Boolean = _state.value.saved.any { it.id == id }

    fun toggleSaved(recipe: Recipe) {
        val updated = if (isSaved(recipe.id)) {
            _state.value.saved.filterNot { it.id == recipe.id }
        } else {
            listOf(recipe) + _state.value.saved
        }
        store.save(updated)
        _state.update { it.copy(saved = updated) }
    }
}
