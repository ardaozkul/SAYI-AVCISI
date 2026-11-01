package com.sayiavcisi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sayiavcisi.app.data.LocalProgressStorage
import com.sayiavcisi.app.ui.SayiAvcisiApp

class MainActivity : ComponentActivity() {

    private val storage: LocalProgressStorage by lazy { LocalProgressStorage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SayiAvcisiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101528)) {
                    SayiAvcisiApp(storage)
                }
            }
        }
    }
}

/** Oyunun kendi pastel paletleri var; Material teması yalnızca bileşenleri aydınlık tutmak için burada. */
@Composable
private fun SayiAvcisiTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF5BD6A0), background = Color(0xFF101528), surface = Color(0xFF1B2340))
    } else {
        lightColorScheme(primary = Color(0xFF1E9E6A), background = Color(0xFFF6FBFF), surface = Color.White)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
