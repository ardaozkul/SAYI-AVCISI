package com.sayiavcisi.app.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Bir dünya (bölüm seti) için tek renk kaynağı: arka plan, kutu yüzeyleri, vurgu ve parıltı renkleri.
 * Tile yüzleri açık (krem/pastel), sayı mürekkebi koyu tutulur; büyük rakamlar net okunsun diye.
 */
data class WorldPalette(
    val key: String,
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val accentDeep: Color,
    val tileFaceTop: Color,
    val tileFaceBottom: Color,
    val tileText: Color,
    val glow: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color
)

/**
 * Çocuk dostu, doygun ama yumuşak palet sistemi (Bloop / Prodigy / Duolingo ABC hissi).
 * Kaynak dosyalara bağımlı değildir; tüm renkler kod içinde tanımlıdır.
 */
object GameVisuals {

    /** Doğru eşleştirme / başarılı hamle yeşili. */
    val ok: Color = Color(0xFF12B76A)

    /** Yanlış hamle / süre bitti kırmızısı (çocuk için yumuşatılmış). */
    val danger: Color = Color(0xFFF04438)

    /** Altın para ve ödül rengi. */
    val gold: Color = Color(0xFFF5A524)

    /** Elmas / özel joker rengi. */
    val gem: Color = Color(0xFF3ABFF8)

    private val orman = WorldPalette(
        key = "orman",
        bgTop = Color(0xFF57D9A3),
        bgBottom = Color(0xFF16A37A),
        accent = Color(0xFFFF8A3D),
        accentDeep = Color(0xFFE2650F),
        tileFaceTop = Color(0xFFFFFDF3),
        tileFaceBottom = Color(0xFFF0F7CF),
        tileText = Color(0xFF103A22),
        glow = Color(0xFFFFE45C),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF12331F),
        muted = Color(0xFF5C8A72)
    )

    private val magara = WorldPalette(
        key = "magara",
        bgTop = Color(0xFFFFB459),
        bgBottom = Color(0xFFE8623C),
        accent = Color(0xFFE8452F),
        accentDeep = Color(0xFFB32E1B),
        tileFaceTop = Color(0xFFFFF6E3),
        tileFaceBottom = Color(0xFFF7DFB4),
        tileText = Color(0xFF43260E),
        glow = Color(0xFFFFD166),
        surface = Color(0xFFFFFBF2),
        onSurface = Color(0xFF43260E),
        muted = Color(0xFFA67F52)
    )

    private val kale = WorldPalette(
        key = "kale",
        bgTop = Color(0xFF8A7CF8),
        bgBottom = Color(0xFF4B3BC4),
        accent = Color(0xFFFFC93C),
        accentDeep = Color(0xFFE0A400),
        tileFaceTop = Color(0xFFFFFFFF),
        tileFaceBottom = Color(0xFFDEE9FF),
        tileText = Color(0xFF14245E),
        glow = Color(0xFFA5B4FC),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B1140),
        muted = Color(0xFF6F65A8)
    )

    private val uzay = WorldPalette(
        key = "uzay",
        bgTop = Color(0xFF0E3A4A),
        bgBottom = Color(0xFF1B1454),
        accent = Color(0xFF22D3EE),
        accentDeep = Color(0xFF0E7490),
        tileFaceTop = Color(0xFFF1FEFF),
        tileFaceBottom = Color(0xFFB6F1FA),
        tileText = Color(0xFF04333D),
        glow = Color(0xFF67E8F9),
        surface = Color(0xFFF3F0FF),
        onSurface = Color(0xFF1B1454),
        muted = Color(0xFF8B85C4)
    )

    private val ada = WorldPalette(
        key = "ada",
        bgTop = Color(0xFF4DE0E8),
        bgBottom = Color(0xFF0FA3B1),
        accent = Color(0xFFFF6F5E),
        accentDeep = Color(0xFFD14334),
        tileFaceTop = Color(0xFFFFFBEB),
        tileFaceBottom = Color(0xFFFFE1A8),
        tileText = Color(0xFF06403F),
        glow = Color(0xFFFFD166),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF06585C),
        muted = Color(0xFF5E9AA0)
    )

    private val lab = WorldPalette(
        key = "lab",
        bgTop = Color(0xFF8FC8FF),
        bgBottom = Color(0xFFE4F4FF),
        accent = Color(0xFF2563EB),
        accentDeep = Color(0xFF1B4FCC),
        tileFaceTop = Color(0xFFFFFFFF),
        tileFaceBottom = Color(0xFFE3F1FF),
        tileText = Color(0xFF0B2545),
        glow = Color(0xFF93C5FD),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0B2545),
        muted = Color(0xFF648096)
    )

    private val byKey: Map<String, WorldPalette> = mapOf(
        orman.key to orman,
        magara.key to magara,
        kale.key to kale,
        uzay.key to uzay,
        ada.key to ada,
        lab.key to lab
    )

    /** worldKey: "orman" | "magara" | "kale" | "uzay" | "ada" | "lab"; bilinmeyen anahtar "orman"e düşer. */
    fun palette(worldKey: String): WorldPalette =
        byKey[worldKey.lowercase().trim()] ?: orman

    /** Ekran arka planı için dikey dünya gradyanı. */
    fun backgroundBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(colors = listOf(p.bgTop, p.bgBottom))

    /** Sayı bloklarının yüzeyi için dikey pastel gradyan. */
    fun tileBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(colors = listOf(p.tileFaceTop, p.tileFaceBottom))
}
