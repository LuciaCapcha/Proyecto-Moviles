package com.example.exchangededivisas.presentation.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.model.MockCurrencyData

@Composable
fun WalletScreen(navController: NavController) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate("deposit") },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Depósito")
            }

            Button(
                onClick = { navController.navigate("withdraw") },
                modifier = Modifier.weight(1f)
            ) {
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
                        text = "%.2f".format(currency.balance),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                HorizontalDivider()
            }
        }
    }
}