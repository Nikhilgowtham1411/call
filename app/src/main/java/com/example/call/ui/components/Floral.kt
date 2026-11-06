package com.example.call.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.call.ui.theme.CallTheme
import com.example.call.ui.theme.DeepPurple

private val PetalPink = Color(0xFFE8B4FF)
private val PetalLilac = Color(0xFFC9A0F5)
private val FlowerCore = Color(0xFFFFD66B)

/** Draws a simple five-petal flower. */
fun DrawScope.drawFlower(
    center: Offset,
    radius: Float,
    petal: Color = PetalPink,
    core: Color = FlowerCore,
    rotation: Float = 0f
) {
    for (i in 0 until 5) {
        rotate(rotation + i * 72f, pivot = center) {
            drawOval(
                color = petal,
                topLeft = Offset(center.x - radius * 0.32f, center.y - radius),
                size = Size(radius * 0.64f, radius * 0.95f)
            )
        }
    }
    drawCircle(core, radius * 0.22f, center)
}

/** Purple header with rounded bottom corners and subtle flowers in both corners. */
@Composable
fun FloralHeader(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val dark = CallTheme.colors.isDark
    val top = if (dark) Color(0xFF3A2556) else Color(0xFF7A47C4)
    val bottom = if (dark) Color(0xFF2A1A40) else primary

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .drawBehind {
                val r = size.height * 0.28f
                drawFlower(Offset(size.width * 0.07f, size.height * 0.62f), r, PetalPink.copy(alpha = 0.55f), rotation = 10f)
                drawFlower(Offset(size.width * 0.16f, size.height * 0.88f), r * 0.6f, PetalLilac.copy(alpha = 0.5f), rotation = 40f)
                drawFlower(Offset(size.width * 0.93f, size.height * 0.30f), r * 0.85f, PetalPink.copy(alpha = 0.5f), rotation = 25f)
                drawFlower(Offset(size.width * 0.84f, size.height * 0.80f), r * 0.55f, PetalLilac.copy(alpha = 0.5f), rotation = 55f)
            }
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.padding(end = 4.dp)) { leading?.invoke() }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Box(Modifier.padding(start = 4.dp)) { trailing?.invoke() }
        }
    }
}

/** Screen background: soft cream with faint flowers peeking in from the bottom corners. */
@Composable
fun FloralBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                val r = size.width * 0.12f
                drawFlower(Offset(size.width * 0.04f, size.height * 0.97f), r, PetalPink.copy(alpha = 0.28f), rotation = 15f)
                drawFlower(Offset(size.width * 0.17f, size.height * 1.0f), r * 0.6f, PetalLilac.copy(alpha = 0.25f), rotation = 45f)
                drawFlower(Offset(size.width * 0.97f, size.height * 0.96f), r * 0.9f, PetalPink.copy(alpha = 0.28f), rotation = 30f)
            },
        content = content
    )
}

val HeaderTextColor = Color.White
val HeaderIconColor = Color.White
val PrimaryOnHeader = DeepPurple
