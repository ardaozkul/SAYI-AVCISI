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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.engine.CurriculumRepository

/**
 * Sade giriş ekranı: büyük ad, tek cümle, OYNA düğmesi, kaldığı yer ve ebeveyn bağlantısı.
 * Günlük puan/altın/elmas kutuları kaldırıldı; sadece küçük bir ilerleme satırı kaldı.
 */
@Composable
fun StartScreen(
    dailyScore: Int,
    completedCount: Int,
    totalLevels: Int,
    resumeLevelId: String?,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onParentPanel: () -> Unit
) {
    val palette = GameVisuals.palette("toplama")
    val resumeTitle = resumeLevelId?.let { CurriculumRepository.levelById(it)?.let(CurriculumRepository::topTitle) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(palette))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.7f))

            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .padding(16.dp)
            ) {
                Text(text = "🦊", fontSize = 46.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "SAYI AVCISI",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = palette.onSurface,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sayıları birleştir, hedefi yakala.",
                fontSize = 15.sp,
                color = palette.muted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(listOf(palette.accent, palette.accentDeep)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OYNA",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            }

            if (resumeLevelId != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                        .border(1.5.dp, Color(0xFFE8DCC8), RoundedCornerShape(18.dp))
                        .clickable(onClick = onResume),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DEVAM ET" + (resumeTitle?.let { " · $it" } ?: ""),
                        color = palette.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "★ Bugün $dailyScore",
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount / $totalLevels bölüm",
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ebeveyn Paneli",
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onParentPanel)
                    .padding(8.dp)
            )
        }
    }
}
