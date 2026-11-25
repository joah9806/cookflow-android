package com.hungerg.cookflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TimerIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier) {
        val color = tint
        val stroke = size.minDimension * 0.075f
        drawCircle(
            color = color,
            radius = size.minDimension * 0.36f,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, size.height * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(size.width * 0.68f, size.height * 0.56f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.36f, size.height * 0.16f),
            end = Offset(size.width * 0.64f, size.height * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier) {
        val color = tint
        val width = size.width * 0.18f
        val top = size.height * 0.24f
        val bottom = size.height * 0.76f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.29f, top),
            size = androidx.compose.ui.geometry.Size(width, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.53f, top),
            size = androidx.compose.ui.geometry.Size(width, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f)
        )
    }
}
