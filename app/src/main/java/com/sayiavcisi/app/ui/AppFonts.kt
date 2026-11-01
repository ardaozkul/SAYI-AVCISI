package com.sayiavcisi.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sayiavcisi.app.R

/**
 * Oyunun tipografisi.
 *
 * Bricolage Grotesque: başlıklar, sayılar ve görev yazıları. Harf biçimi sıradan
 * yuvarlak "oyun fontu" değil; kitap kapağı / afiş hissi veren, Türkçe karakterleri
 * tam destekleyen bir display yazı tipi.
 * Public Sans: açıklamalar, düğmeler ve uzun metinler. Sakin, okunaklı, nötr.
 *
 * İki yazı tipi de SIL Open Font License ile gelir; lisans metinleri docs/fonts altında.
 */
object AppFonts {

    val Display = FontFamily(
        Font(R.font.bricolage_grotesque, FontWeight.Normal),
        Font(R.font.bricolage_grotesque, FontWeight.Medium),
        Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque, FontWeight.Bold),
        Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
    )

    val Body = FontFamily(
        Font(R.font.public_sans, FontWeight.Normal),
        Font(R.font.public_sans, FontWeight.Medium),
        Font(R.font.public_sans, FontWeight.SemiBold),
        Font(R.font.public_sans, FontWeight.Bold),
        Font(R.font.public_sans, FontWeight.ExtraBold)
    )
}
