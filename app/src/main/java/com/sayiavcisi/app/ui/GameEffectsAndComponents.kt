package com.sayiavcisi.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.model.OperationFeedback
import com.sayiavcisi.app.model.OperationType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sevimli Matematik Kaşifi Karakteri ("Kaşif Pati / Sayı Avcısı")
 * Duruma göre sevinç hareketleri, zıplama ve nazik ipucu mimikleri sergiler.
 */
@Composable
fun ExplorerMascotBanner(
    moodMessage: String,
    isCelebrating: Boolean,
    isGentleWarning: Boolean,
    dailyScore: Int,
    goldCoins: Int,
    diamonds: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_bounce")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCelebrating) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isCelebrating) 350 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val animatedScore by animateIntAsState(
        targetValue = dailyScore,
        animationSpec = tween(durationMillis = 500),
        label = "score_anim"
    )
    val animatedGold by animateIntAsState(
        targetValue = goldCoins,
        animationSpec = tween(durationMillis = 500),
        label = "gold_anim"
    )
    val animatedDiamonds by animateIntAsState(
        targetValue = diamonds,
        animationSpec = tween(durationMillis = 500),
        label = "diamond_anim"
    )

    val mascotEmoji = when {
        isCelebrating -> "🦊✨"
        isGentleWarning -> "🦊💡"
        else -> "🦊🧭"
    }

    val borderColor = when {
        isCelebrating -> Color(0xFFFBBF24)
        isGentleWarning -> Color(0xFF38BDF8)
        else -> Color(0xFF94A3B8).copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(bounceScale)
                        .clip(CircleShape)
                        .background(
                            if (isCelebrating) Color(0xFFF59E0B).copy(alpha = 0.3f)
                            else Color(0xFF334155)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mascotEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Kaşif Tilki",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = moodMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Günlük puan (max 100), Altın ve Elmas sayaçları (animasyonlu yükselme)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatPill(icon = "⭐", label = "$animatedScore/100", bgColor = Color(0xFF1E3A8A))
                StatPill(icon = "🪙", label = "$animatedGold", bgColor = Color(0xFF78350F))
                StatPill(icon = "💎", label = "$animatedDiamonds", bgColor = Color(0xFF311042))
            }
        }
    }
}

