package com.nomedia.switcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.ui.AppRoot
import com.nomedia.switcher.ui.theme.NoMediaTheme
import com.nomedia.switcher.ui.theme.TopBarContainer

class MainActivity : ComponentActivity() {
    private var highlightedAlbumId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(TopBarContainer.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
        )
        highlightedAlbumId = extractAlbumId(intent)
        setContent {
            NoMediaTheme {
                AppRoot(
                    highlightedAlbumId = highlightedAlbumId?.let(::AlbumId),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        highlightedAlbumId = extractAlbumId(intent)
    }

    companion object {
        private const val ACTION_OPEN_ALBUM_RESULT = "com.nomedia.switcher.OPEN_ALBUM_RESULT"
        private const val EXTRA_ALBUM_ID = "album_id"

        fun createOpenAlbumResultIntent(
            context: Context,
            albumId: String,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_ALBUM_RESULT
                putExtra(EXTRA_ALBUM_ID, albumId)
            }
        }

        private fun extractAlbumId(intent: Intent?): String? {
            if (intent?.action != ACTION_OPEN_ALBUM_RESULT) {
                return null
            }
            return intent.getStringExtra(EXTRA_ALBUM_ID)
        }
    }
}
