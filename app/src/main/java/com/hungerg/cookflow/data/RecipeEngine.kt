package com.hungerg.cookflow.data

import java.util.Locale

object RecipeEngine {

    private val englishSeasonings = setOf(
        "salt", "pepper", "black pepper", "white pepper", "chilli", "chili", "paprika",
        "turmeric", "cumin", "coriander", "ginger", "garlic", "spice", "masala", "cinnamon",
        "nutmeg", "cardamom", "clove", "fennel", "mustard", "sesame", "oregano", "basil",
        "thyme", "rosemary", "parsley", "fenugreek", "kasuri methi", "soy sauce", "sauce",
        "vinegar", "oil", "olive oil", "sugar", "honey", "syrup", "stock", "broth",
        "bouillon", "chilli flakes", "red chilli", "green chilli", "curry powder", "ketchup",
        "mayonnaise", "mustard", " Worcestershire sauce", "fish sauce", "oyster sauce",
        "hoisin sauce", "miso", "tahini", "lemon juice", "lime juice", "coconut milk"
    )

    private val chineseSeasonings = setOf(
        "盐", "糖", "酱油", "生抽", "老抽", "醋", "料酒", "蚝油", "油", "香油", "花椒油",
        "辣椒油", "豆瓣酱", "甜面酱", "番茄酱", "黑胡椒", "白胡椒", "胡椒粉", "孜然",
        "五香粉", "十三香", "咖喱", "淀粉", "鸡精", "味精", "姜", "蒜", "葱", "辣椒",
        "花椒", "八角", "桂皮", "香叶", "糖浆", "蜂蜜", "鱼露", "豆豉", "腐乳"
    )

    private val actionWords = listOf(
        "add", "season", "sprinkle", "drizzle", "stir", "mix", "pour", "toss", "fry",
        "saute", "sauté", "marinate", "rub", "brush", "melt", "加入", "放入", "倒入",
        "撒", "拌", "调", "腌", "炒", "淋", "刷", "煸", "爆香"
    )

    fun isSeasoning(name: String): Boolean {
        val value = name.trim().lowercase(Locale.ROOT)
        if (value.isBlank()) return false
        if (chineseSeasonings.any { value.contains(it) }) return true
        return englishSeasonings.any { value.contains(it.trim()) }
    }

    fun splitInstructions(raw: String): List<CookStep> {
        if (raw.isBlank()) return emptyList()
        val normalized = raw.replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[\\t ]+"), " ")
            .trim()

        val blocks = normalized.split(
            Regex("""\n\s*\n+|\n(?=\s*(?:\d+[.)、]|Step\s+\d+[:：]?|步骤\s*\d+[:：]?))""", RegexOption.IGNORE_CASE)
        )
        val numbered = Regex("""^\s*(?:(?:\d+[.)、])|(?:Step\s+\d+[:：]?)|(?:步骤\s*\d+[:：]?))\s*""", RegexOption.IGNORE_CASE)

        val sentences = blocks.flatMap { block ->
            val clean = block.lines().joinToString(" ") { it.trim() }
                .replace(numbered, "")
                .replace(Regex("""^[-•*]\s*"""), "")
                .trim()
            splitSentences(clean)
        }.filter { it.isNotBlank() && !isClosingPhrase(it) }

        return sentences.mapIndexed { index, text ->
            CookStep(number = index + 1, text = text, timerSeconds = parseTimerSeconds(text))
        }
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        text.forEachIndexed { index, char ->
            current.append(char)
            if (char in ".!?。！？" && shouldSplit(text, index)) {
                val sentence = current.toString().trim()
                if (sentence.isNotBlank()) result += sentence
                current.clear()
            }
        }
        if (current.isNotBlank()) result += current.toString().trim()
        return result
    }

    private fun shouldSplit(text: String, punctuationIndex: Int): Boolean {
        var next = punctuationIndex + 1
        while (next < text.length && text[next].isWhitespace()) next++
        if (next >= text.length) return false
        if (!text[next].isLetterOrDigit() && text[next] !in "\u4e00-\u9fff".toCharArray()) return false
        if (text[punctuationIndex] == '.') {
            val prefix = text.substring(0, punctuationIndex + 1)
            if (Regex("""(?:\bMr|\bMrs|\bMs|\bDr|\be\.g|\bi\.e|\betc|\bNo|\bvs)\.$""", RegexOption.IGNORE_CASE).containsMatchIn(prefix)) {
                return false
            }
            val numberBefore = punctuationIndex > 0 && text[punctuationIndex - 1].isDigit()
            val numberAfter = text[punctuationIndex + 1].isDigit()
            if (numberBefore && numberAfter) return false
        }
        return true
    }

