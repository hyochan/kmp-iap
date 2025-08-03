package dev.hyo.martie.utils

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.swipeToBack(onBack: () -> Unit): Modifier {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    
    return this.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (offsetX > with(density) { 100.dp.toPx() }) {
                    onBack()
                }
                offsetX = 0f
            }
        ) { _, dragAmount ->
            offsetX += dragAmount
        }
    }
}