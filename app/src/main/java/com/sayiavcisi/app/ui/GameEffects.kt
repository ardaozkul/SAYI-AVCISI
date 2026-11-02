package com.sayiavcisi.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Bir patlama noktası: açı, hız, yarıçap ve başlama gecikmesi (0f..1f normalize). */
private class BurstDot(val angle: Float, val speed: Float, val radiusDp: Float, val startDelay: Float)

/** Bir konfeti parçası: yatay konum, düşüş hızı, faz, boyut ve şekil. */
private class ConfettiPiece(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val sizeDp: Float,
    val sway: Float,
    val colorIndex: Int,
    val square: Boolean
)

/** Kutu/etiket yazıları için koyu mürekkep (açık kartlar üzerinde yüksek kontrast). */
private val InkLabel = Color(0xFF1B2430)

/** Konfeti için kutlamalık renk serisi. */
private val ConfettiColors = listOf(
    Color(0xFFFBBF24),
    Color(0xFF34D399),
    Color(0xFF60A5FA),
    Color(0xFFF472B6),
    Color(0xFFA78BFA),
    Color(0xFFFB7185),
    Color(0xFFFDE68A)
)

/**
 * Taş giriş animasyonu: 0.6 -> 1 hafif taşmalı (overshoot) ölçek + saydamlaşarak gelme.
 * `Modifier.popIn(index * 40)` şeklinde kademeli kullanılabilir; yeniden derlemede tekrar oynamaz.
 */
@Composable
fun Modifier.popIn(delayMillis: Int = 0): Modifier {
    val scaleState = remember { Animatable(0.6f) }
    val fadeState = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        launch { fadeState.animateTo(1f, tween(durationMillis = 170, easing = FastOutSlowInEasing)) }
        scaleState.animateTo(1.06f, tween(durationMillis = 170, easing = FastOutSlowInEasing))
        scaleState.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    return this.scale(scaleState.value).alpha(fadeState.value)
}

/** `trigger` her değiştiğinde kısa yatay sarsıntı; 0 değeri ilk durum sayıldığı için oynamaz. */
@Composable
fun Modifier.shakeOnTrigger(trigger: Int): Modifier {
    val dx = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        dx.snapTo(0f)
        val steps = floatArrayOf(-12f, 12f, -8f, 8f, -4f, 0f)
        for (target in steps) {
            dx.animateTo(target, tween(durationMillis = 48, easing = LinearEasing))
        }
    }
    return this.offset(x = dx.value.dp)
}

/** `active` true olduğu sürece nefes alan hafif ölçek dalgalanması; false iken sabit durur. */
@Composable
fun Modifier.pulseWhile(active: Boolean): Modifier {
    val scaleState = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            while (isActive) {
                scaleState.animateTo(1.07f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
                scaleState.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
            }
        } else {
            scaleState.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        }
    }
    return this.scale(scaleState.value)
}

/**
 * Merkezden dışa doğru `count` noktalı radyal patlama; `trigger` değişince bir kez oynar ve biter.
 * Bitince kendini kaldırır (boşuna kare üretmez) ve dokunmayı engellemez; `modifier` olarak
 * çağıran taraf `Modifier.matchParentSize()` verir.
 */
@Composable
fun ParticleBurst(
    trigger: Int,
    color: Color,
    modifier: Modifier = Modifier,
    count: Int = 14,
    lifeMillis: Int = 700
) {
    val total = count.coerceIn(1, 24)
    val dots = remember(total) {
        List(total) { i ->
            BurstDot(
                angle = (i.toFloat() / total) * 2f * PI.toFloat() + Random.nextFloat() * 0.35f,
                speed = 0.65f + Random.nextFloat() * 0.55f,
                radiusDp = 5f + Random.nextFloat() * 6f,
                startDelay = Random.nextFloat() * 0.12f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    var bursting by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        bursting = true
        progress.animateTo(1f, tween(durationMillis = lifeMillis.coerceIn(200, 2000), easing = LinearEasing))
        bursting = false
    }

    if (bursting) {
        Canvas(modifier = modifier) {
            val travel = min(size.width, size.height) * 0.42f
            val cx = center.x
            val cy = center.y
            val p = progress.value
            for (dot in dots) {
                val local = ((p - dot.startDelay) / (1f - dot.startDelay)).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val r = travel * dot.speed * local
                drawCircle(
                    color = color.copy(alpha = (1f - local).coerceIn(0f, 1f)),
                    radius = dot.radiusDp * density * (1f - 0.45f * local),
                    center = Offset(cx + cos(dot.angle) * r, cy + sin(dot.angle) * r)
                )
            }
        }
    }
}

/**
 * "+3" tarzı etiket: `trigger` değişince yukarı süzülür, büyür ve solar; bitince kaybolur.
 */
@Composable
fun FloatingScore(
    text: String,
    trigger: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    var floating by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        floating = true
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        floating = false
    }

    if (floating) {
        val p = progress.value
        val popScale = if (p < 0.25f) 0.7f + (p / 0.25f) * 0.55f else 1.25f - ((p - 0.25f) / 0.75f) * 0.2f
        val fade = if (p < 0.6f) 1f else 1f - ((p - 0.6f) / 0.4f)
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = color,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier
                    .offset(y = (p * -72f).dp)
                    .scale(popScale)
                    .alpha(fade.coerceIn(0f, 1f))
            )
        }
    }
}

