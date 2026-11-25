package com.hungerg.cookflow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hungerg.cookflow.data.Recipe
import com.hungerg.cookflow.data.SeasoningItem
import com.hungerg.cookflow.data.SeasoningOrderStore
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingScreen(
    recipe: Recipe,
    onExit: () -> Unit,
    onViewRecipe: () -> Unit
) {
    val context = LocalContext.current
    val orderStore = remember(recipe.id) { SeasoningOrderStore(context) }
    val initialSeasonings = remember(recipe.id) { recipe.seasonings }
    var order by remember(recipe.id) {
        mutableStateOf(
            (orderStore.load(recipe.id).filter { id -> initialSeasonings.any { it.id == id } } +
                initialSeasonings.map { it.id }).distinct()
        )
    }
    val seasonings = remember(order, initialSeasonings) {
        order.mapNotNull { id -> initialSeasonings.firstOrNull { it.id == id } }
    }
    val addedIds = remember(recipe.id) { mutableStateListOf<String>() }
    var currentIndex by rememberSaveable(recipe.id) { mutableIntStateOf(0) }
    var finished by rememberSaveable(recipe.id) { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSeasoningSheet by remember { mutableStateOf(false) }
    var timerRemaining by rememberSaveable(recipe.id) { mutableIntStateOf(0) }
    var timerRunning by rememberSaveable(recipe.id) { mutableStateOf(false) }
    var timerFinished by rememberSaveable(recipe.id) { mutableStateOf(false) }
    var timerInitialized by rememberSaveable(recipe.id) { mutableStateOf(false) }
    var previousStepIndex by rememberSaveable(recipe.id) { mutableIntStateOf(-1) }
    val currentStep = recipe.steps.getOrNull(currentIndex)

    fun moveSeasoning(from: Int, to: Int) {
        if (from == to || from !in order.indices || to !in order.indices) return
        order = order.toMutableList().apply {
            val item = removeAt(from)
            add(to, item)
        }
        orderStore.save(recipe.id, order)
    }

    BackHandler(enabled = !showSeasoningSheet) {
        if (finished) onExit() else showExitDialog = true
    }

    LaunchedEffect(currentIndex) {
        if (!timerInitialized || previousStepIndex != currentIndex) {
            timerRemaining = currentStep?.timerSeconds ?: 0
            timerRunning = false
            timerFinished = false
            timerInitialized = true
            previousStepIndex = currentIndex
        }
    }

    LaunchedEffect(timerRunning, timerRemaining) {
        while (timerRunning && timerRemaining > 0) {
            delay(1_000)
            timerRemaining -= 1
        }
        if (timerRunning && timerRemaining == 0) {
            timerRunning = false
            timerFinished = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { TimerIcon(modifier = Modifier.size(24.dp)) },
            title = { Text("暂时退出做饭？") },
            text = { Text("当前步骤和已添加调料不会保留，调料排序会保存在本机。") },
            confirmButton = { TextButton(onClick = onExit) { Text("退出") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("继续做饭") } }
        )
    }

    if (finished) {
        CompletionScreen(
            recipe = recipe,
            addedCount = addedIds.size,
            onViewRecipe = onViewRecipe,
            onExit = onExit
        )
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExitDialog = true }) { Icon(Icons.Filled.Close, contentDescription = "退出做饭") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("正在做饭", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { showSeasoningSheet = true }) {
                    Text("调料顺序")
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(0.85f).height(56.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("上一步")
                    }
                    Button(
                        onClick = {
                            if (currentIndex < recipe.steps.lastIndex) currentIndex++ else finished = true
                        },
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            if (currentIndex == recipe.steps.lastIndex) "完成做饭" else "下一步",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentIndex < recipe.steps.lastIndex) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "步骤 ${(currentIndex + 1).toString().padStart(2, '0')} / ${recipe.steps.size.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnums"),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "已添加 ${addedIds.size}/${seasonings.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnums"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / recipe.steps.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraLarge),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            currentStep?.let { step ->
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    step.number.toString().padStart(2, '0'),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnums")
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(step.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item {
                TimerPanel(
                    suggestedSeconds = currentStep?.timerSeconds ?: 0,
                    remaining = timerRemaining,
                    running = timerRunning,
                    finished = timerFinished,
                    onToggle = {
                        when {
                            timerRemaining > 0 -> {
                                timerRunning = !timerRunning
                                timerFinished = false
                            }
                            timerFinished -> {
                                timerRemaining = (currentStep?.timerSeconds ?: 60).coerceAtLeast(1)
                                timerRunning = true
                                timerFinished = false
                            }
                            else -> {
                                timerRemaining = if ((currentStep?.timerSeconds ?: 0) > 0) currentStep!!.timerSeconds else 60
                                timerRunning = true
                            }
                        }
                    },
                    onReset = {
                        timerRemaining = currentStep?.timerSeconds ?: 0
                        timerRunning = false
                        timerFinished = false
                    }
                )
            }

            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("本步调料", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showSeasoningSheet = true }) { Text("调整顺序") }
                    }
                    val currentSeasonings = seasonings.filter { it.suggestedStep == (currentStep?.number ?: 0) || it.suggestedStep == 0 }
                    if (currentSeasonings.isEmpty()) {
                        Text(
                            "本步没有识别到预设调料，请按上方原文操作。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            currentSeasonings.forEach { seasoning ->
                                SeasoningCheckRow(
                                    seasoning = seasoning,
                                    added = addedIds.contains(seasoning.id),
                                    onToggle = {
                                        if (addedIds.contains(seasoning.id)) addedIds.remove(seasoning.id) else addedIds.add(seasoning.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (recipe.ingredients.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("手边食材", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                recipe.ingredients.joinToString(" · ") { ingredient ->
                                    ingredient.name + if (ingredient.measure.isNotBlank()) " ${ingredient.measure}" else ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSeasoningSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSeasoningSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SeasoningOrderSheet(
                recipe = recipe,
                seasonings = seasonings,
                addedIds = addedIds,
                onMove = ::moveSeasoning,
                onToggleAdded = { id ->
                    if (addedIds.contains(id)) addedIds.remove(id) else addedIds.add(id)
                },
                onReset = {
                    order = initialSeasonings.map { it.id }
                    orderStore.save(recipe.id, order)
                }
            )
        }
    }
}

@Composable
private fun TimerPanel(
    suggestedSeconds: Int,
    remaining: Int,
    running: Boolean,
    finished: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val hasTimer = suggestedSeconds > 0 || remaining > 0 || finished
    val contentColor = if (finished) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(modifier = Modifier.size(20.dp), tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        finished -> "计时结束"
                        running -> "正在计时"
                        remaining > 0 -> "计时已暂停"
                        suggestedSeconds > 0 -> "原文建议 ${formatDuration(suggestedSeconds)}"
                        else -> "本步计时"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
                Text(
                    if (hasTimer && !finished) formatDuration(remaining) else if (finished) "00:00" else if (suggestedSeconds > 0) formatDuration(suggestedSeconds) else "+1 分钟",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnums"
                    ),
                    color = contentColor
                )
            }
            if (hasTimer) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重置计时", modifier = Modifier.size(20.dp), tint = contentColor)
                }
            }
            Button(
                onClick = onToggle,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (running) {
                    PauseIcon(modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (running) "暂停" else if (finished) "再来" else if (remaining > 0) "继续" else "开始")
            }
        }
    }
}

@Composable
private fun SeasoningCheckRow(seasoning: SeasoningItem, added: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = if (added) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (added) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape)
                    .background(if (added) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                if (added) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(seasoning.ingredientName, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (seasoning.measure.isNotBlank()) seasoning.measure else "用量按原菜谱",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(if (added) "已添加" else "点此标记", style = MaterialTheme.typography.labelMedium, color = if (added) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeasoningOrderSheet(
    recipe: Recipe,
    seasonings: List<SeasoningItem>,
    addedIds: List<String>,
    onMove: (Int, Int) -> Unit,
    onToggleAdded: (String) -> Unit,
    onReset: () -> Unit
) {
    var draggingId by remember(recipe.id) { mutableStateOf<String?>(null) }
    var dragOffset by remember(recipe.id) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val latestSeasonings by rememberUpdatedState(seasonings)
    val latestOnMove by rememberUpdatedState(onMove)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("调料添加顺序", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "长按拖动，或用上下箭头调整。排序会保存在本机。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onReset) { Text("恢复") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(seasonings, key = { _, item -> item.id }) { index, seasoning ->
                val rowHeightPx = with(density) { 112.dp.toPx() }
                Card(
                    modifier = Modifier.fillMaxWidth().height(104.dp).graphicsLayer {
                        translationY = if (draggingId == seasoning.id) dragOffset else 0f
                    }.pointerInput(seasoning.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = seasoning.id
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val from = latestSeasonings.indexOfFirst { it.id == seasoning.id }
                                val target = (from + dragOffset / rowHeightPx).roundToInt()
                                    .coerceIn(0, latestSeasonings.lastIndex.coerceAtLeast(0))
                                if (from >= 0 && target != from) {
                                    latestOnMove(from, target)
                                    dragOffset -= (target - from) * rowHeightPx
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                            }
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (draggingId == seasoning.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (draggingId == seasoning.id) 6.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "长按拖动调整顺序", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(seasoning.ingredientName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(
                                    seasoning.measure.takeIf { it.isNotBlank() },
                                    if (seasoning.suggestedStep > 0) "建议第 ${seasoning.suggestedStep} 步" else "按需添加"
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onMove(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移 ${seasoning.ingredientName}", modifier = Modifier.size(20.dp)) }
                        IconButton(
                            onClick = { onMove(index, index + 1) },
                            enabled = index < seasonings.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移 ${seasoning.ingredientName}", modifier = Modifier.size(20.dp)) }
                        IconButton(
                            onClick = { onToggleAdded(seasoning.id) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (seasoning.id in addedIds) Icons.Filled.CheckCircle else Icons.Filled.Check,
                                contentDescription = if (seasoning.id in addedIds) "取消已添加" else "标记已添加",
                                tint = if (seasoning.id in addedIds) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "建议顺序来自原菜谱。跨步骤调整只改变添加清单，不会改写菜谱原文，请根据实际火候判断。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CompletionScreen(
    recipe: Recipe,
    addedCount: Int,
    onViewRecipe: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(24.dp))
        Text("出锅了", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (addedCount > 0) {
                "你完成了「${recipe.title}」的 ${recipe.steps.size} 个步骤，\n并标记了 $addedCount 种调料。"
            } else {
                "你完成了「${recipe.title}」的 ${recipe.steps.size} 个步骤。"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onViewRecipe,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small
        ) { Text("返回菜谱", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small
        ) { Text("再找一道菜") }
    }
}