@Composable
private fun StatPill(icon: String, label: String, bgColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Çıkarma işleminde eksilen sayıdan nesne eksilme animasyonu ve
 * Çarpma işleminde görsel gruplama kutusu!
 * (Bölüm 4 & Bölüm 5 gereksinimleri)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualMathExplanationCard(
    feedback: OperationFeedback?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        if (feedback != null) {
            val cardBg = if (feedback.isCorrect) Color(0xFF065F46).copy(alpha = 0.92f) else Color(0xFF1E3A8A).copy(alpha = 0.92f)
            val stroke = if (feedback.isCorrect) Color(0xFF34D399) else Color(0xFF60A5FA)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.5.dp, stroke, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = feedback.explanationText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Çıkarma işlemi için eksilen nesne animasyonu (Örn: 7 elmadan 3 tanesi soluklaşır/eksilir, 4 kalır)
                if (feedback.operation == OperationType.SUBTRACTION && feedback.operands.size >= 2) {
                    val totalItems = feedback.operands[0].coerceIn(0, 20)
                    val removedItems = feedback.operands.drop(1).sum().coerceIn(0, totalItems)
                    val remainingItems = (totalItems - removedItems).coerceAtLeast(0)

                    if (totalItems in 1..20) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.Center
                        ) {
                            for (i in 0 until remainingItems) {
                                Text(
                                    text = "🍎",
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(1.dp)
                                )
                            }
                            for (i in 0 until removedItems) {
                                Text(
                                    text = "❌",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Eksilen Nesne Gösterimi: $totalItems nesneden $removedItems eksildi → $remainingItems kaldı",
                            color = Color(0xFFD1FAE5),
                            fontSize = 10.sp
                        )
                    }
                }

                // Çarpma işlemi için görsel gruplama (Örn: 3 × 4 -> 3 grup 4'lü yıldız kümesi)
                if (feedback.operation == OperationType.MULTIPLICATION && feedback.operands.size == 2) {
                    val groups = feedback.operands[0].coerceIn(0, 6)
                    val perGroup = feedback.operands[1].coerceIn(0, 6)
                    if (groups in 1..6 && perGroup in 1..6) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (g in 0 until groups) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⭐".repeat(perGroup),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Görsel Gruplama: $groups grup × her grupta $perGroup yıldız",
                            color = Color(0xFFFEF08A),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Doğru işlemde yıldız yağmuru + sayı eşleşince patlama kıvılcımı,
 * Bölüm bitince konfeti ve Yeni bölümde kapı açılma animasyonu!
 */
@Composable
fun StarRainAndBurstOverlay(
    triggerKey: Int,
    isConfettiMode: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = if (isConfettiMode) 1800 else 850, easing = LinearEasing)
            )
        }
    }

    if (triggerKey > 0 && progress.value < 0.99f) {
        val p = progress.value
        val colors = listOf(
            Color(0xFFFBBF24),
            Color(0xFF34D399),
            Color(0xFF60A5FA),
            Color(0xFFF472B6),
            Color(0xFFA78BFA),
            Color(0xFFFB7185)
        )

        Canvas(modifier = modifier.fillMaxSize()) {
            val count = if (isConfettiMode) 48 else 22
            val centerX = size.width / 2f
            val centerY = size.height / 2.3f

            for (i in 0 until count) {
                val angle = (i.toFloat() / count) * 2f * PI.toFloat()
                val speed = (120f + (i % 5) * 65f) * p
                val x = if (isConfettiMode) {
                    ((i * 73) % 100) / 100f * size.width + sin(p * 6f + i) * 24f
                } else {
                    centerX + cos(angle) * speed
                }
                val y = if (isConfettiMode) {
                    (p * size.height * 1.1f + (i * 37) % 240) % size.height
                } else {
                    centerY + sin(angle) * speed + (p * p * 90f)
                }

                val alpha = (1f - p).coerceIn(0f, 1f)
                val radius = if (isConfettiMode) 9f else 7f * (1f - p * 0.4f)
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Yeni bölüm başladığında iki yana açılan "Matematik Tapınağı / Kale Kapısı" animasyonu.
 */
@Composable
fun DoorOpeningOverlay(
    levelId: String,
    themeTitle: String,
    levelTitle: String,
    primaryColor: Color
) {
    val openProgress = remember(levelId) { Animatable(0f) }

    LaunchedEffect(levelId) {
        openProgress.snapTo(0f)
        openProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
    }

    if (openProgress.value < 0.99f) {
        val p = openProgress.value
        val alpha = (1f - p).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val halfW = size.width / 2f
                val slideOffset = halfW * p
                // Sol Kapı Kanadı
                drawRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f * alpha),
                    topLeft = Offset(0f, 0f),
                    size = Size(halfW - slideOffset, size.height)
                )
                // Sağ Kapı Kanadı
                drawRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f * alpha),
                    topLeft = Offset(halfW + slideOffset, 0f),
                    size = Size(halfW - slideOffset, size.height)
                )
                // Kapı kenar altın şeritleri
                drawRect(
                    color = primaryColor.copy(alpha = alpha),
                    topLeft = Offset((halfW - slideOffset - 6f).coerceAtLeast(0f), 0f),
                    size = Size(6f, size.height)
                )
                drawRect(
                    color = primaryColor.copy(alpha = alpha),
                    topLeft = Offset((halfW + slideOffset).coerceAtMost(size.width), 0f),
                    size = Size(6f, size.height)
                )
            }

            if (p < 0.65f) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚪 KAPI AÇILIYOR...",
                        color = Color(0xFFFBBF24),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$themeTitle • $levelTitle",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