    private fun isClosingPhrase(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.ROOT)
            .replace(Regex("[.!。！\\s]+$"), "")
        return normalized in setOf("enjoy", "bon appétit", "bon appetit", "享用", "开动", "趁热享用")
    }

    fun parseTimerSeconds(text: String): Int {
        Regex("""(\d+)\s*分\s*(\d+)\s*秒""").find(text)?.let { match ->
            return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
        }
        Regex("""(\d+)\s*小时\s*(\d+)\s*分""").find(text)?.let { match ->
            return match.groupValues[1].toInt() * 3600 + match.groupValues[2].toInt() * 60
        }
        val chinese = Regex("""(\d+(?:\.\d+)?)\s*(秒钟?|秒|分钟?|分|小时)""")
        chinese.find(text)?.let { match ->
            val amount = match.groupValues[1].toDouble()
            val seconds = when (match.groupValues[2]) {
                "小时" -> amount * 3600
                "分钟", "分" -> amount * 60
                else -> amount
            }
            return seconds.toInt().coerceAtLeast(0)
        }

        val english = Regex("""(\d+(?:\.\d+)?|a|an|one|two|three|couple(?:\s+of)?|half(?:\s+an?)?)\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)""", RegexOption.IGNORE_CASE)
        english.find(text)?.let { match ->
            val amount = when (match.groupValues[1].lowercase(Locale.ROOT)) {
                "a", "an", "one" -> 1.0
                "two", "couple", "couple of" -> 2.0
                "three" -> 3.0
                "half", "half an" -> 0.5
                else -> match.groupValues[1].toDouble()
            }
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val multiplier = when {
                unit.startsWith("h") -> 3600
                unit.startsWith("m") -> 60
                else -> 1
            }
            return (amount * multiplier).toInt()
        }
        return 0
    }

    fun buildSeasoningQueue(recipe: Recipe): List<SeasoningItem> {
        return recipe.ingredients.filter { it.isSeasoning }.flatMap { ingredient ->
            val mentions = recipe.steps.mapIndexedNotNull { index, step ->
                if (mentionsIngredient(step.text, ingredient.name)) index + 1 else null
            }
            if (mentions.isEmpty()) {
                listOf(SeasoningItem(ingredient.name, ingredient.measure, suggestedStep = 0))
            } else {
                mentions.mapIndexed { occurrence, step ->
                    SeasoningItem(
                        ingredientName = ingredient.name,
                        measure = ingredient.measure,
                        suggestedStep = step,
                        occurrence = occurrence + 1
                    )
                }
            }
        }.sortedWith(compareBy({ it.suggestedStep == 0 }, { it.suggestedStep }, { it.ingredientName }, { it.occurrence }))
    }

    private fun mentionsIngredient(step: String, ingredient: String): Boolean {
        val normalizedStep = step.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9\u4e00-\u9fff]+"""), " ")
            .trim()
        val normalizedIngredient = ingredient.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9\u4e00-\u9fff]+"""), " ")
            .trim()
        if (normalizedIngredient.isBlank()) return false
        if (normalizedStep.contains(normalizedIngredient)) return true

        val aliases = when {
            normalizedIngredient.contains("chilli") || normalizedIngredient.contains("chili") -> listOf("chilli", "chili", "chillies", "chilies")
            normalizedIngredient.contains("coriander") -> listOf("coriander", "cilantro")
            normalizedIngredient.contains("kasuri methi") -> listOf("kasuri methi", "fenugreek")
            normalizedIngredient.contains("yogurt") -> listOf("yogurt", "yoghurt")
            normalizedIngredient == "辣椒" -> listOf("辣椒", "小米辣", "干辣椒")
            normalizedIngredient == "葱" -> listOf("葱", "葱花", "小葱")
            normalizedIngredient == "姜" -> listOf("姜", "姜丝", "姜末", "姜片")
            normalizedIngredient == "蒜" -> listOf("蒜", "蒜末", "蒜片", "蒜蓉")
            else -> emptyList()
        }
        if (aliases.any { normalizedStep.contains(it) }) return true

        val singular = normalizedIngredient.removeSuffix("s")
        if (singular.length >= 4 && Regex("(^| )${Regex.escape(singular)}s?( |$)").containsMatchIn(normalizedStep)) {
            return true
        }

        val lastToken = normalizedIngredient.substringAfterLast(' ')
        return lastToken.length >= 3 && normalizedIngredient.contains(' ') &&
            Regex("(^| )${Regex.escape(lastToken.removeSuffix("s"))}( |$)").containsMatchIn(normalizedStep)
    }

    fun hasSeasoningAction(stepText: String): Boolean =
        actionWords.any { stepText.lowercase(Locale.ROOT).contains(it) }
}
