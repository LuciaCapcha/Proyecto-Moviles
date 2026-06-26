package com.example.exchangededivisas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.exchangededivisas.presentation.navigation.AppNavGraph
import com.example.exchangededivisas.ui.theme.ExchangedeDivisasTheme
import com.example.exchangededivisas.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by remember { mutableStateOf(ThemeState.isDarkMode) }
            ExchangedeDivisasTheme(
                darkTheme = isDark,
                dynamicColor = false
            ) {
                AppNavGraph()
            }
        }
    }
}