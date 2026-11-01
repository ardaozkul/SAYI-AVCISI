package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.engine.CurriculumRepository

/**
 * Sade giriş ekranı: özel bir sayı bloğu işareti, büyük ad, tek cümle, OYNA düğmesi,
 * kaldığı yer ve ebeveyn bağlantısı. Emoji ve gereksiz bilgi kutuları yok.
 */
@Composable
fun StartScreen(
    dailyScore: Int,
    completedCount: Int,
    totalLevels: Int,
    endlessBestScore: Int,
    resumeLevelId: String?,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onParentPanel: () -> Unit
) {
    val palette = GameVisuals.palette("toplama")
    val resumeTitle = resumeLevelId?.let {
        CurriculumRepository.levelById(it)?.let(CurriculumRepository::topTitle)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(palette))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.55f))

            LogoMark(palette)

            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = "SAYI AVCISI",
                fontFamily = AppFonts.Display,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.onSurface,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sayıları birleştir, hedefi yakala.",
                fontFamily = AppFonts.Body,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = palette.muted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .background(palette.accent, RoundedCornerShape(18.dp))
                    .border(1.dp, palette.accentDeep, RoundedCornerShape(18.dp))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OYNA  →",
                    color = Color(0xFFFFFCF4),
                    fontFamily = AppFonts.Display,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp
                )
            }

            if (resumeLevelId != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Transparent, RoundedCornerShape(16.dp))
                        .border(1.dp, GameVisuals.line, RoundedCornerShape(16.dp))
                        .clickable(onClick = onResume),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DEVAM ET" + (resumeTitle?.let { " · ${it.uppercase()}" } ?: ""),
                        color = palette.onSurface,
                        fontFamily = AppFonts.Body,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GameVisuals.line.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "★ $dailyScore sarı puan",
                    color = GameVisuals.gold,
                    fontFamily = AppFonts.Body,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "● $endlessBestScore yeşil puan",
                    color = GameVisuals.greenPoint,
                    fontFamily = AppFonts.Body,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$completedCount / $totalLevels bölüm",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ebeveyn Paneli",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onParentPanel)
                    .padding(8.dp)
            )
        }
    }
}

/** Birbirine yaslanmış üç sayı bloğu: oyunun kendi işareti, hazır emoji değil. */
@Composable
private fun LogoMark(palette: WorldPalette) {
    val shape = RoundedCornerShape(13.dp)
    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset(x = (-22).dp, y = 12.dp)
                .rotate(-9f)
                .size(48.dp)
                .shadow(2.dp, shape)
                .background(palette.accentDeep, shape)
                .border(1.dp, palette.accentDeep, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "2",
                color = Color(0xFFFFFCF4),
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .offset(x = 22.dp, y = 14.dp)
                .rotate(8f)
                .size(48.dp)
                .shadow(2.dp, shape)
                .background(palette.accent, shape)
                .border(1.dp, palette.accentDeep, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "3",
                color = Color(0xFFFFFCF4),
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .offset(y = (-18).dp)
                .size(50.dp)
                .shadow(3.dp, shape)
                .background(GameVisuals.gold, shape)
                .border(1.dp, Color(0xFFA87A29), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "5",
                color = Color(0xFF2B2110),
                fontFamily = AppFonts.Display,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = "★",
            color = palette.accentDeep,
            fontFamily = AppFonts.Display,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = 2.dp)
        )
    }
}
