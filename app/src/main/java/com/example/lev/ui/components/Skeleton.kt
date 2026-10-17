package com.example.lev.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerSkeleton(modifier: Modifier = Modifier) {
    val shimmerColors =
        listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing,
                        ),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer_translate",
        )

    val brush =
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value),
        )

    Box(
        modifier =
            modifier
                .background(brush, MaterialTheme.shapes.small),
    )
}

/**
 * Estado de carregamento padrão do design system: blocos shimmer com a altura de um
 * card de conteúdo. Todas as telas devem usar este componente em vez de texto cru.
 */
@Composable
fun LoadingList(
    modifier: Modifier = Modifier,
    count: Int = 3,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) {
            ShimmerSkeleton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp),
            )
        }
    }
}
