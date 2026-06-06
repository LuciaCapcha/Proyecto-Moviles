package com.example.exchangededivisas.presentation.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangededivisas.data.model.MockCurrencyData

@Composable
fun WalletScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mi Billetera Virtual",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Botones de Depósito y Retiro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { /* Acción depósito */ }, modifier = Modifier.weight(1f)) {
                Text(text = "Depósito")
            }
            Button(onClick = { /* Acción retiro */ }, modifier = Modifier.weight(1f)) {
                Text(text = "Retiro")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Mis Activos Disponibles",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Lista de las 29 monedas ordenadas
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(MockCurrencyData.list) { currency ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = currency.code, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = currency.name, fontSize = 12.sp)
                    }
                    Text(
                        text = "$${currency.balance}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Divider()
            }
        }
    }
}