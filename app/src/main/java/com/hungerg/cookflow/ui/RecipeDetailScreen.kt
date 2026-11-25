package com.hungerg.cookflow.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hungerg.cookflow.data.Ingredient
import com.hungerg.cookflow.data.Recipe
import java.util.Locale

// 菜谱详情页面：展示食材、调料顺序和步骤，不修改菜谱数据。
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    isSaved: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
    onStartCooking: () -> Unit
) {
    val context = LocalContext.current
    var checkedIngredients by remember(recipe.id) { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text("菜谱详情", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleSaved) {
                    Icon(
                        if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isSaved) "取消收藏" else "收藏菜谱",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Button(
                    onClick = onStartCooking,
                    enabled = recipe.steps.isNotEmpty() && !loading,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp).heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loading) "正在准备菜谱…" else "开始做饭 · ${recipe.steps.size} 步", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp)
        ) {
            item {
                RecipeImage(
                    recipe = recipe,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(20.dp))
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOf(recipe.category, recipe.area, "${recipe.ingredients.size} 种食材").filter { it.isNotBlank() }.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("食材与调料", "已备好 ${checkedIngredients.size}/${recipe.ingredients.size}")
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipe.ingredients.forEach { ingredient ->
                            IngredientChip(
                                ingredient = ingredient,
                                checked = checkedIngredients.contains(ingredient.name),
                                onToggle = {
                                    checkedIngredients = if (checkedIngredients.contains(ingredient.name)) {
                                        checkedIngredients - ingredient.name
                                    } else {
                                        checkedIngredients + ingredient.name
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("调料添加顺序", "${recipe.seasonings.size} 项 · 可在做饭时拖动调整")
                    if (recipe.seasonings.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recipe.seasonings) { seasoning ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Text(
                                            if (seasoning.suggestedStep > 0) "第 ${seasoning.suggestedStep} 步" else "按需",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(seasoning.ingredientName, style = MaterialTheme.typography.titleMedium)
                                        if (seasoning.measure.isNotBlank()) {
                                            Text(seasoning.measure, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("未识别到明确调料，可在做饭流程中按步骤添加。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(28.dp))
                    SectionTitle("烹饪步骤", "已拆分为 ${recipe.steps.size} 步")
                    Spacer(Modifier.height(12.dp))
                }
            }
            itemsIndexed(recipe.steps) { index, step ->
                StepCard(
                    number = step.number,
                    text = step.text,
                    timerSeconds = step.timerSeconds,
                    isLast = index == recipe.steps.lastIndex,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        "数据来源：${recipe.sourceLabel}。食材用量与做法以原菜谱为准；过敏原、食品安全及熟度请自行确认。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (recipe.sourceUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recipe.sourceUrl)))
                                } catch (_: ActivityNotFoundException) {
                                }
                            }
                        ) {
                            Text("查看原始菜谱", textDecoration = TextDecoration.Underline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IngredientChip(ingredient: Ingredient, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = { onToggle() }
        ),
        shape = MaterialTheme.shapes.small,
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape)
                    .background(if (checked) MaterialTheme.colorScheme.secondary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(ingredient.name, style = MaterialTheme.typography.labelLarge)
                if (ingredient.measure.isNotBlank()) {
                    Text(ingredient.measure, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepCard(number: Int, text: String, timerSeconds: Int, isLast: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        number.toString().padStart(2, '0'),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (!isLast) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(2.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                if (timerSeconds > 0) {
                    Spacer(Modifier.height(8.dp))
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("建议计时 ${formatDuration(timerSeconds)}") },
                        leadingIcon = { TimerIcon(modifier = Modifier.size(20.dp)) },
                        colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val remaining = safe % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remaining)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remaining)
    }
}
