package com.sayiavcisi.app.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tek renk kaynağı.
 *
 * Tema: mürekkep laciverti, adaçayı yeşili ve eski kâğıt. Turuncu yok; parlak
 * neon gradyanlar da yok. Bölümler birbirinden yalnızca sakin bir vurgu tonuyla
 * ayrılır, böylece ekran "üretilmiş" değil, basılmış bir oyun defteri gibi durur.
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

object GameVisuals {

    /** Doğru eşleştirme / başarılı hamle yeşili. */
    val ok: Color = Color(0xFF2E7D5B)

    /** Yanlış hamle / süre bitti kiremit kırmızısı. */
    val danger: Color = Color(0xFFA64B42)

    /** Normal bölümlerin sarı puanı. Turuncuya kaçmayan, kâğıt üstü altın. */
    val gold: Color = Color(0xFFB9963E)

    /** Sonsuz Av'ın yeşil puanı. Normal sarı puandan ayrı bir kayıt. */
    val greenPoint: Color = Color(0xFF26805A)

    /** Elmas / özel ödül rengi. */
    val gem: Color = Color(0xFF466A8C)

    /** Seçili taşın mürekkep vurgusu. */
    val selectedStart: Color = Color(0xFF31536B)
    val selectedEnd: Color = Color(0xFF21394B)

    /** Kâğıt kenarı / ince çizgi. */
    val line: Color = Color(0xFFD8D2C4)

    /** Elle yazılmış defter kenarı; koyu, sıcak bir gölge. */
    val ink: Color = Color(0xFF20282B)

    private fun palette(
        key: String,
        accent: Color,
        accentDeep: Color,
        glow: Color,
        bgTop: Color,
        bgBottom: Color,
        tileTop: Color,
        tileBottom: Color
    ) = WorldPalette(
        key = key,
        bgTop = bgTop,
        bgBottom = bgBottom,
        accent = accent,
        accentDeep = accentDeep,
        tileFaceTop = tileTop,
        tileFaceBottom = tileBottom,
        tileText = Color(0xFF20282B),
        glow = glow,
        surface = Color(0xFFFFFCF5),
        onSurface = Color(0xFF1D272B),
        muted = Color(0xFF6C756F)
    )

    // Her dünya kendi kâğıt tonunu taşır: aynı defter, farklı mürekkep.
    private val toplama = palette(
        "toplama", Color(0xFF2C4A63), Color(0xFF1E3548), Color(0xFFA9BCC9),
        Color(0xFFF3F6F8), Color(0xFFE2EAF0), Color(0xFFFFFEFB), Color(0xFFEAF1F6)
    )
    private val cikarma = palette(
        "cikarma", Color(0xFF5C3B52), Color(0xFF402839), Color(0xFFC6A7B9),
        Color(0xFFF8F1F4), Color(0xFFEDDFE7), Color(0xFFFFFCFD), Color(0xFFF5E9EF)
    )
    private val carpma = palette(
        "carpma", Color(0xFF2E5F5A), Color(0xFF1F4642), Color(0xFF9DC1BA),
        Color(0xFFF1F7F3), Color(0xFFDDECE2), Color(0xFFFDFFFC), Color(0xFFE8F3EC)
    )
    private val bolme = palette(
        "bolme", Color(0xFF3A4E7A), Color(0xFF27365A), Color(0xFFAAB6D1),
        Color(0xFFF2F3FA), Color(0xFFE1E4F2), Color(0xFFFEFEFF), Color(0xFFEAEDF8)
    )
    private val sonsuz = palette(
        "sonsuz", Color(0xFF2E6B4F), Color(0xFF1F4D38), Color(0xFF9BC2A8),
        Color(0xFFF0F8F2), Color(0xFFD9EDDF), Color(0xFFFCFEFB), Color(0xFFE4F2E8)
    )
    private val notr = palette(
        "notr", Color(0xFF4E5B55), Color(0xFF37423D), Color(0xFFB9C2BA),
        Color(0xFFF5F5F0), Color(0xFFE7E9E1), Color(0xFFFFFEFA), Color(0xFFEFF1EA)
    )

    private val byKey: Map<String, WorldPalette> = mapOf(
        toplama.key to toplama,
        cikarma.key to cikarma,
        carpma.key to carpma,
        bolme.key to bolme,
        sonsuz.key to sonsuz,
        notr.key to notr,
        // Eski anahtarlar da aynı sade tasarıma düşer.
        "orman" to toplama,
        "magara" to cikarma,
        "kale" to carpma,
        "uzay" to bolme,
        "ada" to bolme,
        "lab" to notr
    )

    fun palette(worldKey: String): WorldPalette =
        byKey[worldKey.lowercase().trim()] ?: toplama

    /** Ekran arka planı: dünyanın tonuna göre üç duraklı, yumuşak kâğıt geçişi. */
    fun backgroundBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(
            0f to p.bgTop,
            0.55f to p.bgBottom,
            1f to p.bgTop
        )

    /** Sayı bloklarının yüzeyi. */
    fun tileBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(colors = listOf(p.tileFaceTop, p.tileFaceBottom))

    /** Seçili blok: mürekkep laciverti; üzerindeki rakam krem olur. */
    fun selectedBrush(): Brush =
        Brush.verticalGradient(colors = listOf(selectedStart, selectedEnd))
}
