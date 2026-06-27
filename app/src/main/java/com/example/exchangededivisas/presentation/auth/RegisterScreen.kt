package com.example.exchangededivisas.presentation.auth

import android.util.Patterns
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.exchangededivisas.data.repository.AuthRepository
import kotlinx.coroutines.launch

// Paleta del tema oscuro premium
private val rFondoTop = Color(0xFF1A1340)
private val rFondoBottom = Color(0xFF0B1020)
private val rCampoFondo = Color(0xFF141A2E)
private val rBorde = Color(0xFF2A3350)
private val rTextoTenue = Color(0xFF9AA3BD)
private val rNeon = Color(0xFF3B82F6)
private val rNeonCyan = Color(0xFF22D3EE)

// Calcula la fuerza de la contraseña: 0=vacía, 1=débil, 2=media, 3=segura
private fun calcularFuerza(pass: String): Int {
    if (pass.isEmpty()) return 0
    var puntos = 0
    if (pass.length >= 8) puntos++
    if (pass.any { it.isDigit() } && pass.any { it.isLetter() }) puntos++
    if (pass.any { !it.isLetterOrDigit() }) puntos++   // tiene carácter especial
    return puntos
}

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var aceptaTerminos by remember { mutableStateOf(false) }

    // NUEVO (HU-001): para llamar a la BD y mostrar errores
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // --- Validaciones de la US-001 (mínimo 8 sigue siendo lo obligatorio) ---
    val nameError = if (name.isNotBlank() && name.length < 2) "Mínimo 2 caracteres" else null
    val emailError = if (email.isNotBlank() &&
        !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    ) "Correo inválido" else null
    val passwordError = if (password.isNotBlank() && password.length < 8) "Mínimo 8 caracteres" else null
    val confirmError = if (confirmPassword.isNotBlank() && confirmPassword != password) "No coinciden" else null

    val isFormValid = name.length >= 2 &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            password.length >= 8 &&
            confirmPassword == password &&
            aceptaTerminos

    // Fuerza de la contraseña (solo visual, no bloquea)
    val fuerza = calcularFuerza(password)
    val fuerzaTexto = when (fuerza) {
        0 -> ""
        1 -> "Débil"
        2 -> "Media"
        else -> "Segura"
    }
    val fuerzaColor by animateColorAsState(
        targetValue = when (fuerza) {
            1 -> Color(0xFFFF6B6B)
            2 -> Color(0xFFFFC107)
            3 -> Color(0xFF4ADE80)
            else -> rBorde
        },
        label = "colorFuerza"
    )
    val fuerzaProgreso by animateFloatAsState(targetValue = fuerza / 3f, label = "progresoFuerza")

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = rCampoFondo,
        unfocusedContainerColor = rCampoFondo,
        focusedBorderColor = rNeon,
        unfocusedBorderColor = rBorde,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = rNeonCyan,
        unfocusedLabelColor = rTextoTenue,
        cursorColor = rNeonCyan,
        focusedLeadingIconColor = rNeonCyan,
        unfocusedLeadingIconColor = rTextoTenue,
        focusedTrailingIconColor = rNeonCyan,
        unfocusedTrailingIconColor = rTextoTenue
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(rFondoTop, rFondoBottom)))
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // --- Logo (solo el círculo, sin texto, para no repetir con el título) ---
        EzchangeLogo(mostrarTexto = false)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Crea tu cuenta de Ezchange",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Únete a la red de intercambio P2P más segura.",
            color = rTextoTenue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de usuario") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = coloresCampo,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = coloresCampo,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
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
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = coloresCampo,
            modifier = Modifier.fillMaxWidth()
        )

        // --- Barra de fuerza de contraseña (solo se muestra si hay algo escrito) ---
        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fuerza de la contraseña", color = rTextoTenue, fontSize = 12.sp)
                Text(fuerzaTexto, color = fuerzaColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fuerzaProgreso },
                color = fuerzaColor,
                trackColor = rBorde,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Text(
                "Tip: combina mayúsculas, números y símbolos (ej. !, @, #).",
                color = rTextoTenue,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        imageVector = if (confirmVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (confirmVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            isError = confirmError != null,
            supportingText = { if (confirmError != null) Text(confirmError) },
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
            Checkbox(checked = aceptaTerminos, onCheckedChange = { aceptaTerminos = it })
            Text("Acepto los Términos de Servicio", color = rTextoTenue, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = null
                isLoading = true
                scope.launch {
                    AuthRepository.register(name, email, password)
                        .onSuccess {
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                        .onFailure { errorMessage = it.message }
                    isLoading = false
                }
            },
            enabled = isFormValid && !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E),
                disabledContainerColor = Color(0xFF22C55E).copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                if (isLoading) "Registrando..." else "Registrarse",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // NUEVO (HU-001): muestra el error si algo falla (ej. correo repetido)
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text("¿Ya tienes cuenta? Inicia sesión aquí", color = rNeonCyan, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}