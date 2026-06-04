package com.example.exchangededivisas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.exchangededivisas.presentation.navigation.AppNavGraph
import com.example.exchangededivisas.ui.theme.ExchangedeDivisasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExchangedeDivisasTheme {
                AppNavGraph()
            }
        }
    }
}