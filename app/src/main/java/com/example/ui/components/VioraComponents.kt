package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCrimsonPink
import com.example.ui.theme.PrimaryPinkPurple
import com.example.ui.theme.TransparentPurpleGlow
import kotlin.random.Random

// 1. Premium Glassmorphic Modifier
fun Modifier.glassmorphic(
    backgroundColor: Color = Color(0x33180E24),
    borderColor: Color = Color(0x55CE00FF),
    borderRadius: Dp = 16.dp
) = this
    .clip(RoundedCornerShape(borderRadius))
    .background(backgroundColor)
    .border(1.dp, borderColor, RoundedCornerShape(borderRadius))

// 2. Cosmic Purple Glow Modifier
fun Modifier.purpleGlow(
    color: Color = Color(0x77CE00FF),
    radius: Dp = 20.dp
) = this.drawBehind {
    drawContext.canvas.save()
    // Ambient draws can be simulated beautifully using drawBehind + radial gradients
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            radius = radius.toPx() * 2
        ),
        radius = radius.toPx() * 1.5f,
        center = center
    )
    drawContext.canvas.restore()
}

// 3. Premium Interactive Gradient Button
@Composable
fun VioraGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: @Composable (RowScope.() -> Unit)? = null
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFB01DFF), Color(0xFFFF2E93))
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(gradientBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                icon()
            }
        }
    }
}

// 4. Particle Heart Animation
data class HeartParticle(
    val xOffset: Float,
    val startY: Float,
    val scale: Float,
    val alpha: Float,
    val speed: Float,
    val phase: Float
)

@Composable
fun FloatingHeartsBackground(modifier: Modifier = Modifier) {
    val heartCount = 12
    val particles = remember {
        List(heartCount) {
            HeartParticle(
                xOffset = Random.nextFloat(),
                startY = 0.8f + Random.nextFloat() * 0.2f,
                scale = 0.4f + Random.nextFloat() * 0.6f,
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                speed = 0.05f + Random.nextFloat() * 0.08f,
                phase = Random.nextFloat() * 2f * Math.PI.toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hearts")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            // Current progress for this heart
            val progress = (animProgress + p.phase) % 1.0f
            val currentY = height * (p.startY - progress * 1.1f)
            val wobble = kotlin.math.sin(progress * 10f + p.phase) * 40f
            val currentX = width * p.xOffset + wobble
            val scale = p.scale * (1f - (progress * 0.3f))
            val alpha = p.alpha * (if (progress < 0.2f) progress / 0.2f else if (progress > 0.8f) (1f - progress) / 0.2f else 1f)

            if (currentY > 0 && currentY < height && currentX > 0 && currentX < width) {
                drawHeart(
                    cx = currentX,
                    cy = currentY,
                    size = 40f * scale,
                    color = AccentCrimsonPink.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        val width = size
        val height = size
        val x = cx - width / 2
        val y = cy - height / 2

        moveTo(x + width / 2, y + height / 5)
        cubicTo(x + width / 5, y, x, y + height / 3, x + width / 2, y + height * 0.95f)
        cubicTo(x + width, y + height / 3, x + width * 4 / 5, y, x + width / 2, y + height / 5)
    }
    drawPath(path = path, color = color)
}

// 5. Simulated Interactive Waveform
@Composable
fun InteractiveWaveform(
    modifier: Modifier = Modifier,
    activePercent: Float = 0.5f,
    barCount: Int = 24
) {
    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = remember {
            List(barCount) {
                15 + Random.nextInt(25)
            }
        }

        heights.forEachIndexed { index, barHeight ->
            val isActive = (index.toFloat() / barCount) <= activePercent
            val color = if (isActive) AccentCrimsonPink else Color(0xFF4C3B5E)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VioraFlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun VioraInterestTag(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val hash = text.hashCode()
    val absHash = if (hash < 0) -hash else hash
    val tagColors = listOf(
        Pair(Color(0xFF1E0E32), Color(0xFFCE00FF)),
        Pair(Color(0xFF0F2231), Color(0xFF00FF87)),
        Pair(Color(0xFF2F0F1E), Color(0xFFFF2E93)),
        Pair(Color(0xFF1E0F28), Color(0xFFD633FF)),
        Pair(Color(0xFF0C1D2A), Color(0xFF00E676)),
        Pair(Color(0xFF2E1A0F), Color(0xFFFF9E00))
    )
    val colorPair = tagColors[absHash % tagColors.size]
    
    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(Color(0xFFB01DFF), Color(0xFFFF2E93)))
    } else {
        Brush.linearGradient(listOf(colorPair.first, colorPair.first.copy(alpha = 0.8f)))
    }
    
    val borderBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(Color(0xFFD633FF), Color(0xFFFF2E93)))
    } else {
        Brush.linearGradient(listOf(colorPair.second.copy(alpha = 0.5f), colorPair.second.copy(alpha = 0.2f)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
