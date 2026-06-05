package com.example.exchangededivisas.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


private val fondoTop = Color(0xFF1A1340)
private val fondoBottom = Color(0xFF0B1020)
private val campoFondo = Color(0xFF141A2E)
private val borde = Color(0xFF2A3350)
private val textoTenue = Color(0xFF9AA3BD)
private val neon = Color(0xFF3B82F6)
private val neonCyan = Color(0xFF22D3EE)

@Composable
fun LoginScreen(navController: NavController) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var mantenerSesion by remember { mutableStateOf(true) }

    val usuarioValido = "lucia"
    val correoValido = "lucia@esan.edu.pe"
    val passwordValido = "12345678"

    val isFormValid = identifier.isNotBlank() && password.isNotBlank()

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = campoFondo,
        unfocusedContainerColor = campoFondo,
        focusedBorderColor = neon,
        unfocusedBorderColor = borde,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = neonCyan,
        unfocusedLabelColor = textoTenue,
        cursorColor = neonCyan,
        focusedLeadingIconColor = neonCyan,
        unfocusedLeadingIconColor = textoTenue,
        focusedTrailingIconColor = neonCyan,
        unfocusedTrailingIconColor = textoTenue
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(fondoTop, fondoBottom)))
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        EzchangeLogo()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Bienvenido de nuevo",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "El mercado global de divisas, en tus manos.",
            color = textoTenue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it; error = null },
            label = { Text("Correo o usuario") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = coloresCampo,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = coloresCampo,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = mantenerSesion, onCheckedChange = { mantenerSesion = it })
            Text("Mantener sesión iniciada", color = textoTenue, fontSize = 13.sp)
        }

        if (error != null) {
            Text(error!!, color = Color(0xFFFF6B6B), modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val ok = (identifier == usuarioValido || identifier == correoValido) &&
                        password == passwordValido
                if (ok) {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    error = "Credenciales inválidas"
                }
            },
            enabled = isFormValid,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = neon,
                disabledContainerColor = neon.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Iniciar Sesión", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("¿Eres nuevo en Ezchange? Crea una cuenta", color = neonCyan, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}