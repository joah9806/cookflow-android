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

    val braisedPorkRibs = Recipe(
        id = "local-braised-pork-ribs",
        title = "红烧排骨",
        category = "家常菜",
        area = "中国",
        ingredients = listOf(
            Ingredient("排骨", "500 克"),
            Ingredient("冰糖", "20 克", true),
            Ingredient("生抽", "2 汤匙", true),
            Ingredient("老抽", "1 汤匙", true),
            Ingredient("料酒", "1 汤匙", true),
            Ingredient("姜", "4 片", true),
            Ingredient("葱", "1 根", true),
            Ingredient("八角", "2 个", true),
            Ingredient("香叶", "2 片", true),
            Ingredient("盐", "适量", true),
            Ingredient("食用油", "1 汤匙", true)
        ),
        steps = localSteps(
            "排骨切段洗净，冷水下锅，加姜片和料酒，焯水 3 分钟后捞出沥干。",
            "锅中放油，小火炒化冰糖至琥珀色。",
            "倒入排骨翻炒，让每块排骨裹上糖色。",
            "加入生抽、老抽和料酒，翻炒均匀。",
            "放入八角、香叶和葱段，倒入热水没过排骨，大火烧开后转小火炖 35 分钟。",
            "加盐调味，转中火收汁 8 分钟。",
            "待汤汁浓稠并裹在排骨上时，出锅装盘。"
        ),
        sourceLabel = "灶序内置中文菜谱",
        sourceUrl = "",
        isLocal = true
    )

    val all = listOf(tomatoEgg, garlicBroccoli, braisedPorkRibs)

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
