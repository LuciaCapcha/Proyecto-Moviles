package com.example.exchangededivisas.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EzchangeLogo(mostrarTexto: Boolean = true) {
    val cyan = Color(0xFF22D3EE)
    val azul = Color(0xFF3B82F6)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(azul, cyan))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B1020)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "Logo Ezchange",
                    tint = cyan,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        if (mostrarTexto) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "EZCHANGE",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cyan,
                letterSpacing = 3.sp
            )
        }
    }
}