package com.sayiavcisi.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sayiavcisi.app.data.LocalProgressStorage
import com.sayiavcisi.app.ui.AppFonts
import com.sayiavcisi.app.ui.SayiAvcisiApp

class MainActivity : ComponentActivity() {

    private val storage: LocalProgressStorage by lazy { LocalProgressStorage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema her zaman açık krem kâğıt; sistem koyu modda olsa bile durum çubuğu
        // ikonları koyu kalır, böylece beyaz ikonlar krem zeminde kaybolmaz.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            SayiAvcisiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F3EA)) {
                    SayiAvcisiApp(storage)
                }
            }
        }
    }
}

/** Oyunun kendi kâğıt/mürekkep paleti var; Material yalnızca metin ve yüzeyleri taşır. */
@Composable
private fun SayiAvcisiTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF2C4A63),
        onPrimary = Color(0xFFFFFCF5),
        background = Color(0xFFF7F3EA),
        onBackground = Color(0xFF1D272B),
        surface = Color(0xFFFFFCF5),
        onSurface = Color(0xFF1D272B)
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalTextStyle provides androidx.compose.ui.text.TextStyle(
                fontFamily = AppFonts.Body,
                color = Color(0xFF1D272B)
            )
        ) {
            content()
        }
    }
}