/**
 * Yıldız yağmuru: 3-4'lü komboda ve YILDIZ DESTEĞİ'nde ekrana altın yıldızlar saçar.
 * `trigger` her arttığında bir kez oynar.
 */
@Composable
fun StarBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFC93C),
    count: Int = 16
) {
    val total = count.coerceIn(1, 24)
    val stars = remember(total) {
        List(total) { i ->
            BurstDot(
                angle = (i.toFloat() / total) * 2f * PI.toFloat() + Random.nextFloat() * 0.5f,
                speed = 0.6f + Random.nextFloat() * 0.7f,
                radiusDp = 7f + Random.nextFloat() * 7f,
                startDelay = Random.nextFloat() * 0.2f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        active = true
        progress.animateTo(1f, tween(durationMillis = 1200, easing = LinearEasing))
        active = false
    }

    if (active) {
        Canvas(modifier = modifier) {
            val travel = min(size.width, size.height) * 0.46f
            val p = progress.value
            for (star in stars) {
                val local = ((p - star.startDelay) / (1f - star.startDelay)).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val r = travel * star.speed * local
                val cx = center.x + cos(star.angle) * r
                val cy = center.y + sin(star.angle) * r
                val radius = star.radiusDp * density * (1f - 0.35f * local)
                drawPath(
                    path = starPath(cx, cy, radius, 5, local * 3f),
                    color = color.copy(alpha = (1f - local * 0.7f).coerceIn(0f, 1f))
                )
            }
        }
    }
}

/** Beş köşeli yıldız çizimi (Compose Path). */
private fun starPath(cx: Float, cy: Float, radius: Float, points: Int, rotation: Float): Path {
    val path = Path()
    val step = PI.toFloat() / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = i * step - PI.toFloat() / 2f + rotation
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Bölüm bitince ~2.5 saniye yağan konfeti; `active` false'a çekilirse hemen durur.
 * Tam ekran katmanda `Modifier.matchParentSize()` ile kullanılır.
 */
@Composable
fun ConfettiRain(active: Boolean, modifier: Modifier = Modifier) {
    val elapsed = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(active) {
        if (active) {
            elapsed.snapTo(0f)
            running = true
            elapsed.animateTo(1f, tween(durationMillis = 2500, easing = LinearEasing))
            running = false
        } else {
            running = false
        }
    }

    if (running) {
        val pieces = remember {
            List(54) { i ->
                ConfettiPiece(
                    x = Random.nextFloat(),
                    speed = 0.75f + Random.nextFloat() * 0.9f,
                    phase = Random.nextFloat(),
                    sizeDp = 6f + Random.nextFloat() * 7f,
                    sway = 0.6f + Random.nextFloat() * 1.6f,
                    colorIndex = i,
                    square = i % 3 == 0
                )
            }
        }
        val transition = rememberInfiniteTransition(label = "confetti")
        val fall by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "confetti_fall"
        )
        val twinkle by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "confetti_twinkle"
        )
        val endingFade = ((1f - elapsed.value) / 0.18f).coerceIn(0f, 1f)

        Canvas(modifier = modifier) {
            val span = size.height + 80f
            for ((index, piece) in pieces.withIndex()) {
                val cycle = (fall * piece.speed + piece.phase) % 1f
                val y = cycle * span - 40f
                val x = piece.x * size.width + sin(cycle * 6.2832f * piece.sway + index) * 18f
                val paint = ConfettiColors[piece.colorIndex % ConfettiColors.size]
                    .copy(alpha = endingFade * twinkle)
                val half = piece.sizeDp * density * 0.5f
                if (piece.square) {
                    drawRect(
                        color = paint,
                        topLeft = Offset(x - half, y - half),
                        size = Size(half * 2f, half * 1.3f)
                    )
                } else {
                    drawCircle(color = paint, radius = half, center = Offset(x, y))
                }
            }
        }
    }
}

/**
 * Yaşayan arka plan: dünyanın vurgu renginden süzülen, çok yavaş sürüklenen ışık
 * kütleleri ve ince kâğıt benekleri. Ekranı boğmaz; yalnızca tahtanın arkasında
 * nefes alan bir derinlik kurar. `Modifier.fillMaxSize()` ile en altta kullanılır.
 */
@Composable
fun AmbientBackdrop(
    palette: WorldPalette,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_drift"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_breathe"
    )

    // Sabit mikro benekler: kâğıt dokusu hissi, animasyonsuz.
    val seeds = remember { List(18) { Random.nextFloat() to Random.nextFloat() } }

    Canvas(modifier = modifier) {
        val angle = drift * 2f * PI.toFloat()
        val orbs = listOf(
            Triple(0.16f + sin(angle) * 0.05f, 0.20f + cos(angle * 0.8f) * 0.04f, 0.46f),
            Triple(0.84f + cos(angle * 0.7f) * 0.04f, 0.34f + sin(angle * 1.1f) * 0.05f, 0.52f),
            Triple(0.52f + sin(angle * 0.5f) * 0.06f, 0.86f + cos(angle * 0.6f) * 0.03f, 0.40f)
        )
        for ((x, y, radiusFraction) in orbs) {
            val centerPoint = Offset(size.width * x, size.height * y)
            val radius = min(size.width, size.height) * radiusFraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = 0.20f * breathe),
                        palette.accent.copy(alpha = 0.05f * breathe),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = radius
                ),
                radius = radius,
                center = centerPoint
            )
        }
        for ((sx, sy) in seeds) {
            drawCircle(
                color = palette.accent.copy(alpha = 0.055f),
                radius = 1.2f + (sx * 2.2f),
                center = Offset(size.width * sx, size.height * sy)
            )
        }
    }
}

