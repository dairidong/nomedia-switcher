package com.nomedia.switcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nomedia.switcher.ui.AppRoot
import com.nomedia.switcher.ui.theme.NoMediaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoMediaTheme {
                AppRoot()
            }
        }
    }
}
