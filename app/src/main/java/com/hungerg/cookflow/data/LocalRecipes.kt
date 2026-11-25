package com.hungerg.cookflow.data

object LocalRecipes {

    val tomatoEgg = Recipe(
        id = "local-tomato-egg",
        title = "番茄炒蛋",
        category = "家常菜",
        area = "中国",
        ingredients = listOf(
            Ingredient("番茄", "2 个"),
            Ingredient("鸡蛋", "3 个"),
            Ingredient("食用油", "2 汤匙", true),
            Ingredient("盐", "1/2 茶匙", true),
            Ingredient("白糖", "1 茶匙", true),
            Ingredient("葱花", "1 根", true)
        ),
        steps = localSteps(
            "番茄洗净切块，鸡蛋打入碗中，加一小撮盐充分打散。",
            "锅中倒入食用油，油热后倒入蛋液，炒至刚凝固时盛出。",
            "原锅补少许油，放入番茄块，中火翻炒 2 分钟至出汁。",
            "加入白糖和盐调味，翻炒均匀。",
            "倒回炒好的鸡蛋，翻炒 1 分钟，让鸡蛋裹上番茄汁。",
            "出锅前撒入葱花，翻匀后装盘。"
        ),
        sourceLabel = "灶序内置示例",
        sourceUrl = "",
        isLocal = true
    )

    val garlicBroccoli = Recipe(
        id = "local-garlic-broccoli",
        title = "蒜香西兰花",
        category = "素食",
        area = "中国",
        ingredients = listOf(
            Ingredient("西兰花", "1 颗"),
            Ingredient("蒜", "4 瓣", true),
            Ingredient("食用油", "1 汤匙", true),
            Ingredient("盐", "1/2 茶匙", true),
            Ingredient("蚝油", "1 茶匙", true)
        ),
        steps = localSteps(
            "西兰花剪成小朵，用淡盐水浸泡 5 分钟后冲洗干净。",
            "烧开一锅水，加少许盐和几滴油，放入西兰花焯水 1 分钟。",
            "捞出西兰花沥干，蒜切成蒜末。",
            "热锅倒入食用油，放入蒜末小火炒出香味。",
            "转大火倒入西兰花，快速翻炒 1 分钟。",
            "加入盐和蚝油，翻炒均匀后出锅。"
        ),
        sourceLabel = "灶序内置示例",
        sourceUrl = "",
        isLocal = true
    )

    val all = listOf(tomatoEgg, garlicBroccoli)

    private fun localSteps(vararg texts: String): List<CookStep> =
        texts.mapIndexed { index, text ->
            CookStep(index + 1, text, RecipeEngine.parseTimerSeconds(text))
        }

    fun search(query: String): List<Recipe> {
        if (query.isBlank()) return all
        val normalized = query.trim().lowercase()
        return all.filter { recipe ->
            listOf(recipe.title, recipe.category, recipe.area).any { it.lowercase().contains(normalized) } ||
                recipe.ingredients.any { it.name.lowercase().contains(normalized) }
        }
    }
}
