package com.sayiavcisi.app.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tek renk kaynağı.
 *
 * Tasarım sadeleştirildi: sıcak krem zemin, beyaz kartlar, koyu lacivert rakamlar.
 * Doygun yeşil/turuncu gradyanlar kaldırıldı; her işlem yalnızca küçük bir vurgu rengiyle ayrılır.
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
    val ok: Color = Color(0xFF16A34A)

    /** Yanlış hamle / süre bitti kırmızısı. */
    val danger: Color = Color(0xFFDC2626)

    /** Altın / yıldız rengi. */
    val gold: Color = Color(0xFFF59E0B)

    /** Elmas / özel ödül rengi. */
    val gem: Color = Color(0xFF0EA5E9)

    /** Seçili taşın sıcak vurgusu (sarı → turuncu). */
    val selectedStart: Color = Color(0xFFFFD65C)
    val selectedEnd: Color = Color(0xFFFF9F43)

    private fun palette(
        key: String,
        accent: Color,
        accentDeep: Color,
        glow: Color
    ) = WorldPalette(
        key = key,
        bgTop = Color(0xFFFFFBF3),
        bgBottom = Color(0xFFF1E6D2),
        accent = accent,
        accentDeep = accentDeep,
        tileFaceTop = Color(0xFFFFFFFF),
        tileFaceBottom = Color(0xFFFFF8EA),
        tileText = Color(0xFF221F35),
        glow = glow,
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF26243A),
        muted = Color(0xFF7A8091)
    )

    private val toplama = palette("toplama", Color(0xFFFF8A3D), Color(0xFFE3691A), Color(0xFFFFC46B))
    private val cikarma = palette("cikarma", Color(0xFF4C8DFF), Color(0xFF2563EB), Color(0xFF93C5FD))
    private val carpma = palette("carpma", Color(0xFF9B6BFF), Color(0xFF7C3AED), Color(0xFFC4B5FD))
    private val bolme = palette("bolme", Color(0xFF1FB6A6), Color(0xFF0E9384), Color(0xFF99F6E4))
    private val notr = palette("notr", Color(0xFF64748B), Color(0xFF475569), Color(0xFFCBD5E1))

    private val byKey: Map<String, WorldPalette> = mapOf(
        toplama.key to toplama,
        cikarma.key to cikarma,
        carpma.key to carpma,
        bolme.key to bolme,
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

    /** Ekran arka planı: sıcak krem, yumuşak dikey geçiş. */
    fun backgroundBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(colors = listOf(p.bgTop, p.bgBottom))

    /** Sayı bloklarının yüzeyi. */
    fun tileBrush(p: WorldPalette): Brush =
        Brush.verticalGradient(colors = listOf(p.tileFaceTop, p.tileFaceBottom))

    /** Seçili blok: sarıdan turuncuya ısınan yüzey. */
    fun selectedBrush(): Brush =
        Brush.verticalGradient(colors = listOf(selectedStart, selectedEnd))
}