/**
 * `progress` (0f..1f) değerine göre yumuşak süpürülen dairesel ilerleme / geri sayım halkası.
 * Çağrı yapan taraf `modifier` ile boyut vermeli (örn. Modifier.size(96.dp)); etiket ortalanır.
 */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    label: String = ""
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "progress_ring"
    )
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }

    Box(
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val diameter = min(size.width, size.height) - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = InkLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Eşleştirilen iki taşın arasına çizilen, uçtan uça uzayan bağlantı çizgisi; uçlar kendi çizim uzayında.
 * Uçlardan biri null ise hiçbir şey çizmez.
 */
@Composable
fun ConnectionLine(
    from: Offset?,
    to: Offset?,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp
) {
    val start = from ?: return
    val end = to ?: return
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val growth = remember { Animatable(0f) }

    LaunchedEffect(start, end) {
        growth.snapTo(0f)
        growth.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val p = growth.value
        val tip = Offset(start.x + (end.x - start.x) * p, start.y + (end.y - start.y) * p)
        drawLine(
            color = color.copy(alpha = 0.32f),
            start = start,
            end = end,
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = start,
            end = tip,
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawCircle(color = color, radius = strokePx * 1.15f, center = tip)
    }
}

/**
 * İpucu taşı gibi dikkat çekmek için nefes alan yumuşak halka; `active` false iken söner ve durur.
 */
@Composable
fun GlowRing(
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val strength = remember { Animatable(0f) }
    val breathe = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            launch { strength.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing)) }
            while (isActive) {
                breathe.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
                breathe.animateTo(0f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
            }
        } else {
            strength.animateTo(0f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            breathe.snapTo(0f)
        }
    }

    if (strength.value <= 0.001f) return

    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f
        val stroke = (radius * 0.14f).coerceAtLeast(2f)
        val pulse = 1f - 0.16f * breathe.value
        for (i in 0..2) {
            drawCircle(
                color = color.copy(alpha = strength.value * (0.55f - 0.18f * i)),
                radius = (radius * pulse - i * stroke * 1.5f).coerceAtLeast(1f),
                center = center,
                style = Stroke(width = stroke)
            )
        }
    }
}
