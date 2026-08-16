package io.github.stephenwanjala.datatable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A simple checkbox drawn with Canvas, requiring no Material dependency.
 */
@Composable
internal fun SimpleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: DataTableColors = DataTableColors(),
) {
    val borderColor = if (checked) colors.checkboxChecked else colors.checkboxUnchecked
    val fillColor = if (checked) colors.checkboxChecked else Color.Transparent
    val checkmarkColor = colors.checkboxCheckmark

    Canvas(
        modifier = modifier
            .size(20.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        val cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        drawRoundRect(
            color = fillColor,
            cornerRadius = cornerRadius,
            size = size
        )
        drawRoundRect(
            color = borderColor,
            cornerRadius = cornerRadius,
            size = size,
            style = Stroke(width = 2.dp.toPx())
        )

        if (checked) {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.2f, h * 0.5f)
                lineTo(w * 0.4f, h * 0.7f)
                lineTo(w * 0.8f, h * 0.3f)
            }
            drawPath(
                path = path,
                color = checkmarkColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * A simple icon button: a clickable box with centered content.
 *
 * When [hoverColor] is set the button paints a rounded highlight under the cursor, which is
 * what makes a row of icon buttons read as controls rather than as loose glyphs.
 */
@Composable
internal fun SimpleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    hoverColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled && hovered) hoverColor else Color.Transparent)
            .then(
                if (enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Renders an [ImageVector] as an image with optional tint.
 */
@Composable
internal fun VectorIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val painter = rememberVectorPainter(imageVector)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
    )
}
