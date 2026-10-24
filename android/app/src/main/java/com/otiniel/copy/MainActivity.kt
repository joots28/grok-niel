package com.otiniel.copy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.otiniel.copy.ui.CopyApp
import com.otiniel.copy.ui.theme.CopyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CopyTheme {
                CopyApp()
            }
        }
    }
}
