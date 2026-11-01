package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Sayı bloğu: krem yüzey, koyu rakam, ince gölge.
 * Seçilince koyu orman yeşiline döner; doğru hamlede yeşil parlar, yanlışta kiremit
 * kırmızısı olup sarsılır. Temizlenen taş, parlama süresince değerini göstermeye devam eder.
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
    animateAppear: Boolean = true,
    shakeTick: Int = 0,
    textSize: TextUnit = 24.sp
) {
    val shape = RoundedCornerShape(14.dp)
    val active = cell != null && !cell.isCleared
    val flashingCleared = cell != null && cell.isCleared &&
        (flash == TileFlash.Right || flash == TileFlash.Wrong)
    val showNumber = cell != null && (active || flashingCleared)

    val face = when {
        flash == TileFlash.Right -> GameVisuals.ok
        flash == TileFlash.Wrong -> GameVisuals.danger
        isSelected && active -> null
        active -> null
        else -> null
    }

    val base = modifier
        .then(if (animateAppear) Modifier.popIn(delayMillis = appearDelayMillis) else Modifier)
        .scale(if (isSelected && active) 1.045f else 1f)
        .then(
            when {
                face != null -> Modifier
                    .shadow(2.dp, shape)
                    .background(face, shape)

                active -> Modifier
                    .shadow(3.dp, shape)
                    .background(
                        brush = if (isSelected) {
                            GameVisuals.selectedBrush()
                        } else {
                            GameVisuals.tileBrush(palette)
                        },
                        shape = shape
                    )

                else -> Modifier.background(
                    color = Color.White.copy(alpha = if (flashingCleared) 0.9f else 0.12f),
                    shape = shape
                )
            }
        )
        .border(
            width = if (isSelected || flash != TileFlash.None) 2.dp else 1.dp,
            color = when {
                flash == TileFlash.Right -> GameVisuals.ok
                flash == TileFlash.Wrong -> GameVisuals.danger
                isSelected -> GameVisuals.gold
                active -> GameVisuals.line
                else -> Color.White.copy(alpha = 0.2f)
            },
            shape = shape
        )
        .then(
            if (active && flash == TileFlash.Wrong) Modifier.shakeOnTrigger(shakeTick) else Modifier
        )
        .semantics {
            contentDescription = if (cell != null && (active || flashingCleared)) {
                "Sayı ${cell.value}, satır ${cell.row + 1}, sütun ${cell.col + 1}" +
                    if (isSelected) ", seçili" else ""
            } else {
                "Boş kare"
            }
        }

    Box(modifier = base, contentAlignment = Alignment.Center) {
        if (showNumber) {
            Text(
                text = cell.value.toString(),
                color = if (flash == TileFlash.Right || flash == TileFlash.Wrong || isSelected) {
                    Color(0xFFFFFCF4)
                } else {
                    palette.tileText
                },
                fontFamily = AppFonts.Display,
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        } else {
            // Boş kare: dikkat çekmeyen küçük bir nokta.
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Color(0xFF1F2C26).copy(alpha = 0.10f), CircleShape)
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

/** Alt çubuktaki yazılı düğme: sade, okunaklı, ikon yerine metin. */
@Composable
fun TextActionButton(
    label: String,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = if (filled) palette.accent else Color.Transparent,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = if (filled) palette.accentDeep else GameVisuals.line,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (filled) Color(0xFFFFFCF4) else palette.onSurface,
            fontFamily = AppFonts.Body,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}
