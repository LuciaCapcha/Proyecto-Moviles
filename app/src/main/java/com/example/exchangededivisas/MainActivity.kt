package com.example.exchangededivisas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.exchangededivisas.data.notification.NativeNotificationBus
import com.example.exchangededivisas.data.repository.UserNotificationRepository
import com.example.exchangededivisas.data.session.AppSession
import com.example.exchangededivisas.notificaciones.NativeNotificationHelper
import com.example.exchangededivisas.presentation.navigation.AppNavGraph
import com.example.exchangededivisas.ui.theme.ExchangedeDivisasTheme
import com.example.exchangededivisas.ui.theme.ThemeState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("NOTIF", "Permiso concedido")
        } else {
            android.util.Log.d("NOTIF", "Permiso denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        observarNotificacionesLocales()
        observarNotificacionesDeBaseDatos()
        setContent {
            ExchangedeDivisasTheme(
                darkTheme = ThemeState.isDarkMode,
                dynamicColor = false
            ) {
                AppNavGraph()
            }
        }
    }

    private fun observarNotificacionesLocales() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NativeNotificationBus.events.collect { event ->
                    val currentUserId = AppSession.currentUser.value.usuarioId
                    val shouldShow = currentUserId > 0 &&
                        (event.recipientUserId == null || event.recipientUserId == currentUserId)

                    if (shouldShow) {
                        NativeNotificationHelper.show(
                            context = this@MainActivity,
                            title = event.title,
                            body = event.body
                        )
                    }
                }
            }
        }
    }

    private fun observarNotificacionesDeBaseDatos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppSession.currentUser
                    .distinctUntilChangedBy { it.usuarioId }
                    .collectLatest { user ->
                        if (user.usuarioId <= 0 || user.nombreUsuario.isBlank()) {
                            return@collectLatest
                        }

                        UserNotificationRepository.observeNewNotificationsForUser(
                            usuarioId = user.usuarioId,
                            ignoreExisting = true
                        ).collect { notification ->
                            NativeNotificationHelper.show(
                                context = this@MainActivity,
                                title = notification.asunto?.takeIf { it.isNotBlank() }
                                    ?: "Notificación Ezchange",
                                body = notification.cuerpo?.takeIf { it.isNotBlank() }
                                    ?: "Tienes una nueva actualización."
                            )
                        }
                    }
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
