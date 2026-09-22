package com.yugahashimoto.andcode.ui.optimization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An immutable list wrapper guaranteeing Compose compiler stability.
 *
 * Prevents unnecessary recompositions of LazyLists and parent containers
 * when standard Kotlin [List] references are passed.
 */
@Immutable
data class StableList<out T>(
    val items: List<T> = emptyList(),
) : List<T> by items {

    companion object {
        private val EMPTY = StableList<Nothing>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): StableList<T> = EMPTY as StableList<T>

        fun <T> from(list: List<T>): StableList<T> = StableList(list)
    }
}

fun <T> List<T>.toStableList(): StableList<T> = StableList.from(this)

/**
 * Performance optimization modifiers for 60/120fps rendering.
 */
object RecompositionOptimizer {

    /**
     * Caches gradient border drawing operations using [drawWithCache].
     *
     * Unlike standard [border] modifier which recreates shaders and outlines
     * on every recomposition, this creates the brush and outline only when
     * the layout size changes.
     */
    fun Modifier.cachedNeonBorder(
        width: Dp,
        colors: List<Color>,
        shape: Shape,
    ): Modifier = this.drawWithCache {
        val strokeWidthPx = width.toPx()
        val borderBrush = Brush.linearGradient(colors)
        val outline = shape.createOutline(size, layoutDirection, this)

        onDrawWithContent {
            drawContent()
            drawOutline(
                outline = outline,
                brush = borderBrush,
                style = Stroke(width = strokeWidthPx),
            )
        }
    }

    /**
     * Isolates this composable into a GPU hardware layer.
     *
     * Prevents parent screen recompositions or animations from forcing
     * re-rasterization of complex glass panels.
     */
    fun Modifier.hardwareLayer(): Modifier = this.graphicsLayer {
        clip = true
    }
}

/**
 * Debug helper that logs or tracks recomposition counts of expensive UI blocks.
 */
class RecompositionCounter(val name: String) {
    var count = 0
        private set

    fun onRecompose() {
        count++
    }
}

/**
 * Remembers a [RecompositionCounter] and increments it on every recomposition.
 */
@Composable
fun rememberRecompositionCounter(name: String): RecompositionCounter {
    val counter = remember(name) { RecompositionCounter(name) }
    SideEffect {
        counter.onRecompose()
    }
    return counter
}
