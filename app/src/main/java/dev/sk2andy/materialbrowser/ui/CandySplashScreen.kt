package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private val KoiBubbleSpecs = listOf(
    floatArrayOf(-0.30f, 0.06f, 5f),
    floatArrayOf(0.34f, 0.18f, 7f),
    floatArrayOf(-0.38f, 0.30f, 3.5f),
    floatArrayOf(0.26f, 0.42f, 4f),
    floatArrayOf(-0.16f, 0.52f, 6f),
)

@Composable
internal fun CandySplashScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val finished = rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1700, easing = LinearEasing))
        delay(250)
        finished.value.invoke()
    }
    val p = progress.value
    val intro = FastOutSlowInEasing.transform((p / 0.55f).coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.koi_icon_background))
            .testTag("candy_splash")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { finished.value.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val base = size.minDimension
            for (i in 0..2) {
                val t = ((p - 0.14f - i * 0.17f) / 0.6f).coerceIn(0f, 1f)
                if (t > 0f && t < 1f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f * (1f - t)),
                        radius = base * (0.22f + 0.30f * t),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
            for (spec in KoiBubbleSpecs) {
                val t = ((p - spec[1]) / 0.5f).coerceIn(0f, 1f)
                if (t > 0f && t < 1f) {
                    val x = center.x + base * spec[0] + sin(t * 2f * PI.toFloat()) * 4.dp.toPx()
                    val y = center.y + base * 0.34f - base * 0.55f * t
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f * sin(t * PI.toFloat())),
                        radius = spec[2].dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_koi_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 0.88f + 0.12f * intro
                    scaleX = s
                    scaleY = s
                    rotationZ = -5f * (1f - intro) + sin(p * 2f * PI.toFloat() * 1.2f) * 1.5f
                    alpha = intro
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to Color.Black,
                            0.78f to Color.Black,
                            1f to Color.Transparent,
                            center = center,
                            radius = size.minDimension * 0.675f,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}
