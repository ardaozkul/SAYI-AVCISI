package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.model.BlockCell

/** Bir taşın anlık geri bildirimi. */
enum class TileFlash {
    None,
    Right,
    Wrong
}

/**
 * Sayı bloğu: beyaz yüzey, koyu rakam, yumuşak gölge.
 * Seçilince sarıdan turuncuya döner; doğru hamlede yeşil parlar, yanlışta kırmızı olup sarsılır.
 */
@Composable
fun NumberTile(
    cell: BlockCell?,
    isSelected: Boolean,
    isHinted: Boolean,
    flash: TileFlash,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    appearDelayMillis: Int = 0,
    shakeTick: Int = 0,
    textSize: TextUnit = 24.sp
) {
    val shape = RoundedCornerShape(16.dp)
    val activeCell = cell?.takeIf { !it.isCleared }
    val active = activeCell != null

    val face = when {
        !active -> Color.White.copy(alpha = 0.28f)
        flash == TileFlash.Right -> GameVisuals.ok
        flash == TileFlash.Wrong -> GameVisuals.danger
        isSelected -> null
        else -> null
    }

    val base = modifier
        .popIn(delayMillis = appearDelayMillis)
        .scale(if (isSelected) 1.06f else 1f)
        .then(
            if (active) {
                Modifier
                    .shadow(if (isSelected) 0.dp else 3.dp, shape)
                    .background(
                        brush = if (isSelected) GameVisuals.selectedBrush() else GameVisuals.tileBrush(palette),
                        shape = shape
                    )
            } else {
                Modifier.background(color = face ?: Color.Transparent, shape = shape)
            }
        )
        .then(if (face != null) Modifier.background(face ?: Color.Transparent, shape) else Modifier)
        .border(
            width = if (isSelected || flash != TileFlash.None) 2.5.dp else 1.dp,
            color = when {
                flash == TileFlash.Right -> GameVisuals.ok
                flash == TileFlash.Wrong -> GameVisuals.danger
                isSelected -> Color(0xFFFFB020)
                active -> Color(0xFFEDE2CE)
                else -> Color.White.copy(alpha = 0.35f)
            },
            shape = shape
        )
        .then(
            if (active && flash == TileFlash.Wrong) Modifier.shakeOnTrigger(shakeTick) else Modifier
        )
        .semantics {
            contentDescription = if (activeCell != null) {
                "Sayı ${activeCell.value}, satır ${activeCell.row + 1}, sütun ${activeCell.col + 1}" +
                    if (isSelected) ", seçili" else ""
            } else {
                "Boş kare"
            }
        }

    Box(modifier = base, contentAlignment = Alignment.Center) {
        if (activeCell != null) {
            Text(
                text = activeCell.value.toString(),
                color = if (flash == TileFlash.Right || flash == TileFlash.Wrong) Color.White else palette.tileText,
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        } else {
            Box(
                modifier = Modifier
                    .scale(0.32f)
                    .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(5.dp)
            )
        }
        if (active && isHinted && flash == TileFlash.None) {
            GlowRing(
                active = true,
                color = GameVisuals.gold,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Alt çubuktaki yazılı düğme: sadece ikon değil, çocuğun okuyabileceği etiket. */
@Composable
fun TextActionButton(
    label: String,
    emoji: String,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .background(
                color = if (filled) palette.accent else palette.surface.copy(alpha = 0.92f),
                shape = shape
            )
            .border(
                width = 1.5.dp,
                color = if (filled) palette.accentDeep else Color(0xFFE6DAC5),
                shape = shape
            )
            .clickable(onClick = onClick)
            .alpha(0.98f)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 15.sp)
        Text(
            text = label,
            color = if (filled) Color.White else palette.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
